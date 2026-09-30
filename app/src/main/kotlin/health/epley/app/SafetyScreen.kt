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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import health.epley.core.Safety
import health.epley.core.SafetyOutcome

/**
 * The check before every run, in the v2 system (handoff §4.4, §4.5).
 *
 * Unskippable and never behind the paywall. Two questions rather than the handoff's one, because
 * the app's second question carries the trial's exclusion criteria — never diagnosed, a neck or
 * back problem, a recent head injury, an attack unlike the diagnosed ones — and dropping it to
 * match a mockup would remove a clinical gate. Both are styled identically to the handoff's.
 */
@Composable
fun SafetyScreen(
    onFinished: (SafetyOutcome) -> Unit,
    onCancel: () -> Unit,
) {
    var emergency by remember { mutableStateOf<Boolean?>(null) }
    var notToTreat by remember { mutableStateOf<Boolean?>(null) }
    val context = LocalContext.current
    val c = Ds

    when (val outcome = Safety.assess(emergency, notToTreat)) {
        is SafetyOutcome.Incomplete -> {
            val first = outcome.nextQuestion == 1
            DsScreen(
                top = {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = Space.s),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Space.m),
                    ) {
                        DsChip("Safety check", Icons.Rounded.Warning, ChipStyle.Outlined, c.coral)
                        Text(
                            if (first) "Before every run" else "One more",
                            style = DsType.label,
                            color = c.muted,
                        )
                    }
                },
                bottom = {
                    DsButton(
                        label = if (first) "Yes, one or more" else "Yes, one applies",
                        onClick = { if (first) emergency = true else notToTreat = true },
                        fill = c.coral,
                        contentColor = Color(0xFF17161C),
                    )
                    DsButton(
                        label = if (first) "No, none of these" else "No, none apply",
                        onClick = { if (first) emergency = false else notToTreat = false },
                    )
                },
            ) {
                Text(
                    if (first) "Do you have any of these right now?" else "Do any of these apply to you?",
                    style = DsType.title,
                    color = c.ink,
                    modifier = Modifier.padding(start = Space.s).enter(0),
                )
                Text(
                    "This step can't be skipped.",
                    style = DsType.body,
                    color = c.muted,
                    modifier = Modifier.padding(start = Space.s, bottom = Space.s).enter(1),
                )
                FlagCard(
                    items = if (first) Safety.emergencySigns else Safety.reasonsNotToTreat,
                    modifier = Modifier.enter(2),
                )
            }
        }

        // Hard stop. The only way on is out of the app; "Back to home" exists so nobody is
        // trapped, and it never leads to the manoeuvre.
        SafetyOutcome.Emergency -> HardStop(
            chip = "Red flag",
            headline = "Stop. Get emergency help now.",
            body = "These can be signs of a stroke, not BPPV. Do not start the treatment. Call " +
                "your emergency number, or ask someone to call for you.",
            note = "Note the time your symptoms started. Doctors will ask.",
            onCall = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
            onHome = { onFinished(outcome) },
        )

        SafetyOutcome.SeeDoctor -> DsScreen(
            bottom = { DsButton("Back to home", { onFinished(outcome) }) },
        ) {
            Spacer(Modifier.height(Space.l))
            Box(Modifier.enter(0)) {
                DsIconTile(
                    Icons.Rounded.Warning,
                    size = 64.dp,
                    iconSize = 34.dp,
                    fill = if (c.night) c.coralTint else c.coral,
                    tint = if (c.night) c.coral else Color(0xFF17161C),
                )
            }
            Text(
                "See a doctor before using this",
                style = DsType.title,
                color = c.ink,
                modifier = Modifier.padding(start = Space.s).enter(1),
            )
            Text(
                "The head positions aren't safe to do on your own in this situation. A doctor can " +
                    "check what's causing the dizziness and show you what to do.",
                style = DsType.body,
                color = c.muted,
                modifier = Modifier.padding(start = Space.s).enter(2),
            )
        }

        SafetyOutcome.Clear -> DsScreen(
            bottom = { DsButton("Continue", { onFinished(outcome) }) },
        ) {
            Spacer(Modifier.height(Space.l))
            Box(Modifier.enter(0)) {
                DsIconTile(
                    Icons.Rounded.Check,
                    size = 64.dp,
                    iconSize = 34.dp,
                    fill = if (c.night) c.mintTint else c.mint,
                    tint = if (c.night) c.mint else Color(0xFF17161C),
                )
            }
            Text(
                "Safety check passed",
                style = DsType.title,
                color = c.ink,
                modifier = Modifier.padding(start = Space.s).enter(1),
            )
            Text(
                "If any of those signs appear during the manoeuvre, stop and get help.",
                style = DsType.body,
                color = c.muted,
                modifier = Modifier.padding(start = Space.s).enter(2),
            )
        }
    }
}

/**
 * The flags in one card, 52dp rows with hairlines between.
 *
 * A grouped list rather than bullets in a paragraph: each row is its own object, which is how
 * someone skims a list they are frightened of rather than reading it as prose.
 */
@Composable
private fun FlagCard(items: List<String>, modifier: Modifier = Modifier) {
    DsCard(modifier = modifier, padding = Space.s) {
        items.forEachIndexed { index, item ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = Space.m, vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item, style = DsType.rowTitle, color = Ds.ink)
            }
            if (index != items.lastIndex) DsDivider(start = Space.m)
        }
    }
}

/**
 * The hard-stop family: coral floods the screen on day, and becomes a coral-outlined tint card on
 * night so a dark room is never filled with a bright field.
 */
@Composable
private fun HardStop(
    chip: String,
    headline: String,
    body: String,
    note: String?,
    onCall: () -> Unit,
    onHome: () -> Unit,
    extra: (@Composable ColumnScopeShim.() -> Unit)? = null,
) {
    val c = Ds
    DsScreen(
        background = if (c.night) c.ground else c.coral,
        bottom = {
            DsButton(
                label = "Call emergency",
                onClick = onCall,
                fill = if (c.night) c.coral else Color(0xFF17161C),
                contentColor = if (c.night) Color(0xFF17161C) else Color.White,
                leading = Icons.Rounded.Call,
            )
            DsButton(
                label = "Back to home",
                onClick = onHome,
                fill = if (c.night) c.surface else Color(0x33FFFFFF),
                contentColor = c.ink,
            )
        },
    ) {
        val content: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.l)) {
                Box(Modifier.enter(0)) {
                    DsChip(chip, Icons.Rounded.Warning, ChipStyle.Filled)
                }
                Text(
                    headline,
                    style = DsType.result,
                    color = c.ink,
                    modifier = Modifier.enter(1),
                )
                Text(body, style = DsType.body, color = c.ink, modifier = Modifier.enter(2))
                if (note != null) {
                    Box(Modifier.enter(3)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Space.m),
                        ) {
                            Box(
                                Modifier.size(4.dp, 4.dp).clip(RoundedCornerShape(2.dp))
                                    .background(c.ink).padding(top = Space.s),
                            )
                            Text(note, style = DsType.label, color = c.ink)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(Space.l))
        if (c.night) {
            DsCard(fill = c.coralTint, tint = c.coralTint, outline = c.coral) { content() }
        } else {
            content()
        }
    }
}

/** Marker type so [HardStop]'s optional slot compiles without pulling in ColumnScope. */
interface ColumnScopeShim

/**
 * The muted note used by screens not yet moved to the v2 system. Replaced by [DsNote] as each
 * screen is converted.
 */
@Composable
fun InfoNote(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Palette.Surface)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            Icons.Rounded.Warning,
            contentDescription = null,
            tint = Palette.InkFaint,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text,
            color = Palette.InkMuted,
            fontSize = AppType.ReadingFloor,
            lineHeight = AppType.LabelLine,
            fontFamily = AppType.Sans,
        )
    }
}
