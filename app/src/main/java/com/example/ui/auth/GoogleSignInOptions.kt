package com.example.ui.auth

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption

fun buildGoogleButtonOption(serverClientId: String): GetSignInWithGoogleOption =
    GetSignInWithGoogleOption.Builder(serverClientId)
        .build()
