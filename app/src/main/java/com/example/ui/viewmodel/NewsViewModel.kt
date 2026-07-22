package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.NewsRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.notification.NotificationTopicManager
import com.example.worker.NewsBackgroundWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

class NewsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NewsRepository(application)
    private val userPrefs = UserPreferencesRepository(application)

    init {
        try {
            NewsBackgroundWorker.schedulePeriodicSync(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Current Navigation Tab Index (0: Gündem, 1: Keşfet, 2: Kaydedilenler, 3: Bildirimler, 4: Profil)
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Selected Category Filter Name
    private val _selectedCategory = MutableStateFlow("Sana Özel")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Refreshing / Loading state
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    // Currently Selected Detail Article ID
    private val _selectedArticleId = MutableStateFlow<String?>(null)
    val selectedArticleId: StateFlow<String?> = _selectedArticleId.asStateFlow()

    // Onboarding State
    val onboardingCompleted = userPrefs.onboardingCompleted.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )

    val followedCategories = userPrefs.followedCategories.stateIn(
        viewModelScope, SharingStarted.Eagerly, setOf("Sana Özel", "Son Dakika", "Yapay Zekâ", "Teknoloji")
    )

    val followedTopics = userPrefs.followedTopics.stateIn(
        viewModelScope, SharingStarted.Eagerly, setOf("openai", "google", "gemini", "fenerbahce")
    )

    val notificationCategories = userPrefs.notificationCategories.stateIn(
        viewModelScope, SharingStarted.Eagerly, emptySet()
    )

    val authCompleted = userPrefs.authCompleted.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    val darkThemeEnabled = userPrefs.darkThemeEnabled.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )

    val userEmail = userPrefs.userEmail.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    val userName = userPrefs.userName.stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    val isProUser = userPrefs.isProUser.stateIn(
        viewModelScope, SharingStarted.Eagerly, false
    )

    val proPlanPeriod = userPrefs.proPlanPeriod.stateIn(
        viewModelScope, SharingStarted.Eagerly, "YEARLY"
    )

    private val authRepository = com.example.data.auth.AuthRepository(getApplication(), userPrefs)
    val playBillingManager = com.example.data.billing.PlayBillingManager(getApplication()) { sku ->
        viewModelScope.launch {
            if (sku == null) {
                userPrefs.setProUserStatus(false, proPlanPeriod.value)
            } else {
                val period = if (sku.contains("monthly")) "MONTHLY" else "YEARLY"
                userPrefs.setProUserStatus(true, period)
            }
        }
    }

    val billingConnectionState = playBillingManager.connectionState
    val billingPurchaseState = playBillingManager.purchaseState
    val billingAvailableProducts = playBillingManager.availableProducts

    suspend fun signUpWithEmail(email: String, password: String, name: String): com.example.data.auth.AuthResult {
        val result = authRepository.signUpWithEmail(email, password, name)
        if (result is com.example.data.auth.AuthResult.Success) {
            userPrefs.setAuthCompleted(true)
        }
        return result
    }

    suspend fun signInWithEmail(email: String, password: String): com.example.data.auth.AuthResult {
        val result = authRepository.signInWithEmail(email, password)
        if (result is com.example.data.auth.AuthResult.Success) {
            userPrefs.setAuthCompleted(true)
        }
        return result
    }

    suspend fun signInWithGoogle(idToken: String): com.example.data.auth.AuthResult {
        val result = authRepository.signInWithGoogleIdToken(idToken)
        if (result is com.example.data.auth.AuthResult.Success) {
            userPrefs.setAuthCompleted(true)
        }
        return result
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            authRepository.continueAsGuest()
            userPrefs.setAuthCompleted(true)
        }
    }

    fun completeAuth(email: String, name: String) {
        viewModelScope.launch {
            userPrefs.setUserAccount(email, name)
            userPrefs.setAuthCompleted(true)
        }
    }

    fun verifyProSubscription(onResult: (Boolean, String) -> Unit) {
        if (userEmail.value.isNullOrBlank()) {
            onResult(false, "Misafir modunda abonelik doğrulanamaz. Lütfen giriş yapın.")
            return
        }
        restoreProSubscription(onResult)
    }

    fun logoutUser() {
        viewModelScope.launch {
            authRepository.logout()
            userPrefs.setAuthCompleted(false)
        }
    }

    fun deleteAccount(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            when (val result = authRepository.deleteCurrentAccount()) {
                is com.example.data.auth.AuthResult.Success -> {
                    runCatching { repository.clearAllUserData() }
                    onResult(true, "Hesabınız ve bu cihazdaki kişisel verileriniz silindi.")
                }
                is com.example.data.auth.AuthResult.Error -> onResult(false, result.message)
            }
        }
    }

    fun navigateToAuthScreen() {
        viewModelScope.launch {
            userPrefs.setAuthCompleted(false)
        }
    }

    fun purchaseProSubscription(activity: android.app.Activity, planPeriod: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val currentEmail = userEmail.value
            if (currentEmail.isNullOrBlank()) {
                onResult(false, "Misafir okuyucular Pro üyelik alamaz. Lütfen önce hesabınızla giriş yapın.")
                return@launch
            }

            val sku = if (planPeriod == "MONTHLY") {
                com.example.data.billing.PlayBillingManager.SKU_PRO_MONTHLY
            } else {
                com.example.data.billing.PlayBillingManager.SKU_PRO_YEARLY
            }

            val launched = playBillingManager.launchSubscriptionPurchase(activity, sku)
            if (launched) {
                onResult(true, "Google Play satın alma ekranı başlatıldı.")
            } else {
                onResult(false, "Google Play satın alma ekranı açılamadı. Ürün ve test hesabı yapılandırmasını kontrol edin.")
            }
        }
    }

    fun restoreProSubscription(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val currentEmail = userEmail.value
            if (currentEmail.isNullOrBlank()) {
                onResult(false, "Misafir modundasınız. Aboneliklerinizi geri yüklemek için önce hesabınıza giriş yapmalısınız.")
                return@launch
            }
            playBillingManager.queryExistingPurchases { success, msg ->
                onResult(success, msg)
            }
        }
    }

    fun resetBillingPurchaseState() {
        playBillingManager.resetPurchaseState()
    }

    // Articles Feed Flow
    val articlesFeed: StateFlow<List<NewsArticle>> = combine(_selectedCategory, _searchQuery) { cat, query ->
        Pair(cat, query)
    }.flatMapLatest { (category, query) ->
        if (query.isNotBlank()) {
            repository.searchArticles(query)
        } else {
            repository.getArticlesByCategory(category)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Bookmarks Flow
    val bookmarkedArticles: StateFlow<List<NewsArticle>> = repository.getBookmarkedArticles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications Flow
    val notifications: StateFlow<List<UserNotification>> = repository.getNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search History Flow
    val recentSearches: StateFlow<List<SearchHistoryItem>> = repository.getRecentSearches()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently Selected Article Detail Flow
    val selectedArticleDetail: StateFlow<NewsArticle?> = _selectedArticleId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getArticleById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            notificationCategories.collectLatest { categories ->
                runCatching { NotificationTopicManager.syncSubscriptions(categories) }
            }
        }
        refreshNews()
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        refreshNews()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank()) {
            viewModelScope.launch {
                repository.addSearchQuery(query)
            }
        }
    }

    fun selectArticle(id: String?) {
        _selectedArticleId.value = id
    }

    fun toggleBookmark(articleId: String, currentStatus: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmark(articleId, currentStatus)
        }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }

    fun clearLocalCache(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.clearLocalCache()
            onComplete()
        }
    }

    fun toggleCategoryNotification(categoryName: String) {
        viewModelScope.launch {
            val current = notificationCategories.value.toMutableSet()
            if (current.contains(categoryName)) {
                current.remove(categoryName)
            } else {
                current.add(categoryName)
            }
            userPrefs.updateNotificationCategories(current)
        }
    }

    fun openArticleFromNotification(articleId: String) {
        if (articleId.isBlank()) return
        viewModelScope.launch {
            repository.fetchAndRefreshNews(forceRefresh = true)
            _selectedArticleId.value = articleId
        }
    }

    fun refreshNews(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repository.fetchAndRefreshNews(_selectedCategory.value, forceRefresh)
                    .onSuccess { _syncError.value = null }
                    .onFailure { _syncError.value = "Haber sunucusuna ulaşılamadı. İnternet bağlantınızı kontrol edip tekrar deneyin." }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun completeOnboarding(selectedCats: Set<String>, selectedTopics: Set<String>) {
        viewModelScope.launch {
            userPrefs.updateFollowedCategories(selectedCats)
            userPrefs.updateFollowedTopics(selectedTopics)
            userPrefs.updateNotificationCategories(selectedCats - "Sana Özel")
            userPrefs.setOnboardingCompleted(true)
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            userPrefs.setOnboardingCompleted(false)
        }
    }

    fun toggleDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            userPrefs.setDarkThemeEnabled(enabled)
        }
    }

    fun toggleTopicFollow(topicId: String) {
        viewModelScope.launch {
            val current = followedTopics.value.toMutableSet()
            if (current.contains(topicId)) {
                current.remove(topicId)
            } else {
                current.add(topicId)
            }
            userPrefs.updateFollowedTopics(current)
        }
    }
}
