package com.example.data.ads

import android.app.Activity
import com.example.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object AdConsentManager {
    private val initialized = AtomicBoolean(false)
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired

    fun gatherConsent(activity: Activity) {
        if (!BuildConfig.ADS_ENABLED) {
            _canRequestAds.value = false
            _privacyOptionsRequired.value = false
            return
        }
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                updatePrivacyRequirement(consentInformation)
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    updateState(activity, consentInformation)
                }
                updateState(activity, consentInformation)
            },
            {
                updatePrivacyRequirement(consentInformation)
                updateState(activity, consentInformation)
            }
        )
    }

    fun showPrivacyOptions(activity: Activity, onComplete: (String?) -> Unit = {}) {
        if (!BuildConfig.ADS_ENABLED) {
            onComplete(null)
            return
        }
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
            updateState(activity, consentInformation)
            onComplete(error?.message)
        }
    }

    private fun updatePrivacyRequirement(consentInformation: ConsentInformation) {
        _privacyOptionsRequired.value = consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    private fun updateState(activity: Activity, consentInformation: ConsentInformation) {
        _canRequestAds.value = consentInformation.canRequestAds()
        if (_canRequestAds.value && initialized.compareAndSet(false, true)) {
            CoroutineScope(Dispatchers.IO).launch {
                MobileAds.initialize(activity) {}
            }
        }
    }
}
