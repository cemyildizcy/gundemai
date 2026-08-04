package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.example.data.auth.AuthResult
import com.example.data.model.DailyBrief
import com.example.data.model.DailyBriefItem
import com.example.data.model.NewsArticle
import com.example.data.billing.BillingConnectionState
import com.example.data.billing.BillingPurchaseState
import com.example.ui.components.PlayBillingPaywallSheet
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.theme.GundemAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserFacingUiContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun authModeControlsStaySeparatedOnCompactLayouts() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                AuthScreen(
                    onAuthSuccess = { _, _ -> },
                    onGuestContinue = {},
                    onEmailSignUp = { _, _, _ -> AuthResult.Error("test") },
                    onEmailSignIn = { _, _ -> AuthResult.Error("test") },
                    onGoogleSignIn = { AuthResult.Error("test") },
                )
            }
        }

        val signInBounds = composeRule.onNodeWithTag("auth_mode_sign_in")
            .fetchSemanticsNode().boundsInRoot
        val registerBounds = composeRule.onNodeWithTag("auth_mode_register")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(signInBounds.right <= registerBounds.left)
    }

    @Test
    fun notificationScreenUsesReaderLanguageInsteadOfImplementationDetails() {
        composeRule.setContent {
            GundemAITheme {
                NotificationsScreen(
                    notifications = emptyList(),
                    followedCategories = emptySet(),
                    onCategoryToggle = {},
                    onNotificationClick = {},
                    onMarkRead = {},
                    onMarkAllRead = {},
                    onClearAll = {},
                )
            }
        }

        composeRule.onNodeWithText("WorkManager", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Bildirimler yalnızca seçtiğiniz kategoriler için gönderilir.", substring = true)
            .assertExists()
    }

    @Test
    fun bookmarksScreenExplainsOfflineReadingWithoutDatabaseTerminology() {
        composeRule.setContent {
            GundemAITheme {
                BookmarksScreen(
                    bookmarkedArticles = listOf(bookmarkedArticle()),
                    onArticleClick = {},
                    onBookmarkToggle = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("yerel veritaban", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("Kaydettiğiniz haberleri internet bağlantısı olmadan da okuyabilirsiniz.", substring = true)
            .assertExists()
    }

    @Test
    fun profileScreenDoesNotExposeSdkNamesOrPromiseUnimplementedAdFormats() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                ProfileScreen(
                    darkThemeEnabled = false,
                    onDarkThemeToggle = {},
                    followedCategories = emptySet(),
                    followedTopics = emptySet(),
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

        composeRule.onNodeWithText("AdMob", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("PLAY BILLING", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Firebase", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("geçiş reklam", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("1.5 Sa", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("%100", substring = true).assertDoesNotExist()
    }

    @Test
    fun paywallOnlyPromisesTheAdFormatThatProActuallyRemoves() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                PlayBillingPaywallSheet(
                    connectionState = BillingConnectionState.Disconnected,
                    purchaseState = BillingPurchaseState.Idle,
                    availableProducts = emptyList(),
                    isProUser = false,
                    userEmail = null,
                    onDismiss = {},
                    onPurchaseClicked = { _, _ -> },
                    onRestoreClicked = {},
                    onResetPurchaseState = {},
                )
            }
        }

        composeRule.onNodeWithText("Billing", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("%100", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("tüm reklam", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("banner reklamsız", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun exploreScreenStartsWithAReadablePurposeAndSourceSearchLanguage() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                ExploreScreen(
                    followedTopics = emptySet(),
                    recentSearches = emptyList(),
                    onTopicClick = {},
                    onSearchTagClick = {},
                )
            }
        }

        composeRule.onNodeWithText("Keşfet").assertExists()
        composeRule.onNodeWithText("İlgi alanlarını ve haber kaynaklarını düzenle.", substring = true)
            .assertExists()
        composeRule.onNodeWithText("Canlı Haber Kaynakları", substring = true)
            .assertDoesNotExist()
    }

    @Test
    fun emptyCategoryIsNotPresentedAsANetworkFailure() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                HomeScreen(
                    articles = emptyList(),
                    isRefreshing = false,
                    selectedCategory = "Bilim",
                    onRefresh = {},
                    onShowAll = {},
                    onArticleClick = {},
                    onBookmarkToggle = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Tekrar dene", ignoreCase = true).assertDoesNotExist()
        composeRule.onNodeWithText("Bilim kategorisinde henüz haber yok.", substring = true)
            .assertExists()
        composeRule.onNodeWithText("Tüm haberleri göster").assertExists()
    }

    @Test
    fun homeShowsTheSameSharedDailyBriefAboveTheFeed() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                HomeScreen(
                    articles = listOf(bookmarkedArticle()),
                    dailyBrief = DailyBrief(
                        dateKey = "2026-07-30",
                        title = "Bugünün Gündemi",
                        summary = "Bugünün en kritik gelişmeleri kısa ve ortak bir özet halinde sunuluyor.",
                        items = listOf(
                            DailyBriefItem("news-1", "Birinci gelişme", "Birinci kısa özet.", "Dünya", 3L),
                            DailyBriefItem("news-2", "İkinci gelişme", "İkinci kısa özet.", "Ekonomi", 2L),
                            DailyBriefItem("news-3", "Üçüncü gelişme", "Üçüncü kısa özet.", "Teknoloji", 1L),
                        ),
                        generatedAt = 4L,
                    ),
                    isRefreshing = false,
                    selectedCategory = "Tümü",
                    onRefresh = {},
                    onArticleClick = {},
                    onBookmarkToggle = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Bugünün Gündemi").assertExists()
        composeRule.onNodeWithText("Herkes için ortak yapay zekâ özeti").assertExists()
        composeRule.onNodeWithText("Birinci gelişme").assertExists()
    }

    @Test
    fun homeFeedKeepsCardsHeadlineFirstAndMovesActionsToDetail() {
        val article = bookmarkedArticle()
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                HomeScreen(
                    articles = listOf(article),
                    isRefreshing = false,
                    selectedCategory = "Tümü",
                    onRefresh = {},
                    onArticleClick = {},
                    onBookmarkToggle = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText(article.title).assertExists()
        composeRule.onNodeWithText(article.summary).assertDoesNotExist()
        composeRule.onNodeWithText(article.whyImportant).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Haberi kaydet").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Kaydı kaldır").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Haberi paylaş").assertDoesNotExist()
    }

    @Test
    fun articleDetailKeepsPlayableVideoInsideTheApp() {
        composeRule.setContent {
            GundemAITheme(darkTheme = false) {
                DetailScreen(
                    article = bookmarkedArticle().copy(
                        videoUrl = "https://cdn.example.com/news/video.mp4",
                    ),
                    onBackClick = {},
                    onBookmarkToggle = { _, _ -> },
                    isProUser = true,
                )
            }
        }

        composeRule.onNodeWithText("Haber videosu").assertExists()
        composeRule.onNodeWithTag("article_video_player").assertExists()
    }

    private fun bookmarkedArticle() = NewsArticle(
        id = "bookmark-1",
        title = "Kaydedilmiş haber",
        summary = "Haber özeti",
        category = "Türkiye",
        sourceName = "Kaynak",
        sourceUrl = "https://example.com/news",
        publishedAt = 1L,
        publishedAtFormatted = "",
        whatHappened = "Bir gelişme yaşandı.",
        whyImportant = "Okurlar için önem taşıyor.",
        isBookmarked = true,
    )
}
