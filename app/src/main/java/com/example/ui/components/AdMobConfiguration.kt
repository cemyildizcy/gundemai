package com.example.ui.components

internal const val GOOGLE_TEST_BANNER_AD_UNIT_ID =
    "ca-app-pub-3940256099942544/9214589741"

internal fun resolveBannerAdUnitId(
    isDebug: Boolean,
    configuredAdUnitId: String,
): String = if (isDebug) {
    GOOGLE_TEST_BANNER_AD_UNIT_ID
} else {
    configuredAdUnitId
}

internal fun shouldRequestBannerAds(
    isProUser: Boolean,
    adsEnabled: Boolean,
    canRequestAds: Boolean,
    remoteAdsEnabled: Boolean = true,
): Boolean = !isProUser && adsEnabled && remoteAdsEnabled && canRequestAds
