package com.example.data.remote

import com.example.data.model.NewsArticle
import com.example.data.model.SourceTimelineItem
import com.example.util.DateUtils
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ReadyNewsApiService {
    @GET("v1/news")
    suspend fun getReadyNews(
        @Query("category") category: String? = null,
        @Query("limit") limit: Int = 100
    ): ReadyNewsResponse
}

interface BackendDiscoveryApiService {
    @GET("backend.json")
    suspend fun getBackend(): BackendDiscoveryDto
}

@JsonClass(generateAdapter = true)
data class BackendDiscoveryDto(
    val apiBaseUrl: String
)

@JsonClass(generateAdapter = true)
data class ReadyNewsResponse(
    val articles: List<ReadyNewsDto>,
    val sharedAnalysis: Boolean,
    val generatedAt: Long
)

@JsonClass(generateAdapter = true)
data class ReadySourceDto(
    val name: String,
    val url: String,
    val publishedAt: Long,
    val headline: String? = null
)

@JsonClass(generateAdapter = true)
data class ReadyNewsDto(
    val id: String,
    val status: String,
    val title: String,
    val summary: String,
    val category: String,
    val imageUrl: String? = null,
    val sourceName: String,
    val sourceUrl: String,
    val sourceCount: Int,
    val sources: List<ReadySourceDto>,
    val publishedAt: Long,
    val readyAt: Long,
    val whatHappened: String,
    val whyImportant: String,
    val missingInformation: String,
    val verificationStatus: String,
    val confidenceScore: Int,
    val possibleImpacts: List<String>,
    val unverifiedClaims: List<String>,
    val contradictions: List<String>,
    val verifiedFacts: List<String>,
    val analysisVersion: Int
)

object ServerCategory {
    private val canonicalToDisplay = mapOf(
        "Son Dakika" to "Son Dakika",
        "Yapay Zeka" to "Yapay Zekâ",
        "Teknoloji" to "Teknoloji",
        "Turkiye" to "Türkiye",
        "Dunya" to "Dünya",
        "Ekonomi" to "Ekonomi",
        "Finans" to "Finans",
        "Kripto" to "Kripto",
        "Spor" to "Spor",
        "Transfer" to "Transfer",
        "Bilim" to "Bilim",
        "Oyun" to "Oyun",
        "Girisimcilik" to "Girişimcilik",
        "Kultur ve Sanat" to "Kültür ve Sanat",
        "Saglik" to "Sağlık"
    )

    fun toDisplayName(canonical: String): String = canonicalToDisplay[canonical] ?: canonical

    fun toCanonicalName(display: String): String = canonicalToDisplay.entries
        .firstOrNull { it.value == display }
        ?.key
        ?: display
}

private val mapperMoshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val stringListAdapter = mapperMoshi.adapter<List<String>>(
    Types.newParameterizedType(List::class.java, String::class.java)
)

private val timelineListAdapter = mapperMoshi.adapter<List<SourceTimelineItem>>(
    Types.newParameterizedType(List::class.java, SourceTimelineItem::class.java)
)

fun ReadyNewsDto.toEntity(isBookmarked: Boolean = false): NewsArticle {
    require(status == "READY") { "Only READY articles may be displayed" }

    val timeline = sources.map { source ->
        SourceTimelineItem(
            sourceName = source.name,
            publishedAtTime = DateUtils.formatClockTime(source.publishedAt),
            url = source.url,
            headline = source.headline ?: title
        )
    }

    return NewsArticle(
        id = id,
        title = title.trim(),
        summary = summary.trim(),
        category = ServerCategory.toDisplayName(category),
        imageUrl = imageUrl,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        publishedAt = publishedAt,
        publishedAtFormatted = DateUtils.formatRelativeTime(publishedAt),
        whatHappened = whatHappened.trim(),
        whyImportant = whyImportant.trim(),
        missingInformation = missingInformation.trim(),
        verificationStatus = verificationStatus,
        confidenceScore = confidenceScore.coerceIn(0, 100),
        sourceCount = sourceCount.coerceAtLeast(sources.size).coerceAtLeast(1),
        sourcesJson = timelineListAdapter.toJson(timeline),
        possibleImpactsJson = stringListAdapter.toJson(possibleImpacts),
        unverifiedClaimsJson = stringListAdapter.toJson(unverifiedClaims),
        contradictionsJson = stringListAdapter.toJson(contradictions),
        verifiedFactsJson = stringListAdapter.toJson(verifiedFacts),
        isBookmarked = isBookmarked,
        isBreaking = category == "Son Dakika",
        isAiAnalyzed = true,
        topicTagsJson = stringListAdapter.toJson(listOf(ServerCategory.toDisplayName(category)))
    )
}
