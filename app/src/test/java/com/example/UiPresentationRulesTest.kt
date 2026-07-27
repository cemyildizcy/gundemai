package com.example

import com.example.data.model.VerificationStatus
import com.example.ui.presentation.shouldShowFeedControls
import com.example.ui.presentation.shouldShowVerificationBadgeInFeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiPresentationRulesTest {

    @Test
    fun `feed controls are only visible on the home tab`() {
        assertTrue(shouldShowFeedControls(0))
        assertFalse(shouldShowFeedControls(1))
        assertFalse(shouldShowFeedControls(2))
        assertFalse(shouldShowFeedControls(3))
        assertFalse(shouldShowFeedControls(4))
    }

    @Test
    fun `single source state does not create a warning badge on every feed card`() {
        assertFalse(shouldShowVerificationBadgeInFeed("SINGLE_SOURCE_REPORT"))
        assertFalse(shouldShowVerificationBadgeInFeed("DEVELOPING_STORY"))
        assertFalse(shouldShowVerificationBadgeInFeed("INSUFFICIENT_INFORMATION"))
        assertFalse(shouldShowVerificationBadgeInFeed(null))
        assertFalse(shouldShowVerificationBadgeInFeed("UNKNOWN_STATUS"))
        assertTrue(shouldShowVerificationBadgeInFeed("MULTI_SOURCE_CONFIRMED"))
        assertTrue(shouldShowVerificationBadgeInFeed("OFFICIAL_CONFIRMED"))
        assertTrue(shouldShowVerificationBadgeInFeed("UNVERIFIED_CLAIM"))
        assertTrue(shouldShowVerificationBadgeInFeed("SOURCES_CONFLICT"))
    }

    @Test
    fun `unknown verification state is never presented as confirmed`() {
        assertEquals(
            VerificationStatus.INSUFFICIENT_INFORMATION,
            VerificationStatus.fromString("UNKNOWN_STATUS")
        )
        assertEquals(
            VerificationStatus.INSUFFICIENT_INFORMATION,
            VerificationStatus.fromString(null)
        )
    }
}
