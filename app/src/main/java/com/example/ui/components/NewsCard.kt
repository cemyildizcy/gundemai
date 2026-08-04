package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.NewsArticle
import com.example.ui.presentation.newsImageAspectRatio
import com.example.ui.presentation.shouldShowVerificationBadgeInFeed
import com.example.ui.presentation.shouldShowArticleImage
import com.example.ui.theme.BrandTeal
import com.example.util.DateUtils

@Composable
fun NewsCard(
    article: NewsArticle,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("news_card_${article.id}"),
        onClick = onClick,
    ) {
        if (shouldShowArticleImage(article.imageUrl)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(newsImageAspectRatio(article.imageUrl))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                ArticleImage(
                    imageUrl = article.imageUrl,
                    title = article.title,
                    category = article.category,
                    modifier = Modifier.fillMaxSize(),
                )
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    contentColor = BrandTeal,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                ) {
                    Text(
                        text = article.category,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!shouldShowArticleImage(article.imageUrl)) {
                    Surface(
                        color = BrandTeal.copy(alpha = 0.10f),
                        contentColor = BrandTeal,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            article.category,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
                Surface(shape = CircleShape, color = BrandTeal.copy(alpha = 0.12f)) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = BrandTeal,
                        modifier = Modifier.padding(6.dp).size(14.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = article.sourceName.ifBlank { "GündemAI haber akışı" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = DateUtils.formatRelativeTime(article.publishedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (shouldShowVerificationBadgeInFeed(article.verificationStatus)) {
                    VerificationBadge(article.verificationStatus, article.confidenceScore)
                }
            }

            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

        }
    }
}
