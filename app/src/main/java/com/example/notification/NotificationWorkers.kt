package com.example.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first

data class NotificationWorkPayload(
    val articleId: String,
    val title: String,
    val category: String,
    val sourceName: String,
    val isBreaking: Boolean,
) {
    fun toData(): Data = Data.Builder()
        .putString(KEY_ARTICLE_ID, articleId)
        .putString(KEY_TITLE, title)
        .putString(KEY_CATEGORY, category)
        .putString(KEY_SOURCE_NAME, sourceName)
        .putBoolean(KEY_IS_BREAKING, isBreaking)
        .build()

    companion object {
        private const val KEY_ARTICLE_ID = "article_id"
        private const val KEY_TITLE = "title"
        private const val KEY_CATEGORY = "category"
        private const val KEY_SOURCE_NAME = "source_name"
        private const val KEY_IS_BREAKING = "is_breaking"

        fun fromData(data: Data): NotificationWorkPayload? {
            val articleId = data.getString(KEY_ARTICLE_ID).orEmpty()
            val title = data.getString(KEY_TITLE).orEmpty()
            if (articleId.isBlank() || title.isBlank()) return null
            return NotificationWorkPayload(
                articleId = articleId,
                title = title,
                category = data.getString(KEY_CATEGORY).orEmpty(),
                sourceName = data.getString(KEY_SOURCE_NAME).orEmpty(),
                isBreaking = data.getBoolean(KEY_IS_BREAKING, false),
            )
        }
    }
}

class NotificationMessageWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val payload = NotificationWorkPayload.fromData(inputData) ?: return Result.failure()
        return runCatching {
            NewsNotificationDispatcher(applicationContext).dispatch(
                articleId = payload.articleId,
                articleTitle = payload.title,
                category = payload.category,
                sourceName = payload.sourceName,
                isBreaking = payload.isBreaking,
            )
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}

class NotificationTopicSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = runCatching {
        val categories = UserPreferencesRepository(applicationContext).notificationCategories.first()
        NotificationTopicManager.syncSubscriptions(categories)
    }.fold(
        onSuccess = { Result.success() },
        onFailure = { Result.retry() },
    )
}

object NotificationWorkScheduler {
    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun enqueueMessage(context: Context, payload: NotificationWorkPayload) {
        val request = OneTimeWorkRequestBuilder<NotificationMessageWorker>()
            .setInputData(payload.toData())
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "gundem_message_${payload.articleId}",
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueTopicSync(context: Context) {
        val request = OneTimeWorkRequestBuilder<NotificationTopicSyncWorker>()
            .setConstraints(networkConstraint)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "gundem_notification_topic_sync",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
