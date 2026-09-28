package health.epley.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The one paid thing, and a plain statement of everything that is not.
 *
 * Treating your own vertigo is the app. Charging for any part of it — the safety check most of all
 * — would be charging for care, so the paywall stands in front of exactly one feature: a written
 * history to hand to a doctor. The list of what stays free is on the screen, not just in a README.
 */
@Composable
fun PaywallScreen(
    entitlements: Entitlements,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    var message by remember { mutableStateOf<String?>(null) }

    FlowFrame(
        stepLabel = "History export",
        progress = null,
        onBack = onCancel,
        bottom = {
            PrimaryButton(
                label = "Unlock export · ${entitlements.priceLabel}",
                onClick = {
                    entitlements.purchase { ok ->
                        if (ok) onDone() else message = "That didn't go through. Nothing was charged."
                    }
                },
            )
            SecondaryButton(
                label = "Restore a previous purchase",
                onClick = {
                    entitlements.restore { ok ->
                        if (ok) onDone() else message = "No previous purchase found on this account."
                    }
                },
            )
            SecondaryButton("Not now", onCancel)
        },
    ) {
        Title("Take your history to your doctor")
        Body(
            "Exports every run: when, which ear, and how you felt afterwards, as plain text you " +
                "can send or print. Useful at an appointment booked weeks after the attack, when " +
                "nobody remembers the detail.",
        )
        Card {
            Text("Always free, forever", color = Palette.Action, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Body("The safety check")
            Body("The six questions that identify the ear")
            Body("The guided manoeuvre, voice and vibration")
            Body("After-care advice and your history on this phone")
        }
        Body("One payment. No subscription, no account, and nothing leaves your phone unless you send it.", secondary = true)
        if (entitlements.isPlaceholder) {
            Text(
                "Test build: the store is not connected yet, so this unlocks without taking payment.",
                color = Palette.Move,
                fontSize = 16.sp,
            )
        }
        message?.let { Text(it, color = Palette.Move, fontSize = 17.sp) }
    }
}
