package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.VerificationStatus

@Composable
fun VerificationBadge(
    statusString: String,
    confidenceScore: Int,
    modifier: Modifier = Modifier
) {
    val status = VerificationStatus.fromString(statusString)
    val color = status.badgeColor

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val icon = when (status) {
            VerificationStatus.OFFICIAL_CONFIRMED,
            VerificationStatus.MULTI_SOURCE_CONFIRMED -> Icons.Default.CheckCircle
            VerificationStatus.SINGLE_SOURCE_REPORT,
            VerificationStatus.UNVERIFIED_CLAIM,
            VerificationStatus.SOURCES_CONFLICT -> Icons.Default.Warning
            else -> Icons.Default.Info
        }

        Icon(
            imageVector = icon,
            contentDescription = status.label,
            tint = color,
            modifier = Modifier.size(14.dp)
        )

        Text(
            text = "${status.label} • %$confidenceScore",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
