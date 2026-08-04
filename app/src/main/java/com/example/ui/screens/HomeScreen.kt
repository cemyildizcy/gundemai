package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyBrief
import com.example.data.model.NewsArticle
import com.example.ui.components.AdMobTestNativeCard
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassEmptyState
import com.example.ui.components.GlassSectionHeader
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.NewsCard
import com.example.ui.components.OfflineHeader
import com.example.ui.components.SkeletonLoadingFeed
import com.example.ui.presentation.shouldShowCachedOfflineHeader
import com.example.ui.presentation.resolveFeedControlsVisibility
import com.example.ui.theme.GundemDesignTokens
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.BrandTealTint
import com.example.ui.theme.BreakingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    articles: List<NewsArticle>,
    dailyBrief: DailyBrief? = null,
    isRefreshing: Boolean,
    syncError: String? = null,
    isProUser: Boolean = false,
    selectedCategory: String = "Tümü",
    searchQuery: String = "",
    onRefresh: () -> Unit,
    onShowAll: () -> Unit = {},
    onArticleClick: (articleId: String) -> Unit,
    onBookmarkToggle: (articleId: String, currentStatus: Boolean) -> Unit,
    feedControlsVisible: Boolean = true,
    onFeedControlsVisibilityChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentFeedControlsVisible by rememberUpdatedState(feedControlsVisible)
    val currentVisibilityCallback by rememberUpdatedState(onFeedControlsVisibilityChange)
    var accumulatedScroll by remember { mutableFloatStateOf(0f) }
    val feedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y == 0f) return Offset.Zero
                accumulatedScroll = if (
                    accumulatedScroll == 0f ||
                    (accumulatedScroll > 0f) == (available.y > 0f)
                ) {
                    accumulatedScroll + available.y
                } else {
                    available.y
                }
                val resolved = resolveFeedControlsVisibility(
                    currentlyVisible = currentFeedControlsVisible,
                    scrollDelta = accumulatedScroll,
                )
                if (resolved != currentFeedControlsVisible) {
                    currentVisibilityCallback(resolved)
                    accumulatedScroll = 0f
                }
                return Offset.Zero
            }
        }
    }

    LiquidGlassBackground(modifier = modifier) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                articles.isEmpty() && isRefreshing -> {
                    SkeletonLoadingFeed(
                        count = 3,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }

                articles.isEmpty() -> {
                    val hasActiveFilter = searchQuery.isNotBlank() || selectedCategory != "Tümü"
                    val title = when {
                        syncError != null -> "Haber akışına ulaşılamadı"
                        searchQuery.isNotBlank() -> "Aradığın haberi bulamadık"
                        hasActiveFilter -> "Bu başlıkta henüz haber yok"
                        else -> "Yeni haberler hazırlanıyor"
                    }
                    val message = when {
                        syncError != null -> syncError
                        searchQuery.isNotBlank() -> "“${searchQuery.trim()}” için başka bir ifade deneyebilirsin."
                        selectedCategory != "Tümü" ->
                            "$selectedCategory kategorisinde henüz haber yok. Tüm gündeme dönerek diğer gelişmeleri inceleyebilirsin."
                        hasActiveFilter -> "Tüm gündeme dönerek diğer gelişmeleri inceleyebilirsin."
                        else -> "Kaynaklar taranıyor. Biraz sonra yeniden deneyebilirsin."
                    }
                    GlassEmptyState(
                        icon = if (syncError != null) Icons.Default.Newspaper else Icons.Default.Newspaper,
                        title = title,
                        message = message,
                        actionLabel = if (hasActiveFilter && syncError == null) "Tüm haberleri göster" else "Yeniden dene",
                        onAction = if (hasActiveFilter && syncError == null) onShowAll else onRefresh,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp)
                            .widthIn(max = 560.dp),
                    )
                }

                else -> {
                    val breakingNews = articles.firstOrNull { it.isBreaking }
                    val feedArticles = breakingNews?.let { breaking ->
                        articles.filterNot { it.id == breaking.id }
                    } ?: articles

                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxSize()
                            .nestedScroll(feedScrollConnection)
                            .align(Alignment.TopCenter),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        item {
                            GlassSectionHeader(
                                title = when {
                                    searchQuery.isNotBlank() -> "Arama sonuçları"
                                    selectedCategory == "Tümü" -> "Bugünün gündemi"
                                    else -> selectedCategory
                                },
                                subtitle = "${articles.size} güncel gelişme • en yeniden eskiye",
                            )
                        }

                        if (shouldShowCachedOfflineHeader(articles.size, syncError)) {
                            item { OfflineHeader() }
                        }

                        if (
                            dailyBrief != null &&
                            searchQuery.isBlank() &&
                            selectedCategory in setOf("Sana Özel", "Tümü")
                        ) {
                            item(key = "daily-brief-${dailyBrief.dateKey}") {
                                DailyBriefCard(
                                    dailyBrief = dailyBrief,
                                    onArticleClick = onArticleClick,
                                )
                            }
                        }

                        if (breakingNews != null) {
                            item {
                                GlassCard(
                                    highlighted = true,
                                    onClick = { onArticleClick(breakingNews.id) },
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        Surface(
                                            color = Color.Transparent,
                                            shape = RoundedCornerShape(GundemDesignTokens.smallRadius),
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(bottom = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Surface(
                                                    color = BreakingRed,
                                                    shape = RoundedCornerShape(12.dp),
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Bolt,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                        )
                                                        Text(
                                                            "Son dakika",
                                                            color = Color.White,
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                    }
                                                }
                                                Text(
                                                    breakingNews.sourceName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                        Text(
                                            text = breakingNews.title,
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Spacer(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(top = 2.dp),
                                        )
                                    }
                                }
                            }
                        }

                        itemsIndexed(feedArticles, key = { _, article -> article.id }) { index, article ->
                            NewsCard(
                                article = article,
                                onClick = { onArticleClick(article.id) },
                            )
                            if (index > 0 && index % 6 == 0) {
                                AdMobTestNativeCard(
                                    isProUser = isProUser,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBriefCard(
    dailyBrief: DailyBrief,
    onArticleClick: (articleId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable(dailyBrief.dateKey) { mutableStateOf(false) }
    val visibleItems = if (expanded) dailyBrief.items else dailyBrief.items.take(3)

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        highlighted = true,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = BrandTeal,
                    shape = CircleShape,
                    modifier = Modifier.size(42.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dailyBrief.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Herkes için ortak yapay zekâ özeti",
                        style = MaterialTheme.typography.labelMedium,
                        color = BrandTeal,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Text(
                text = dailyBrief.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f))

            visibleItems.forEachIndexed { index, item ->
                Surface(
                    onClick = { onArticleClick(item.articleId) },
                    color = Color.Transparent,
                    shape = RoundedCornerShape(GundemDesignTokens.smallRadius),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            color = BrandTealTint,
                            shape = CircleShape,
                            modifier = Modifier.size(27.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTeal,
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = item.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (expanded) 3 else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            if (dailyBrief.items.size > 3) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(if (expanded) "Daha az göster" else "Tüm özeti gör")
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                    )
                }
            }
        }
    }
}
