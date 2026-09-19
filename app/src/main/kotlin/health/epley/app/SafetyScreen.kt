package health.epley.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.Safety
import health.epley.core.SafetyOutcome

/**
 * The safety check before every run: two questions, two taps, never paywalled.
 *
 * The logic and the item lists live in [Safety], where they are tested. This screen only asks.
 */
@Composable
fun SafetyScreen(
    onFinished: (SafetyOutcome) -> Unit,
    onCancel: () -> Unit,
) {
    var emergency by remember { mutableStateOf<Boolean?>(null) }
    var notToTreat by remember { mutableStateOf<Boolean?>(null) }
    val context = LocalContext.current

    when (val outcome = Safety.assess(emergency, notToTreat)) {
        is SafetyOutcome.Incomplete -> {
            val first = outcome.nextQuestion == 1
            FlowFrame(
                stepLabel = "Step 1 of 6 · safety check",
                progress = if (first) 0.08f else 0.14f,
                onBack = onCancel,
                bottom = {
                    ChoiceButton("Yes, one or more", { if (first) emergency = true else notToTreat = true }, container = Palette.Raised)
                    ChoiceButton("No, none of these", { if (first) emergency = false else notToTreat = false }, container = Palette.Raised)
                },
            ) {
                Title(if (first) "Do you have any of these right now?" else "Do any of these apply to you?")
                Card {
                    for (item in if (first) Safety.emergencySigns else Safety.reasonsNotToTreat) {
                        Body("•  $item")
                    }
                }
            }
        }

        SafetyOutcome.Emergency -> FlowFrame(
            stepLabel = "Safety check",
            progress = null,
            onBack = null,
            bottom = {
                Button(
                    // Opens the dialer with the number filled in; the person still presses call.
                    // No permission is needed and nothing is dialled without them.
                    onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Danger, contentColor = Palette.TextPrimary),
                ) { Text("Call emergency · 112", fontSize = 19.sp, fontWeight = FontWeight.Medium) }
                SecondaryButton("Done", { onFinished(outcome) })
            },
        ) {
            Title("Get emergency help now", color = Palette.Danger)
            Body(
                "These can be signs of a stroke, which can look like vertigo. Don't do the " +
                    "manoeuvre. Call your local emergency number — 112 in India and Europe, 911 in the US.",
            )
        }

        SafetyOutcome.SeeDoctor -> FlowFrame(
            stepLabel = "Safety check",
            progress = null,
            onBack = null,
            bottom = { PrimaryButton("Done", { onFinished(outcome) }) },
        ) {
            Title("Don't self-treat today", color = Palette.Move)
            Body(
                "One of those means this may not be the BPPV you were diagnosed with, or the " +
                    "manoeuvre may not be safe for you. See a doctor before doing it.",
            )
        }

        SafetyOutcome.Clear -> FlowFrame(
            stepLabel = "Step 1 of 6 · safety check",
            progress = 1 / 6f,
            onBack = onCancel,
            bottom = { PrimaryButton("Continue", { onFinished(outcome) }) },
        ) {
            Title("Safety check passed", color = Palette.Action)
            Body("If any of those signs appear during the manoeuvre, stop and get help.", secondary = true)
        }
    }
}
