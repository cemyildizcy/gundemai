package com.example

import com.example.data.remote.DailyBriefDto
import com.example.data.remote.DailyBriefItemDto
import com.example.data.remote.toModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyBriefMapperTest {
    private val items = listOf(
        DailyBriefItemDto("news-1", "Birinci haber", "Birinci haber kısa özeti", "Dunya", 1L),
        DailyBriefItemDto("news-2", "İkinci haber", "İkinci haber kısa özeti", "Ekonomi", 2L),
        DailyBriefItemDto("news-3", "Üçüncü haber", "Üçüncü haber kısa özeti", "Teknoloji", 3L),
    )

    @Test
    fun `shared daily brief maps to one common app model`() {
        val result = DailyBriefDto(
            dateKey = "2026-07-30",
            title = "Bugünün Gündemi",
            summary = "Bugünün en kritik gelişmelerinin herkese gösterilen ortak özeti.",
            items = items,
            generatedAt = 42L,
            shared = true,
        ).toModel()

        requireNotNull(result)
        assertEquals("Dünya", result.items.first().category)
        assertEquals(listOf("news-1", "news-2", "news-3"), result.items.map { it.articleId })
    }

    @Test
    fun `personal or incomplete daily brief is rejected`() {
        assertNull(
            DailyBriefDto(
                dateKey = "2026-07-30",
                title = "Bugünün Gündemi",
                summary = "Kişiye özel olmaması gereken bir özet.",
                items = items,
                generatedAt = 42L,
                shared = false,
            ).toModel(),
        )
        assertNull(
            DailyBriefDto(
                dateKey = "2026-07-30",
                title = "Bugünün Gündemi",
                summary = "Yetersiz haber içeren ortak özet.",
                items = items.take(2),
                generatedAt = 42L,
                shared = true,
            ).toModel(),
        )
    }
}
