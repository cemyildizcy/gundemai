package com.example.data.remote

data class RssFeedSource(val name: String, val url: String, val category: String)

object RssFeedConfig {
    val SOURCES = listOf(
        RssFeedSource("TRT Haber Son Dakika", "https://www.trthaber.com/sondakika_articles.rss", "Son Dakika"),
        RssFeedSource("TRT Haber Gündem", "https://www.trthaber.com/gundem_articles.rss", "Türkiye"),
        RssFeedSource("Anadolu Ajansı Güncel", "https://www.aa.com.tr/tr/rss/default?cat=guncel", "Türkiye"),
        RssFeedSource("NTV Gündem", "https://www.ntv.com.tr/gundem.rss", "Türkiye"),
        RssFeedSource("BBC Türkçe", "https://feeds.bbci.co.uk/turkce/rss.xml", "Dünya"),
        RssFeedSource("ShiftDelete.Net", "https://shiftdelete.net/feed", "Teknoloji"),
        RssFeedSource("Webtekno", "https://www.webtekno.com/rss.xml", "Teknoloji"),
        RssFeedSource("DonanımHaber", "https://www.donanimhaber.com/rss/tum/", "Teknoloji"),
        RssFeedSource("TechCrunch AI", "https://techcrunch.com/category/artificial-intelligence/feed/", "Yapay Zekâ"),
        RssFeedSource("OpenAI News", "https://openai.com/news/rss.xml", "Yapay Zekâ"),
        RssFeedSource("MIT Technology Review AI", "https://www.technologyreview.com/topic/artificial-intelligence/feed", "Yapay Zekâ"),
        RssFeedSource("The Verge AI", "https://www.theverge.com/ai-artificial-intelligence/rss/index.xml", "Yapay Zekâ"),
        RssFeedSource("Google AI", "https://blog.google/technology/ai/rss/", "Yapay Zekâ"),
        RssFeedSource("Bloomberg HT", "https://www.bloomberght.com/rss", "Finans"),
        RssFeedSource("NTV Ekonomi", "https://www.ntv.com.tr/ekonomi.rss", "Ekonomi"),
        RssFeedSource("TRT Haber Ekonomi", "https://www.trthaber.com/ekonomi_articles.rss", "Ekonomi"),
        RssFeedSource("Koin Bülteni", "https://koinbulteni.com/feed", "Kripto"),
        RssFeedSource("TRT Spor", "https://www.trtspor.com.tr/rss/", "Spor"),
        RssFeedSource("NTV Spor", "https://www.ntvspor.net/rss", "Spor"),
        RssFeedSource("Fanatik", "https://www.fanatik.com.tr/rss/", "Spor"),
        RssFeedSource("Evrim Ağacı", "https://evrimagaci.org/rss.xml", "Bilim"),
        RssFeedSource("Webrazzi", "https://webrazzi.com/feed/", "Girişimcilik"),
        RssFeedSource("NTV Sanat", "https://www.ntv.com.tr/sanat.rss", "Kültür ve Sanat"),
        RssFeedSource("NTV Sağlık", "https://www.ntv.com.tr/saglik.rss", "Sağlık"),
        RssFeedSource("BBC World", "https://feeds.bbci.co.uk/news/world/rss.xml", "Dünya"),
        RssFeedSource("DW Türkçe", "https://rss.dw.com/xml/rss-tur-all", "Dünya")
    )
}

data class TelegramChannelConfig(
    val channelHandle: String,
    val displayName: String,
    val category: String,
    val alternativeHandles: List<String> = emptyList()
) {
    val allHandles: List<String> get() = (listOf(channelHandle) + alternativeHandles).distinct()
}

object TelegramConfig {
    val CHANNELS = listOf(
        TelegramChannelConfig("pusholder", "Pusholder", "Son Dakika"),
        TelegramChannelConfig("bpthaber", "BPT", "Son Dakika"),
        TelegramChannelConfig("solcugazete", "Solcu Gazete", "Son Dakika"),
        TelegramChannelConfig("darkwebhaber", "DarkWeb Haber", "Son Dakika"),
        TelegramChannelConfig("trthaber", "TRT Haber", "Türkiye"),
        TelegramChannelConfig("anadoluajansi", "Anadolu Ajansı", "Türkiye"),
        TelegramChannelConfig("ntvhaber", "NTV Haber", "Türkiye"),
        TelegramChannelConfig("webtekno", "Webtekno", "Teknoloji"),
        TelegramChannelConfig("shiftdeletenet", "ShiftDelete", "Teknoloji"),
        TelegramChannelConfig("donanimhaber", "DonanımHaber", "Teknoloji")
    )
}
