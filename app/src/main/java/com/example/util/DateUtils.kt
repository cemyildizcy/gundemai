package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val dateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.US),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    )

    fun parseToEpochMillis(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()

        val cleanStr = dateStr.trim()
        
        // Try standard formats
        for (format in dateFormats) {
            try {
                val date = format.parse(cleanStr)
                if (date != null) {
                    val time = date.time
                    if (time > 0 && time <= System.currentTimeMillis() + 86400_000L) {
                        return time
                    }
                }
            } catch (_: Exception) { }
        }

        // Try numeric epoch
        cleanStr.toLongOrNull()?.let { num ->
            if (num > 1000000000L) {
                return if (num < 1000000000000L) num * 1000L else num
            }
        }

        return System.currentTimeMillis()
    }

    fun formatRelativeTime(epochMillis: Long): String {
        val now = System.currentTimeMillis()
        val diffMillis = now - epochMillis

        if (diffMillis < 0) {
            return formatClockTime(epochMillis)
        }

        val diffMinutes = diffMillis / (60 * 1000)
        val diffHours = diffMillis / (60 * 60 * 1000)

        return when {
            diffMinutes < 1 -> "Az önce"
            diffMinutes < 60 -> "$diffMinutes dk önce"
            diffHours < 24 -> "$diffHours saat önce"
            diffHours < 48 -> "Dün " + formatClockTime(epochMillis)
            else -> SimpleDateFormat("dd MMM HH:mm", Locale("tr", "TR")).format(Date(epochMillis))
        }
    }

    fun formatClockTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale("tr", "TR"))
        return sdf.format(Date(epochMillis))
    }
}
