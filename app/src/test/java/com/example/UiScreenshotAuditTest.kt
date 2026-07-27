package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.example.data.model.NewsArticle
import com.example.data.auth.AuthResult
import com.example.ui.screens.AuthScreen
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
    ),
    sampleArticle(
        id = "economy",
        title = "Ekonomi gündemindeki son veriler değerlendirildi",
        category = "Ekonomi",
    ),
)

private fun sampleArticle(
    id: String,
    title: String,
    category: String,
    isBreaking: Boolean = false,
) = NewsArticle(
    id = id,
    title = title,
    summary = "Haberin temel ayrıntıları kaynak metinlerine dayanarak özetlendi.",
    category = category,
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
