package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SourceTimelineItem(
    val sourceName: String,
    val sourceType: String = "Haber Kaynağı", // e.g., "Resmî Açıklama", "Haber Ajansı", "Teknoloji Blogu"
    val publishedAtTime: String, // e.g. "10:04" or "22 Temmuz 10:04"
    val url: String,
    val headline: String
)
