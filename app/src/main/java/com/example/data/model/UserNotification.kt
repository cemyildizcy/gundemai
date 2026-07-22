package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class UserNotification(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val timestamp: Long,
    val timestampFormatted: String,
    val articleId: String? = null,
    val type: String = "BREAKING_NEWS", // BREAKING_NEWS, TOPIC_ALERT, CLAIM_VERIFIED
    val isRead: Boolean = false
)
