package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.NewsDao
import com.example.data.model.NewsArticle
import com.example.data.model.SearchHistoryItem
import com.example.data.model.UserNotification
import com.example.data.remote.ReadyNewsApiService
import com.example.data.remote.BackendDiscoveryApiService
import com.example.data.remote.toEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

data class NewsSyncResult(
    val newArticles: List<NewsArticle>,
    val totalCount: Int,
    val fromCache: Boolean,
    val wasInitialSync: Boolean
)

class NewsRepository(
    context: Context,
    private val newsDao: NewsDao = AppDatabase.getInstance(context).newsDao(),
    private val readyNewsApi: ReadyNewsApiService = createReadyNewsApi(BuildConfig.GUNDEMAI_API_BASE_URL),
    private val discoveryApi: BackendDiscoveryApiService = createDiscoveryApi()
) {
    private val syncMutex = Mutex()
    @Volatile private var lastFetchAt = 0L

    fun getAllArticles(): Flow<List<NewsArticle>> = newsDao.getAllArticles()

    fun getArticlesByCategory(category: String): Flow<List<NewsArticle>> =
        if (category == "Sana Özel") newsDao.getAllArticles() else newsDao.getArticlesByCategory(category)

    fun getBookmarkedArticles(): Flow<List<NewsArticle>> = newsDao.getBookmarkedArticles()

    fun getArticleById(id: String): Flow<NewsArticle?> = newsDao.getArticleById(id)

    fun searchArticles(query: String): Flow<List<NewsArticle>> = newsDao.searchArticles(query.trim())

    suspend fun toggleBookmark(articleId: String, currentStatus: Boolean) {
        newsDao.updateBookmarkStatus(articleId, !currentStatus)
    }

    fun getNotifications(): Flow<List<UserNotification>> = newsDao.getNotifications()

    suspend fun insertNotification(notification: UserNotification) {
        newsDao.insertNotification(notification)
    }

    suspend fun notificationExists(id: String): Boolean = newsDao.notificationExists(id)

    suspend fun markNotificationRead(id: String) {
        newsDao.markNotificationRead(id)
    }

    suspend fun markAllNotificationsRead() {
        newsDao.markAllNotificationsRead()
    }

    suspend fun clearAllNotifications() {
        newsDao.clearAllNotifications()
    }

    fun getRecentSearches(): Flow<List<SearchHistoryItem>> = newsDao.getRecentSearches()

    suspend fun addSearchQuery(query: String) {
        if (query.isNotBlank()) newsDao.insertSearchQuery(SearchHistoryItem(query = query.trim()))
    }

    suspend fun clearLocalCache() = withContext(Dispatchers.IO) {
        newsDao.deleteUnbookmarkedArticles()
        newsDao.clearSearchHistory()
        lastFetchAt = 0L
    }

    suspend fun clearAllUserData() = withContext(Dispatchers.IO) {
        newsDao.clearAllArticles()
        newsDao.clearAllNotifications()
        newsDao.clearSearchHistory()
        lastFetchAt = 0L
    }

    suspend fun fetchAndRefreshNews(
        forceRefresh: Boolean = false
    ): Result<NewsSyncResult> = withContext(Dispatchers.IO) {
        runCatching {
            syncMutex.withLock {
                val now = System.currentTimeMillis()
                val cachedCount = newsDao.getAllArticlesCount()
                if (!forceRefresh && cachedCount > 0 && now - lastFetchAt < FETCH_CACHE_TIMEOUT_MS) {
                    return@withLock NewsSyncResult(
                        newArticles = emptyList(),
                        totalCount = cachedCount,
                        fromCache = true,
                        wasInitialSync = false
                    )
                }

                val activeApi = resolveReadyNewsApi()
                val response = runCatching {
                    activeApi.getReadyNews(limit = MAX_READY_ARTICLES)
                }.getOrElse { error ->
                    if (activeApi === readyNewsApi) throw error
                    discoveredReadyNewsApi = null
                    readyNewsApi.getReadyNews(limit = MAX_READY_ARTICLES)
                }
                check(response.sharedAnalysis) { "Sunucu ortak analiz akışı döndürmedi" }

                val existingIds = newsDao.getAllArticleIdsSync().toSet()
                val bookmarkedIds = newsDao.getBookmarkedArticleIdsSync().toSet()
                val validDtos = response.articles
                    .asSequence()
                    .filter { it.status == "READY" }
                    .filter { it.id.isNotBlank() && it.title.isNotBlank() && it.summary.isNotBlank() }
                    .filter { it.whatHappened.isNotBlank() && it.whyImportant.isNotBlank() }
                    .toList()
                val mapped = validDtos.map { dto ->
                    dto.toEntity(isBookmarked = dto.id in bookmarkedIds)
                }

                val recentNewIds = validDtos
                    .asSequence()
                    .filter { it.id !in existingIds }
                    .filter { it.readyAt >= now - NEW_ARTICLE_WINDOW_MS }
                    .map { it.id }
                    .toSet()
                val newArticles = mapped.filter { it.id in recentNewIds }
                if (mapped.isNotEmpty()) {
                    newsDao.replaceReadyFeed(mapped, LOCAL_ARTICLE_LIMIT)
                }
                lastFetchAt = now

                NewsSyncResult(
                    newArticles = newArticles,
                    totalCount = mapped.size,
                    fromCache = false,
                    wasInitialSync = cachedCount == 0
                )
            }
        }
    }

    private suspend fun resolveReadyNewsApi(): ReadyNewsApiService {
        discoveredReadyNewsApi?.let { return it }
        val discoveredUrl = runCatching { discoveryApi.getBackend().apiBaseUrl.trim() }.getOrNull()
        if (discoveredUrl.isNullOrBlank() ||
            !discoveredUrl.startsWith("https://") ||
            !discoveredUrl.endsWith("/")
        ) {
            return readyNewsApi
        }
        return createReadyNewsApi(discoveredUrl).also { discoveredReadyNewsApi = it }
    }

    companion object {
        private const val FETCH_CACHE_TIMEOUT_MS = 5 * 60 * 1000L
        private const val NEW_ARTICLE_WINDOW_MS = 2 * 60 * 60 * 1000L
        private const val MAX_READY_ARTICLES = 250
        private const val LOCAL_ARTICLE_LIMIT = 300

        @Volatile private var discoveredReadyNewsApi: ReadyNewsApiService? = null

        private fun createHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

        private fun createReadyNewsApi(baseUrl: String): ReadyNewsApiService {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(createHttpClient())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ReadyNewsApiService::class.java)
        }

        private fun createDiscoveryApi(): BackendDiscoveryApiService {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            return Retrofit.Builder()
                .baseUrl(BuildConfig.GUNDEMAI_API_BASE_URL)
                .client(createHttpClient())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(BackendDiscoveryApiService::class.java)
        }
    }
}
