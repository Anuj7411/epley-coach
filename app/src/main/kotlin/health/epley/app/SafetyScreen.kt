package health.epley.app

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import health.epley.core.Safety
import health.epley.core.SafetyOutcome

/**
 * The check before every run (handoff §4.4, §4.5). Unskippable, never behind the paywall.
 *
 * Question 1 is the design's screen exactly. Question 2 carries the trial's exclusion criteria —
 * never diagnosed, a neck or back problem, a recent head injury, an attack unlike the diagnosed
 * ones — and uses the identical layout: dropping it to match the design would remove a clinical
 * gate. "No" on both goes straight to the questions, as the design does; there is no "passed"
 * screen in between.
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
            SafetyQuestion(
                caption = if (first) "Before every run" else "One more",
                question = if (first) "Do you have any of these right now?" else "Do any of these apply to you?",
                items = if (first) Safety.emergencySigns else Safety.reasonsNotToTreat,
                yes = if (first) "Yes, one or more" else "Yes, one applies",
                no = if (first) "No, none of these" else "No, none apply",
                onYes = { if (first) emergency = true else notToTreat = true },
                onNo = { if (first) emergency = false else notToTreat = false },
            )
        }

        // Hard stop: one action. Back still leaves (MainActivity), so nobody is trapped, and it
        // never leads to the manoeuvre.
        SafetyOutcome.Emergency -> EmergencyStop(
            onCall = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
        )

        SafetyOutcome.SeeDoctor -> SeeDoctorStop(onHome = { onFinished(outcome) })

        SafetyOutcome.Clear -> LaunchedEffect(Unit) { onFinished(outcome) }
    }
}

/** One safety question: chip, headline, the flags in one card, Yes (coral) above No. */
@Composable
fun SafetyQuestion(
    caption: String,
    question: String,
    items: List<String>,
    yes: String,
    no: String,
    onYes: () -> Unit,
    onNo: () -> Unit,
) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        Row(
            Modifier.padding(horizontal = 8.dp).enter(0),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                Modifier
                    .box(if (n) Color.Transparent else c.coral, 999.dp, ring = if (n) c.coral else null)
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("health_and_safety", 20f, if (n) c.coral else Ink, weight = 700)
                Txt("Safety check", type(16f, 700), if (n) c.coral else Ink, maxLines = 1)
            }
            Txt(caption, type(16f, 600), c.muted, maxLines = 1)
        }
        Column(
            Modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 16.dp).enter(1),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Txt(question, type(34f, 800, lineHeight = 1.05f, letterSpacing = -0.03f, wrap = Wrap.Balance), c.ink)
            Txt("This step can’t be skipped.", type(17f, 500, lineHeight = 1.45f, wrap = Wrap.Pretty), c.muted)
        }
        Column(Modifier.enter(2).box(c.surface, 28.dp).padding(horizontal = 24.dp, vertical = 4.dp)) {
            items.forEachIndexed { i, item ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(if (n) c.surface2 else c.ground))
                Box(Modifier.fillMaxWidth().heightIn(min = 52.dp), contentAlignment = Alignment.CenterStart) {
                    Txt(item, type(17f, 600), c.ink)
                }
            }
        }
        Spacer(Modifier.flex())
        PillButton(yes, onYes, fill = c.coral, content = Ink)
        PillButton(no, onNo, fill = if (n) c.lilac else Ink, content = if (n) Ink else Color.White)
    }
}

/** The emergency hard stop (§4.5): coral floods the day screen; night is a coral-ringed card. */
@Composable
fun EmergencyStop(onCall: () -> Unit) {
    val c = Ds
    val n = c.night
    DScreen(bg = if (n) c.ground else c.coral) {
        Column(
            Modifier
                .flex()
                .then(
                    if (n) Modifier.box(c.coralTint, 32.dp, ring = c.coral).padding(24.dp)
                    else Modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp),
                )
                .enter(0),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                Modifier
                    .box(if (n) c.coral else Ink, 999.dp)
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("warning", 20f, if (n) Ink else c.coral, fill = true, weight = 700)
                Txt("Red flag", type(16f, 700), if (n) Ink else c.coral, maxLines = 1)
            }
            Txt(
                "Stop. Get emergency help now.",
                type(if (n) 56f else 64f, 800, lineHeight = 0.95f, letterSpacing = -0.045f, wrap = Wrap.Balance),
                c.ink,
            )
            Txt(
                "Do not start the treatment.",
                type(20f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty),
                if (n) c.soft else Ink,
            )
        }
        PillButton(
            "Call emergency", onCall,
            fill = if (n) c.coral else Ink, content = if (n) Ink else Color.White, icon = "call",
        )
    }
}

/** Question 2 answered yes: the app's own stop, in the same family as "Can't treat". */
@Composable
private fun SeeDoctorStop(onHome: () -> Unit) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        Column(
            Modifier
                .flex()
                .enter(0)
                .box(c.surface, 32.dp, ring = c.ink)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                Modifier
                    .box(Color.Transparent, 999.dp, ring = c.ink)
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("stethoscope", 20f, c.ink, weight = 700)
                Txt("See a doctor", type(16f, 700), c.ink, maxLines = 1)
            }
            Txt(
                "See a doctor before using this",
                type(44f, 800, lineHeight = 1f, letterSpacing = -0.035f, wrap = Wrap.Balance),
                c.ink,
            )
            Txt(
                "The head positions aren’t safe to do on your own in this situation. A doctor " +
                    "can check what’s causing the dizziness and show you what to do.",
                type(19f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty),
                if (n) c.soft else Ink,
            )
        }
        PillButton("Back to home", onHome, fill = if (n) c.lilac else Ink, content = if (n) Ink else Color.White)
    }
}
