package health.epley.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
 * Treating your own vertigo is the app. Charging for any part of it — the safety check most of
 * all — would be charging for care, so the paywall stands in front of exactly one feature: a
 * written history to hand to a doctor. The list of what stays free is on the screen, not just in
 * a README.
 */
@Composable
fun PaywallScreen(
    entitlements: Entitlements,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    var message by remember { mutableStateOf<String?>(null) }
    val c = Ds

    DsScreen(
        top = {
            Row(
                Modifier.fillMaxWidth().padding(vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.l),
            ) {
                DsIconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onCancel)
                Text("History export", style = DsType.label, color = c.muted)
            }
        },
        bottom = {
            DsButton(
                label = "Unlock export · ${entitlements.priceLabel}",
                onClick = {
                    entitlements.purchase { ok ->
                        if (ok) onDone() else message = "That didn't go through. Nothing was charged."
                    }
                },
            )
            DsButton(
                label = "Restore a previous purchase",
                onClick = {
                    entitlements.restore { ok ->
                        if (ok) onDone() else message = "No previous purchase found on this account."
                    }
                },
                fill = c.surface,
                contentColor = c.ink,
            )
            DsButton("Not now", onCancel, fill = c.surface, contentColor = c.ink)
        },
    ) {
        Box(Modifier.enter(0)) {
            DsCard(fill = c.lilac, tint = c.lilacTint, outline = c.lilac, radius = Radius.hero) {
                DsIconTile(
                    Icons.Rounded.Share,
                    size = 64.dp,
                    iconSize = 30.dp,
                    fill = if (c.night) c.lilac else Color(0xFF17161C),
                    tint = if (c.night) Color(0xFF17161C) else Color.White,
                )
                Text(
                    "Take your history to your doctor",
                    style = DsType.cardTitle,
                    color = if (c.night) c.ink else Color(0xFF17161C),
                )
                Text(
                    "Exports every run: when, which ear, and how you felt afterwards, as plain " +
                        "text you can send or print. Useful at an appointment booked weeks after " +
                        "the attack, when nobody remembers the detail.",
                    style = DsType.body,
                    color = if (c.night) c.soft else Color(0xCC17161C),
                )
            }
        }

        // The list of what stays free is longer than the thing being sold, and deliberately so.
        // Every clinical feature is on it. A paywall in a health app has to be able to survive
        // being read closely by someone who suspects the worst of it.
        Box(Modifier.enter(1)) {
            DsCard {
                Text("Free forever, with or without this", style = DsType.cardTitle, color = c.ink)
                for (item in listOf(
                    "The safety check",
                    "The six questions that identify the ear",
                    "Unlimited guided treatment runs",
                    "Practice mode and after-care advice",
                    "Your run history on this phone",
                    "The sensor readout",
                )) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Space.m),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = null,
                            tint = c.mint,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(item, style = DsType.rowTitle, color = c.ink)
                    }
                }
            }
        }

        Box(Modifier.enter(2)) {
            DsNote(
                "One payment. No subscription, no account, and nothing leaves your phone unless " +
                    "you send it.",
            )
        }
        if (entitlements.isPlaceholder) {
            Box(Modifier.enter(3)) {
                DsNote(
                    "Test build: the store is not connected yet, so this unlocks without taking " +
                        "payment.",
                )
            }
        }
        message?.let {
            Box(Modifier.enter(4)) { DsWarning("Not charged", it) }
        }
    }
}
