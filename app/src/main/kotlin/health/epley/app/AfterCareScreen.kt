package health.epley.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import health.epley.core.AfterCare
import health.epley.core.AfterCareAdvice
import health.epley.core.Epley
import health.epley.core.Feeling

/**
 * Done and after-care, in the v2 system (handoff §4.10).
 *
 * The after-care copy is AAO-HNS 2017 and is used verbatim: there are **no** postural
 * restrictions. Any mockup that says "stay upright" or "sleep propped up" is wrong, and this
 * screen is the one place someone might copy that from.
 */
@Composable
fun AfterCareScreen(
    practice: Boolean,
    runsBefore: Int,
    driftDegrees: Double? = null,
    onDone: (Feeling?) -> Unit,
) {
    var feeling by remember { mutableStateOf<Feeling?>(null) }
    val c = Ds

    if (practice) {
        DsScreen(bottom = { DsButton("Done", { onDone(null) }) }) {
            DoneHeader("Practice complete", "You've been through all five positions")
            Box(Modifier.enter(2)) {
                DsCard {
                    Text(
                        "When you do it for real, the phone goes against your cheek and the voice " +
                            "guides you the same way.",
                        style = DsType.body,
                        color = c.muted,
                    )
                }
            }
        }
        return
    }

    val chosen = feeling
    if (chosen == null) {
        DsScreen(
            bottom = {
                DsButton("Better, no spinning", { feeling = Feeling.BETTER }, fill = c.surface, contentColor = c.ink)
                DsButton("Still dizzy when I move", { feeling = Feeling.SAME }, fill = c.surface, contentColor = c.ink)
                DsButton("Worse than before", { feeling = Feeling.WORSE }, fill = c.surface, contentColor = c.ink)
            },
        ) {
            DoneHeader("All five positions held.", null)
            PositionList(Modifier.enter(2))
            Box(Modifier.enter(3)) {
                DsCard {
                    Text("How do you feel now?", style = DsType.cardTitle, color = c.ink)
                    Text(
                        "Stay sitting for a minute before you stand up.",
                        style = DsType.body,
                        color = c.muted,
                    )
                }
            }
        }
        return
    }

    val (title, body) = when (AfterCare.advise(chosen, runsThisEpisode = runsBefore + 1)) {
        AfterCareAdvice.Done -> Pair(
            "Good. You're done.",
            "If the spinning comes back another day, answer the six questions again. It may be " +
                "a different ear or canal next time.",
        )
        AfterCareAdvice.RepeatInAnHour -> Pair(
            "Try once more in an hour",
            "It often takes more than one go. The trial this app follows had people repeat the " +
                "manoeuvre one hour later. Come back then, and do the safety check again first.",
        )
        AfterCareAdvice.SeeDoctor -> Pair(
            "See a doctor",
            if (chosen == Feeling.WORSE) {
                "Don't repeat the manoeuvre. If you have any of the emergency signs from the " +
                    "safety check, get help now."
            } else {
                "You've tried twice today and it hasn't helped. It may not be the type of BPPV " +
                    "this manoeuvre treats. A doctor can check."
            },
        )
    }

    DsScreen(bottom = { DsButton("Finish", { onDone(chosen) }) }) {
        DoneHeader(title, null)
        Box(Modifier.enter(2)) {
            DsCard {
                Text(body, style = DsType.body, color = c.muted)
            }
        }
        // AAO-HNS 2017, verbatim. No postural restrictions, ever.
        Box(Modifier.enter(3)) {
            DsCard(fill = c.mint, tint = c.mintTint, outline = c.mint) {
                AfterCareLine(
                    Icons.Rounded.Check,
                    "No restrictions afterwards. You can lie flat and sleep normally.",
                )
                AfterCareLine(Icons.Rounded.DateRange, "Still dizzy? Repeat once, an hour later.")
                AfterCareLine(
                    Icons.Rounded.Person,
                    "Worse, or still dizzy after the repeat? See a doctor.",
                )
            }
        }
        if (driftDegrees != null) {
            // The app checking itself: the last position is the one it was calibrated in, so any
            // reading other than zero there is drift it has just measured on this run.
            Box(Modifier.enter(4)) {
                DsNote(
                    "Measured drift on this run: %.1f°. The app checks itself at the last position, "
                        .format(driftDegrees) + "where it knows the answer should be zero.",
                )
            }
        }
    }
}

@Composable
private fun DoneHeader(title: String, subtitle: String?) {
    val c = Ds
    DsCard(
        modifier = Modifier.enter(0),
        fill = c.lilac,
        tint = c.lilacTint,
        outline = c.lilac,
        radius = Radius.hero,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(Radius.tile))
                .background(if (c.night) c.lilac else Color(0xFF17161C)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = if (c.night) Color(0xFF17161C) else Color.White,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(Space.s))
        Text(
            title,
            style = DsType.title,
            color = if (c.night) c.ink else Color(0xFF17161C),
        )
        if (subtitle != null) {
            Text(
                subtitle,
                style = DsType.body,
                color = if (c.night) c.soft else Color(0xCC17161C),
            )
        }
    }
}

/** The five positions with the hold time each one actually required. */
@Composable
private fun PositionList(modifier: Modifier = Modifier) {
    val c = Ds
    DsCard(modifier = modifier, padding = Space.s) {
        Epley.steps().forEachIndexed { index, step ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Space.m, vertical = Space.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}", style = DsType.label, color = c.muted)
                Spacer(Modifier.width(Space.m))
                Text(step.title, style = DsType.rowTitle, color = c.ink, modifier = Modifier.weight(1f))
                Text("${step.holdSeconds} s", style = DsType.label, color = c.muted)
                Spacer(Modifier.width(Space.s))
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = c.mint,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (index != 4) DsDivider(start = Space.m)
        }
    }
}

@Composable
private fun AfterCareLine(icon: ImageVector, text: String) {
    val c = Ds
    val ink = if (c.night) c.ink else Color(0xFF17161C)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.m)) {
        Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
        Text(text, style = DsType.body, color = ink)
    }
}
