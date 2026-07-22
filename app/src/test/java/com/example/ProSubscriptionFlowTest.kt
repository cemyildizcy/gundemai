package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthRepository
import com.example.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProSubscriptionFlowTest {

    private lateinit var context: Context
    private lateinit var userPrefs: UserPreferencesRepository
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        userPrefs = UserPreferencesRepository(context)
        authRepository = AuthRepository(context, userPrefs)
    }

    @Test
    fun `default subscription status is non pro`() = runTest {
        val isPro = userPrefs.isProUser.first()
        val plan = userPrefs.proPlanPeriod.first()

        assertFalse("Default state should be non-Pro", isPro)
        assertEquals("YEARLY", plan)
    }

    @Test
    fun `activating Pro subscription updates preferences correctly`() = runTest {
        // When activating monthly Pro
        userPrefs.setProUserStatus(true, "MONTHLY")

        assertTrue("Pro status should be active", userPrefs.isProUser.first())
        assertEquals("MONTHLY", userPrefs.proPlanPeriod.first())

        // When switching to yearly Pro
        userPrefs.setProUserStatus(true, "YEARLY")

        assertTrue("Pro status should remain active", userPrefs.isProUser.first())
        assertEquals("YEARLY", userPrefs.proPlanPeriod.first())
    }

    @Test
    fun `deactivating Pro subscription resets Pro status`() = runTest {
        userPrefs.setProUserStatus(true, "YEARLY")
        assertTrue(userPrefs.isProUser.first())

        // When subscription expires or is cancelled
        userPrefs.setProUserStatus(false, "YEARLY")

        assertFalse("Pro status should be inactive", userPrefs.isProUser.first())
    }

}
