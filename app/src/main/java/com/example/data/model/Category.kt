package com.example.data.model

data class Category(
    val id: String,
    val displayName: String,
    val iconName: String = "Article"
) {
    companion object {
        val ALL_CATEGORIES = listOf(
            Category("SANA_OZEL", "Sana Özel", "AutoAwesome"),
            Category("SON_DAKIKA", "Son Dakika", "Bolt"),
            Category("YAPAY_ZEKA", "Yapay Zekâ", "Psychology"),
            Category("TEKNOLOJI", "Teknoloji", "Devices"),
            Category("TURKIYE", "Türkiye", "Flag"),
            Category("DUNYA", "Dünya", "Public"),
            Category("EKONOMI", "Ekonomi", "TrendingUp"),
            Category("SPOR", "Spor", "SportsSoccer"),
            Category("TRANSFER", "Transfer", "SwapHoriz"),
            Category("BILIM", "Bilim", "Science"),
            Category("OYUN", "Oyun", "SportsEsports"),
            Category("GIRISIMCILIK", "Girişimcilik", "RocketLaunch"),
            Category("FINANS", "Finans", "AccountBalance"),
            Category("KRIPTO", "Kripto", "CurrencyBitcoin"),
            Category("KULTUR_SANAT", "Kültür ve Sanat", "Palette"),
            Category("SAGLIK", "Sağlık", "HealthAndSafety")
        )
    }
}

data class FollowedTopic(
    val id: String,
    val name: String,
    val category: String,
    val isFollowed: Boolean = false
) {
    companion object {
        val POPULAR_TOPICS = listOf(
            FollowedTopic("openai", "OpenAI", "Yapay Zekâ", true),
            FollowedTopic("google", "Google", "Teknoloji", true),
            FollowedTopic("anthropic", "Anthropic", "Yapay Zekâ", false),
            FollowedTopic("gemini", "Gemini", "Yapay Zekâ", true),
            FollowedTopic("fenerbahce", "Fenerbahçe", "Spor", true),
            FollowedTopic("galatasaray", "Galatasaray", "Spor", false),
            FollowedTopic("yapay_zeka_trendler", "Yapay zekâ", "Yapay Zekâ", true),
            FollowedTopic("turkiye_ekonomisi", "Türkiye ekonomisi", "Ekonomi", false),
            FollowedTopic("apple", "Apple", "Teknoloji", false),
            FollowedTopic("nvidia", "NVIDIA", "Teknoloji", false),
            FollowedTopic("bitcoin", "Bitcoin", "Kripto", false),
            FollowedTopic("bist100", "BIST 100", "Finans", false)
        )
    }
}
