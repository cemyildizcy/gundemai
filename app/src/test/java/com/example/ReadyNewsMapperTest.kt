package com.example

import com.example.data.remote.ReadyNewsDto
import com.example.data.remote.ReadySourceDto
import com.example.data.remote.toEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadyNewsMapperTest {
    @Test
    fun `ready server article maps to a fully analyzed local article`() {
        val dto = ReadyNewsDto(
            id = "event-42",
            status = "READY",
            title = "TCMB faiz kararını açıkladı",
            summary = "Merkez Bankası politika faizine ilişkin yeni kararını yayımladı.",
            category = "Turkiye",
            imageUrl = "https://cdn.example.com/image.jpg",
            sourceName = "TCMB",
            sourceUrl = "https://example.com/news",
            sourceCount = 2,
            sources = listOf(
                ReadySourceDto("TCMB", "https://tcmb.gov.tr/duyuru", 1_721_630_000_000, "PPK kararı"),
                ReadySourceDto("Reuters", "https://reuters.com/story", 1_721_630_060_000, "Turkey rates")
            ),
            publishedAt = 1_721_630_000_000,
            readyAt = 1_721_630_120_000,
            whatHappened = "Karar metni resmi internet sitesinde yayımlandı.",
            whyImportant = "Karar kredi ve mevduat maliyetlerini doğrudan etkileyebilir.",
            missingInformation = "Kararın piyasa fiyatlarına kalıcı etkisi henüz bilinmiyor.",
            verificationStatus = "OFFICIAL_CONFIRMED",
            confidenceScore = 96,
            possibleImpacts = listOf("Kredi faizleri değişebilir"),
            unverifiedClaims = emptyList(),
            contradictions = emptyList(),
            verifiedFacts = listOf("Karar TCMB tarafından yayımlandı"),
            analysisVersion = 1
        )

        val article = dto.toEntity(isBookmarked = true)

        assertEquals("Türkiye", article.category)
        assertEquals("OFFICIAL_CONFIRMED", article.verificationStatus)
        assertEquals(96, article.confidenceScore)
        assertEquals(2, article.sourceCount)
        assertTrue(article.isAiAnalyzed)
        assertTrue(article.isBookmarked)

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val listType = Types.newParameterizedType(List::class.java, String::class.java)
        val facts = moshi.adapter<List<String>>(listType).fromJson(article.verifiedFactsJson)
        assertEquals(listOf("Karar TCMB tarafından yayımlandı"), facts)
        assertTrue(article.sourcesJson.contains("TCMB"))
        assertTrue(article.sourcesJson.contains("Reuters"))
    }

    @Test
    fun `canonical server categories map to exact app labels`() {
        val pairs = mapOf(
            "Yapay Zeka" to "Yapay Zekâ",
            "Turkiye" to "Türkiye",
            "Dunya" to "Dünya",
            "Girisimcilik" to "Girişimcilik",
            "Kultur ve Sanat" to "Kültür ve Sanat",
            "Saglik" to "Sağlık"
        )

        pairs.forEach { (canonical, display) ->
            assertEquals(display, com.example.data.remote.ServerCategory.toDisplayName(canonical))
            assertEquals(canonical, com.example.data.remote.ServerCategory.toCanonicalName(display))
        }
    }

    @Test
    fun `server source count is not inflated by duplicate publisher URLs`() {
        val dto = ReadyNewsDto(
            id = "event-single-publisher",
            status = "READY",
            title = "Aynı yayıncıdan güncellenen haber",
            summary = "Aynı yayıncının iki farklı bağlantısı tek bağımsız kaynak olarak değerlendirilir.",
            category = "Turkiye",
            sourceName = "NTV Gündem",
            sourceUrl = "https://www.ntv.com.tr/turkiye/haber-1",
            sourceCount = 1,
            sources = listOf(
                ReadySourceDto("NTV Gündem", "https://www.ntv.com.tr/turkiye/haber-1", 1_721_630_000_000),
                ReadySourceDto("NTV Gündem", "https://www.ntv.com.tr/turkiye/haber-2", 1_721_630_060_000)
            ),
            publishedAt = 1_721_630_000_000,
            readyAt = 1_721_630_120_000,
            whatHappened = "Yayıncı aynı olayı iki farklı bağlantıda aktardı.",
            whyImportant = "Bağımsız kaynak sayısı doğrulama etiketinin güvenilirliğini doğrudan etkiler.",
            missingInformation = "İkinci bağımsız bir yayıncı bulunmuyor.",
            verificationStatus = "SINGLE_SOURCE_REPORT",
            confidenceScore = 70,
            possibleImpacts = emptyList(),
            unverifiedClaims = emptyList(),
            contradictions = emptyList(),
            verifiedFacts = listOf("İki bağlantı da aynı yayıncıya ait."),
            analysisVersion = 3
        )

        assertEquals(1, dto.toEntity().sourceCount)
    }

    @Test
    fun `feed endpoint is never treated as an article image`() {
        val dto = ReadyNewsDto(
            id = "event-invalid-image",
            status = "READY",
            title = "Görseli olmayan haber",
            summary = "Kaynak akış adresi görsel olarak gösterilmemelidir.",
            category = "Teknoloji",
            imageUrl = "https://shiftdelete.net/feed",
            sourceName = "ShiftDelete.Net",
            sourceUrl = "https://shiftdelete.net/haber",
            sourceCount = 1,
            sources = listOf(
                ReadySourceDto("ShiftDelete.Net", "https://shiftdelete.net/haber", 1_721_630_000_000)
            ),
            publishedAt = 1_721_630_000_000,
            readyAt = 1_721_630_120_000,
            whatHappened = "Yeni bir haber yayımlandı.",
            whyImportant = "Boş görsel alanları kullanıcı deneyimini etkiler.",
            missingInformation = "",
            verificationStatus = "SINGLE_SOURCE_REPORT",
            confidenceScore = 70,
            possibleImpacts = emptyList(),
            unverifiedClaims = emptyList(),
            contradictions = emptyList(),
            verifiedFacts = emptyList(),
            analysisVersion = 3
        )

        assertNull(dto.toEntity().imageUrl)
    }
}
