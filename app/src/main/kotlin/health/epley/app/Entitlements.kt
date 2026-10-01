package health.epley.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Who has paid for what.
 *
 * An interface, not a direct call into a billing SDK, for one reason: **nothing clinical may ever
 * depend on it.** The safety check, the questionnaire, the manoeuvre and the after-care must work
 * whether or not a purchase, a network call or a store is available. Keeping the boundary this
 * narrow makes that easy to see and hard to break — the only thing behind it is exporting history.
 *
 * [PlaceholderEntitlements] is the stand-in until the RevenueCat key exists; the RevenueCat-backed
 * implementation replaces it without any screen changing.
 */
interface Entitlements {

    /** True when the export is unlocked. Compose state: reading it in a composable recomposes. */
    val hasExport: Boolean

    /** What the purchase costs, as the store reports it. */
    val priceLabel: String

    /** True while this is a stand-in rather than a real store, so the UI can say so. */
    val isPlaceholder: Boolean

    /** True when a purchase takes no real money: the stand-in, or RevenueCat's Test Store. */
    val isSimulated: Boolean get() = isPlaceholder

    fun purchase(onResult: (Boolean) -> Unit)

    fun restore(onResult: (Boolean) -> Unit)
}

/**
 * A local stand-in so the paywall can be built and used before the store is connected.
 *
 * It takes no money and talks to nothing. The paywall says so on screen while this is in use —
 * a purchase button that silently pretends would be a lie to anyone trying the app.
 */
class PlaceholderEntitlements(context: Context) : Entitlements {

    private val prefs = context.getSharedPreferences("entitlements", Context.MODE_PRIVATE)

    override var hasExport by mutableStateOf(prefs.getBoolean(KEY, false))
        private set

    override val priceLabel = "no charge"

    override val isPlaceholder = true

    override fun purchase(onResult: (Boolean) -> Unit) {
        set(true)
        onResult(true)
    }

    override fun restore(onResult: (Boolean) -> Unit) = onResult(hasExport)

    private fun set(value: Boolean) {
        hasExport = value
        prefs.edit().putBoolean(KEY, value).apply()
    }

    private companion object {
        const val KEY = "has_export"
    }
}
