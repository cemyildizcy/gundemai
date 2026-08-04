package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyBrief
import com.example.data.model.DailyBriefItem
import com.example.data.model.NewsArticle
import com.example.data.auth.AuthResult
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.theme.GundemAITheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class PhoneUiScreenshotAuditTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun phoneHomeLight() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                HomeScreen(
                    articles = sampleArticles(),
                    dailyBrief = sampleDailyBrief(),
                    isRefreshing = false,
                    isProUser = true,
                    onRefresh = {},
                    onArticleClick = {},
                    onBookmarkToggle = { _, _ -> },
                )
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "build/reports/ui-audit/phone-home-light.png",
        )
    }

    @Test
    fun phoneDetailLight() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                DetailScreen(
                    article = sampleArticles()[1],
                    onBackClick = {},
                    onBookmarkToggle = { _, _ -> },
                    isProUser = true,
                )
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "build/reports/ui-audit/phone-detail-light.png",
        )
    }

    @Test
    fun emptyBookmarksActionReturnsToNewsFeed() {
        var browseRequested = false
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                BookmarksScreen(
                    bookmarkedArticles = emptyList(),
                    onArticleClick = {},
                    onBookmarkToggle = { _, _ -> },
                    onBrowseNews = { browseRequested = true },
                )
            }
        }

        composeRule.onNodeWithText("Gündeme göz at").performClick()
        assertTrue(browseRequested)
    }

    @Test
    fun phoneEmptyBookmarksDark() {
        composeRule.setContent {
            GundemAITheme(darkTheme = true) {
                BookmarksScreen(
                    bookmarkedArticles = emptyList(),
                    onArticleClick = {},
                    onBookmarkToggle = { _, _ -> },
                    onBrowseNews = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "build/reports/ui-audit/phone-bookmarks-empty-dark.png",
        )
    }

    @Test
    fun phoneNotificationsDark() {
        composeRule.setContent {
            GundemAITheme(darkTheme = true) {
                NotificationsScreen(
                    notifications = emptyList(),
                    followedCategories = setOf("Teknoloji", "Türkiye"),
                    onCategoryToggle = {},
                    onNotificationClick = {},
                    onMarkRead = {},
                    onMarkAllRead = {},
                    onClearAll = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "build/reports/ui-audit/phone-notifications-dark.png",
        )
    }

    @Test
    fun phoneProfileLight() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                ProfileScreen(
                    darkThemeEnabled = false,
                    onDarkThemeToggle = {},
                    followedCategories = setOf("Teknoloji"),
                    followedTopics = setOf("google"),
                    userEmail = null,
                    userName = null,
                    isProUser = false,
                    proPlanPeriod = "YEARLY",
                    onLogout = {},
                    onPurchaseSubscription = { _, _, _ -> },
                    onRestorePurchases = {},
                    onResetOnboarding = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "build/reports/ui-audit/phone-profile-light.png",
        )
    }

    @Test
    fun phoneAuthLight() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                AuthScreen(
                    onAuthSuccess = { _, _ -> },
                    onGuestContinue = {},
                    onEmailSignUp = { _, _, _ -> AuthResult.Error("Test") },
                    onEmailSignIn = { _, _ -> AuthResult.Error("Test") },
                    onGoogleSignIn = { AuthResult.Error("Test") },
                )
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "build/reports/ui-audit/phone-auth-light.png",
        )
    }

    @Test
    fun phoneExploreLight() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                ExploreScreen(
                    followedTopics = setOf("openai", "google"),
                    recentSearches = emptyList(),
                    onTopicClick = {},
                    onSearchTagClick = {},
                )
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "build/reports/ui-audit/phone-explore-light.png",
        )
    }
}

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1280dp-h800dp-xhdpi", sdk = [36])
class TabletUiScreenshotAuditTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tabletHomeDark() {
        composeRule.setContent {
            GundemAITheme(darkTheme = true) {
                HomeScreen(
                    articles = sampleArticles(),
                    isRefreshing = false,
                    isProUser = true,
                    onRefresh = {},
                    onArticleClick = {},
                    onBookmarkToggle = { _, _ -> },
                )
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "build/reports/ui-audit/tablet-home-dark.png",
        )
    }

    @Test
    fun tabletFeedKeepsReadableEditorialWidth() {
        composeRule.setContent {
            GundemAITheme(darkTheme = true) {
                HomeScreen(
                    articles = sampleArticles(),
                    isRefreshing = false,
                    isProUser = true,
                    onRefresh = {},
                    onArticleClick = {},
                    onBookmarkToggle = { _, _ -> },
                )
            }
        }

        val cardBounds = composeRule
            .onNodeWithTag("news_card_technology")
            .getUnclippedBoundsInRoot()
        val cardWidth = cardBounds.right - cardBounds.left
        assertTrue("Tablet feed card was $cardWidth wide", cardWidth <= 720.dp)
    }
}

private fun sampleArticles() = listOf(
    sampleArticle(
        id = "breaking",
        title = "Türkiye gündemindeki önemli gelişme açıklandı",
        category = "Son Dakika",
        isBreaking = true,
    ),
    sampleArticle(
        id = "technology",
        title = "Yapay zekâ alanında yeni araştırma sonuçları yayımlandı",
        category = "Teknoloji",
        imageUrl = "https://cdn.example.com/news/technology.jpg",
        videoUrl = "https://cdn.example.com/news/technology.m3u8",
    ),
    sampleArticle(
        id = "economy",
        title = "Ekonomi gündemindeki son veriler değerlendirildi",
        category = "Ekonomi",
    ),
)

private fun sampleDailyBrief() = DailyBrief(
    dateKey = "2026-07-30",
    title = "Bugünün Gündemi",
    summary = "Günün en kritik gelişmeleri, doğrulanmış haberlerden seçilerek kısa ve anlaşılır biçimde özetlendi.",
    items = listOf(
        DailyBriefItem(
            articleId = "breaking",
            title = "Türkiye gündemindeki önemli gelişme açıklandı",
            summary = "Kararın geniş bir kitleyi ilgilendiren doğrudan etkileri bulunuyor.",
            category = "Türkiye",
            publishedAt = 3L,
        ),
        DailyBriefItem(
            articleId = "technology",
            title = "Yeni teknoloji yatırımı duyuruldu",
            summary = "Yatırım, teknoloji ekosistemindeki üretim kapasitesini etkileyebilir.",
            category = "Teknoloji",
            publishedAt = 2L,
        ),
        DailyBriefItem(
            articleId = "economy",
            title = "Ekonomi verileri güncellendi",
            summary = "Açıklanan veriler piyasa beklentileri açısından günün öne çıkan başlıkları arasında.",
            category = "Ekonomi",
            publishedAt = 1L,
        ),
    ),
    generatedAt = 4L,
)

private fun sampleArticle(
    id: String,
    title: String,
    category: String,
    isBreaking: Boolean = false,
    imageUrl: String? = null,
    videoUrl: String? = null,
) = NewsArticle(
    id = id,
    title = title,
    summary = "Haberin temel ayrıntıları kaynak metinlerine dayanarak özetlendi.",
    category = category,
    imageUrl = imageUrl,
    videoUrl = videoUrl,
    sourceName = "Örnek Haber Kaynağı",
    sourceUrl = "https://example.com/news/$id",
    publishedAt = System.currentTimeMillis() - 60_000,
    publishedAtFormatted = "",
    whatHappened = "Kaynaklar yeni gelişmenin ayrıntılarını paylaştı.",
    whyImportant = "Gelişme ilgili alanı ve okurların gündemini etkileyebilir.",
    confidenceScore = 82,
    sourceCount = 1,
    isBreaking = isBreaking,
    isAiAnalyzed = true,
)
