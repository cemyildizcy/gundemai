package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FollowedTopic
import com.example.data.model.SearchHistoryItem
import com.example.data.remote.RssFeedConfig
import com.example.data.remote.TelegramConfig
import com.example.ui.components.GlassSectionHeader
import com.example.ui.components.liquidGlassBackground

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExploreScreen(
    followedTopics: Set<String>,
    recentSearches: List<SearchHistoryItem>,
    onTopicClick: (topicId: String) -> Unit,
    onSearchTagClick: (query: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.background
    val cardBgColor = MaterialTheme.colorScheme.surface
    val cardBorderColor = MaterialTheme.colorScheme.outlineVariant
    val accentBlue = MaterialTheme.colorScheme.primary
    val primaryTextColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    var showAllTelegramSources by rememberSaveable { mutableStateOf(false) }
    var showAllRssSources by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .liquidGlassBackground(),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
        item {
            GlassSectionHeader(
                title = "Keşfet",
                subtitle = "İlgi alanlarını ve haber kaynaklarını düzenle.",
            )
        }

        // --- 1. SON ARAMALAR (RECENT SEARCHES) ---
        if (recentSearches.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Son Aramalar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentSearches.take(8).forEach { search ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(cardBgColor)
                                    .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                                    .clickable { onSearchTagClick(search.query) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = secondaryTextColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = search.query,
                                        fontSize = 12.sp,
                                        color = primaryTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. POPÜLER KONULAR VE TAKİP LİSTESİ ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor.copy(alpha = 0.78f)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "İlgi Alanların",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                    }

                    Text(
                        text = "Takip etmek istediğin konuları seç. Sana Özel akışı seçimlerine göre ve daima yeniden eskiye sıralanır.",
                        fontSize = 12.sp,
                        color = secondaryTextColor
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FollowedTopic.POPULAR_TOPICS.forEach { topic ->
                            val isFollowed = followedTopics.contains(topic.id)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isFollowed) accentBlue else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        1.dp,
                                        if (isFollowed) accentBlue else cardBorderColor,
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable { onTopicClick(topic.id) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                                    .testTag("topic_chip_${topic.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFollowed) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (isFollowed) Color.White else secondaryTextColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = topic.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isFollowed) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isFollowed) Color.White else primaryTextColor
                                    )
                                    Text(
                                        text = "• ${topic.category}",
                                        fontSize = 10.sp,
                                        color = if (isFollowed) Color.White.copy(alpha = 0.8f) else secondaryTextColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. HABER KAYNAKLARI (TELEGRAM & AJANSLAR GRUPLANDIRILMIŞ) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Kaynaklarda Ara",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )

                // Telegram Channels Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBgColor.copy(alpha = 0.78f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0088CC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Text(
                            text = "Telegram Kaynakları (${TelegramConfig.CHANNELS.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TelegramConfig.CHANNELS
                                .take(if (showAllTelegramSources) Int.MAX_VALUE else 6)
                                .forEach { channel ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0xFF0088CC).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFF0088CC).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                        .clickable { onSearchTagClick(channel.displayName) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF38BDF8))
                                        )
                                        Text(
                                            text = channel.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = primaryTextColor
                                        )
                                    }
                                }
                            }
                        }
                        if (TelegramConfig.CHANNELS.size > 6) {
                            TextButton(onClick = {
                                showAllTelegramSources = !showAllTelegramSources
                            }) {
                                Text(
                                    if (showAllTelegramSources) "Daha az göster"
                                    else "Tüm Telegram kaynaklarını göster"
                                )
                            }
                        }
                    }
                }

                // National & Global RSS Feeds Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBgColor.copy(alpha = 0.78f)),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RssFeed,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = "Haber Siteleri (${RssFeedConfig.SOURCES.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RssFeedConfig.SOURCES
                                .take(if (showAllRssSources) Int.MAX_VALUE else 8)
                                .forEach { source ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, cardBorderColor, RoundedCornerShape(18.dp))
                                        .clickable { onSearchTagClick(source.name) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF10B981))
                                        )
                                        Text(
                                            text = source.name,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = primaryTextColor
                                        )
                                    }
                                }
                            }
                        }
                        if (RssFeedConfig.SOURCES.size > 8) {
                            TextButton(onClick = {
                                showAllRssSources = !showAllRssSources
                            }) {
                                Text(
                                    if (showAllRssSources) "Daha az göster"
                                    else "Tüm haber sitelerini göster"
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. GÜNDEM AI AKILLI DOĞRULAMA SİSTEMİ ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Kaynak ve Analiz Şeffaflığı",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    Text(
                        text = "GündemAI her haberde kaynak sayısını açıkça gösterir; farklı anlatımlar bulunduğunda çelişkileri ve doğrulanmamış iddiaları analizde ayırır.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = 17.sp
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
        }
    }
}
