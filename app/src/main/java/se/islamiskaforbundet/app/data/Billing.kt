package se.islamiskaforbundet.app.data

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import se.islamiskaforbundet.app.Config

/** Google Play Billing for the Premium subscription. */
object Billing {
    private var client: BillingClient? = null
    private var details: ProductDetails? = null

    var premium by mutableStateOf(false)
        private set
    var price by mutableStateOf<String?>(null)
        private set
    var status by mutableStateOf<String?>(null)

    fun start(context: Context) {
        premium = Prefs.premium
        if (client != null) return
        val c = BillingClient.newBuilder(context.applicationContext)
            .setListener { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(purchases.orEmpty())
                else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) status = "Köpet kunde inte genomföras (${result.responseCode})"
            }
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        client = c
        c.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) return
                c.queryPurchasesAsync(
                    QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
                ) { r, purchases ->
                    if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                        val active = purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                        setPremium(active)
                        handle(purchases)
                    }
                }
                val params = QueryProductDetailsParams.newBuilder().setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(Config.PREMIUM_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                ).build()
                c.queryProductDetailsAsync(params) { r, list ->
                    if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                        details = list.firstOrNull()
                        price = details?.subscriptionOfferDetails?.firstOrNull()
                            ?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice
                    }
                }
            }
            override fun onBillingServiceDisconnected() { client = null }
        })
    }

    fun buy(activity: Activity) {
        val c = client
        val d = details
        val offer = d?.subscriptionOfferDetails?.firstOrNull()
        if (c == null || d == null || offer == null) {
            status = "Premium är inte tillgängligt just nu. Appen måste vara installerad från Google Play."
            return
        }
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(d)
                    .setOfferToken(offer.offerToken)
                    .build()
            )
        ).build()
        c.launchBillingFlow(activity, params)
    }

    private fun handle(purchases: List<Purchase>) {
        purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { p ->
            setPremium(true)
            if (!p.isAcknowledged) {
                client?.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
                ) { }
            }
        }
    }

    private fun setPremium(on: Boolean) {
        Prefs.premium = on
        premium = on
        if (on) status = null
    }
}
