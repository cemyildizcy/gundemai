package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GundemDesignTokens
import com.example.ui.theme.BrandTeal

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String,
)

val BOTTOM_NAV_ITEMS = listOf(
    NavItem("Gündem", Icons.Filled.Newspaper, Icons.Outlined.Newspaper, "nav_gundem"),
    NavItem("Keşfet", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_kesfet"),
    NavItem("Kaydedilen", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder, "nav_kaydedilenler"),
    NavItem("Bildirim", Icons.Filled.Notifications, Icons.Outlined.Notifications, "nav_bildirimler"),
    NavItem("Profil", Icons.Filled.Person, Icons.Outlined.Person, "nav_profil"),
)

@Composable
fun GundemBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
            shape = RoundedCornerShape(GundemDesignTokens.navigationRadius),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = GundemDesignTokens.floatingElevation,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BOTTOM_NAV_ITEMS.forEachIndexed { index, item ->
                    val selected = selectedTab == index
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) BrandTeal
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(GundemDesignTokens.motionFastMs),
                        label = "nav_color",
                    )
                    Surface(
                        onClick = { onTabSelected(index) },
                        shape = RoundedCornerShape(24.dp),
                        color = if (selected) BrandTeal.copy(alpha = 0.13f) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .testTag(item.tag),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = contentColor,
                                modifier = Modifier.size(21.dp),
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = contentColor,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}
