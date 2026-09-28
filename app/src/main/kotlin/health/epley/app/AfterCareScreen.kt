package health.epley.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import health.epley.core.AfterCare
import health.epley.core.AfterCareAdvice
import health.epley.core.Feeling

/**
 * After the manoeuvre: how do you feel, and what to do next.
 *
 * The advice comes from [AfterCare], where the rules and their sources are tested. In practice mode
 * there is nothing to feel better from, so the screen just closes the rehearsal.
 *
 * @param runsBefore treatment runs already recorded in this episode, not counting this one
 * @param driftDegrees error measured at the final position, which is the calibration pose
 */
@Composable
fun AfterCareScreen(
    practice: Boolean,
    runsBefore: Int,
    driftDegrees: Double? = null,
    onDone: (Feeling?) -> Unit,
) {
    var feeling by remember { mutableStateOf<Feeling?>(null) }

    if (practice) {
        FlowFrame("Practice complete", null, null, bottom = { PrimaryButton("Done", { onDone(null) }) }) {
            Title("You've been through all five positions")
            Body(
                "When you do it for real, the phone goes against your cheek and the voice guides " +
                    "you the same way.",
            )
        }
        return
    }

    val chosen = feeling
    if (chosen == null) {
        FlowFrame("Manoeuvre complete", null, null, bottom = {
            ChoiceButton("Better, no spinning", { feeling = Feeling.BETTER })
            ChoiceButton("Still dizzy when I move", { feeling = Feeling.SAME })
            ChoiceButton("Worse than before", { feeling = Feeling.WORSE })
        }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Palette.Holding, modifier = Modifier.size(22.dp))
                Text(
                    "All 5 positions done, saved",
                    color = Palette.Holding,
                    fontSize = AppType.LabelSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppType.Sans,
                )
            }
            Title("How do you feel now?")
            Body("Stay sitting for a minute before you stand up.", secondary = true)
        }
        return
    }

    val (title, body, colour) = when (AfterCare.advise(chosen, runsThisEpisode = runsBefore + 1)) {
        AfterCareAdvice.Done -> Triple(
            "Good. You're done.",
            "If the spinning comes back another day, answer the six questions again. It may be " +
                "a different ear or canal next time.",
            Palette.Action,
        )
        AfterCareAdvice.RepeatInAnHour -> Triple(
            "Try once more in an hour",
            "It often takes more than one go. The trial this app follows had people repeat the " +
                "manoeuvre one hour later. Come back then, and do the safety check again first.",
            Palette.Action,
        )
        AfterCareAdvice.SeeDoctor -> Triple(
            "See a doctor",
            if (chosen == Feeling.WORSE) {
                "Don't repeat the manoeuvre. If you have any of the emergency signs from the " +
                    "safety check, get help now."
            } else {
                "You've tried twice today and it hasn't helped. It may not be the type of BPPV " +
                    "this manoeuvre treats. A doctor can check."
            },
            Palette.Move,
        )
    }
    FlowFrame("Manoeuvre complete", null, null, bottom = { PrimaryButton("Done", { onDone(chosen) }) }) {
        Title(title, color = colour)
        Body(body)
        // Two lines that are true whatever the answer was, each with its own icon, because they
        // are the part people most often get wrong from memory: there are no posture rules, and
        // there is a threshold at which this stops being self-treatable.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = Palette.InkMuted, modifier = Modifier.size(22.dp))
            Body(AfterCare.noRestrictionsNote, secondary = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Palette.Urgent, modifier = Modifier.size(22.dp))
            Body(
                "See a doctor if it isn't better after a few days, keeps coming back, or you " +
                    "notice anything new.",
                secondary = true,
            )
        }
        if (driftDegrees != null) {
            // The app checking itself: the last position is the one it was calibrated in, so any
            // reading other than zero there is drift it has just measured on this run.
            Body(
                "Measured drift on this run: %.1f°. The app checks itself at the last position, ".format(driftDegrees) +
                    "where it knows the answer should be zero.",
                secondary = true,
            )
        }
    }
}
