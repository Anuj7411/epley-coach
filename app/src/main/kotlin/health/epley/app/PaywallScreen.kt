package health.epley.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
    var message by remember { mutableStateOf<String?>(null) }
    val c = Ds
    val n = c.night

    DScreen(bg = c.ground) {
        Row(Modifier.enter(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(48.dp).pressable(onClick = onCancel).box(c.surface, 24.dp), contentAlignment = Alignment.Center) {
                Sym("arrow_back", 24f, c.ink)
            }
            Txt("Share with your doctor", type(16f, 600), c.muted, maxLines = 1)
        }
        Column(
            Modifier.flex().heightIn(min = 232.dp).enter(1)
                .box(if (n) c.lilacTint else c.lilac, 32.dp, ring = if (n) c.lilac else null).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(56.dp).box(if (n) c.lilac else Ink, 28.dp), contentAlignment = Alignment.Center) {
                Sym("ios_share", 24f, if (n) Ink else Color.White)
            }
            Spacer(Modifier.weight(1f))
            Txt("Take your history to your doctor", type(34f, 800, lineHeight = 1.05f, letterSpacing = -0.03f, wrap = Wrap.Balance), if (n) c.ink else Ink)
            Txt(
                "A PDF of every run: when, which ear, how long, and the angle and hold time of each position. " +
                    "Useful at an appointment booked weeks after the attack, when nobody remembers the detail.",
                type(17f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty),
                if (n) c.soft else Ink,
            )
        }
        Column(Modifier.enter(2).box(c.surface, 28.dp).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Txt("Free forever, with or without this", type(17f, 700), c.ink)
            for (item in listOf(
                "The safety check",
                "The six questions that identify the ear",
                "Unlimited guided treatment runs",
                "Practice mode and after-care advice",
                "Your run history on this phone",
                "The sensor check",
            )) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Sym("check_circle", 22f, if (n) c.mint else Ink, fill = true)
                    Txt(item, type(16f, 600), c.ink, modifier = Modifier.weight(1f))
                }
            }
        }
        val notes = buildList {
            add("One payment. No subscription, no account, and nothing leaves your phone unless you send it.")
            if (entitlements.isPlaceholder) add("Test build: the store is not connected, so this unlocks without taking payment.")
        }
        notes.forEach { note ->
            Row(
                Modifier.box(Color.Transparent, 24.dp, ring = c.line, ringWidth = 1.5.dp).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Sym("info", 24f, c.muted)
                Txt(note, type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), c.ink, modifier = Modifier.weight(1f))
            }
        }
        message?.let {
            Row(
                Modifier.box(if (n) c.coralTint else c.coral, 28.dp, ring = if (n) c.coral else null).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Sym("warning", 24f, if (n) c.coral else Ink)
                Txt(it, type(16f, 600, lineHeight = 1.35f), if (n) c.ink else Ink, modifier = Modifier.weight(1f))
            }
        }
        PillButton(
            "Unlock the PDF · ${entitlements.priceLabel}",
            onClick = {
                entitlements.purchase { ok -> if (ok) onDone() else message = "That didn’t go through. Nothing was charged." }
            },
            fill = if (n) c.lilac else Ink, content = if (n) Ink else Color.White,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(
                "Restore", onClick = {
                    entitlements.restore { ok -> if (ok) onDone() else message = "No previous purchase found on this account." }
                },
                fill = c.surface, content = c.ink, modifier = Modifier.weight(1f),
            )
            PillButton("Not now", onCancel, fill = c.surface, content = c.ink, modifier = Modifier.weight(1f))
        }
    }
}
