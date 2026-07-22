package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserNotification

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationsScreen(
    notifications: List<UserNotification>,
    followedCategories: Set<String>,
    onCategoryToggle: (categoryName: String) -> Unit,
    onNotificationClick: (articleId: String?) -> Unit,
    onMarkRead: (id: String) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = Color(0xFF0F172A)
    val cardBgColor = Color(0xFF1E293B)
    val cardBorderColor = Color(0xFF334155)
    val accentBlue = Color(0xFF3B82F6)
    val primaryTextColor = Color(0xFFF8FAFC)
    val secondaryTextColor = Color(0xFF94A3B8)

    var selectedFilter by remember { mutableStateOf("Tümü") }
    var isSettingsExpanded by remember { mutableStateOf(true) }

    val allCategoryOptions = listOf(
        "Son Dakika", "Yapay Zekâ", "Teknoloji", "Ekonomi", "Finans", "Kripto", "Spor", "Transfer", "Türkiye", "Dünya", "Bilim"
    )

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    val filteredNotifications = remember(notifications, selectedFilter) {
        when (selectedFilter) {
            "Okunmamış" -> notifications.filter { !it.isRead }
            "Son Dakika" -> notifications.filter { it.type == "BREAKING_NEWS" }
            else -> notifications
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- HEADER BANNER ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = accentBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Anında Bildirimler",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryTextColor
                        )
                    }

                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$unreadCount Yeni",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFCA5A5)
                            )
                        }
                    }
                }

                Text(
                    text = "Dilediğiniz kategorileri seçerek anlık sıcak haber ve doğrulama bildirimlerini aktifleştirin.",
                    fontSize = 13.sp,
                    color = secondaryTextColor
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = 0.12f))
                        .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Arka Plan Servisi (WorkManager) Aktif: Bildirimler ana performansı etkilemeden arka planda işlenir.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF93C5FD)
                    )
                }
            }
        }

        // --- 1. KATEGORİ BİLDİRİM TERCİHLERİ CARD ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSettingsExpanded = !isSettingsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Kategoriye Göre Anında Bildirimler",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                        }

                        Icon(
                            imageVector = if (isSettingsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = secondaryTextColor
                        )
                    }

                    if (isSettingsExpanded) {
                        Divider(color = cardBorderColor)

                        Text(
                            text = "Seçilen kategorilerde yeni bir haber doğrulandığında anında bildirim alırsınız:",
                            fontSize = 12.sp,
                            color = secondaryTextColor
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allCategoryOptions.forEach { category ->
                                val isEnabled = followedCategories.contains(category)

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isEnabled) Color(0xFF10B981) else Color(0xFF0F172A))
                                        .border(
                                            1.dp,
                                            if (isEnabled) Color(0xFF10B981) else cardBorderColor,
                                            RoundedCornerShape(20.dp)
                                        )
                                        .clickable { onCategoryToggle(category) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                        .testTag("notif_cat_toggle_$category")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                            contentDescription = null,
                                            tint = if (isEnabled) Color.White else secondaryTextColor,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = category,
                                            fontSize = 12.sp,
                                            fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isEnabled) Color.White else primaryTextColor
                                        )
                                    }
                                }
                            }
                        }

                    }
                }
            }
        }

        // --- 2. BİLDİRİM GEÇMİŞİ FİLTRELERİ & ACTIONS ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Tümü", "Okunmamış", "Son Dakika").forEach { filter ->
                        val isSel = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSel) accentBlue else cardBgColor)
                                .border(1.dp, if (isSel) accentBlue else cardBorderColor, RoundedCornerShape(16.dp))
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else secondaryTextColor
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (unreadCount > 0) {
                        IconButton(
                            onClick = onMarkAllRead,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Tümünü Okundu İşaretle",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (notifications.isNotEmpty()) {
                        IconButton(
                            onClick = onClearAll,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Tümünü Temizle",
                                tint = secondaryTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. BİLDİRİM LİSTESİ ---
        if (filteredNotifications.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBgColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = null,
                            tint = secondaryTextColor,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Bildirim Bulunmuyor",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Seçtiğiniz kategorilerde yeni canlı gelişmeler olduğunda anında burada görüntülenecektir.",
                            fontSize = 12.sp,
                            color = secondaryTextColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = filteredNotifications,
                key = { notif -> notif.id }
            ) { notif ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (notif.isRead) cardBgColor.copy(alpha = 0.6f) else cardBgColor
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (notif.isRead) cardBorderColor.copy(alpha = 0.5f) else accentBlue.copy(alpha = 0.4f),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onMarkRead(notif.id)
                            onNotificationClick(notif.articleId)
                        }
                        .testTag("notification_item_${notif.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (notif.type == "BREAKING_NEWS") Color(0xFFEF4444).copy(alpha = 0.15f)
                                    else accentBlue.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (notif.type == "BREAKING_NEWS") Icons.Default.Bolt else Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (notif.type == "BREAKING_NEWS") Color(0xFFEF4444) else accentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (!notif.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(accentBlue)
                                        )
                                    }
                                    Text(
                                        text = notif.title,
                                        fontWeight = if (notif.isRead) FontWeight.Medium else FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = primaryTextColor
                                    )
                                }

                                Text(
                                    text = notif.timestampFormatted,
                                    fontSize = 11.sp,
                                    color = secondaryTextColor
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = notif.body,
                                fontSize = 12.sp,
                                color = secondaryTextColor,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
