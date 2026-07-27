package com.example

import com.example.ui.components.GOOGLE_TEST_BANNER_AD_UNIT_ID
import com.example.ui.components.resolveBannerAdUnitId
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
}
