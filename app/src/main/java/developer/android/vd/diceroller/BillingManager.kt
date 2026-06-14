package developer.android.vd.diceroller

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection() // Recommended for PBL 8.0+
        .build()

    interface BillingListener {
        fun onPurchaseSuccess()
        fun onPurchaseFailure(error: String)
        fun onBillingClientReady()
        fun onPurchasePending() {} // Default empty implementation so consumers don't have to implement it if they don't want to
    }

    private var listener: BillingListener? = null
    private var proProductDetails: ProductDetails? = null

    companion object {
        const val PRODUCT_PRO = "dice_roller_pro_lifetime"
        private const val TAG = "BillingManager"
        
        private const val BASE_64_ENCODED_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtHfH4pfh7EWB3d+/MGwn1h1ICpXlEQ39HlE7f6eUFtgU0aWw9lu0DQbCm/HnGlB67G7oJt3qqqcGtUWvBzaRxeaTLzYayWlPSctDRZj+0l+URF73bAsd2xVgy8Xh5mAMPpBZ5m/TtnycwdadwG4MwEX3bNB24JwE/yZzvinm7e2obE0iGtK9xYP2NlrGgKnx55MHWnqgDmpHz7DBhyGSuPNbaV+hN+RH7Em3z+/Noi9oIcCB84qRUg7NPOaeC83IK9H/ihIRU6uXGOjDLduOH747/g+Nj1EOvIGe/DkFTXdKNSXJK6YZXA5+tKiuEn0UD83DmO0OkDNjKzi1bwDSbwIDAQAB"
    }

    fun setListener(listener: BillingListener) {
        this.listener = listener
    }

    fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing Client Setup Finished")
                    queryProductDetails()
                    restorePurchases()
                    listener?.onBillingClientReady()
                } else {
                    Log.e(TAG, "Billing Client Setup Failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing Service Disconnected")
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        // Updated for PBL 8.0+: Added QueryProductDetailsResult in callback
        billingClient.queryProductDetailsAsync(params
        ) { billingResult, result ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = result.productDetailsList
                for (details in productDetailsList) {
                    if (details.productId == PRODUCT_PRO) {
                        proProductDetails = details
                        break
                    }
                }
                Log.d(TAG, "Product Details Query Success: ${proProductDetails?.name}")
            } else {
                Log.e(TAG, "Product Details Query Failed: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        val details = proProductDetails
        if (details != null) {
            launchBillingFlowWithDetails(activity, details)
            return
        }

        // Handle race condition: details aren't loaded yet. Fetch them now.
        Log.d(TAG, "Product details not loaded, fetching on demand...")
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_PRO)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val productDetailsList = result.productDetailsList
                var fetchedDetails: ProductDetails? = null
                
                if (productDetailsList != null) {
                    for (item in productDetailsList) {
                        if (item.productId == PRODUCT_PRO) {
                            fetchedDetails = item
                            proProductDetails = item // Cache it
                            break
                        }
                    }
                }
                
                if (fetchedDetails != null) {
                    launchBillingFlowWithDetails(activity, fetchedDetails)
                } else {
                    listener?.onPurchaseFailure("Item not found on Google Play.")
                }
            } else {
                Log.e(TAG, "On-demand query failed: ${billingResult.debugMessage}")
                listener?.onPurchaseFailure("Could not load checkout. Check internet connection.")
            }
        }
    }

    private fun launchBillingFlowWithDetails(activity: Activity, details: ProductDetails) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User Canceled Purchase")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "Item Already Owned. Triggering restore...")
                restorePurchases() // Automatically recover the lost state
            }
            else -> {
                Log.e(TAG, "Purchase Update Failed: ${billingResult.debugMessage}")
                listener?.onPurchaseFailure(billingResult.debugMessage)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Verify the cryptographic signature of the purchase
            if (!Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, purchase.originalJson, purchase.signature)) {
                Log.e(TAG, "Signature verification failed for purchase.")
                listener?.onPurchaseFailure("Purchase verification failed for security reasons.")
                return
            }

            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.d(TAG, "Purchase Acknowledged")
                        markProPurchased()
                    }
                }
            } else {
                markProPurchased()
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase is pending.")
            listener?.onPurchasePending()
        }
    }

    private fun markProPurchased() {
        PrefsHelper.setProPurchased(context)
        listener?.onPurchaseSuccess()
    }

    fun restorePurchases(onResult: ((restored: Boolean) -> Unit)? = null) {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params
        ) { billingResult, purchases ->
            var anyRestored = false
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.products.contains(PRODUCT_PRO) &&
                        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        if (Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, purchase.originalJson, purchase.signature)) {
                            markProPurchased()
                            anyRestored = true
                        } else {
                            Log.e(TAG, "Restored purchase verification failed for security reasons.")
                        }
                    }
                }
            }
            onResult?.invoke(anyRestored)
        }
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }
}
