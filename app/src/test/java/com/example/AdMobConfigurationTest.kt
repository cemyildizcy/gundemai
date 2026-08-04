package com.example

import com.example.ui.components.GOOGLE_TEST_BANNER_AD_UNIT_ID
import com.example.ui.components.resolveBannerAdUnitId
import com.example.ui.components.shouldRequestBannerAds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class AdMobConfigurationTest {
    private val configuredProductionId = "ca-app-pub-1111111111111111/2222222222"

    @Test
    fun debugBuildUsesGoogleDemoBannerInsteadOfProductionInventory() {
        assertEquals(
            GOOGLE_TEST_BANNER_AD_UNIT_ID,
            resolveBannerAdUnitId(
                isDebug = true,
                configuredAdUnitId = configuredProductionId,
            ),
        )
    }

    @Test
    fun releaseBuildKeepsConfiguredProductionBanner() {
        assertEquals(
            configuredProductionId,
            resolveBannerAdUnitId(
                isDebug = false,
                configuredAdUnitId = configuredProductionId,
            ),
        )
    }

    @Test
    fun proUsersNeverRequestBannerAds() {
        assertFalse(
            shouldRequestBannerAds(
                isProUser = true,
                adsEnabled = true,
                canRequestAds = true,
            ),
        )
    }

    @Test
    fun freeUsersRequestBannerOnlyAfterAdsAndConsentAreReady() {
        assertFalse(shouldRequestBannerAds(false, adsEnabled = false, canRequestAds = true))
        assertFalse(shouldRequestBannerAds(false, adsEnabled = true, canRequestAds = false))
        assertTrue(shouldRequestBannerAds(false, adsEnabled = true, canRequestAds = true))
    }

    @Test
    fun remoteConfigCanDisableAdsWithoutAPlayStoreUpdate() {
        assertFalse(
            shouldRequestBannerAds(
                isProUser = false,
                adsEnabled = true,
                canRequestAds = true,
                remoteAdsEnabled = false,
            ),
        )
    }
}
