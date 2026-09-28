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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
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
                    Text(
                        stepLabel,
                        color = Palette.InkMuted,
                        fontSize = AppType.LabelSize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppType.Sans,
                        modifier = Modifier.weight(1f).padding(top = 12.dp),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Text(
                            "Back",
                            color = Palette.Ink,
                            fontSize = AppType.ReadingFloor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppType.Sans,
                        )
                    }
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

/**
 * The one thing a screen asks. Set heavy and tight: weight carries the hierarchy so that size does
 * not have to, which keeps the whole screen inside a 160% system font scale.
 */
@Composable
fun Title(text: String, color: androidx.compose.ui.graphics.Color = Palette.Ink) =
    Text(
        text,
        color = color,
        fontSize = AppType.TitleSize,
        lineHeight = AppType.TitleLine,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = AppType.Sans,
    )

/**
 * Body copy, at 17sp with 24sp leading.
 *
 * Secondary text is InkMuted rather than a lower alpha: a faded white on a warm dark ground loses
 * contrast faster than a chosen grey, and everything here has to clear 7:1.
 */
@Composable
fun Body(text: String, secondary: Boolean = false) =
    Text(
        text,
        color = if (secondary) Palette.InkMuted else Palette.InkBody,
        fontSize = AppType.BodySize,
        lineHeight = AppType.BodyLine,
        fontFamily = AppType.Sans,
    )

/** A card for grouped information. Depth by a lighter surface, never a shadow. */
@Composable
fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
    onExport: (() -> Unit)?,
    onInstrument: () -> Unit,
) {
    FlowFrame(
        stepLabel = null,
        progress = null,
        onBack = null,
        bottom = {
            // Ordered by how often it is wanted, with the most-wanted nearest the thumb.
            TextButton(onClick = onInstrument, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(
                    "Raw sensor readout",
                    color = Palette.InkMuted,
                    fontSize = AppType.ReadingFloor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppType.Sans,
                )
            }
            SecondaryButton("Practise in your hand", onPractice)
            PrimaryButton("Start treatment", onStart)
        },
    ) {
        Spacer(Modifier.height(28.dp))
        Text(
            "Epley Coach",
            color = Palette.Ink,
            fontSize = AppType.DisplaySize,
            lineHeight = AppType.DisplayLine,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = AppType.Sans,
        )
        Text(
            "Talks you through the five head positions that treat BPPV vertigo, using your phone " +
                "to check each one.",
            color = Palette.InkMuted,
            fontSize = AppType.BodyLargeSize,
            lineHeight = AppType.BodyLargeLine,
            fontFamily = AppType.Sans,
        )
        EpisodeHistory(episodes, onExport)
    }
}

/**
 * The last few real treatment runs. Practice runs are left out: they measured a phone.
 *
 * Each row states its outcome in a word *and* an icon *and* a colour, so a stopped run is
 * distinguishable from a completed one without seeing hue at all. BPPV coming back is the whole
 * premise of the app, so the person it keeps happening to should be able to see the pattern.
 */
@Composable
fun EpisodeHistory(episodes: List<Episode>, onExport: (() -> Unit)?) {
    val treatments = EpisodeLog.treatments(episodes)
    if (treatments.isEmpty()) {
        Spacer(Modifier.height(8.dp))
        Card {
            Body("Your runs will appear here.", secondary = true)
        }
        return
    }
    val format = SimpleDateFormat("EEE d MMM", Locale.getDefault())

    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Your runs",
            color = Palette.InkMuted,
            fontSize = AppType.LabelSize,
            fontWeight = FontWeight.Bold,
            fontFamily = AppType.Sans,
            modifier = Modifier.weight(1f),
        )
        if (onExport != null) {
            TextButton(onClick = onExport, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(
                    "Export",
                    color = Palette.Selected,
                    fontSize = AppType.LabelSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppType.Sans,
                )
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        val rows = treatments.take(5)
        rows.forEachIndexed { index, e ->
            val completed = e.completed
            val outcome = when {
                !completed -> "Stopped at ${e.positionsCompleted}"
                e.feeling == Feeling.BETTER -> "Completed, felt better"
                e.feeling == Feeling.SAME -> "Completed, no change"
                e.feeling == Feeling.WORSE -> "Completed, felt worse"
                else -> "Completed"
            }
            // Grouped-list radii: the outer corners of the group are round, the inner ones tight,
            // so a run of rows reads as one object rather than a stack of separate cards.
            val top = if (index == 0) 20.dp else 6.dp
            val bottom = if (index == rows.lastIndex) 20.dp else 6.dp
            // The outcome carries no weight, so the Row measures it first and hands it the width
            // the date needed — at 160% text "Completed, felt better" left the date broken one
            // word per line. Past the cutoff the outcome drops to its own line, where both read
            // whole. Side by side the pair needs approximately 256dp x scale + 28dp; a 360dp
            // phone has 328dp to give, so it runs out just past 115%. Wider phones could hold on
            // until about 145%, but one cutoff for every screen is worth more than the few extra
            // scales a second rule would buy.
            val stacked = LocalDensity.current.fontScale >= 1.15f
            val rowSurface = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                .background(Palette.Surface)
                .heightIn(min = 64.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp)
            val dateAndEar = @Composable {
                Column {
                    Text(
                        format.format(Date(e.epochMillis)),
                        color = Palette.Ink,
                        fontSize = AppType.BodyLargeSize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppType.Sans,
                    )
                    Text(
                        "${e.side.word.replaceFirstChar { it.uppercase() }} ear",
                        color = Palette.InkMuted,
                        fontSize = AppType.ReadingFloor,
                        fontFamily = AppType.Sans,
                    )
                }
            }
            val verdict = @Composable {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (completed) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (completed) Palette.Holding else Palette.Urgent,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        outcome,
                        color = if (completed) Palette.Holding else Palette.Urgent,
                        fontSize = AppType.ReadingFloor,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppType.Sans,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
            if (stacked) {
                Column(rowSurface) {
                    dateAndEar()
                    Spacer(Modifier.height(6.dp))
                    verdict()
                }
            } else {
                Row(rowSurface, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { dateAndEar() }
                    verdict()
                }
            }
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
        ChoiceButton("Against my cheek, I'll hold it", { onMode(MountMode.CHEEK) })
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
    countdown: Int?,
    onCalibrate: () -> Unit,
    onBack: () -> Unit,
) {
    FlowFrame(
        stepLabel,
        progress,
        onBack,
        bottom = {
            if (countdown == null) {
                PrimaryButton("Start, then get into position", onCalibrate)
            } else {
                CountdownCard(countdown)
            }
        },
    ) {
        Title(if (mode.tracksTheHead) "Sit up straight and look ahead" else "Hold the phone upright")
        Body(mode.instruction)
        Body(
            if (mode.tracksTheHead) {
                "Tap now, while you can still see the screen. The app counts you down out loud, " +
                    "then captures the position once you are still, so you never have to press " +
                    "anything with the phone against your face."
            } else {
                "Screen toward your face, like reading it. Tap, then hold it still."
            },
            secondary = true,
        )
        if (message != null) Text(message, color = Palette.Move, fontSize = 17.sp, lineHeight = 24.sp)
    }
}

/** The countdown, for anyone who can still see the screen. The spoken count is the real one. */
@Composable
private fun CountdownCard(countdown: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            if (countdown > 0) "$countdown" else "Hold still",
            color = Palette.Action,
            fontSize = if (countdown > 0) 64.sp else 30.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            if (countdown > 0) "Get into position" else "Capturing when you stop moving",
            color = Palette.TextSecondary,
            fontSize = 17.sp,
        )
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
    countdown: Int?,
    onLearn: () -> Unit,
    onBack: () -> Unit,
) {
    val word = side.word
    FlowFrame(
        stepLabel,
        progress,
        onBack,
        bottom = {
            if (countdown == null) {
                PrimaryButton("Start, then turn to my $word", onLearn)
            } else {
                CountdownCard(countdown)
            }
        },
    ) {
        Title(if (practice) "Turn the phone to your $word" else "Turn your head to your $word")
        Body(
            if (practice) {
                "Tap now, then turn it like a key, about halfway, and hold it there."
            } else {
                "Tap now, then turn about halfway to your shoulder and hold it there. The app " +
                    "counts you down out loud and captures it once you are still."
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
            "Hold either volume button for a moment to stop. A quick press still changes the volume.",
            color = Palette.Action,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
