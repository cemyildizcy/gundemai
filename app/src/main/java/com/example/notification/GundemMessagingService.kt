package com.example.notification

import com.example.data.remote.ServerCategory
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.worker.NewsBackgroundWorker

class GundemMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val articleId = message.data["article_id"].orEmpty()
        val title = message.data["title"].orEmpty()
        val canonicalCategory = message.data["category"].orEmpty()
        val displayCategory = ServerCategory.toDisplayName(canonicalCategory)
        val sourceName = message.data["source_name"].orEmpty()
        val isBreaking = message.data["is_breaking"].toBoolean()

        NotificationWorkScheduler.enqueueMessage(
            applicationContext,
            NotificationWorkPayload(
                articleId = articleId,
                title = title,
                category = displayCategory,
                sourceName = sourceName,
                isBreaking = isBreaking,
            ),
        )
    }

    override fun onDeletedMessages() {
        NewsBackgroundWorker.triggerRecoverySync(applicationContext)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        NotificationWorkScheduler.enqueueTopicSync(applicationContext)
    }
}
