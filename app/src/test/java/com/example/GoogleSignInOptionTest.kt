package com.example

import com.example.ui.auth.buildGoogleButtonOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleSignInOptionTest {

    @Test
    fun `explicit Google button uses the button credential flow`() {
        val option = buildGoogleButtonOption("test.apps.googleusercontent.com")

        assertTrue(option is GetSignInWithGoogleOption)
    }
}
