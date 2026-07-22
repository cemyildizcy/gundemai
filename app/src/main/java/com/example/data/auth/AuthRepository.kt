package com.example.data.auth

import android.content.Context
import com.example.data.repository.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern

sealed class AuthResult {
    data class Success(val email: String, val displayName: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    context: Context,
    private val userPrefs: UserPreferencesRepository
) {
    private val firebaseAuth: FirebaseAuth? = try {
        FirebaseAuth.getInstance()
    } catch (_: Exception) {
        null
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): AuthResult {
        if (!isValidEmail(email)) return AuthResult.Error("Lütfen geçerli bir e-posta adresi girin.")
        if (password.length < 6) return AuthResult.Error("Şifreniz en az 6 karakter olmalıdır.")
        if (name.isBlank()) return AuthResult.Error("Lütfen adınızı ve soyadınızı girin.")
        val auth = firebaseAuth ?: return firebaseUnavailable()

        return try {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: return AuthResult.Error("Kullanıcı hesabı oluşturulamadı.")
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
            val userEmail = user.email ?: email.trim()
            userPrefs.setUserAccount(userEmail, name.trim())
            AuthResult.Success(userEmail, name.trim())
        } catch (error: Exception) {
            AuthResult.Error(authErrorMessage(error))
        }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult {
        if (!isValidEmail(email)) return AuthResult.Error("Lütfen geçerli bir e-posta adresi girin.")
        if (password.isBlank()) return AuthResult.Error("Lütfen şifrenizi girin.")
        val auth = firebaseAuth ?: return firebaseUnavailable()

        return try {
            val user = auth.signInWithEmailAndPassword(email.trim(), password).await().user
                ?: return AuthResult.Error("Oturum açılamadı.")
            val userEmail = user.email ?: email.trim()
            val displayName = user.displayName?.takeIf { it.isNotBlank() }
                ?: userEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
            userPrefs.setUserAccount(userEmail, displayName)
            AuthResult.Success(userEmail, displayName)
        } catch (error: Exception) {
            AuthResult.Error(authErrorMessage(error))
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): AuthResult {
        if (idToken.isBlank()) return AuthResult.Error("Google kimlik doğrulama bilgisi alınamadı.")
        val auth = firebaseAuth ?: return firebaseUnavailable()
        return try {
            val user = auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await().user
                ?: return AuthResult.Error("Google hesabıyla oturum açılamadı.")
            val email = user.email ?: return AuthResult.Error("Google hesabında e-posta bilgisi bulunamadı.")
            val name = user.displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore("@")
            userPrefs.setUserAccount(email, name)
            AuthResult.Success(email, name)
        } catch (error: Exception) {
            AuthResult.Error(authErrorMessage(error))
        }
    }

    suspend fun continueAsGuest() {
        userPrefs.setUserAccount(null, null)
    }

    suspend fun logout() {
        firebaseAuth?.signOut()
        userPrefs.setUserAccount(null, null)
    }

    suspend fun deleteCurrentAccount(): AuthResult {
        val auth = firebaseAuth ?: return firebaseUnavailable()
        val user = auth.currentUser ?: return AuthResult.Error("Silinecek aktif bir hesap bulunamadı.")
        val email = user.email.orEmpty()
        val displayName = user.displayName.orEmpty()

        return try {
            user.delete().await()
            auth.signOut()
            userPrefs.clearForAccountDeletion()
            AuthResult.Success(email, displayName)
        } catch (error: Exception) {
            val needsLogin = error.message?.contains("recent", ignoreCase = true) == true ||
                error.javaClass.simpleName.contains("RecentLogin", ignoreCase = true)
            if (needsLogin) {
                AuthResult.Error("Güvenlik için hesabınıza yeniden giriş yapıp silme işlemini tekrar deneyin.")
            } else {
                AuthResult.Error("Hesap silinemedi. İnternet bağlantınızı kontrol edip tekrar deneyin.")
            }
        }
    }

    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}\$"
        return Pattern.compile(emailRegex).matcher(email.trim()).matches()
    }

    private fun firebaseUnavailable() = AuthResult.Error(
        "Giriş servisi yapılandırılmamış. Firebase google-services.json dosyasını ekleyip tekrar deneyin."
    )

    private fun authErrorMessage(error: Exception): String = when {
        error.message?.contains("password", ignoreCase = true) == true -> "E-posta veya şifre hatalı."
        error.message?.contains("network", ignoreCase = true) == true -> "Giriş servisine ulaşılamadı. İnternet bağlantınızı kontrol edin."
        else -> "Giriş işlemi tamamlanamadı. Lütfen bilgilerinizi kontrol edip tekrar deneyin."
    }
}
