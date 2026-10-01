package health.epley.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The one paid thing, and a plain statement of everything that is not.
 *
 * Treating your own vertigo is the app. Charging for any part of it — the safety check most of all
 * — would be charging for care, so the paywall stands in front of exactly one feature: the PDF to
 * hand to a doctor. The list of what stays free is on the screen, and it is longer than the thing
 * being sold, deliberately: a paywall in a health app has to survive being read closely by
 * someone who suspects the worst of it.
 */
@Composable
fun PaywallScreen(
    entitlements: Entitlements,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    var error by remember { mutableStateOf<PaywallError?>(null) }
    PaywallView(
        price = entitlements.priceLabel,
        simulated = entitlements.isSimulated,
        error = error,
        onBuy = { error = null; entitlements.purchase { ok -> if (ok) onDone() else error = PaywallError.Charge } },
        onRestore = { error = null; entitlements.restore { ok -> if (ok) onDone() else error = PaywallError.Restore } },
        onBack = onCancel,
    )
}

enum class PaywallError(val text: String) {
    Charge("That didn’t go through. Nothing was charged."),
    Restore("No previous purchase found on this account."),
}

/** §16 G. Scrolls; the spacer above Unlock only takes up what the content leaves. */
@Composable
fun PaywallView(
    price: String,
    simulated: Boolean,
    error: PaywallError?,
    onBuy: () -> Unit = {},
    onRestore: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        Row(Modifier.enter(0), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).label("Back").pressable(onClick = onBack).box(c.surface, 24.dp), contentAlignment = Alignment.Center) {
                Sym("arrow_back", 24f, c.ink)
            }
        }
        Column(
            Modifier.enter(1).box(if (n) c.lilacTint else c.lilac, 32.dp, ring = if (n) c.lilac else null).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier
                    .box(if (n) Color.Transparent else Color.White.copy(alpha = 0.55f), 999.dp, ring = if (n) c.lilac else null)
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("picture_as_pdf", 20f, if (n) c.lilac else Ink)
                Txt("Doctor’s PDF", type(16f, 700), if (n) c.lilac else Ink, maxLines = 1)
            }
            Txt("Take your history to your doctor", type(40f, 800, lineHeight = 1f, letterSpacing = -0.035f, wrap = Wrap.Balance), if (n) c.ink else Ink)
            Txt(
                "A PDF of every run: when, which ear, how long, and the angle and hold time of each position.",
                type(17f, 600, lineHeight = 1.45f, wrap = Wrap.Pretty), if (n) c.soft else Ink,
            )
        }
        Column(Modifier.enter(2).box(c.surface, 28.dp).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
            Txt("Free forever, with or without this", type(14f, 700), c.muted, modifier = Modifier.padding(bottom = 4.dp))
            listOf(
                "The safety check and the six questions",
                "Unlimited guided runs",
                "Practice mode and after-care advice",
                "Your run history on this phone",
                "The sensor check",
            ).forEachIndexed { i, item ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(if (n) c.surface2 else c.ground))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Sym("check_circle", 24f, if (n) c.mint else Ink, fill = true)
                    Txt(item, type(17f, 700, lineHeight = 1.3f), c.ink, modifier = Modifier.weight(1f))
                }
            }
        }
        Txt(
            "One payment. No subscription, no account, and nothing leaves your phone unless you send it.",
            type(16f, 600, lineHeight = 1.45f, wrap = Wrap.Pretty), c.muted, modifier = Modifier.padding(horizontal = 8.dp),
        )
        if (simulated) {
            Row(
                Modifier.box(Color.Transparent, 24.dp, ring = c.line, ringWidth = 1.5.dp).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Sym("info", 24f, c.muted)
                Txt(
                    "Test build: the store is not connected. Purchases here are simulated.",
                    type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), c.ink, modifier = Modifier.weight(1f),
                )
            }
        }
        if (error != null) {
            Row(
                Modifier.box(if (n) c.coralTint else c.coral, 24.dp, ring = if (n) c.coral else null).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Sym("warning", 24f, if (n) c.coral else Ink, fill = true)
                Txt(error.text, type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), c.ink, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.flex())
        PillButton(
            "Unlock the PDF · $price", onBuy,
            fill = if (n) c.lilac else Ink, content = if (n) Ink else Color.White, icon = "lock_open",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextAction("Restore", onRestore, Modifier.weight(1f))
            TextAction("Not now", onBack, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier) {
    Box(modifier.heightIn(min = 48.dp).pressable(onClick = onClick), contentAlignment = Alignment.Center) {
        Txt(text, type(17f, 700), Ds.ink, maxLines = 1)
    }
}
