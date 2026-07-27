package com.example

import com.example.data.model.VerificationStatus
import com.example.data.model.NewsArticle
import com.example.data.model.Category
import com.example.ui.presentation.BackNavigationAction
import com.example.ui.presentation.articleMatchesSearch
import com.example.ui.presentation.normalizeSubmittedSearchQuery
import com.example.ui.presentation.presentArticles
import com.example.ui.presentation.resolveOpenedSearch
import com.example.ui.presentation.resolveBackNavigation
import com.example.ui.presentation.sanitizeInterestCategories
import com.example.ui.presentation.sanitizeNotificationCategories
import com.example.ui.presentation.shouldShowFeedControls
import com.example.ui.presentation.shouldShowVerificationBadgeInFeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiPresentationRulesTest {

    @Test
    fun `feed controls are only visible on the home tab`() {
        assertTrue(shouldShowFeedControls(0))
        assertFalse(shouldShowFeedControls(1))
        assertFalse(shouldShowFeedControls(2))
        assertFalse(shouldShowFeedControls(3))
        assertFalse(shouldShowFeedControls(4))
    }

    @Test
    fun `single source state does not create a warning badge on every feed card`() {
        assertFalse(shouldShowVerificationBadgeInFeed("SINGLE_SOURCE_REPORT"))
        assertFalse(shouldShowVerificationBadgeInFeed("DEVELOPING_STORY"))
        assertFalse(shouldShowVerificationBadgeInFeed("INSUFFICIENT_INFORMATION"))
        assertFalse(shouldShowVerificationBadgeInFeed(null))
        assertFalse(shouldShowVerificationBadgeInFeed("UNKNOWN_STATUS"))
        assertTrue(shouldShowVerificationBadgeInFeed("MULTI_SOURCE_CONFIRMED"))
        assertTrue(shouldShowVerificationBadgeInFeed("OFFICIAL_CONFIRMED"))
        assertTrue(shouldShowVerificationBadgeInFeed("UNVERIFIED_CLAIM"))
        assertTrue(shouldShowVerificationBadgeInFeed("SOURCES_CONFLICT"))
    }

    @Test
    fun `unknown verification state is never presented as confirmed`() {
        assertEquals(
            VerificationStatus.INSUFFICIENT_INFORMATION,
            VerificationStatus.fromString("UNKNOWN_STATUS")
        )
        assertEquals(
            VerificationStatus.INSUFFICIENT_INFORMATION,
            VerificationStatus.fromString(null)
        )
    }

    @Test
    fun `article search includes publisher name`() {
        val article = NewsArticle(
            id = "source-search",
            title = "Yeni teknoloji yatırımı açıklandı",
            summary = "Yatırımın ayrıntıları kamuoyuyla paylaşıldı.",
            category = "Teknoloji",
            sourceName = "Anadolu Ajansı",
            sourceUrl = "https://example.com/news",
            publishedAt = 1L,
            publishedAtFormatted = "",
            whatHappened = "Yeni yatırım açıklandı.",
            whyImportant = "Sektörü etkileyebilir.",
        )

        assertTrue(articleMatchesSearch(article, "anadolu"))
        assertTrue(articleMatchesSearch(article, "  ANADOLU AJANSI  "))
        assertFalse(articleMatchesSearch(article, "Reuters"))
    }

    @Test
    fun `search history only stores intentional useful queries`() {
        assertEquals(null, normalizeSubmittedSearchQuery(""))
        assertEquals(null, normalizeSubmittedSearchQuery(" a "))
        assertEquals("Anadolu Ajansı", normalizeSubmittedSearchQuery("  Anadolu Ajansı  "))
    }

    @Test
    fun `opening a one character recent search keeps it active without resaving it`() {
        val openedSearch = resolveOpenedSearch(" a ")

        assertEquals("a", openedSearch.activeQuery)
        assertEquals(null, openedSearch.historyQuery)
    }

    @Test
    fun `feed filters and selectable interest categories are separate`() {
        assertEquals("Tümü", Category.FEED_FILTER_CATEGORIES.first().displayName)
        assertFalse(Category.INTEREST_CATEGORIES.any { it.displayName == "Tümü" })
        assertFalse(Category.NOTIFICATION_CATEGORIES.any { it.displayName == "Tümü" })
        assertEquals(
            Category.FEED_FILTER_CATEGORIES.size,
            Category.FEED_FILTER_CATEGORIES.map { it.id }.distinct().size,
        )
    }

    @Test
    fun `synthetic feed filters are removed before category preferences are saved`() {
        assertEquals(
            setOf("Sana Özel", "Teknoloji"),
            sanitizeInterestCategories(setOf("Tümü", "Sana Özel", "Teknoloji")),
        )
        assertEquals(
            setOf("Teknoloji"),
            sanitizeNotificationCategories(setOf("Tümü", "Sana Özel", "Teknoloji")),
        )
    }

    @Test
    fun `all feeds and searches are always newest first`() {
        val articles = listOf(
            article("old", "Teknoloji", 10),
            article("new", "Dünya", 30),
            article("middle", "Ekonomi", 20),
        )

        assertEquals(
            listOf("new", "middle", "old"),
            presentArticles(articles, "Tümü", "", emptySet()).map { it.id },
        )
        assertEquals(
            listOf("new", "middle", "old"),
            presentArticles(articles, "Tümü", "haber", emptySet()).map { it.id },
        )
    }

    @Test
    fun `personalized and specific category feeds remain chronological`() {
        val articles = listOf(
            article("technology-old", "Teknoloji", 10),
            article("world-new", "Dünya", 30),
            article("technology-new", "Teknoloji", 20),
        )

        assertEquals(
            listOf("technology-new", "technology-old"),
            presentArticles(articles, "Sana Özel", "", setOf("Teknoloji")).map { it.id },
        )
        assertEquals(
            listOf("technology-new", "technology-old"),
            presentArticles(articles, "Teknoloji", "", emptySet()).map { it.id },
        )
    }

    @Test
    fun `back navigation unwinds app state before exiting`() {
        assertEquals(
            BackNavigationAction.CLOSE_ARTICLE,
            resolveBackNavigation(hasSelectedArticle = true, selectedTab = 3, searchQuery = "gündem"),
        )
        assertEquals(
            BackNavigationAction.OPEN_HOME,
            resolveBackNavigation(hasSelectedArticle = false, selectedTab = 3, searchQuery = ""),
        )
        assertEquals(
            BackNavigationAction.CLEAR_SEARCH,
            resolveBackNavigation(hasSelectedArticle = false, selectedTab = 0, searchQuery = "gündem"),
        )
        assertEquals(
            BackNavigationAction.EXIT,
            resolveBackNavigation(hasSelectedArticle = false, selectedTab = 0, searchQuery = ""),
        )
    }

    private fun article(id: String, category: String, publishedAt: Long) = NewsArticle(
        id = id,
        title = "$id haber",
        summary = "haber özeti",
        category = category,
        sourceName = "Kaynak",
        sourceUrl = "https://example.com/$id",
        publishedAt = publishedAt,
        publishedAtFormatted = "",
        whatHappened = "haber",
        whyImportant = "önemli",
    )
}
