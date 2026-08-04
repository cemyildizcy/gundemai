package com.example

import com.example.notification.NotificationWorkPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationWorkPayloadTest {
    @Test
    fun `notification payload survives WorkManager data serialization`() {
        val payload = NotificationWorkPayload(
            articleId = "article-42",
            title = "Yeni gelişme açıklandı",
            category = "Teknoloji",
            sourceName = "Kaynak",
            isBreaking = true,
        )

        assertEquals(payload, NotificationWorkPayload.fromData(payload.toData()))
    }

    @Test
    fun `invalid notification work data is rejected`() {
        assertNull(NotificationWorkPayload.fromData(androidx.work.Data.EMPTY))
    }
}
