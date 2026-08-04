package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.FollowedTopic
import com.example.ui.components.liquidGlassBackground
import com.example.ui.theme.BrandTeal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    onComplete: (selectedCategories: Set<String>, selectedTopics: Set<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategories by remember {
        mutableStateOf(setOf("Sana Özel", "Son Dakika", "Yapay Zekâ", "Teknoloji", "Türkiye"))
    }

    var selectedTopics by remember {
        mutableStateOf(setOf("openai", "google", "gemini", "fenerbahce"))
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.padding(16.dp)
            ) {
                Surface(
                    onClick = { onComplete(selectedCategories, selectedTopics) },
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(BrandTeal, RoundedCornerShape(18.dp))
                        .testTag("onboarding_start_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "GündemAI'ye Başla",
                            fontSize = 16.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .liquidGlassBackground()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.gundemai_logo_mark),
                            contentDescription = "GündemAI Logo",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "GündemAI'ye Hoş Geldiniz",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Text(
                    text = "Kaynakları görünür, ortak yapay zekâ analiziyle hazırlanan haber akışınızı ilgi alanlarınıza göre düzenleyin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Categories Section
            Text(
                text = "1. İlgi Duyduğunuz Kategoriler",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Category.INTEREST_CATEGORIES.forEach { category ->
                    val isSelected = selectedCategories.contains(category.displayName)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(18.dp)
                            )
                            .clickable {
                                selectedCategories = if (isSelected) {
                                    selectedCategories - category.displayName
                                } else {
                                    selectedCategories + category.displayName
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = category.displayName,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Topics Section
            Text(
                text = "2. Takip Etmek İstediğiniz Konu ve Kurumlar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FollowedTopic.POPULAR_TOPICS.forEach { topic ->
                    val isSelected = selectedTopics.contains(topic.id)

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTopics = if (isSelected) {
                                selectedTopics - topic.id
                            } else {
                                selectedTopics + topic.id
                            }
                        },
                        label = { Text(topic.name, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        shape = RoundedCornerShape(18.dp)
                    )
                }
            }
        }
    }
}
