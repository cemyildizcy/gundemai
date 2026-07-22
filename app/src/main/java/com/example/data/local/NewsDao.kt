package com.example.data.local

import androidx.room.*
import com.example.data.model.NewsArticle
import com.example.data.model.SearchHistoryItem
import com.example.data.model.UserNotification
import kotlinx.coroutines.flow.Flow

@Dao
interface NewsDao {
    @Query("SELECT * FROM news_articles ORDER BY publishedAt DESC, id ASC")
    fun getAllArticles(): Flow<List<NewsArticle>>

    @Query("SELECT * FROM news_articles WHERE category = :category ORDER BY publishedAt DESC, id ASC")
    fun getArticlesByCategory(category: String): Flow<List<NewsArticle>>

    @Query("SELECT COUNT(*) FROM news_articles")
    suspend fun getAllArticlesCount(): Int

    @Query("SELECT id FROM news_articles")
    suspend fun getAllArticleIdsSync(): List<String>

    @Query("SELECT * FROM news_articles WHERE isBookmarked = 1 ORDER BY publishedAt DESC, id ASC")
    fun getBookmarkedArticles(): Flow<List<NewsArticle>>

    @Query("SELECT * FROM news_articles WHERE id = :id")
    fun getArticleById(id: String): Flow<NewsArticle?>

    @Query("SELECT * FROM news_articles WHERE id = :id")
    suspend fun getArticleByIdSync(id: String): NewsArticle?

    @Query("SELECT * FROM news_articles WHERE title LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' OR whatHappened LIKE '%' || :query || '%' ORDER BY publishedAt DESC, id ASC")
    fun searchArticles(query: String): Flow<List<NewsArticle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<NewsArticle>)

    @Query("DELETE FROM news_articles WHERE isBookmarked = 0")
    suspend fun deleteUnbookmarkedArticles()

    @Query("DELETE FROM news_articles")
    suspend fun clearAllArticles()

    @Query("UPDATE news_articles SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarkStatus(id: String, isBookmarked: Boolean)

    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getNotifications(): Flow<List<UserNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: UserNotification)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationRead(id: String)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsRead()

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<SearchHistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(item: SearchHistoryItem)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
