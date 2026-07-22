package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.data.model.NewsArticle
import com.example.data.model.UserNotification
import com.example.data.repository.NewsRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

class NewsBackgroundWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result = try {
        val repository = NewsRepository(appContext)
        val userPrefs = UserPreferencesRepository(appContext)
        val followed = userPrefs.followedCategories.firstOrNull().orEmpty()
        val targetCategory = inputData.getString(KEY_TARGET_CATEGORY)
        val sync = repository.fetchAndRefreshNews(forceRefresh = true).getOrThrow()

        sync.newArticles
            .asSequence()
            .filter { article ->
                when {
                    !targetCategory.isNullOrBlank() -> article.category == targetCategory
                    followed.isEmpty() -> false
                    "Sana Özel" in followed -> true
                    else -> article.category in followed
                }
            }
            .take(MAX_NOTIFICATIONS_PER_SYNC)
            .forEach { article -> sendArticleNotification(article, repository) }

        ListenableWorker.Result.success()
    } catch (_: Exception) {
        ListenableWorker.Result.retry()
    }

    private suspend fun sendArticleNotification(article: NewsArticle, repository: NewsRepository) {
        val notification = UserNotification(
            id = "news_${article.id}",
            title = "Yeni ${article.category} haberi",
            body = article.title,
            timestamp = System.currentTimeMillis(),
            timestampFormatted = DateUtils.formatRelativeTime(System.currentTimeMillis()),
            type = if (article.isBreaking) "BREAKING_NEWS" else "TOPIC_ALERT",
            isRead = false
        )
        repository.insertNotification(notification)
        showSystemNotification(notification.title, notification.body, article.id.hashCode())
    }

    private fun showSystemNotification(title: String, body: String, notificationId: Int) {
        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "GündemAI Haberleri", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Takip edilen kategorilerde yayımlanan yeni analizli haberler"
                }
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "gundem_ai_category_channel"
        const val KEY_TARGET_CATEGORY = "target_category"
        const val WORK_NAME_PERIODIC = "gundem_ai_periodic_news_sync"
        private const val MAX_NOTIFICATIONS_PER_SYNC = 3

        fun schedulePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<NewsBackgroundWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun triggerInstantBackgroundSync(context: Context, categoryName: String) {
            val request = OneTimeWorkRequestBuilder<NewsBackgroundWorker>()
                .setInputData(workDataOf(KEY_TARGET_CATEGORY to categoryName))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
