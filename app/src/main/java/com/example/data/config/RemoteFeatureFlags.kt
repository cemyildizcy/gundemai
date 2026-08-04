package com.example.data.config

import com.example.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RemoteFeatureFlags {
    private const val ADS_ENABLED_KEY = "ads_enabled"
    private const val RELEASE_FETCH_INTERVAL_SECONDS = 12 * 60 * 60L

    private val _adsEnabled = MutableStateFlow(true)
    val adsEnabled = _adsEnabled.asStateFlow()

    fun initialize() {
        try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(
                    if (BuildConfig.DEBUG) 0 else RELEASE_FETCH_INTERVAL_SECONDS
                )
                .build()

            remoteConfig.setConfigSettingsAsync(settings)
            remoteConfig.setDefaultsAsync(mapOf(ADS_ENABLED_KEY to true))
            updateFrom(remoteConfig)
            remoteConfig.fetchAndActivate().addOnCompleteListener {
                updateFrom(remoteConfig)
                if (!it.isSuccessful) {
                    FirebaseCrashlytics.getInstance()
                        .log("Remote Config yenilenemedi; yerel reklam varsayılanı kullanılıyor.")
                }
            }
        } catch (error: Exception) {
            FirebaseCrashlytics.getInstance().recordException(error)
        }
    }

    private fun updateFrom(remoteConfig: FirebaseRemoteConfig) {
        _adsEnabled.value = remoteConfig.getBoolean(ADS_ENABLED_KEY)
    }
}
