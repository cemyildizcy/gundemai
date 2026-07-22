package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast

import com.example.BuildConfig
import com.example.ui.components.PlayBillingPaywallSheet
import com.example.data.billing.BillingConnectionState
import com.example.data.billing.BillingPurchaseState
import com.example.data.billing.BillingSubscriptionProduct

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    darkThemeEnabled: Boolean,
    onDarkThemeToggle: (Boolean) -> Unit,
    followedCategories: Set<String>,
    followedTopics: Set<String>,
    userEmail: String?,
    userName: String?,
    isProUser: Boolean,
    proPlanPeriod: String,
    billingConnectionState: BillingConnectionState = BillingConnectionState.Connected,
    billingPurchaseState: BillingPurchaseState = BillingPurchaseState.Idle,
    billingAvailableProducts: List<BillingSubscriptionProduct> = emptyList(),
    onLogout: () -> Unit,
    onDeleteAccount: (onResult: (Boolean, String) -> Unit) -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onVerifySubscription: (onResult: (Boolean, String) -> Unit) -> Unit = {},
    onPurchaseSubscription: (activity: android.app.Activity, planPeriod: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onRestorePurchases: (onResult: (Boolean, String) -> Unit) -> Unit,
    onResetPurchaseState: () -> Unit = {},
    onClearCache: (onComplete: () -> Unit) -> Unit = { it() },
    privacyOptionsRequired: Boolean = false,
    onPrivacyOptionsClick: () -> Unit = {},
    onResetOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity

    val bgColor = Color(0xFF0F172A)
    val cardBgColor = Color(0xFF1E293B)
    val cardBorderColor = Color(0xFF334155)
    val accentBlue = Color(0xFF3B82F6)
    val goldAccent = Color(0xFFF59E0B)
    val primaryTextColor = Color(0xFFF8FAFC)
    val secondaryTextColor = Color(0xFF94A3B8)

    // State variables
    var selectedPlanPeriod by remember { mutableStateOf(proPlanPeriod) } // "MONTHLY" or "YEARLY"
    var showSubscriptionDialog by remember { mutableStateOf(false) }
    var showGuestWarningDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackText by remember { mutableStateOf("") }
    var isVerifyingSubscription by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. USER ACCOUNT & HESAP YÖNETİMİ CARD ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (isProUser) Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                                else Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isProUser) Icons.Default.WorkspacePremium else Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = userName ?: "Misafir Okuyucu",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isProUser) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = goldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = if (userEmail != null) userEmail else "Giriş yapılmadı • Reklamlı mod",
                            fontSize = 11.5.sp,
                            color = secondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isProUser) Color(0xFF10B981).copy(alpha = 0.2f) else accentBlue.copy(alpha = 0.2f))
                            .border(1.dp, if (isProUser) Color(0xFF10B981) else accentBlue, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            if (isProUser) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(11.dp))
                            }
                            Text(
                                text = if (isProUser) "REKLAMSIZ" else "REKLAMLI",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isProUser) Color(0xFF10B981) else accentBlue,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Login or Logout buttons
                if (userEmail == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Google Hesabı ile Giriş Yap",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        Text(
                            text = "Satın aldığınız reklamsız Pro aboneliği Google hesabınıza tanımlanır.",
                            fontSize = 11.5.sp,
                            color = secondaryTextColor,
                            lineHeight = 16.sp
                        )
                        Button(
                            onClick = onNavigateToAuth,
                            colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Text("Giriş Yap / Kayıt Ol", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Giriş Yapıldı: $userEmail",
                                fontSize = 12.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            TextButton(onClick = onLogout) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Çıkış Yap", color = Color(0xFFEF4444), fontSize = 12.sp)
                            }
                        }
                        TextButton(
                            onClick = { showDeleteAccountDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFEF4444))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hesabı ve verilerimi sil", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = cardBorderColor)

                // User Usage Analytics Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StatItem(number = "${followedCategories.size}", label = "Kategori", icon = Icons.Default.Category)
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StatItem(number = "${followedTopics.size}", label = "Konu", icon = Icons.Default.TrendingUp)
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StatItem(number = "1.5 Sa", label = "Tasarruf", icon = Icons.Default.Speed)
                    }
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        StatItem(number = "%100", label = "Doğruluk", icon = Icons.Default.CheckCircle)
                    }
                }
            }
        }

        // --- 1.B FIREBASE AUTH & FIRESTORE REALTIME SUBSCRIPTION & REKLAMSIZ DENEYİM CARD ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isProUser) Color(0xFF064E3B).copy(alpha = 0.9f) else Color(0xFF1E293B)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    if (isProUser) Color(0xFF10B981) else Color(0xFFF59E0B).copy(alpha = 0.5f),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isProUser) Color(0xFF10B981).copy(alpha = 0.25f) else goldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isProUser) Icons.Default.CheckCircle else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isProUser) Color(0xFF10B981) else goldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isProUser) "Reklamsız Deneyim (Doğrulandı)" else "Abonelik Durumu: Ücretsiz",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Google Play abonelik doğrulaması",
                                fontSize = 11.sp,
                                color = if (isProUser) Color(0xFFA7F3D0) else secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Dynamic Tag Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isProUser) Color(0xFF10B981) else Color(0xFFF59E0B).copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isProUser) "✓ REKLAMSIZ" else "REKLAMLI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isProUser) Color.Black else goldAccent,
                            maxLines = 1
                        )
                    }
                }

                Text(
                    text = if (isProUser)
                        "Google Play hesabınızdaki aktif abonelik doğrulandı. Uygulama içindeki reklamlar kaldırılmıştır."
                    else
                        "Şu anda reklamlı ücretsiz moddasınız. Başka bir cihazda satın aldıysanız Google Play satın alma geçmişinden geri yükleyebilirsiniz.",
                    fontSize = 12.sp,
                    color = if (isProUser) Color(0xFFD1FAE5) else secondaryTextColor,
                    lineHeight = 17.sp
                )

                Button(
                    onClick = {
                        isVerifyingSubscription = true
                        onVerifySubscription { success, msg ->
                            isVerifyingSubscription = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isProUser) Color(0xFF10B981) else accentBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    enabled = !isVerifyingSubscription
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isVerifyingSubscription) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = if (isProUser) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = if (isVerifyingSubscription) "Google Play sorgulanıyor..." else "Google Play ile durumu doğrula",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isProUser) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        // --- 2. GOOGLE PLAY IN-APP BILLING SUBSCRIPTION CARD ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isProUser) Color(0xFF064E3B) else Color(0xFF1E1B4B)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.5.dp,
                    if (isProUser) Color(0xFF10B981) else Color(0xFF6366F1),
                    RoundedCornerShape(20.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(goldAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = if (isProUser) "Google Play Pro Üyelik Aktif" else "Reklamları Kaldır & Pro'ya Yükselt",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    if (!isProUser) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "PLAY BILLING",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }

                Text(
                    text = if (isProUser)
                        "Google Play hesabınız üzerinden aktif Pro üyeliğiniz doğrulandı. AdMob banner ve geçiş reklamları tamamen kaldırıldı."
                    else
                        "İsteyen kullanıcılar giriş yapmadan da reklamlı ücretsiz modda kullanabilir. Reklamsız satın almak isteyenler için Google Play In-App Subscription (Uygulama İçi Satın Alma) güvencesiyle hesabınıza tanımlanır.",
                    fontSize = 12.sp,
                    color = Color(0xFFC7D2FE),
                    lineHeight = 18.sp
                )

                // Plan Perks Bullet Points
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProPerkRow("🚫 %100 Reklamsız Haber Okuma Deneyimi (AdMob Engellendi)")
                    ProPerkRow("⚡ Takip edilen kategoriler için yeni haber bildirimleri")
                    ProPerkRow("🤖 Sunucuda hazırlanmış ortak yapay zekâ analizleri")
                    ProPerkRow("🔄 Google Play hesabıyla abonelik geri yükleme")
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (isProUser) {
                                Toast.makeText(context, "Google Play Pro üyeliğiniz zaten aktif!", Toast.LENGTH_SHORT).show()
                            } else if (userEmail.isNullOrBlank()) {
                                showGuestWarningDialog = true
                            } else {
                                showSubscriptionDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isProUser) Color(0xFF10B981) else goldAccent
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isProUser) Icons.Default.CheckCircle else Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Text(
                                text = if (isProUser) "Aktif Pro Üye" else "Abonelik Seçenekleri",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onRestorePurchases { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Geri Yükle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- 3. GÖRÜNÜM TERCİHLERİ ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚙️ Arayüz Tercihleri",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )

                // Dark Mode Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (darkThemeEnabled) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = accentBlue
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Koyu Tema (Göz Dostu)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = primaryTextColor
                            )
                            Text(
                                text = "Gece okumalarında gözü yormayan lacivert tuval",
                                fontSize = 11.sp,
                                color = secondaryTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = darkThemeEnabled,
                        onCheckedChange = onDarkThemeToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentBlue
                        ),
                        modifier = Modifier.testTag("dark_theme_switch")
                    )
                }

            }
        }

        // --- 4. VERİ, ÖNBELLEK VE DÜZENLEME ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "💾 Veri, Saklama & Temizlik",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Yerel Veritabanı Büyüklüğü",
                            fontSize = 13.sp,
                            color = primaryTextColor
                        )
                        Text(
                            text = "Yer imleri korunarak haber ve arama önbelleği temizlenir",
                            fontSize = 11.sp,
                            color = secondaryTextColor
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            onClearCache {
                                Toast.makeText(context, "Yerel haber ve arama önbelleği temizlendi.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Temizle", fontSize = 12.sp)
                    }
                }

                HorizontalDivider(color = cardBorderColor)

                // Reset Onboarding Interests Button
                OutlinedButton(
                    onClick = onResetOnboarding,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("İlgi Alanı ve Kategori Tercihlerini Sıfırla")
                }
            }
        }

        // --- 5. DESTEK, BİLDİRİM VE YASAL ---
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "💬 Destek & İletişim",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFeedbackDialog = true }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Feedback, contentDescription = null, tint = accentBlue)
                        Text("Geri Bildirim Gönder / Hata Bildir", fontSize = 13.sp, color = primaryTextColor)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = secondaryTextColor)
                }

                HorizontalDivider(color = cardBorderColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                            val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                            runCatching { context.startActivity(market) }
                                .onFailure { context.startActivity(web) }
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.StarRate, contentDescription = null, tint = goldAccent)
                        Text("Uygulamayı Puanla (5 Yıldız Ver)", fontSize = 13.sp, color = primaryTextColor)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = secondaryTextColor)
                }

                HorizontalDivider(color = cardBorderColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL)))
                        }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = secondaryTextColor)
                        Text("Gizlilik Politikası ve Kullanım Koşulları", fontSize = 13.sp, color = primaryTextColor)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = secondaryTextColor)
                }

                if (privacyOptionsRequired) {
                    HorizontalDivider(color = cardBorderColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onPrivacyOptionsClick)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.AdUnits, contentDescription = null, tint = secondaryTextColor)
                            Text("Reklam gizlilik tercihleri", fontSize = 13.sp, color = primaryTextColor)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = secondaryTextColor)
                    }
                }
            }
        }

        // App Footer Metadata
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "GündemAI v${BuildConfig.VERSION_NAME}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = secondaryTextColor
            )
            Text(
                text = "Merkezi sunucu analizi ve güvenilir kaynak akışı",
                fontSize = 10.sp,
                color = secondaryTextColor.copy(alpha = 0.7f)
            )
        }
    }

    // --- SUBSCRIPTION DIALOG MODAL ---
    if (showSubscriptionDialog) {
        PlayBillingPaywallSheet(
            connectionState = billingConnectionState,
            purchaseState = billingPurchaseState,
            availableProducts = billingAvailableProducts,
            isProUser = isProUser,
            userEmail = userEmail,
            onDismiss = { showSubscriptionDialog = false },
            onPurchaseClicked = { act, planPeriod ->
                onPurchaseSubscription(act, planPeriod) { success, msg ->
                    if (success) {
                        showSubscriptionDialog = false
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            onRestoreClicked = onRestorePurchases,
            onResetPurchaseState = onResetPurchaseState,
            onNavigateToAuth = {
                showSubscriptionDialog = false
                onNavigateToAuth()
            }
        )
    }

    // --- GUEST WARNING DIALOG MODAL ---
    if (showGuestWarningDialog) {
        AlertDialog(
            onDismissRequest = { showGuestWarningDialog = false },
            containerColor = cardBgColor,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = goldAccent)
                    Text("Giriş Yapılması Gerekiyor", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                }
            },
            text = {
                Text(
                    text = "Misafir okuyucu modunda Pro üyelik satın alınamaz. Pro avantajlarından faydalanabilmek ve satın almayı hesabınıza tanımlamak için lütfen e-posta veya Google hesabınızla giriş yapın.",
                    fontSize = 13.sp,
                    color = secondaryTextColor,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showGuestWarningDialog = false
                        onNavigateToAuth()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Giriş Yap Ekranına Git", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGuestWarningDialog = false }) {
                    Text("İptal", color = secondaryTextColor)
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
            containerColor = cardBgColor,
            title = { Text("Hesabı kalıcı olarak sil", color = primaryTextColor, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Firebase hesabınız ve bu cihazdaki kişisel tercihleriniz kalıcı olarak silinir. " +
                        "Bu işlem Google Play aboneliğinizi iptal etmez; aktif aboneliği Play Store'dan ayrıca iptal etmelisiniz.",
                    color = secondaryTextColor,
                    lineHeight = 19.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDeletingAccount = true
                        onDeleteAccount { success, message ->
                            isDeletingAccount = false
                            if (success) showDeleteAccountDialog = false
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    if (isDeletingAccount) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Kalıcı olarak sil")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    enabled = !isDeletingAccount
                ) {
                    Text("Vazgeç", color = secondaryTextColor)
                }
            }
        )
    }

    // --- FEEDBACK DIALOG MODAL ---
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            containerColor = cardBgColor,
            shape = RoundedCornerShape(20.dp),
            title = { Text("Geri Bildirim Gönder", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryTextColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Görüş, öneri veya karşılaştığınız hataları bize iletebilirsiniz:", fontSize = 12.sp, color = secondaryTextColor)
                    OutlinedTextField(
                        value = feedbackText,
                        onValueChange = { feedbackText = it },
                        placeholder = { Text("Görüşlerinizi yazın...", fontSize = 12.sp, color = secondaryTextColor) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentBlue,
                            unfocusedBorderColor = cardBorderColor,
                            focusedTextColor = primaryTextColor,
                            unfocusedTextColor = primaryTextColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackText.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${BuildConfig.SUPPORT_EMAIL}")
                                putExtra(Intent.EXTRA_SUBJECT, "GündemAI geri bildirimi")
                                putExtra(Intent.EXTRA_TEXT, feedbackText)
                            }
                            runCatching { context.startActivity(intent) }
                                .onSuccess {
                                    feedbackText = ""
                                    showFeedbackDialog = false
                                }
                                .onFailure {
                                    Toast.makeText(context, "E-posta uygulaması bulunamadı.", Toast.LENGTH_SHORT).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentBlue)
                ) {
                    Text("Gönder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Kapat", color = secondaryTextColor)
                }
            }
        )
    }
}

@Composable
private fun StatItem(number: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(18.dp))
        Text(text = number, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF8FAFC), maxLines = 1)
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProPerkRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFFE2E8F0),
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp
        )
    }
}
