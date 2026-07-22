package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
        when {
            articles.isEmpty() && isRefreshing -> {
                SkeletonLoadingFeed(count = 3, modifier = Modifier.padding(12.dp))
            }
            articles.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = syncError ?: "Henüz yayıma hazır haber bulunmuyor.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    FilledTonalButton(onClick = onRefresh) { Text("Tekrar dene") }
                }
            }
            else -> {
                val breakingNews = articles.firstOrNull { it.isBreaking }
                val feedArticles = breakingNews?.let { breaking ->
                    articles.filterNot { it.id == breaking.id }
                } ?: articles

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (syncError != null) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = syncError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }

                    if (breakingNews != null) {
                        item {
                            Surface(
                                onClick = { onArticleClick(breakingNews.id) },
                                color = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.Bolt, contentDescription = null)
                                    Column(Modifier.weight(1f)) {
                                        Text("SON DAKİKA", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = breakingNews.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }

                    itemsIndexed(feedArticles, key = { _, article -> article.id }) { index, article ->
                        NewsCard(
                            article = article,
                            onClick = { onArticleClick(article.id) },
                            onBookmarkToggle = { onBookmarkToggle(article.id, article.isBookmarked) }
                        )
                        if (!isProUser && index > 0 && index % 6 == 0) {
                            Spacer(Modifier.height(4.dp))
                            AdMobTestNativeCard()
                        }
                    }
                }
            }
        }
    }
}
