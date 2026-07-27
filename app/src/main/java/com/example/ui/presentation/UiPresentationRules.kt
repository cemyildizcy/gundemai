package com.example.ui.presentation

import com.example.data.model.NewsArticle
import com.example.data.model.FollowedTopic
import com.example.data.model.Category

fun shouldShowFeedControls(selectedTab: Int): Boolean = selectedTab == 0

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
