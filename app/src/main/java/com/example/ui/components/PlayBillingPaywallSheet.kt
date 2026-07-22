package com.example.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.billing.BillingConnectionState
import com.example.data.billing.BillingPurchaseState
import com.example.data.billing.BillingSubscriptionProduct

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayBillingPaywallSheet(
    connectionState: BillingConnectionState,
    purchaseState: BillingPurchaseState,
    availableProducts: List<BillingSubscriptionProduct>,
    isProUser: Boolean,
    userEmail: String?,
    onDismiss: () -> Unit,
    onPurchaseClicked: (activity: Activity, planPeriod: String) -> Unit,
    onRestoreClicked: (onResult: (Boolean, String) -> Unit) -> Unit,
    onResetPurchaseState: () -> Unit,
    onNavigateToAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var selectedPlanPeriod by remember { mutableStateOf("YEARLY") }
    var isRestoring by remember { mutableStateOf(false) }
    val yearlyProduct = availableProducts.firstOrNull { it.period == "YEARLY" }
    val monthlyProduct = availableProducts.firstOrNull { it.period == "MONTHLY" }
    val selectedProduct = if (selectedPlanPeriod == "YEARLY") yearlyProduct else monthlyProduct

    val cardBgColor = Color(0xFF1E293B)
    val primaryTextColor = Color(0xFFF8FAFC)
    val secondaryTextColor = Color(0xFF94A3B8)
    val goldAccent = Color(0xFFF59E0B)
    val accentBlue = Color(0xFF3B82F6)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        containerColor = cardBgColor,
        shape = RoundedCornerShape(24.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Close Button and Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(goldAccent, Color(0xFFD97706)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.Black, modifier = Modifier.size(22.dp))
                        }

                        Text(
                            text = "GündemAI Pro'ya Geç",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = secondaryTextColor)
                    }
                }

                // Connection Status Chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (connectionState) {
                        is BillingConnectionState.Connected -> "Google Play Billing Bağlı"
                        is BillingConnectionState.Connecting -> "Google Play Bağlanıyor..."
                        else -> "Google Play Billing Kullanılamıyor"
                    }

                    val statusColor = when (connectionState) {
                        is BillingConnectionState.Connected -> Color(0xFF10B981)
                        is BillingConnectionState.Connecting -> goldAccent
                        else -> Color(0xFF60A5FA)
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = statusColor)
                        }
                    }

                    if (userEmail != null) {
                        Text("Hesap: $userEmail", fontSize = 11.sp, color = secondaryTextColor)
                    } else {
                        Text("Misafir Modu", fontSize = 11.sp, color = secondaryTextColor)
                    }
                }

                // Subtitle
                Text(
                    text = "Tüm reklamları kaldırın ve haber akışını kesintisiz okuyun.",
                    fontSize = 13.sp,
                    color = secondaryTextColor,
                    lineHeight = 18.sp
                )

                // Feature Checklist
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureCheckItem("🚫 %100 Reklamsız Haber Okuma Deneyimi")
                    FeatureCheckItem("🤖 Sunucuda hazırlanmış ortak yapay zekâ analizleri")
                    FeatureCheckItem("⚡ Son Dakika WorkManager Anlık Push Notifications")
                    FeatureCheckItem("🔄 Google Play hesabıyla tüm cihazlarda otomatik senkronizasyon")
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Plan Cards Selection
                Text(
                    text = "Abonelik Planınızı Seçin",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryTextColor
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Yearly Plan Card
                    val isYearly = selectedPlanPeriod == "YEARLY"
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isYearly) Color(0xFF1E1B4B) else Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = yearlyProduct != null) {
                                selectedPlanPeriod = "YEARLY"
                                onResetPurchaseState()
                            }
                            .border(
                                width = if (isYearly) 2.dp else 1.dp,
                                color = if (isYearly) goldAccent else Color(0xFF334155),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(goldAccent)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("YILLIK", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            }

                            Text("Yıllık Plan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                            Text(yearlyProduct?.formattedPrice ?: "Kullanılamıyor", fontSize = 15.sp, fontWeight = FontWeight.Black, color = goldAccent)
                            Text(yearlyProduct?.description ?: "Play Console ürünü bekleniyor", fontSize = 10.sp, color = secondaryTextColor, maxLines = 2)
                        }
                    }

                    // Monthly Plan Card
                    val isMonthly = selectedPlanPeriod == "MONTHLY"
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMonthly) Color(0xFF1E1B4B) else Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = monthlyProduct != null) {
                                selectedPlanPeriod = "MONTHLY"
                                onResetPurchaseState()
                            }
                            .border(
                                width = if (isMonthly) 2.dp else 1.dp,
                                color = if (isMonthly) accentBlue else Color(0xFF334155),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF334155))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ESNEK PLAN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Text("Aylık Plan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                            Text(monthlyProduct?.formattedPrice ?: "Kullanılamıyor", fontSize = 15.sp, fontWeight = FontWeight.Black, color = accentBlue)
                            Text(monthlyProduct?.description ?: "Play Console ürünü bekleniyor", fontSize = 10.sp, color = secondaryTextColor, maxLines = 2)
                        }
                    }
                }

                // Purchase State Status Banner
                when (purchaseState) {
                    is BillingPurchaseState.Processing -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = goldAccent, modifier = Modifier.size(20.dp))
                                Text("Google Play ödeme ekranı başlatılıyor...", fontSize = 12.sp, color = Color.White)
                            }
                        }
                    }
                    is BillingPurchaseState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34D399))
                                Text("Aboneliğiniz başarıyla aktifleşti! Teşekkür ederiz.", fontSize = 12.sp, color = Color(0xFFA7F3D0), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    is BillingPurchaseState.UserCanceled -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF94A3B8))
                                Text("Satın alma işlemi iptal edildi. Dilediğiniz zaman tekrar başlatabilirsiniz.", fontSize = 12.sp, color = Color(0xFFE2E8F0))
                            }
                        }
                    }
                    is BillingPurchaseState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFFCA5A5))
                                    Text("Google Play Billing Hatası", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5))
                                }
                                Text(purchaseState.message, fontSize = 12.sp, color = Color(0xFFFEE2E2))
                                Button(
                                    onClick = onResetPurchaseState,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Yeniden Dene", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                    else -> {}
                }

                if (userEmail.isNullOrBlank()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFCA5A5))
                            Column {
                                Text("Misafir Okuyucu Modundasınız", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFCA5A5))
                                Text("Misafir modunda Pro üyelik satın alınamaz. Aboneliğinizin hesabınıza tanımlanabilmesi için lütfen önce giriş yapın.", fontSize = 11.sp, color = Color(0xFFFEE2E2), lineHeight = 15.sp)
                            }
                        }
                    }
                }

                // CTA Button: Google Play Purchase or Auth Redirection
                if (userEmail.isNullOrBlank()) {
                    Button(
                        onClick = {
                            onDismiss()
                            onNavigateToAuth()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentBlue),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, tint = Color.White)
                            Text(
                                text = "Pro Almak İçin Giriş Yapın",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (activity != null) {
                                onPurchaseClicked(activity, selectedPlanPeriod)
                            } else {
                                Toast.makeText(context, "Google Play satın alma ekranı yükleniyor...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = goldAccent),
                        enabled = activity != null && selectedProduct != null && connectionState is BillingConnectionState.Connected,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color.Black)
                            Text(
                                text = "Google Play ile Satın Al & Başlat",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }

                // Restore Purchases Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = {
                            isRestoring = true
                            onRestoreClicked { success, msg ->
                                isRestoring = false
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isRestoring
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(color = accentBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Abonelikler Sorgulanıyor...", fontSize = 12.sp, color = secondaryTextColor)
                        } else {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp), tint = accentBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Önceki Satın Almaları / Aboneliği Geri Yükle", fontSize = 12.sp, color = accentBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Google Play Terms Disclaimer
                Text(
                    text = "Abonelik Google Play hesabınız üzerinden otomatik olarak tahsil edilir. İstediğiniz an Google Play Store -> Abonelikler menüsünden iptal edebilirsiniz.",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    lineHeight = 14.sp
                )
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun FeatureCheckItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF10B981),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFFE2E8F0)
        )
    }
}
