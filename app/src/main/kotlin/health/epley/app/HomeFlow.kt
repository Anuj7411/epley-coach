package health.epley.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.Episode
import health.epley.core.EpisodeLog
import health.epley.core.Feeling
import health.epley.core.MountMode
import health.epley.core.Side
import health.epley.core.word
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Every flow screen shares one frame: a step label and progress bar at the top, content in the
 * middle, and the single main action anchored at the bottom within thumb reach (docs/DESIGN.md).
 * No transitions between screens — motion is what the people using this are sensitive to.
 */
@Composable
fun FlowFrame(
    stepLabel: String?,
    progress: Float?,
    onBack: (() -> Unit)?,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        if (stepLabel != null || onBack != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                if (stepLabel != null) {
                    Text(stepLabel, color = Palette.TextSecondary, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(top = 12.dp))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (onBack != null) {
                    TextButton(onClick = onBack) { Text("Back", color = Palette.TextPrimary, fontSize = 16.sp) }
                }
            }
        }
        if (progress != null) {
            Box(
                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Palette.Raised),
            ) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(Palette.Action))
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = bottom,
        )
    }
}

@Composable
fun Title(text: String, color: androidx.compose.ui.graphics.Color = Palette.TextPrimary) =
    Text(text, color = color, fontSize = 28.sp, fontWeight = FontWeight.Medium, lineHeight = 34.sp)

@Composable
fun Body(text: String, secondary: Boolean = false) =
    Text(text, color = if (secondary) Palette.TextSecondary else Palette.TextPrimary, fontSize = 17.sp, lineHeight = 25.sp)

/** A raised card for grouped information. Depth by a lighter grey, not a shadow. */
@Composable
fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Palette.Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

/**
 * The first screen. One sentence of what it is for, one big action for the real thing, one for
 * practice, and the record of past runs — because "BPPV comes back" is the whole premise, and
 * the person it keeps coming back to should see that.
 */
@Composable
fun HomeScreen(
    episodes: List<Episode>,
    onStart: () -> Unit,
    onPractice: () -> Unit,
    onInstrument: () -> Unit,
) {
    FlowFrame(
        stepLabel = null,
        progress = null,
        onBack = null,
        bottom = {
            PrimaryButton("I'm dizzy now — start", onStart)
            SecondaryButton("Practise with the phone", onPractice)
            TextButton(onClick = onInstrument, modifier = Modifier.fillMaxWidth()) {
                Text("Instrument (raw sensor view)", color = Palette.TextSecondary, fontSize = 15.sp)
            }
        },
    ) {
        Spacer(Modifier.height(24.dp))
        Title("Epley Coach")
        Body("Guides the Epley manoeuvre when BPPV a doctor has already diagnosed comes back.", secondary = true)
        Body(
            "It checks it's safe, works out which ear from six questions, then uses the phone's " +
                "own sensor to guide your head into each position — by voice, so you can keep " +
                "your eyes shut.",
        )
        EpisodeHistory(episodes)
    }
}

/** The last few real treatment runs. Practice runs are left out: they measured a phone. */
@Composable
fun EpisodeHistory(episodes: List<Episode>) {
    val treatments = EpisodeLog.treatments(episodes)
    if (treatments.isEmpty()) return
    val format = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())
    Card {
        Text("Your runs", color = Palette.TextSecondary, fontSize = 15.sp)
        for (e in treatments.take(5)) {
            val outcome = when {
                !e.completed -> "stopped early"
                e.feeling == Feeling.BETTER -> "felt better"
                e.feeling == Feeling.SAME -> "no change"
                e.feeling == Feeling.WORSE -> "felt worse"
                else -> "finished"
            }
            Text("${format.format(Date(e.epochMillis))} · ${e.side.word} ear · $outcome", color = Palette.TextPrimary, fontSize = 16.sp)
        }
    }
}

/** Practice: which ear to rehearse. No safety check or questions — the phone is the head. */
@Composable
fun PracticeSideScreen(onSide: (Side) -> Unit, onBack: () -> Unit) {
    FlowFrame("Practice · step 1 of 4", 0.25f, onBack, bottom = {
        ChoiceButton("Right ear", { onSide(Side.RIGHT) })
        ChoiceButton("Left ear", { onSide(Side.LEFT) })
    }) {
        Title("Which ear do you want to practise for?")
        Body(
            "In practice the phone stands in for your head. You hold it in front of you and " +
                "move it the way your head would go. Nothing here is a treatment.",
            secondary = true,
        )
    }
}

/** How the phone is held. Only the two head-mounted choices; practice sets its own. */
@Composable
fun HoldScreen(onMode: (MountMode) -> Unit, onBack: () -> Unit) {
    FlowFrame("Step 3 of 6", 3 / 6f, onBack, bottom = {
        ChoiceButton("Against my cheek — I'll hold it", { onMode(MountMode.CHEEK) })
        ChoiceButton("In a headband or cap", { onMode(MountMode.HEADBAND) })
    }) {
        Title("How will you hold the phone?")
        Body("It has to move with your head, so it goes against the side of your face.", secondary = true)
        Card {
            Text("Against your cheek", color = Palette.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Body("Needs nothing. Hold it flat on your cheekbone, screen facing out, top of the phone toward the top of your head.", secondary = true)
        }
        Card {
            Text("In a headband or cap", color = Palette.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Body("Most accurate, and both hands stay free. Tuck it against the side of your head.", secondary = true)
        }
    }
}

/**
 * Capture the reference pose. The instruction is specific to the mount, and a refused
 * calibration says exactly what to change rather than just failing.
 */
@Composable
fun CalibrateScreen(
    stepLabel: String,
    progress: Float,
    mode: MountMode,
    message: String?,
    onCalibrate: () -> Unit,
    onBack: () -> Unit,
) {
    FlowFrame(stepLabel, progress, onBack, bottom = { PrimaryButton("I'm in position — set", onCalibrate) }) {
        Title(if (mode.tracksTheHead) "Sit up straight and look ahead" else "Hold the phone upright")
        Body(mode.instruction)
        Body(
            if (mode.tracksTheHead) {
                "Keep still for a moment, then tap. This tells the app what \"straight ahead\" looks like for you."
            } else {
                "Screen toward your face, like reading it. Keep it still, then tap."
            },
            secondary = true,
        )
        if (message != null) Text(message, color = Palette.Move, fontSize = 17.sp, lineHeight = 24.sp)
    }
}

/** Learn which way is which, by asking for one turn toward the identified ear. */
@Composable
fun DirectionScreen(
    stepLabel: String,
    progress: Float,
    side: Side,
    practice: Boolean,
    message: String?,
    onLearn: () -> Unit,
    onBack: () -> Unit,
) {
    val word = side.word
    FlowFrame(stepLabel, progress, onBack, bottom = { PrimaryButton("I'm turned to my $word", onLearn) }) {
        Title(if (practice) "Turn the phone to your $word" else "Turn your head to your $word")
        Body(
            if (practice) {
                "Turn it like a key, about halfway, and hold it there. Then tap."
            } else {
                "About halfway to your shoulder, and hold it there. Then tap."
            },
        )
        Body("This teaches the app which direction is which. It can't work that out on its own.", secondary = true)
        if (message != null) Text(message, color = Palette.Move, fontSize = 17.sp, lineHeight = 24.sp)
    }
}

/** Everything confirmed, the stop gesture explained, and one button. */
@Composable
fun ReadyScreen(
    stepLabel: String,
    progress: Float,
    side: Side,
    mode: MountMode,
    practice: Boolean,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    FlowFrame(stepLabel, progress, onBack, bottom = { PrimaryButton("Start", onStart) }) {
        Title("Ready")
        Card {
            Body(if (practice) "Practice · ${side.word} ear" else "${side.word.replaceFirstChar { it.uppercase() }} ear · posterior canal")
            Body(if (practice) "Phone in your hand" else if (mode == MountMode.HEADBAND) "Phone in a headband" else "Phone against your cheek")
            Body("Direction learned")
        }
        if (!practice) {
            Body("Sit on the bed so that when you lie back, your head can hang over the end.", secondary = true)
        }
        Body("The voice will guide you through five positions. You can keep your eyes shut.")
        Text(
            "Press either volume button to stop at any time.",
            color = Palette.Action,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
