package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.data.model.NewsArticle
import com.example.data.billing.BillingConnectionState
import com.example.data.billing.BillingPurchaseState
import com.example.ui.components.PlayBillingPaywallSheet
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.GundemAITheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserFacingUiContentTest {
    @get:Rule
    val composeRule = createComposeRule()

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
