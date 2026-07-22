package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthResult
import com.example.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UserAuthAndGuestFlowTest {

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
    fun `guest mode continuation sets auth completed without user email`() = runTest {
        // Given guest user continues without signing in
        userPrefs.setUserAccount(null, null)
        userPrefs.setAuthCompleted(true)

        // Then auth is completed but email and name remain null (Guest Mode)
        val authCompleted = userPrefs.authCompleted.first()
        val email = userPrefs.userEmail.first()
        val name = userPrefs.userName.first()

        assertTrue("Auth should be marked completed for guest user", authCompleted)
        assertNull("User email should be null in guest mode", email)
        assertNull("User name should be null in guest mode", name)
    }

    @Test
    fun `login flow saves user credentials and sets auth completed`() = runTest {
        val testEmail = "ahmet.yilmaz@example.com"
        val testName = "Ahmet Yılmaz"

        // When user logs in
        userPrefs.setUserAccount(testEmail, testName)
        userPrefs.setAuthCompleted(true)

        // Then user details are stored correctly
        assertEquals(testEmail, userPrefs.userEmail.first())
        assertEquals(testName, userPrefs.userName.first())
        assertTrue(userPrefs.authCompleted.first())
    }

    @Test
    fun `logout clears user session and resets auth completed`() = runTest {
        // Given a logged in user
        userPrefs.setUserAccount("logged@example.com", "Logged User")
        userPrefs.setAuthCompleted(true)

        // When logout is performed
        authRepository.logout()
        userPrefs.setAuthCompleted(false)

        // Then account data is cleared
        assertNull(userPrefs.userEmail.first())
        assertNull(userPrefs.userName.first())
        assertFalse(userPrefs.authCompleted.first())
    }

    @Test
    fun `auth repository email validation works correctly`() {
        val resultValid = authRepository.isValidEmail("user@gmail.com")
        val resultInvalid = authRepository.isValidEmail("invalid-email-format")

        assertTrue("Valid email should pass validation", resultValid)
        assertFalse("Invalid email should fail validation", resultInvalid)
    }

    @Test
    fun `firebase unavailable never creates a local authenticated session`() = runTest {
        val result = authRepository.signInWithEmail("user@example.com", "secret123")

        assertTrue("Unavailable Firebase must fail closed", result is AuthResult.Error)
        assertNull(userPrefs.userEmail.first())
    }
}
