package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.repository.NewsRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.notification.NewsNotificationDispatcher
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class NewsBackgroundWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): ListenableWorker.Result = try {
        val repository = NewsRepository(applicationContext)
        val selectedCategories = UserPreferencesRepository(applicationContext)
            .notificationCategories
            .first()
        val sync = repository.fetchAndRefreshNews(forceRefresh = true).getOrThrow()

        // The first sync establishes a baseline and must not announce older feed items as new.
        if (!sync.wasInitialSync && selectedCategories.isNotEmpty()) {
            val dispatcher = NewsNotificationDispatcher(applicationContext)
            sync.newArticles
                .asSequence()
                .filter { it.category in selectedCategories }
                .take(MAX_NOTIFICATIONS_PER_SYNC)
                .forEach { article ->
                    dispatcher.dispatch(
                        articleId = article.id,
                        articleTitle = article.title,
                        category = article.category,
                        sourceName = article.sourceName,
                        isBreaking = article.isBreaking
                    )
                }
        }
        ListenableWorker.Result.success()
    } catch (_: Exception) {
        ListenableWorker.Result.retry()
    }

    companion object {
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

        fun triggerRecoverySync(context: Context) {
            val request = OneTimeWorkRequestBuilder<NewsBackgroundWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
