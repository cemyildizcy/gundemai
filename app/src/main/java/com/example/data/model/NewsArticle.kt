package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "news_articles")
data class NewsArticle(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val category: String,
    val imageUrl: String? = null,
    val sourceName: String,
    val sourceUrl: String,
    val publishedAt: Long,
    val publishedAtFormatted: String,
    val whatHappened: String,
    val whyImportant: String,
    val missingInformation: String = "Resmî makamların ayrıntılı bildirimi beklenmektedir.",
    val verificationStatus: String = "MULTI_SOURCE_CONFIRMED",
    val confidenceScore: Int = 88,
    val sourceCount: Int = 1,
    val sourcesJson: String = "[]",
    val possibleImpactsJson: String = "[]",
    val unverifiedClaimsJson: String = "[]",
    val contradictionsJson: String = "[]",
    val verifiedFactsJson: String = "[]",
    val isBookmarked: Boolean = false,
    val isBreaking: Boolean = false,
    val isAiAnalyzed: Boolean = true,
    val topicTagsJson: String = "[]"
)
