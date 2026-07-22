package com.example.notification

import com.example.data.remote.ServerCategory
import com.example.data.repository.UserPreferencesRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.worker.NewsBackgroundWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class GundemMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val articleId = message.data["article_id"].orEmpty()
        val title = message.data["title"].orEmpty()
        val canonicalCategory = message.data["category"].orEmpty()
        val displayCategory = ServerCategory.toDisplayName(canonicalCategory)
        val sourceName = message.data["source_name"].orEmpty()
        val isBreaking = message.data["is_breaking"].toBoolean()

        runBlocking(Dispatchers.IO) {
            NewsNotificationDispatcher(applicationContext).dispatch(
                articleId = articleId,
                articleTitle = title,
                category = displayCategory,
                sourceName = sourceName,
                isBreaking = isBreaking
            )
        }
    }

    override fun onDeletedMessages() {
        NewsBackgroundWorker.triggerRecoverySync(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        runBlocking(Dispatchers.IO) {
            val selectedCategories = UserPreferencesRepository(applicationContext)
                .notificationCategories
                .first()
            NotificationTopicManager.syncSubscriptions(selectedCategories)
        }
    }
}
