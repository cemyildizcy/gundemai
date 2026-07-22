package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.example.data.ads.AdConsentManager
import com.example.ui.components.GundemBottomBar
import com.example.ui.components.GundemTopBar
import com.example.ui.screens.*
import com.example.ui.theme.GundemAITheme
import com.example.ui.viewmodel.NewsViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: NewsViewModel by viewModels()
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdConsentManager.gatherConsent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val darkThemeEnabled by viewModel.darkThemeEnabled.collectAsStateWithLifecycle()
            val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()
            val authCompleted by viewModel.authCompleted.collectAsStateWithLifecycle()

            val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
            val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
            val syncError by viewModel.syncError.collectAsStateWithLifecycle()

            val articles by viewModel.articlesFeed.collectAsStateWithLifecycle()
            val bookmarkedArticles by viewModel.bookmarkedArticles.collectAsStateWithLifecycle()
            val notifications by viewModel.notifications.collectAsStateWithLifecycle()
            val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
            val selectedArticle by viewModel.selectedArticleDetail.collectAsStateWithLifecycle()

            val followedCategories by viewModel.followedCategories.collectAsStateWithLifecycle()
            val followedTopics by viewModel.followedTopics.collectAsStateWithLifecycle()
            val notificationCategories by viewModel.notificationCategories.collectAsStateWithLifecycle()

            val userEmail by viewModel.userEmail.collectAsStateWithLifecycle()
            val userName by viewModel.userName.collectAsStateWithLifecycle()
            val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()
            val proPlanPeriod by viewModel.proPlanPeriod.collectAsStateWithLifecycle()

            val billingConnectionState by viewModel.billingConnectionState.collectAsStateWithLifecycle()
            val billingPurchaseState by viewModel.billingPurchaseState.collectAsStateWithLifecycle()
            val billingAvailableProducts by viewModel.billingAvailableProducts.collectAsStateWithLifecycle()
            val privacyOptionsRequired by AdConsentManager.privacyOptionsRequired.collectAsStateWithLifecycle()

            GundemAITheme(darkTheme = darkThemeEnabled) {
                if (!authCompleted) {
                    AuthScreen(
                        onAuthSuccess = { email, name ->
                            viewModel.completeAuth(email, name)
                        },
                        onGuestContinue = {
                            viewModel.continueAsGuest()
                        },
                        onEmailSignUp = { email, pwd, name ->
                            viewModel.signUpWithEmail(email, pwd, name)
                        },
                        onEmailSignIn = { email, pwd ->
                            viewModel.signInWithEmail(email, pwd)
                        },
                        onGoogleSignIn = { idToken ->
                            viewModel.signInWithGoogle(idToken)
                        }
                    )
                } else if (!onboardingCompleted) {
                    OnboardingScreen(
                        onComplete = { cats, topics ->
                            viewModel.completeOnboarding(cats, topics)
                        }
                    )
                } else if (selectedArticle != null) {
                    DetailScreen(
                        article = selectedArticle,
                        onBackClick = { viewModel.selectArticle(null) },
                        onBookmarkToggle = { id, status -> viewModel.toggleBookmark(id, status) },
                        isProUser = isProUser
                    )
                } else {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            GundemTopBar(
                                selectedCategory = selectedCategory,
                                onCategorySelected = { viewModel.selectCategory(it) },
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.setSearchQuery(it) },
                                onRefreshClick = { viewModel.refreshNews(forceRefresh = true) }
                            )
                        },
                        bottomBar = {
                            GundemBottomBar(
                                selectedTab = selectedTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }
                    ) { innerPadding ->
                        val modifier = Modifier.padding(innerPadding)

                        when (selectedTab) {
                            0 -> HomeScreen(
                                articles = articles,
                                isRefreshing = isRefreshing,
                                syncError = syncError,
                                isProUser = isProUser,
                                onRefresh = { viewModel.refreshNews(forceRefresh = true) },
                                onArticleClick = { id -> viewModel.selectArticle(id) },
                                onBookmarkToggle = { id, status -> viewModel.toggleBookmark(id, status) },
                                modifier = modifier
                            )
                            1 -> ExploreScreen(
                                followedTopics = followedTopics,
                                recentSearches = recentSearches,
                                onTopicClick = { topicId -> viewModel.toggleTopicFollow(topicId) },
                                onSearchTagClick = { query ->
                                    viewModel.setSearchQuery(query)
                                    viewModel.selectTab(0)
                                },
                                modifier = modifier
                            )
                            2 -> BookmarksScreen(
                                bookmarkedArticles = bookmarkedArticles,
                                onArticleClick = { id -> viewModel.selectArticle(id) },
                                onBookmarkToggle = { id, status -> viewModel.toggleBookmark(id, status) },
                                modifier = modifier
                            )
                            3 -> NotificationsScreen(
                                notifications = notifications,
                                followedCategories = notificationCategories,
                                onCategoryToggle = { cat -> viewModel.toggleCategoryNotification(cat) },
                                onNotificationClick = { articleId ->
                                    if (articleId != null) {
                                        viewModel.openArticleFromNotification(articleId)
                                    }
                                },
                                onMarkRead = { id -> viewModel.markNotificationRead(id) },
                                onMarkAllRead = { viewModel.markAllNotificationsRead() },
                                onClearAll = { viewModel.clearAllNotifications() },
                                modifier = modifier
                            )
                            4 -> ProfileScreen(
                                darkThemeEnabled = darkThemeEnabled,
                                onDarkThemeToggle = { enabled -> viewModel.toggleDarkTheme(enabled) },
                                followedCategories = followedCategories,
                                followedTopics = followedTopics,
                                userEmail = userEmail,
                                userName = userName,
                                isProUser = isProUser,
                                proPlanPeriod = proPlanPeriod,
                                billingConnectionState = billingConnectionState,
                                billingPurchaseState = billingPurchaseState,
                                billingAvailableProducts = billingAvailableProducts,
                                onLogout = { viewModel.logoutUser() },
                                onDeleteAccount = { onRes -> viewModel.deleteAccount(onRes) },
                                onNavigateToAuth = { viewModel.navigateToAuthScreen() },
                                onVerifySubscription = { onRes -> viewModel.verifyProSubscription(onRes) },
                                onPurchaseSubscription = { act, plan, onRes -> viewModel.purchaseProSubscription(act, plan, onRes) },
                                onRestorePurchases = { onRes -> viewModel.restoreProSubscription(onRes) },
                                onResetPurchaseState = { viewModel.resetBillingPurchaseState() },
                                onClearCache = { done -> viewModel.clearLocalCache(done) },
                                privacyOptionsRequired = privacyOptionsRequired,
                                onPrivacyOptionsClick = {
                                    AdConsentManager.showPrivacyOptions(this@MainActivity)
                                },
                                onResetOnboarding = { viewModel.resetOnboarding() },
                                modifier = modifier
                            )
                        }
                    }
                }
            }
        }
        handleNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val articleId = intent?.getStringExtra(EXTRA_ARTICLE_ID) ?: return
        intent.removeExtra(EXTRA_ARTICLE_ID)
        viewModel.openArticleFromNotification(articleId)
    }

    companion object {
        const val EXTRA_ARTICLE_ID = "article_id"
    }
}
