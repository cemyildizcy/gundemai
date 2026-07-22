package com.example.data.repository

import android.content.Context
import com.example.BuildConfig
import com.example.data.local.AppDatabase
import com.example.data.local.NewsDao
import com.example.data.model.NewsArticle
import com.example.data.model.SearchHistoryItem
import com.example.data.model.UserNotification
import com.example.data.remote.ReadyNewsApiService
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
    val fromCache: Boolean
)

class NewsRepository(
    context: Context,
    private val newsDao: NewsDao = AppDatabase.getInstance(context).newsDao(),
    private val readyNewsApi: ReadyNewsApiService = createReadyNewsApi()
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
        category: String = "Sana Özel",
        forceRefresh: Boolean = false
    ): Result<NewsSyncResult> = withContext(Dispatchers.IO) {
        runCatching {
            syncMutex.withLock {
                val now = System.currentTimeMillis()
                val cachedCount = newsDao.getAllArticlesCount()
                if (!forceRefresh && cachedCount > 0 && now - lastFetchAt < FETCH_CACHE_TIMEOUT_MS) {
                    return@withLock NewsSyncResult(emptyList(), cachedCount, fromCache = true)
                }

                val response = readyNewsApi.getReadyNews(limit = MAX_READY_ARTICLES)
                check(response.sharedAnalysis) { "Sunucu ortak analiz akışı döndürmedi" }

                val existingIds = newsDao.getAllArticleIdsSync().toSet()
                val validDtos = response.articles
                    .asSequence()
                    .filter { it.status == "READY" }
                    .filter { it.id.isNotBlank() && it.title.isNotBlank() && it.summary.isNotBlank() }
                    .filter { it.whatHappened.isNotBlank() && it.whyImportant.isNotBlank() }
                    .toList()
                val mapped = buildList {
                    for (dto in validDtos) {
                        val bookmarked = newsDao.getArticleByIdSync(dto.id)?.isBookmarked == true
                        add(dto.toEntity(isBookmarked = bookmarked))
                    }
                }

                val newArticles = mapped.filterNot { it.id in existingIds }
                newsDao.deleteUnbookmarkedArticles()
                if (mapped.isNotEmpty()) newsDao.insertArticles(mapped)
                lastFetchAt = now

                NewsSyncResult(newArticles, mapped.size, fromCache = false)
            }
        }
    }

    companion object {
        private const val FETCH_CACHE_TIMEOUT_MS = 5 * 60 * 1000L
        private const val MAX_READY_ARTICLES = 100

        private fun createReadyNewsApi(): ReadyNewsApiService {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BuildConfig.GUNDEMAI_API_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(ReadyNewsApiService::class.java)
        }
    }
}
