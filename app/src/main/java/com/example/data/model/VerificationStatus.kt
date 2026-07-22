package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

enum class VerificationStatus(val label: String, val badgeColor: Color) {
    OFFICIAL_CONFIRMED("Resmî Doğrulandı", StatusOfficialConfirmed),
    MULTI_SOURCE_CONFIRMED("Çoklu Kaynak Tarafından Doğrulandı", StatusMultiSourceConfirmed),
    SINGLE_SOURCE_REPORT("Tek Kaynak İddiası", StatusSingleSourceReport),
    UNVERIFIED_CLAIM("Doğrulanmamış İddia", StatusUnverifiedClaim),
    DEVELOPING_STORY("Gelişmekte Olan Haber", StatusDevelopingStory),
    SOURCES_CONFLICT("Kaynaklar Arasında Çelişki Var", StatusSourcesConflict),
    INSUFFICIENT_INFORMATION("Yetersiz Bilgi", StatusInsufficientInfo);

    companion object {
        fun fromString(status: String?): VerificationStatus {
            return entries.firstOrNull { it.name.equals(status, ignoreCase = true) }
                ?: MULTI_SOURCE_CONFIRMED
        }
    }
}
