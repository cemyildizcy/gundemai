package com.example.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BillingConnectionState {
    object Disconnected : BillingConnectionState()
    object Connecting : BillingConnectionState()
    object Connected : BillingConnectionState()
    data class Error(val message: String, val responseCode: Int) : BillingConnectionState()
}

sealed class BillingPurchaseState {
    object Idle : BillingPurchaseState()
    object Processing : BillingPurchaseState()
    data class Success(val productId: String, val isAcknowledged: Boolean) : BillingPurchaseState()
    object UserCanceled : BillingPurchaseState()
    data class Error(val message: String, val responseCode: Int) : BillingPurchaseState()
}

data class BillingSubscriptionProduct(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String,
    val period: String, // "MONTHLY" or "YEARLY"
    val originalDetails: ProductDetails? = null
)

class PlayBillingManager(
    private val context: Context,
    private val onSubscriptionStatusChanged: (productId: String?) -> Unit
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _connectionState = MutableStateFlow<BillingConnectionState>(BillingConnectionState.Disconnected)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState

    private val _purchaseState = MutableStateFlow<BillingPurchaseState>(BillingPurchaseState.Idle)
    val purchaseState: StateFlow<BillingPurchaseState> = _purchaseState

    private val _availableProducts = MutableStateFlow<List<BillingSubscriptionProduct>>(emptyList())
    val availableProducts: StateFlow<List<BillingSubscriptionProduct>> = _availableProducts

    private var billingClient: BillingClient? = null
    private var retryCount = 0

    companion object {
        const val SKU_PRO_MONTHLY = "gundemai_pro_monthly"
        const val SKU_PRO_YEARLY = "gundemai_pro_yearly"
    }

    init {
        initBillingClient()
    }

    fun initBillingClient() {
        try {
            _connectionState.value = BillingConnectionState.Connecting
            billingClient = BillingClient.newBuilder(context)
                .setListener(this)
                .enablePendingPurchases()
                .build()

            startConnection()
        } catch (e: Exception) {
            _connectionState.value = BillingConnectionState.Error(
                message = "Google Play Billing istemcisi başlatılamadı: ${e.localizedMessage}",
                responseCode = -1
            )
            _availableProducts.value = emptyList()
        }
    }

    fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = BillingConnectionState.Connected
                    retryCount = 0
                    querySubscriptions()
                    queryExistingPurchases()
                } else {
                    val errorMsg = mapResponseCodeToMessage(billingResult.responseCode)
                    _connectionState.value = BillingConnectionState.Error(errorMsg, billingResult.responseCode)
                    _availableProducts.value = emptyList()
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingConnectionState.Disconnected
                if (retryCount < 3) {
                    retryCount++
                    startConnection()
                } else {
                    _availableProducts.value = emptyList()
                }
            }
        }) ?: run {
            _availableProducts.value = emptyList()
        }
    }

    private fun querySubscriptions() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_PRO_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_PRO_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val mapped = productDetailsList.map { pd ->
                    val offer = pd.subscriptionOfferDetails?.firstOrNull()
                    val price = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "₺49,99"
                    val period = if (pd.productId == SKU_PRO_MONTHLY) "MONTHLY" else "YEARLY"
                    BillingSubscriptionProduct(
                        productId = pd.productId,
                        title = pd.title,
                        description = pd.description,
                        formattedPrice = price,
                        period = period,
                        originalDetails = pd
                    )
                }
                _availableProducts.value = mapped
            } else {
                _availableProducts.value = emptyList()
            }
        }
    }

    fun queryExistingPurchases(onResult: ((Boolean, String) -> Unit)? = null) {
        billingClient?.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                var foundActive = false
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        handlePurchase(purchase)
                        foundActive = true
                    }
                }
                if (foundActive) {
                    onResult?.invoke(true, "Google Play hesabınızdaki aktif Pro aboneliğiniz doğrulandı ve yüklendi.")
                } else {
                    onSubscriptionStatusChanged(null)
                    onResult?.invoke(false, "Google Play hesabınızda aktif bir Pro aboneliği bulunamadı.")
                }
            } else {
                val errorMsg = mapResponseCodeToMessage(billingResult.responseCode)
                onResult?.invoke(false, errorMsg)
            }
        } ?: run {
            onResult?.invoke(false, "Google Play Billing istemcisi aktif değil.")
        }
    }

    fun launchSubscriptionPurchase(activity: Activity, productId: String): Boolean {
        _purchaseState.value = BillingPurchaseState.Processing

        val productItem = _availableProducts.value.find { it.productId == productId }
        val productDetails = productItem?.originalDetails

        if (productDetails != null && billingClient?.isReady == true) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
            val productDetailsParamsList = listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()

            val result = billingClient?.launchBillingFlow(activity, billingFlowParams)
            if (result?.responseCode != BillingClient.BillingResponseCode.OK) {
                val errorMsg = mapResponseCodeToMessage(result?.responseCode ?: -1)
                _purchaseState.value = BillingPurchaseState.Error(errorMsg, result?.responseCode ?: -1)
                return false
            }
            return true
        }

        _purchaseState.value = BillingPurchaseState.Error(
            "Google Play ürün bilgisi hazır değil. Play Console ürünlerini ve test hesabını kontrol edin.",
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE
        )
        return false
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = BillingPurchaseState.UserCanceled
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _purchaseState.value = BillingPurchaseState.Error(
                    message = "Bu aboneliğe zaten sahipsiniz. 'Abonelikleri Geri Yükle' butonuna dokunabilirsiniz.",
                    responseCode = billingResult.responseCode
                )
            }
            else -> {
                val message = mapResponseCodeToMessage(billingResult.responseCode)
                _purchaseState.value = BillingPurchaseState.Error(message, billingResult.responseCode)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val productId = purchase.products.firstOrNull() ?: SKU_PRO_YEARLY
            if (productId !in setOf(SKU_PRO_MONTHLY, SKU_PRO_YEARLY)) return
            if (purchase.isAcknowledged) {
                _purchaseState.value = BillingPurchaseState.Success(productId, true)
                onSubscriptionStatusChanged(productId)
            } else {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { ackResult ->
                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _purchaseState.value = BillingPurchaseState.Success(productId, true)
                        onSubscriptionStatusChanged(productId)
                    } else {
                        _purchaseState.value = BillingPurchaseState.Error(
                            mapResponseCodeToMessage(ackResult.responseCode),
                            ackResult.responseCode
                        )
                    }
                }
            }
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = BillingPurchaseState.Idle
    }

    fun mapResponseCodeToMessage(code: Int): String {
        return when (code) {
            BillingClient.BillingResponseCode.OK -> "İşlem başarılı."
            BillingClient.BillingResponseCode.USER_CANCELED -> "Satın alma işlemi kullanıcı tarafından iptal edildi."
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> "Google Play Hizmetlerine ulaşılamıyor. İnternet bağlantınızı kontrol edin."
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE -> "Google Play Uygulama İçi Satın Alma bu cihazda desteklenmiyor veya hesabınız yetkisiz."
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> "İstenen ürün şu anda satışta değil."
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> "Geliştirici yapılandırma hatası. Google Play Console konsol ürün eşleşmesini kontrol edin."
            BillingClient.BillingResponseCode.ERROR -> "Google Play Billing sunucularında genel bir hata oluştu."
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> "Bu ürün zaten hesabınızda tanımlı."
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED -> "Bu ürüne sahip değilsiniz."
            else -> "Bilinmeyen Google Play Billing Hatası (Kod: $code)."
        }
    }
}
