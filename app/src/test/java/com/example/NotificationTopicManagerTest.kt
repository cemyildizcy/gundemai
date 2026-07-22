package com.example

import com.example.notification.NotificationTopicManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationTopicManagerTest {
    @Test
    fun `canonical categories use the same stable topics as the server`() {
        assertEquals("news_yapay_zeka", NotificationTopicManager.topicForCanonicalCategory("Yapay Zeka"))
        assertEquals("news_turkiye", NotificationTopicManager.topicForCanonicalCategory("Turkiye"))
        assertEquals("news_kultur_sanat", NotificationTopicManager.topicForCanonicalCategory("Kultur ve Sanat"))
        assertNull(NotificationTopicManager.topicForCanonicalCategory("Sana Özel"))
    }
}
