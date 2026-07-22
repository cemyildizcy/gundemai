package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.VerificationStatus

@Composable
fun VerificationBadge(
    statusString: String,
    confidenceScore: Int,
    modifier: Modifier = Modifier
) {
    val status = VerificationStatus.fromString(statusString)
    val color = status.badgeColor
    val shortLabel = when (status) {
        VerificationStatus.OFFICIAL_CONFIRMED -> "Resmî"
        VerificationStatus.MULTI_SOURCE_CONFIRMED -> "Çoklu kaynak"
        VerificationStatus.SINGLE_SOURCE_REPORT -> "Tek kaynak"
        VerificationStatus.UNVERIFIED_CLAIM -> "Doğrulanmadı"
        VerificationStatus.DEVELOPING_STORY -> "Gelişiyor"
        VerificationStatus.SOURCES_CONFLICT -> "Kaynak çelişkisi"
        VerificationStatus.INSUFFICIENT_INFORMATION -> "Yetersiz bilgi"
    }
    val icon = when (status) {
        VerificationStatus.OFFICIAL_CONFIRMED,
        VerificationStatus.MULTI_SOURCE_CONFIRMED -> Icons.Default.CheckCircle
        VerificationStatus.SINGLE_SOURCE_REPORT,
        VerificationStatus.UNVERIFIED_CLAIM,
        VerificationStatus.SOURCES_CONFLICT -> Icons.Default.Warning
        else -> Icons.Default.Info
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "${status.label}, yüzde $confidenceScore güven",
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = shortLabel,
            style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1
        )
    }
}
