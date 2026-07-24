package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.UserNotification
import com.example.data.repository.NewsRepository
import com.example.util.DateUtils

class NewsNotificationDispatcher(private val context: Context) {
    private val repository = NewsRepository(context)
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun dispatch(
        articleId: String,
        articleTitle: String,
        category: String,
        sourceName: String,
        isBreaking: Boolean
    ) {
        if (articleId.isBlank() || articleTitle.isBlank()) return
        val localId = "news_$articleId"
        if (repository.notificationExists(localId)) return

        val now = System.currentTimeMillis()
        val body = listOf(category, sourceName).filter { it.isNotBlank() }.joinToString(" • ")
        repository.insertNotification(
            UserNotification(
                id = localId,
                title = articleTitle,
                body = body,
                timestamp = now,
                timestampFormatted = DateUtils.formatRelativeTime(now),
                articleId = articleId,
                type = if (isBreaking) "BREAKING_NEWS" else "TOPIC_ALERT",
                isRead = false
            )
        )
        if (reserveSystemNotificationSlot(now)) {
            showSystemNotification(articleId, articleTitle, body)
        }
    }

    private fun reserveSystemNotificationSlot(now: Long): Boolean = synchronized(notificationLock) {
        val lastShownAt = preferences.getLong(KEY_LAST_SYSTEM_NOTIFICATION_AT, 0L)
        if (now - lastShownAt < MIN_SYSTEM_NOTIFICATION_INTERVAL_MS) {
            false
        } else {
            preferences.edit().putLong(KEY_LAST_SYSTEM_NOTIFICATION_AT, now).commit()
        }
    }

    private fun showSystemNotification(articleId: String, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "GündemAI Haberleri", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Seçtiğiniz kategorilerde yayımlanan yeni haberler"
                }
            )
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ARTICLE_ID, articleId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            articleId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.app_primary))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(title))
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(articleId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "gundem_ai_category_channel_v2"
        private const val PREFERENCES_NAME = "gundem_notification_delivery"
        private const val KEY_LAST_SYSTEM_NOTIFICATION_AT = "last_system_notification_at"
        private const val MIN_SYSTEM_NOTIFICATION_INTERVAL_MS = 2 * 60 * 1000L
        private val notificationLock = Any()
    }
}
