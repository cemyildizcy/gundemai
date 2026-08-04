package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import com.example.data.model.NewsArticle
import com.example.ui.components.NewsCard
import com.example.ui.components.GlassEmptyState
import com.example.ui.components.liquidGlassBackground

@Composable
fun BookmarksScreen(
    bookmarkedArticles: List<NewsArticle>,
    onArticleClick: (articleId: String) -> Unit,
    onBookmarkToggle: (articleId: String, currentStatus: Boolean) -> Unit,
    onBrowseNews: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgColor = MaterialTheme.colorScheme.background
    val cardBgColor = MaterialTheme.colorScheme.surface
    val cardBorderColor = MaterialTheme.colorScheme.outlineVariant
    val accentBlue = MaterialTheme.colorScheme.primary
    val primaryTextColor = MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Tümü") }

    val categories = remember(bookmarkedArticles) {
        val cats = bookmarkedArticles.map { it.category }.distinct().filter { it.isNotBlank() }
        listOf("Tümü") + cats
    }

    val filteredArticles = remember(bookmarkedArticles, searchQuery, selectedCategory) {
        bookmarkedArticles.filter { article ->
            val matchesCategory = (selectedCategory == "Tümü" || article.category.equals(selectedCategory, ignoreCase = true))
            val matchesSearch = searchQuery.isBlank() || 
                article.title.contains(searchQuery, ignoreCase = true) || 
                article.summary.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .liquidGlassBackground()
    ) {
        if (bookmarkedArticles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassEmptyState(
                    icon = Icons.Default.BookmarkBorder,
                    title = "Henüz kaydedilmiş haber yok",
                    message = "İlgini çeken haberleri kaydet; çevrimdışıyken bile yeniden okuyabilirsin.",
                    actionLabel = "Gündeme göz at",
                    onAction = onBrowseNews,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Banner
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
                                    imageVector = Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = accentBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Kaydedilenler",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryTextColor
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(accentBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${bookmarkedArticles.size} Kayıtlı Haber",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentBlue
                                )
                            }
                        }

                        // Offline Reader Status Pill
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Kaydettiğiniz haberleri internet bağlantısı olmadan da okuyabilirsiniz.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Search Field inside Bookmarks
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Kaydedilenlerde ara...", color = secondaryTextColor, fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = secondaryTextColor, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = cardBgColor,
                                unfocusedContainerColor = cardBgColor,
                                focusedBorderColor = accentBlue,
                                unfocusedBorderColor = cardBorderColor,
                                focusedTextColor = primaryTextColor,
                                unfocusedTextColor = primaryTextColor
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )

                        // Category Chips
                        if (categories.size > 1) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(categories) { cat ->
                                    val isSelected = selectedCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(if (isSelected) accentBlue else cardBgColor)
                                            .border(1.dp, if (isSelected) accentBlue else cardBorderColor, RoundedCornerShape(18.dp))
                                            .clickable { selectedCategory = cat }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else secondaryTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Filtered Articles List
                if (filteredArticles.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aramanıza veya filtrenize uygun kayıtlı haber bulunamadı.",
                                fontSize = 13.sp,
                                color = secondaryTextColor
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredArticles,
                        key = { article -> article.id }
                    ) { article ->
                        NewsCard(
                            article = article,
                            onClick = { onArticleClick(article.id) },
                        )
                    }
                }
            }
        }
    }
}
