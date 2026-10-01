package health.epley.app

import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith

/**
 * [Entitlements] backed by RevenueCat.
 *
 * The whole store lives behind the same four members the stand-in implements, for the reason given
 * in [Entitlements]: **nothing clinical may depend on a purchase, a network call or a store.** If
 * RevenueCat is unreachable, every call here fails quietly into "not paid" and the manoeuvre, the
 * safety check, the questionnaire and the after-care carry on exactly as before. There is no code
 * path where a billing failure can stop someone treating their vertigo.
 *
 * Purchases are driven by the *offering* configured in the dashboard rather than a product
 * identifier hard-coded here, so the price and the product can be changed without shipping a build.
 */
class RevenueCatEntitlements(private val activity: Activity, apiKey: String) : Entitlements {

    override var hasExport by mutableStateOf(false)
        private set

    /**
     * The store's own price string, so it is right in every currency and region.
     *
     * Until the offering arrives this reads as unavailable rather than guessing a number — a
     * placeholder price on a purchase button is a small lie that gets found out at the till.
     */
    override var priceLabel by mutableStateOf("price unavailable")
        private set

    override val isPlaceholder = false

    /** A Test Store key: RevenueCat's own simulated store, which never charges (§16 G). */
    override val isSimulated = apiKey.startsWith("test_")

    /** The package to buy, from the current offering. Null until it loads, or if none is set up. */
    private var offering: Package? = null

    init {
        Purchases.logLevel = LogLevel.WARN
        Purchases.configure(PurchasesConfiguration.Builder(activity, apiKey).build())

        // Entitlement changes can arrive without a purchase in this session — a restore on another
        // device, or a subscription lapsing — so state follows the listener, not just our calls.
        // A Java SAM interface, so a method reference will not convert on its own.
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener(::apply)

        Purchases.sharedInstance.getCustomerInfoWith(onError = {}, onSuccess = ::apply)
        Purchases.sharedInstance.getOfferingsWith(onError = {}) { offerings ->
            val available = offerings.current?.availablePackages.orEmpty().firstOrNull()
            offering = available
            priceLabel = available?.product?.price?.formatted ?: "price unavailable"
        }
    }

    override fun purchase(onResult: (Boolean) -> Unit) {
        val package_ = offering
        if (package_ == null) {
            // No offering means the dashboard is not configured yet. Report failure rather than
            // throwing: the paywall already says "nothing was charged", which is true.
            onResult(false)
            return
        }
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, package_).build(),
            onError = { _, _ -> onResult(false) },
            onSuccess = { _, customerInfo ->
                apply(customerInfo)
                onResult(hasExport)
            },
        )
    }

    override fun restore(onResult: (Boolean) -> Unit) {
        Purchases.sharedInstance.restorePurchasesWith(onError = { onResult(false) }) { customerInfo ->
            apply(customerInfo)
            onResult(hasExport)
        }
    }

    private fun apply(customerInfo: CustomerInfo) {
        hasExport = customerInfo.entitlements[ENTITLEMENT]?.isActive == true
    }

    private companion object {
        /** Must match the entitlement identifier in the RevenueCat dashboard. */
        const val ENTITLEMENT = "export"
    }
}
