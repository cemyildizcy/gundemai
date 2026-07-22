package com.example.ui.screens

import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.auth.AuthResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: (email: String, name: String) -> Unit,
    onGuestContinue: () -> Unit,
    onEmailSignUp: suspend (email: String, password: String, name: String) -> AuthResult,
    onEmailSignIn: suspend (email: String, password: String) -> AuthResult,
    onGoogleSignIn: suspend (idToken: String) -> AuthResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isRegisterMode by remember { mutableStateOf(false) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val primaryBg = MaterialTheme.colorScheme.background
    val cardBg = MaterialTheme.colorScheme.surface
    val accentBlue = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    LaunchedEffect(isRegisterMode) {
        errorMessage = null
    }

    Scaffold(
        containerColor = primaryBg
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // App Logo & Header
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, accentBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_launcher_foreground),
                        contentDescription = "GündemAI Logo",
                        modifier = Modifier.size(76.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "GündemAI",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Yapay Zekâ Destekli Tarafsız Haber & Analiz",
                        fontSize = 14.sp,
                        color = subTextColor
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Error Banner
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFFCA5A5))
                                Text(msg, color = Color(0xFFFCA5A5), fontSize = 13.sp)
                            }
                        }
                    }
                }

                // 1. Google Quick Sign-In Option
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Hızlı Giriş Seçeneği",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = subTextColor
                        )

                        Button(
                            onClick = {
                                errorMessage = null
                                if (BuildConfig.GOOGLE_WEB_CLIENT_ID.startsWith("YOUR_")) {
                                    errorMessage = "Google girişi henüz yapılandırılmamış."
                                    return@Button
                                }
                                scope.launch {
                                    isLoading = true
                                    try {
                                        val option = GetGoogleIdOption.Builder()
                                            .setFilterByAuthorizedAccounts(false)
                                            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                                            .build()
                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(option)
                                            .build()
                                        val credential = credentialManager.getCredential(context, request).credential
                                        if (credential !is CustomCredential ||
                                            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                        ) {
                                            errorMessage = "Google hesabı doğrulanamadı."
                                            return@launch
                                        }
                                        val idToken = GoogleIdTokenCredential
                                            .createFrom(credential.data)
                                            .idToken
                                        when (val result = onGoogleSignIn(idToken)) {
                                            is AuthResult.Success -> {
                                                onAuthSuccess(result.email, result.displayName)
                                                Toast.makeText(context, "Google hesabıyla giriş yapıldı.", Toast.LENGTH_SHORT).show()
                                            }
                                            is AuthResult.Error -> errorMessage = result.message
                                        }
                                    } catch (_: NoCredentialException) {
                                        errorMessage = "Kullanılabilir bir Google hesabı bulunamadı. Cihaza hesap ekleyip tekrar deneyin."
                                    } catch (_: Exception) {
                                        errorMessage = "Google girişi tamamlanamadı veya iptal edildi."
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "Google ile Giriş Yap",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Divider Or
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "  veya E-Posta ile  ",
                        fontSize = 12.sp,
                        color = subTextColor
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }

                // 2. Email Auth Form Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Toggle Tab Row (Giriş Yap vs Kayıt Ol)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(4.dp)
                        ) {
                            Button(
                                onClick = { isRegisterMode = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isRegisterMode) accentBlue else Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Giriş Yap",
                                    color = if (!isRegisterMode) Color.White else subTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = { isRegisterMode = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRegisterMode) accentBlue else Color.Transparent
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Kayıt Ol",
                                    color = if (isRegisterMode) Color.White else subTextColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        // Name Field (Only in Register Mode)
                        AnimatedVisibility(visible = isRegisterMode) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Ad Soyad") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = accentBlue) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentBlue,
                                    unfocusedBorderColor = Color(0xFF475569),
                                    focusedTextColor = textColor,
                                    unfocusedTextColor = textColor,
                                    focusedLabelColor = accentBlue,
                                    unfocusedLabelColor = subTextColor
                                )
                            )
                        }

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("E-Posta Adresi") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = accentBlue) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentBlue,
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedLabelColor = accentBlue,
                                unfocusedLabelColor = subTextColor
                            )
                        )

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Şifre") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = accentBlue) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = subTextColor
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentBlue,
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = textColor,
                                unfocusedTextColor = textColor,
                                focusedLabelColor = accentBlue,
                                unfocusedLabelColor = subTextColor
                            )
                        )

                        // Action Submit Button
                        Button(
                            onClick = {
                                errorMessage = null
                                if (isRegisterMode) {
                                    if (name.isBlank()) {
                                        errorMessage = "Lütfen adınızı girin."
                                        isLoading = false
                                        return@Button
                                    }
                                    if (!email.contains("@") || email.isBlank()) {
                                        errorMessage = "Geçerli bir e-posta girin."
                                        isLoading = false
                                        return@Button
                                    }
                                    if (password.length < 6) {
                                        errorMessage = "Şifre en az 6 karakter olmalıdır."
                                        isLoading = false
                                        return@Button
                                    }
                                } else {
                                    if (!email.contains("@") || email.isBlank()) {
                                        errorMessage = "Geçerli bir e-posta girin."
                                        isLoading = false
                                        return@Button
                                    }
                                    if (password.isBlank()) {
                                        errorMessage = "Lütfen şifrenizi girin."
                                        isLoading = false
                                        return@Button
                                    }
                                }
                                scope.launch {
                                    isLoading = true
                                    val result = if (isRegisterMode) {
                                        onEmailSignUp(email.trim(), password, name.trim())
                                    } else {
                                        onEmailSignIn(email.trim(), password)
                                    }
                                    when (result) {
                                        is AuthResult.Success -> {
                                            onAuthSuccess(result.email, result.displayName)
                                            Toast.makeText(context, "Hoş geldiniz, ${result.displayName}!", Toast.LENGTH_SHORT).show()
                                        }
                                        is AuthResult.Error -> errorMessage = result.message
                                    }
                                    isLoading = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = if (isRegisterMode) "Kayıt Ol ve Giriş Yap" else "Giriş Yap",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 3. Guest Continue Option
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onGuestContinue,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Giriş Yapmadan Misafir Olarak Devam Et",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "Misafir modunda reklamlı sürüm kullanılır. Dilediğiniz zaman Profil sekmesinden e-posta veya Google ile giriş yapıp reklamsız Pro'ya geçebilirsiniz.",
                        fontSize = 11.sp,
                        color = subTextColor,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        lineHeight = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
