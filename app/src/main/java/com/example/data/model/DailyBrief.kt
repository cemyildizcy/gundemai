package com.example.data.model

data class DailyBrief(
    val dateKey: String,
    val title: String,
    val summary: String,
    val items: List<DailyBriefItem>,
    val generatedAt: Long,
)

data class DailyBriefItem(
    val articleId: String,
    val title: String,
    val summary: String,
    val category: String,
    val publishedAt: Long,
)
