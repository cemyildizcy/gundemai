package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NewsArticle
import com.example.ui.components.AdMobTestAdaptiveBanner
import com.example.ui.components.ArticleImage
import com.example.ui.components.ArticleVideoPlayer
import com.example.util.DateUtils
import com.example.ui.components.SourceTimelineView
import com.example.ui.components.VerificationBadge
import com.example.ui.presentation.safeHttpUrl
import com.example.ui.presentation.shouldShowArticleImage
import com.example.ui.presentation.formatOriginalSourceLabel
import com.example.ui.components.liquidGlassBackground
import com.example.ui.components.GlassIconButton
import com.example.ui.theme.BrandTeal
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    article: NewsArticle?,
    onBackClick: () -> Unit,
    onBookmarkToggle: (articleId: String, currentStatus: Boolean) -> Unit,
    isProUser: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: AI Özeti & Analiz, 1: Orijinal İçerik & Kaynaklar

    val bgColor = MaterialTheme.colorScheme.background
    val cardBgColor = MaterialTheme.colorScheme.surface
    val cardBorderColor = MaterialTheme.colorScheme.outlineVariant
    val accentBlue = MaterialTheme.colorScheme.primary
    val primaryTextColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    if (article == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = accentBlue)
        }
        return
    }

    val verifiedFacts = parseJsonList(article.verifiedFactsJson)
    val unverifiedClaims = parseJsonList(article.unverifiedClaimsJson)
    val possibleImpacts = parseJsonList(article.possibleImpactsJson)
    val contradictions = parseJsonList(article.contradictionsJson)

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier.liquidGlassBackground(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(BrandTeal.copy(alpha = 0.14f))
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = article.category,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTeal
                            )
                        }
                    }
                },
                navigationIcon = {
                    GlassIconButton(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri",
                        onClick = onBackClick,
                        modifier = Modifier.testTag("detail_back_button")
                    )
                },
                actions = {
                    GlassIconButton(
                        imageVector = if (article.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Kaydet",
                        active = article.isBookmarked,
                        onClick = { onBookmarkToggle(article.id, article.isBookmarked) },
                    )
                    Spacer(Modifier.width(6.dp))
                    GlassIconButton(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Paylaş",
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "${article.title}\n\nGündemAI ile oku: ${article.sourceUrl}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Haberi Paylaş"))
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        openSourceUrl(context, article.sourceUrl)
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("open_original_source_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = Color.White)
                        Text(
                            text = formatOriginalSourceLabel(article.sourceName),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }

                AdMobTestAdaptiveBanner(isProUser = isProUser)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. HERO MEDIA & HEADLINE GROUP ---
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (shouldShowArticleImage(article.imageUrl)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                    ) {
                        ArticleImage(
                            imageUrl = article.imageUrl,
                            title = article.title,
                            category = article.category,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.16f))
                        )
                    }
                }

                Text(
                    text = article.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor,
                    lineHeight = 27.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.weight(1f, fill = false)) {
                        VerificationBadge(
                            statusString = article.verificationStatus,
                            confidenceScore = article.confidenceScore
                        )
                    }
                    Text(
                        text = "•",
                        color = secondaryTextColor,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${article.sourceCount} Kaynak",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                    Text(
                        text = "•",
                        color = secondaryTextColor,
                        fontSize = 11.sp
                    )
                    Text(
                        text = DateUtils.formatRelativeTime(article.publishedAt),
                        fontSize = 11.sp,
                        color = secondaryTextColor,
                        maxLines = 1
                    )
                }
            }

            article.videoUrl?.let { videoUrl ->
                ArticleVideoPlayer(
                    videoUrl = videoUrl,
                    posterUrl = article.imageUrl,
                    articleTitle = article.title,
                    category = article.category,
                )
            }

            // --- 3. MODULAR TAB SWITCHER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardBgColor)
                    .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab 0: AI Analysis
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) accentBlue else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color.White else secondaryTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "AI Özeti & Analiz",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) Color.White else secondaryTextColor
                        )
                    }
                }

                // Tab 1: Original Content & Sources
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) accentBlue else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Article,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color.White else secondaryTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "İçerik & Kaynaklar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) Color.White else secondaryTextColor
                        )
                    }
                }
            }

            // --- 4. TAB CONTENTS ---
            if (selectedTab == 0) {
                // TAB 0: AI ANALYSIS & SUMMARY MODULES
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // AI Disclaimer
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Bu analiz, haber kaynaklarındaki veriler işlenerek yapay zekâ tarafından sentezlenmiştir.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    AnalysisSectionCard(
                        title = "Haber Analizi",
                        icon = Icons.Default.AutoAwesome,
                        iconTint = BrandTeal,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            CompactAnalysisSection(
                                title = "Ne oldu?",
                                icon = Icons.Default.Info,
                                iconTint = accentBlue,
                                body = article.whatHappened.ifBlank { article.summary },
                            )
                            CompactAnalysisSection(
                                title = "Neden önemli?",
                                icon = Icons.Default.Lightbulb,
                                iconTint = BrandTeal,
                                body = article.whyImportant.ifBlank {
                                    "Bu gelişme bölgesel ve küresel ölçekte yakından takip edilmektedir."
                                },
                                showDivider = true,
                            )
                            if (verifiedFacts.isNotEmpty()) {
                                CompactAnalysisSection(
                                    title = "Kesin olarak bilinenler",
                                    icon = Icons.Default.CheckCircle,
                                    iconTint = Color(0xFF10B981),
                                    items = verifiedFacts,
                                    showDivider = true,
                                )
                            }
                            if (unverifiedClaims.isNotEmpty()) {
                                CompactAnalysisSection(
                                    title = "Doğrulanmamış iddialar",
                                    icon = Icons.Default.Help,
                                    iconTint = MaterialTheme.colorScheme.tertiary,
                                    items = unverifiedClaims,
                                    showDivider = true,
                                )
                            }
                            if (article.missingInformation.isNotBlank()) {
                                CompactAnalysisSection(
                                    title = "Eksik bilgiler",
                                    icon = Icons.Default.Search,
                                    iconTint = MaterialTheme.colorScheme.tertiary,
                                    body = article.missingInformation,
                                    showDivider = true,
                                )
                            }
                            if (possibleImpacts.isNotEmpty()) {
                                CompactAnalysisSection(
                                    title = "Olası etkiler",
                                    icon = Icons.Default.TrendingUp,
                                    iconTint = BrandTeal,
                                    items = possibleImpacts,
                                    showDivider = true,
                                )
                            }
                            if (contradictions.isNotEmpty()) {
                                CompactAnalysisSection(
                                    title = "Kaynaklar arasındaki çelişkiler",
                                    icon = Icons.Default.Warning,
                                    iconTint = MaterialTheme.colorScheme.error,
                                    items = contradictions,
                                    showDivider = true,
                                )
                            }
                        }
                    }
                }
            } else {
                // TAB 1: ORIGINAL CONTENT & SOURCES TIMELINE
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Clean Typography Article Reader Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Article,
                                    contentDescription = null,
                                    tint = accentBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Orijinal Haber İçeriği",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )
                            }

                            HorizontalDivider(color = cardBorderColor)

                            // Clean readable body typography
                            Text(
                                text = article.summary.ifBlank { article.title },
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 25.sp,
                                fontWeight = FontWeight.Normal
                            )

                            if (article.whatHappened.isNotBlank() && article.whatHappened != article.summary) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = article.whatHappened,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 25.sp
                                )
                            }
                        }
                    }

                    // Source Timeline View
                    SourceTimelineView(
                        sourcesJson = article.sourcesJson,
                        onSourceClick = { url ->
                            openSourceUrl(context, url)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun openSourceUrl(context: android.content.Context, value: String) {
    val url = safeHttpUrl(value)
    if (url == null) {
        Toast.makeText(context, "Bu kaynak bağlantısı güvenli değil.", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "Kaynak bağlantısı açılamadı.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AnalysisSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.80f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
            }
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            content()
        }
    }
}

@Composable
private fun CompactAnalysisSection(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    body: String? = null,
    items: List<String> = emptyList(),
    showDivider: Boolean = false,
) {
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    body?.takeIf { it.isNotBlank() }?.let { value ->
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 21.sp,
        )
    }
    items.forEach { item ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("•", fontWeight = FontWeight.Bold, color = iconTint)
            Text(
                text = item,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp,
            )
        }
    }
}

private fun parseJsonList(json: String): List<String> {
    return try {
        val moshi = Moshi.Builder().build()
        val adapter = moshi.adapter<List<String>>(
            Types.newParameterizedType(List::class.java, String::class.java)
        )
        adapter.fromJson(json) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}
