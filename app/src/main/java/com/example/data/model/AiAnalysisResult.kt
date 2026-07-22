package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AiAnalysisResult(
    @Json(name = "title") val title: String,
    @Json(name = "summary") val summary: String,
    @Json(name = "what_happened") val whatHappened: String,
    @Json(name = "why_important") val whyImportant: String,
    @Json(name = "missing_information") val missingInformation: String = "Süreç devam ediyor, resmî detaylar bekleniyor.",
    @Json(name = "verification_status") val verificationStatus: String = "MULTI_SOURCE_CONFIRMED",
    @Json(name = "confidence_score") val confidenceScore: Int = 85,
    @Json(name = "possible_impacts") val possibleImpacts: List<String> = emptyList(),
    @Json(name = "unverified_claims") val unverifiedClaims: List<String> = emptyList(),
    @Json(name = "contradictions") val contradictions: List<String> = emptyList(),
    @Json(name = "verified_facts") val verifiedFacts: List<String> = emptyList(),
    @Json(name = "category") val category: String? = null
)
