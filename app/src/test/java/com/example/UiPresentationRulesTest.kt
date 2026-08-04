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
import com.example.ui.presentation.newsImageAspectRatio
import com.example.ui.presentation.safeHttpUrl
import com.example.ui.presentation.shouldInvalidateStoredSession
import com.example.ui.presentation.notificationPermissionFeedback
import com.example.ui.presentation.shouldShowCachedOfflineHeader
import com.example.ui.presentation.notificationSectionLabel
import com.example.ui.presentation.shouldShowArticleImage
import com.example.ui.presentation.formatOriginalSourceLabel
import com.example.ui.presentation.resolveFeedControlsVisibility
import com.example.ui.presentation.shouldOutlineNotificationCategory
import com.example.ui.presentation.safePlayableVideoUrl
import com.example.ui.presentation.shouldShowArticleVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

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
    fun `feed controls follow intentional vertical scroll direction`() {
        assertFalse(resolveFeedControlsVisibility(currentlyVisible = true, scrollDelta = -32f))
        assertTrue(resolveFeedControlsVisibility(currentlyVisible = false, scrollDelta = 32f))
        assertTrue(resolveFeedControlsVisibility(currentlyVisible = true, scrollDelta = -4f))
        assertFalse(resolveFeedControlsVisibility(currentlyVisible = false, scrollDelta = 4f))
    }

    @Test
    fun `original source label never adds doubled parentheses`() {
        assertEquals("Orijinal Habere Git (BBC)", formatOriginalSourceLabel("BBC"))
        assertEquals("Orijinal Habere Git (BBC)", formatOriginalSourceLabel("(BBC)"))
        assertEquals("Orijinal Habere Git", formatOriginalSourceLabel("  "))
    }

    @Test
    fun `selected notification category relies on fill instead of a second outline`() {
        assertFalse(shouldOutlineNotificationCategory(isSelected = true))
        assertTrue(shouldOutlineNotificationCategory(isSelected = false))
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

    @Test
    fun `missing article images use a compact placeholder instead of a full media frame`() {
        assertEquals(16f / 9f, newsImageAspectRatio("https://example.com/image.jpg"))
        assertEquals(3f, newsImageAspectRatio(null))
        assertEquals(3f, newsImageAspectRatio("  "))
    }

    @Test
    fun `news layouts reserve a media frame only for real image urls`() {
        assertTrue(shouldShowArticleImage("https://example.com/image.jpg"))
        assertFalse(shouldShowArticleImage(null))
        assertFalse(shouldShowArticleImage("  "))
    }

    @Test
    fun `article video accepts direct streams and rejects web pages`() {
        assertEquals(
            "https://cdn.example.com/news/video.mp4",
            safePlayableVideoUrl("https://cdn.example.com/news/video.mp4"),
        )
        assertEquals(
            "https://cdn.example.com/news/master.m3u8?token=abc",
            safePlayableVideoUrl("https://cdn.example.com/news/master.m3u8?token=abc"),
        )
        assertEquals(null, safePlayableVideoUrl("https://youtube.com/watch?v=123"))
        assertEquals(null, safePlayableVideoUrl("https://example.com/news/story"))
        assertTrue(shouldShowArticleVideo("https://cdn.example.com/news/clip.webm"))
        assertFalse(shouldShowArticleVideo(null))
    }

    @Test
    fun `article links only allow web schemes`() {
        assertEquals("https://example.com/news", safeHttpUrl("https://example.com/news"))
        assertEquals("http://example.com/news", safeHttpUrl("http://example.com/news"))
        assertEquals(null, safeHttpUrl("javascript:alert(1)"))
        assertEquals(null, safeHttpUrl("not a url"))
    }

    @Test
    fun `stored signed in state is invalidated when Firebase no longer has that user`() {
        assertTrue(shouldInvalidateStoredSession(true, "user@example.com", false))
        assertFalse(shouldInvalidateStoredSession(true, "user@example.com", true))
        assertFalse(shouldInvalidateStoredSession(true, null, false))
        assertFalse(shouldInvalidateStoredSession(false, "user@example.com", false))
    }

    @Test
    fun `notification permission denial gives a useful settings path`() {
        assertEquals(null, notificationPermissionFeedback(true))
        assertEquals(
            "Bildirim izni verilmedi. Daha sonra Android Ayarları > Uygulamalar > GündemAI bölümünden açabilirsiniz.",
            notificationPermissionFeedback(false),
        )
    }

    @Test
    fun `offline banner only appears when cached articles remain visible`() {
        assertTrue(shouldShowCachedOfflineHeader(articleCount = 12, syncError = "Bağlantı yok"))
        assertFalse(shouldShowCachedOfflineHeader(articleCount = 0, syncError = "Bağlantı yok"))
        assertFalse(shouldShowCachedOfflineHeader(articleCount = 12, syncError = null))
    }

    @Test
    fun `notification dates are grouped into reader friendly sections`() {
        val day = 24 * 60 * 60 * 1000L
        val now = 10 * day + 12 * 60 * 60 * 1000L
        val utc = TimeZone.getTimeZone("UTC")
        assertEquals("Bugün", notificationSectionLabel(now - 1_000, now, utc))
        assertEquals("Dün", notificationSectionLabel(now - day, now, utc))
        assertEquals("Daha önce", notificationSectionLabel(now - 3 * day, now, utc))
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
