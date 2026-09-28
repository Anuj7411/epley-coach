package health.epley.app

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.Safety
import health.epley.core.SafetyOutcome

/**
 * The safety check before every run: two questions, two taps, never paywalled.
 *
 * ## The one thing this screen must get right
 *
 * The two answers must not look alike. On the first build they were the same grey button, one
 * above the other, which meant a dizzy person could send themselves past a stroke check by muscle
 * memory. The dangerous answer is now an outlined coral pill and the safe one a filled cream one:
 * different shape, different weight, different colour, in that order of importance. A person who
 * cannot read the words still cannot confuse them.
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
                stepLabel = if (first) "Safety check 1 of 2" else "Safety check 2 of 2",
                progress = if (first) 0.08f else 0.14f,
                onBack = onCancel,
                bottom = {
                    DangerOutlineButton(
                        label = if (first) "Yes, I have one of these" else "Yes, one applies",
                        onClick = { if (first) emergency = true else notToTreat = true },
                    )
                    PrimaryButton(
                        if (first) "No, none of these" else "No, none apply",
                        { if (first) emergency = false else notToTreat = false },
                    )
                },
            ) {
                Title(if (first) "Do you have any of these right now?" else "Do any of these apply to you?")
                WarningList(if (first) Safety.emergencySigns else Safety.reasonsNotToTreat, urgent = first)
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
                    shape = RoundedCornerShape(percent = 50),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Palette.Urgent, contentColor = Palette.Ground),
                ) {
                    Text(
                        "Call 112",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = AppType.Sans,
                    )
                }
                SecondaryButton("Back to home", { onFinished(outcome) })
            },
        ) {
            HeroTile(Icons.Filled.Warning, filled = true)
            Text(
                "Stop. Get emergency help now.",
                color = Palette.Ink,
                fontSize = AppType.DisplaySize,
                lineHeight = AppType.DisplayLine,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = AppType.Sans,
            )
            Text(
                "These can be signs of a stroke, not BPPV. Don't do the head positions. Call your " +
                    "emergency number, or ask someone to call for you.",
                color = Palette.InkBody,
                fontSize = AppType.BodyLargeSize,
                lineHeight = AppType.BodyLargeLine,
                fontFamily = AppType.Sans,
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Divider))
            InfoNote("Note the time your symptoms started. Doctors will ask.")
        }

        SafetyOutcome.SeeDoctor -> FlowFrame(
            stepLabel = "Safety check",
            progress = null,
            onBack = null,
            bottom = { PrimaryButton("Back to home", { onFinished(outcome) }) },
        ) {
            HeroTile(Icons.Filled.Warning, filled = false)
            Text(
                "See a doctor before using this",
                color = Palette.Ink,
                fontSize = AppType.DisplaySize,
                lineHeight = AppType.DisplayLine,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = AppType.Sans,
            )
            Text(
                "The head positions aren't safe to do on your own in this situation. A doctor can " +
                    "check what's causing the dizziness and show you what to do.",
                color = Palette.InkBody,
                fontSize = AppType.BodyLargeSize,
                lineHeight = AppType.BodyLargeLine,
                fontFamily = AppType.Sans,
            )
        }

        SafetyOutcome.Clear -> FlowFrame(
            stepLabel = "Safety check",
            progress = 1 / 6f,
            onBack = onCancel,
            bottom = { PrimaryButton("Continue", { onFinished(outcome) }) },
        ) {
            HeroTile(Icons.Filled.Check, filled = true, colour = Palette.Holding)
            Title("Safety check passed")
            Body("If any of those signs appear during the manoeuvre, stop and get help.", secondary = true)
        }
    }
}

/**
 * The six or four items, one per row.
 *
 * A grouped list rather than bullets in a paragraph. Each row is its own object with its own icon,
 * which is how someone skims a list they are frightened of rather than reading it as prose. The
 * previous version wrapped mid-item and hung the remainder at the left margin, which made a
 * six-item list look like nine.
 */
@Composable
private fun WarningList(items: List<String>, urgent: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEachIndexed { index, item ->
            val top = if (index == 0) 20.dp else 6.dp
            val bottom = if (index == items.lastIndex) 20.dp else 6.dp
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                    .background(Palette.Surface)
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (urgent) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Palette.Urgent,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Text(
                    item,
                    color = Palette.Ink,
                    fontSize = AppType.BodySize,
                    lineHeight = AppType.BodyLine,
                    fontFamily = AppType.Sans,
                )
            }
        }
    }
}

/** A quiet aside, never a warning. Icon plus muted text, no box. */
@Composable
fun InfoNote(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = Palette.InkMuted, modifier = Modifier.size(20.dp))
        Text(
            text,
            color = Palette.InkMuted,
            fontSize = AppType.ReadingFloor,
            lineHeight = 23.sp,
            fontFamily = AppType.Sans,
        )
    }
}

/** A 72dp icon tile, so an outcome screen states its nature before a word is read. */
@Composable
private fun HeroTile(icon: androidx.compose.ui.graphics.vector.ImageVector, filled: Boolean, colour: androidx.compose.ui.graphics.Color = Palette.Urgent) {
    Box(
        Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (filled) colour else Palette.Surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (filled) Palette.Ground else colour,
            modifier = Modifier.size(40.dp),
        )
    }
}
