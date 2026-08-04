package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.ads.AdConsentManager
import com.example.data.config.RemoteFeatureFlags
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobTestNativeCard(
    isProUser: Boolean,
    modifier: Modifier = Modifier,
) {
    AdMobBanner(isProUser, modifier.testTag("admob_feed_banner"))
}

@Composable
fun AdMobTestAdaptiveBanner(
    isProUser: Boolean,
    modifier: Modifier = Modifier,
) {
    AdMobBanner(isProUser, modifier.testTag("admob_detail_banner"))
}

@Composable
private fun AdMobBanner(isProUser: Boolean, modifier: Modifier) {
    if (isProUser || !BuildConfig.ADS_ENABLED) return
    val canRequestAds by AdConsentManager.canRequestAds.collectAsStateWithLifecycle()
    val remoteAdsEnabled by RemoteFeatureFlags.adsEnabled.collectAsStateWithLifecycle()
    if (!shouldRequestBannerAds(
            isProUser = isProUser,
            adsEnabled = BuildConfig.ADS_ENABLED,
            canRequestAds = canRequestAds,
            remoteAdsEnabled = remoteAdsEnabled,
        )
    ) return

    val adViewState = remember { mutableStateOf<AdView?>(null) }
    DisposableEffect(Unit) {
        onDispose { adViewState.value?.destroy() }
    }

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        factory = { context ->
            AdView(context).apply {
                adUnitId = resolveBannerAdUnitId(
                    isDebug = BuildConfig.DEBUG,
                    configuredAdUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID,
                )
                setAdSize(AdSize.BANNER)
                loadAd(AdRequest.Builder().build())
                adViewState.value = this
            }
        }
    )
}
