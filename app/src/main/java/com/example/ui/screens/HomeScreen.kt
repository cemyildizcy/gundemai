package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NewsArticle
import com.example.ui.components.AdMobTestNativeCard
import com.example.ui.components.NewsCard
import com.example.ui.components.SkeletonLoadingFeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    articles: List<NewsArticle>,
    isRefreshing: Boolean,
    syncError: String? = null,
    isProUser: Boolean = false,
    onRefresh: () -> Unit,
    onArticleClick: (articleId: String) -> Unit,
    onBookmarkToggle: (articleId: String, currentStatus: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        if (articles.isEmpty() && isRefreshing) {
            SkeletonLoadingFeed(count = 3, modifier = Modifier.padding(16.dp))
        } else if (articles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = syncError ?: "Henüz yayıma hazır haber bulunmuyor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onRefresh) { Text("Tekrar dene") }
            }
        } else {
            val breakingNews = articles.firstOrNull { it.isBreaking }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (syncError != null) {
                    item {
                        Text(
                            text = syncError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                // Breaking News Banner Top
                if (breakingNews != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onArticleClick(breakingNews.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                        .padding(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = "Son Dakika",
                                        tint = Color(0xFFFDE047),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SON DAKİKA GÜNDEM",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFDE047),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = breakingNews.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        lineHeight = 17.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Main News List with Native Ads every 6 items
                itemsIndexed(
                    items = articles,
                    key = { _, article -> article.id }
                ) { index, article ->
                    NewsCard(
                        article = article,
                        onClick = { onArticleClick(article.id) },
                        onBookmarkToggle = { onBookmarkToggle(article.id, article.isBookmarked) }
                    )

                    // Insert a consent-gated AdMob banner every 6 articles for free users.
                    if (!isProUser && index > 0 && index % 6 == 0) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AdMobTestNativeCard()
                    }
                }
            }
        }
    }
}
