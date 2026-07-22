package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.NewsArticle
import com.example.ui.components.AdMobTestAdaptiveBanner
import com.example.ui.components.SourceTimelineView
import com.example.ui.components.VerificationBadge
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

    val bgColor = Color(0xFF0F172A)
    val cardBgColor = Color(0xFF1E293B)
    val cardBorderColor = Color(0xFF334155)
    val accentBlue = Color(0xFF3B82F6)
    val primaryTextColor = Color(0xFFF8FAFC)
    val secondaryTextColor = Color(0xFF94A3B8)

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
        containerColor = bgColor,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentBlue)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = article.category.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = primaryTextColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onBookmarkToggle(article.id, article.isBookmarked) }) {
                        Icon(
                            imageVector = if (article.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Kaydet",
                            tint = if (article.isBookmarked) accentBlue else secondaryTextColor
                        )
                    }
                    IconButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "${article.title}\n\nGündemAI ile oku: ${article.sourceUrl}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Haberi Paylaş"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Paylaş",
                            tint = secondaryTextColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bgColor)
            )
        },
        bottomBar = {
            Surface(
                color = bgColor,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = (0.5).dp, color = cardBorderColor)
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.sourceUrl))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
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
                                text = "Orijinal Habere Git (${article.sourceName})",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (!isProUser) AdMobTestAdaptiveBanner()
                }
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
                if (!article.imageUrl.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, cardBorderColor, RoundedCornerShape(14.dp))
                    ) {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = article.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.05f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.4f)
                                        )
                                    )
                                )
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
                        color = Color(0xFF60A5FA),
                        maxLines = 1
                    )
                    Text(
                        text = "•",
                        color = secondaryTextColor,
                        fontSize = 11.sp
                    )
                    Text(
                        text = article.publishedAtFormatted,
                        fontSize = 11.sp,
                        color = secondaryTextColor,
                        maxLines = 1
                    )
                }
            }

            // --- 3. MODULAR TAB SWITCHER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBgColor)
                    .border(1.dp, cardBorderColor, RoundedCornerShape(14.dp))
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI",
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Bu analiz, haber kaynaklarındaki veriler işlenerek yapay zekâ tarafından sentezlenmiştir.",
                                fontSize = 12.sp,
                                color = Color(0xFFE0E7FF),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // 1. Ne Oldu? (Kısa Özet)
                    AnalysisSectionCard(
                        title = "📌 Ne Oldu?",
                        icon = Icons.Default.Info,
                        iconTint = accentBlue
                    ) {
                        Text(
                            text = article.whatHappened.ifBlank { article.summary },
                            fontSize = 14.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 22.sp
                        )
                    }

                    // 2. Neden Önemli?
                    AnalysisSectionCard(
                        title = "💡 Neden Önemli?",
                        icon = Icons.Default.Lightbulb,
                        iconTint = Color(0xFFF59E0B)
                    ) {
                        Text(
                            text = article.whyImportant.ifBlank { "Bu gelişme bölgesel ve küresel ölçekte yakından takip edilmektedir." },
                            fontSize = 14.sp,
                            color = Color(0xFFE2E8F0),
                            lineHeight = 22.sp
                        )
                    }

                    // 3. Kesin Olarak Bilinenler
                    if (verifiedFacts.isNotEmpty()) {
                        AnalysisSectionCard(
                            title = "✅ Kesin Olarak Bilinenler",
                            icon = Icons.Default.CheckCircle,
                            iconTint = Color(0xFF10B981)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                verifiedFacts.forEach { fact ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("•", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                        Text(fact, fontSize = 13.sp, color = Color(0xFFE2E8F0), lineHeight = 19.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 4. Doğrulanmamış İddialar
                    if (unverifiedClaims.isNotEmpty()) {
                        AnalysisSectionCard(
                            title = "❓ Doğrulanmamış İddialar",
                            icon = Icons.Default.Help,
                            iconTint = Color(0xFFEC4899)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                unverifiedClaims.forEach { claim ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("•", fontWeight = FontWeight.Bold, color = Color(0xFFEC4899))
                                        Text(claim, fontSize = 13.sp, color = Color(0xFFE2E8F0), lineHeight = 19.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 5. Haberde Eksik Bilgiler
                    if (article.missingInformation.isNotBlank()) {
                        AnalysisSectionCard(
                            title = "🔍 Haberde Hangi Bilgiler Eksik?",
                            icon = Icons.Default.Search,
                            iconTint = Color(0xFF06B6D4)
                        ) {
                            Text(
                                text = article.missingInformation,
                                fontSize = 13.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 19.sp
                            )
                        }
                    }

                    // 6. Olası Etkiler
                    if (possibleImpacts.isNotEmpty()) {
                        AnalysisSectionCard(
                            title = "📈 Olası Etkiler",
                            icon = Icons.Default.TrendingUp,
                            iconTint = Color(0xFF8B5CF6)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                possibleImpacts.forEach { impact ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("•", fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                        Text(impact, fontSize = 13.sp, color = Color(0xFFE2E8F0), lineHeight = 19.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 7. Çelişkiler
                    if (contradictions.isNotEmpty()) {
                        AnalysisSectionCard(
                            title = "⚠️ Kaynaklar Arasındaki Çelişkiler",
                            icon = Icons.Default.Warning,
                            iconTint = Color(0xFFEF4444)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                contradictions.forEach { item ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("•", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                        Text(item, fontSize = 13.sp, color = Color(0xFFE2E8F0), lineHeight = 19.sp)
                                    }
                                }
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
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
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
                                color = Color(0xFFCBD5E1),
                                lineHeight = 25.sp,
                                fontWeight = FontWeight.Normal
                            )

                            if (article.whatHappened.isNotBlank() && article.whatHappened != article.summary) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = article.whatHappened,
                                    fontSize = 15.sp,
                                    color = Color(0xFFCBD5E1),
                                    lineHeight = 25.sp
                                )
                            }
                        }
                    }

                    // Source Timeline View
                    SourceTimelineView(
                        sourcesJson = article.sourcesJson,
                        onSourceClick = { url ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AnalysisSectionCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = Color(0xFF334155),
                shape = RoundedCornerShape(16.dp)
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
                    color = Color.White
                )
            }

            content()
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
