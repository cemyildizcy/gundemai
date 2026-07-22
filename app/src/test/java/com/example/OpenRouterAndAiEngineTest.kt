package com.example

import com.example.data.model.AiAnalysisResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class OpenRouterAndAiEngineTest {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun testAiAnalysisResultJsonParsing() {
        val sampleJson = """
            {
              "title": "Samsung Galaxy Z Flip 8 Tanıtıldı: Teknik Özellikleri ve Fiyatı",
              "summary": "Samsung Galaxy Z Flip 8; 6.7 inç AMOLED ekran, Snapdragon 8 Gen 3 ve 999$ fiyatla duyuruldu.",
              "category": "Teknoloji",
              "what_happened": "Samsung Unpacked etkinliğinde yeni katlanabilir modelini tanıttı.",
              "why_important": "Katlanabilir pazarındaki amiral gemisi rekabetini artırıyor.",
              "missing_information": "Türkiye teslimat tarihi henüz açıklanmadı.",
              "verification_status": "MULTI_SOURCE_CONFIRMED",
              "confidence_score": 98,
              "possible_impacts": ["Eski modellerde indirim bekleniyor"],
              "unverified_claims": [],
              "contradictions": [],
              "verified_facts": ["Başlangıç Fiyatı: 999 USD", "Ekran: 6.7 inç 120Hz AMOLED"]
            }
        """.trimIndent()

        val adapter = moshi.adapter(AiAnalysisResult::class.java)
        val result = adapter.fromJson(sampleJson)

        assertNotNull(result)
        assertEquals("Samsung Galaxy Z Flip 8 Tanıtıldı: Teknik Özellikleri ve Fiyatı", result?.title)
        assertEquals("Teknoloji", result?.category)
        assertEquals(98, result?.confidenceScore)
        assertEquals(2, result?.verifiedFacts?.size)
        assertEquals("Başlangıç Fiyatı: 999 USD", result?.verifiedFacts?.get(0))
    }
}
