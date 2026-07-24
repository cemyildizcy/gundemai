package com.example.notification

import com.example.data.remote.ServerCategory
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

object NotificationTopicManager {
    private val topicsByCanonicalCategory = mapOf(
        "Son Dakika" to "news_son_dakika",
        "Yapay Zeka" to "news_yapay_zeka",
        "Teknoloji" to "news_teknoloji",
        "Turkiye" to "news_turkiye",
        "Dunya" to "news_dunya",
        "Ekonomi" to "news_ekonomi",
        "Finans" to "news_finans",
        "Kripto" to "news_kripto",
        "Spor" to "news_spor",
        "Transfer" to "news_transfer",
        "Bilim" to "news_bilim",
        "Oyun" to "news_oyun",
        "Girisimcilik" to "news_girisimcilik",
        "Kultur ve Sanat" to "news_kultur_sanat",
        "Saglik" to "news_saglik"
    )

    suspend fun syncSubscriptions(selectedDisplayCategories: Set<String>) {
        val selectedTopics = selectedDisplayCategories
            .map(ServerCategory::toCanonicalName)
            .mapNotNull(topicsByCanonicalCategory::get)
            .toSet()
        val messaging = FirebaseMessaging.getInstance()
        topicsByCanonicalCategory.values.forEach { topic ->
            val operation: () -> com.google.android.gms.tasks.Task<Void> = {
                if (topic in selectedTopics) messaging.subscribeToTopic(topic)
                else messaging.unsubscribeFromTopic(topic)
            }
            runCatching { operation().await() }.getOrElse {
                delay(500)
                operation().await()
            }
        }
    }

    fun topicForCanonicalCategory(category: String): String? = topicsByCanonicalCategory[category]
}
