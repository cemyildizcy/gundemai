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
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobTestNativeCard(modifier: Modifier = Modifier) {
    AdMobBanner(modifier.testTag("admob_feed_banner"))
}

@Composable
fun AdMobTestAdaptiveBanner(modifier: Modifier = Modifier) {
    AdMobBanner(modifier.testTag("admob_detail_banner"))
}

@Composable
private fun AdMobBanner(modifier: Modifier) {
    val canRequestAds by AdConsentManager.canRequestAds.collectAsStateWithLifecycle()
    if (!canRequestAds) return

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
                adUnitId = BuildConfig.ADMOB_BANNER_AD_UNIT_ID
                setAdSize(AdSize.BANNER)
                loadAd(AdRequest.Builder().build())
                adViewState.value = this
            }
        }
    )
}
