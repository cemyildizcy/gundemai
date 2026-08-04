package com.example.ui.presentation

import com.example.data.model.NewsArticle
import com.example.data.model.FollowedTopic
import com.example.data.model.Category
import java.net.URI
import java.util.Calendar
import java.util.TimeZone

fun shouldShowFeedControls(selectedTab: Int): Boolean = selectedTab == 0

fun resolveFeedControlsVisibility(
    currentlyVisible: Boolean,
    scrollDelta: Float,
    threshold: Float = 24f,
): Boolean = when {
    scrollDelta <= -threshold -> false
    scrollDelta >= threshold -> true
    else -> currentlyVisible
}

fun formatOriginalSourceLabel(sourceName: String): String {
    val cleanSource = sourceName.trim().removeSurrounding("(", ")").trim()
    return if (cleanSource.isBlank()) {
        "Orijinal Habere Git"
    } else {
        "Orijinal Habere Git ($cleanSource)"
    }
}

fun shouldOutlineNotificationCategory(isSelected: Boolean): Boolean = !isSelected

fun newsImageAspectRatio(imageUrl: String?): Float =
    if (imageUrl.isNullOrBlank()) 3f else 16f / 9f

fun shouldShowArticleImage(imageUrl: String?): Boolean = !imageUrl.isNullOrBlank()

fun safePlayableVideoUrl(value: String?): String? = runCatching {
    val trimmed = value?.trim().orEmpty()
    val uri = URI(trimmed)
    val path = uri.path.orEmpty().lowercase()
    trimmed.takeIf {
        (uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("http", ignoreCase = true)) &&
            !uri.host.isNullOrBlank() &&
            listOf(".mp4", ".m3u8", ".webm", ".mov").any(path::endsWith)
    }
}.getOrNull()

fun shouldShowArticleVideo(videoUrl: String?): Boolean = safePlayableVideoUrl(videoUrl) != null

fun safeHttpUrl(value: String): String? = runCatching {
    val uri = URI(value.trim())
    value.trim().takeIf {
        uri.scheme.equals("https", ignoreCase = true) ||
            uri.scheme.equals("http", ignoreCase = true)
    }?.takeIf { !uri.host.isNullOrBlank() }
}.getOrNull()

fun shouldInvalidateStoredSession(
    authCompleted: Boolean,
    storedEmail: String?,
    firebaseUserPresent: Boolean,
): Boolean = authCompleted && !storedEmail.isNullOrBlank() && !firebaseUserPresent

fun notificationPermissionFeedback(granted: Boolean): String? =
    if (granted) {
        null
    } else {
        "Bildirim izni verilmedi. Daha sonra Android Ayarları > Uygulamalar > GündemAI bölümünden açabilirsiniz."
    }

fun shouldShowCachedOfflineHeader(articleCount: Int, syncError: String?): Boolean =
    articleCount > 0 && !syncError.isNullOrBlank()

fun notificationSectionLabel(
    timestamp: Long,
    now: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault(),
): String {
    val todayStart = Calendar.getInstance(timeZone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val yesterdayStart = (todayStart.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return when {
        timestamp >= todayStart.timeInMillis -> "Bugün"
        timestamp >= yesterdayStart.timeInMillis -> "Dün"
        else -> "Daha önce"
    }
}

fun normalizeSubmittedSearchQuery(query: String): String? =
    query.trim().takeIf { it.length >= 2 }

data class OpenedSearch(
    val activeQuery: String,
    val historyQuery: String?,
)

fun resolveOpenedSearch(query: String): OpenedSearch {
    val activeQuery = query.trim()
    return OpenedSearch(
        activeQuery = activeQuery,
        historyQuery = normalizeSubmittedSearchQuery(activeQuery),
    )
}

fun sanitizeInterestCategories(categories: Set<String>): Set<String> {
    val selectableNames = Category.INTEREST_CATEGORIES.mapTo(mutableSetOf()) { it.displayName }
    return categories.intersect(selectableNames)
}

fun sanitizeNotificationCategories(categories: Set<String>): Set<String> {
    val selectableNames = Category.NOTIFICATION_CATEGORIES.mapTo(mutableSetOf()) { it.displayName }
    return categories.intersect(selectableNames)
}

enum class BackNavigationAction {
    CLOSE_ARTICLE,
    OPEN_HOME,
    CLEAR_SEARCH,
    EXIT,
}

fun resolveBackNavigation(
    hasSelectedArticle: Boolean,
    selectedTab: Int,
    searchQuery: String,
): BackNavigationAction = when {
    hasSelectedArticle -> BackNavigationAction.CLOSE_ARTICLE
    selectedTab != 0 -> BackNavigationAction.OPEN_HOME
    searchQuery.isNotBlank() -> BackNavigationAction.CLEAR_SEARCH
    else -> BackNavigationAction.EXIT
}

fun shouldShowVerificationBadgeInFeed(status: String?): Boolean =
    status.equals("OFFICIAL_CONFIRMED", ignoreCase = true) ||
        status.equals("MULTI_SOURCE_CONFIRMED", ignoreCase = true) ||
        status.equals("UNVERIFIED_CLAIM", ignoreCase = true) ||
        status.equals("SOURCES_CONFLICT", ignoreCase = true)

fun articleMatchesSearch(article: NewsArticle, query: String): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return article.title.contains(needle, ignoreCase = true) ||
        article.summary.contains(needle, ignoreCase = true) ||
        article.whatHappened.contains(needle, ignoreCase = true) ||
        article.sourceName.contains(needle, ignoreCase = true)
}

fun presentArticles(
    articles: List<NewsArticle>,
    category: String,
    query: String,
    selectedCategories: Set<String>,
    selectedTopics: Set<String> = emptySet(),
): List<NewsArticle> {
    val filtered = when {
        query.isNotBlank() -> articles.filter { articleMatchesSearch(it, query) }
        category == "Tümü" -> articles
        category == "Sana Özel" -> {
            val personalizedCategories = selectedCategories - setOf("Tümü", "Sana Özel")
            val topicNames = FollowedTopic.POPULAR_TOPICS
                .filter { it.id in selectedTopics }
                .map { it.name }
            if (personalizedCategories.isEmpty() && topicNames.isEmpty()) {
                articles
            } else {
                articles.filter { article ->
                    article.category in personalizedCategories ||
                        topicNames.any { topic ->
                            article.title.contains(topic, ignoreCase = true) ||
                                article.summary.contains(topic, ignoreCase = true)
                        }
                }
            }
        }
        else -> articles.filter { it.category == category }
    }
    return filtered.sortedWith(
        compareByDescending<NewsArticle> { it.publishedAt }.thenBy { it.id }
    )
}
