package health.epley.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.MountMode
import health.epley.core.Side
import health.epley.core.word

// ---------------------------------------------------------------------------------------------
// The setup steps, in the v2 system.
//
// Only the first of these exists in the handoff (§4.12). The other four are this app's own: the
// handoff's prototype assumes the phone already knows where the head is, and a real one has to be
// told. They are built from the same parts so the run does not change language halfway through.
// ---------------------------------------------------------------------------------------------

/** Back circle plus a caption, the header every setup step carries. */
@Composable
private fun SetupHeader(caption: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        DsIconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
        Text(caption, style = DsType.label, color = Ds.muted)
    }
}

/** Back circle, caption and the step segments, for the four numbered steps. */
@Composable
private fun SetupProgressHeader(caption: String, step: Int, total: Int, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = Space.s),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.l),
        ) {
            DsIconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Text(caption, style = DsType.label, color = Ds.muted)
        }
        DsProgressSegments(total = total, current = step)
    }
}

// ---------------------------------------------------------------------------------------------
// Practice: which side to rehearse (README §4.12)
// ---------------------------------------------------------------------------------------------

/**
 * The only screen in the app where anyone picks an ear.
 *
 * Nothing is measured here and nothing is treated, so the pick costs nothing if it is wrong.
 * Everywhere else the ear comes from the six questions, because a person choosing their own side
 * resolved 42.9% of the time against 72.4% in the trial this app follows.
 */
@Composable
fun PracticeSideScreen(
    onSide: (Side) -> Unit,
    onTakeQuestions: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Ds
    val viewport = LocalViewportHeight.current
    var chosen by remember { mutableStateOf<Side?>(null) }
    val tile = (viewport * 0.50f).coerceIn(230.dp, 400.dp)

    DsScreen(
        top = { SetupHeader("Practice mode", onBack) },
        bottom = {
            DsButton(
                label = "Start practice",
                onClick = { chosen?.let(onSide) },
                enabled = chosen != null,
                trailing = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        },
    ) {
        Text(
            "Which side\nto rehearse?",
            style = DsType.title,
            color = c.ink,
            modifier = Modifier.padding(start = Space.s).enter(0),
        )
        Text(
            "Nothing is measured. Walk through the moves with the phone in your hand before you " +
                "try them lying down.",
            style = DsType.body,
            color = c.muted,
            modifier = Modifier.padding(start = Space.s, bottom = Space.s).enter(1),
        )
        Row(
            Modifier.fillMaxWidth().enter(2),
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            SideTileV2("L", "Left side", chosen == Side.LEFT, tile, Modifier.weight(1f)) {
                chosen = Side.LEFT
            }
            SideTileV2("R", "Right side", chosen == Side.RIGHT, tile, Modifier.weight(1f)) {
                chosen = Side.RIGHT
            }
        }
        Box(Modifier.enter(3)) {
            DsCard(padding = Space.l, onClick = onTakeQuestions) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Space.m),
                ) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(Radius.tile))
                            .background(c.surface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("?", style = DsType.cardTitle, color = c.ink)
                    }
                    Text(
                        "Not sure which ear? Take the 6 questions",
                        style = DsType.rowTitle,
                        color = c.ink,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = c.muted,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/** L / R: the letter large enough to read at arm's length, the word underneath for certainty. */
@Composable
private fun SideTileV2(
    letter: String,
    label: String,
    selected: Boolean,
    minHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = Ds
    val shape = RoundedCornerShape(Radius.card)
    Column(
        modifier
            .heightIn(min = minHeight)
            .pressable(onClick = onClick)
            .clip(shape)
            .background(if (selected) (if (c.night) c.lilacTint else c.lilac) else c.surface)
            .then(if (selected) Modifier.border(3.dp, if (c.night) c.lilac else c.ink, shape) else Modifier)
            .padding(Space.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.s)) {
            RadioMark(selected)
            if (selected) {
                Text(
                    "Selected",
                    style = DsType.rowTitle,
                    color = if (c.night) c.ink else Color(0xFF17161C),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            letter,
            style = DsType.count(72.sp),
            color = if (selected && !c.night) Color(0xFF17161C) else c.ink,
        )
        Spacer(Modifier.height(Space.m))
        Text(
            label,
            style = DsType.cardTitle,
            color = if (selected && !c.night) Color(0xFF17161C) else c.ink,
        )
    }
}

@Composable
private fun RadioMark(selected: Boolean) {
    val c = Ds
    val edge = if (c.night) c.ink else Color(0xFF17161C)
    Box(
        Modifier.size(28.dp).clip(RoundedCornerShape(50)).border(2.5.dp, edge, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(14.dp).clip(RoundedCornerShape(50)).background(edge))
    }
}

// ---------------------------------------------------------------------------------------------
// How the phone is held
// ---------------------------------------------------------------------------------------------

/**
 * Only the two head-mounted choices. Practice sets its own mount, because there the phone is the
 * head and there is nothing to strap it to.
 */
@Composable
fun HoldScreen(step: Int, total: Int, onMode: (MountMode) -> Unit, onBack: () -> Unit) {
    val c = Ds
    DsScreen(top = { SetupProgressHeader("Step $step of $total", step, total, onBack) }) {
        Text(
            "How will you hold the phone?",
            style = DsType.title,
            color = c.ink,
            modifier = Modifier.padding(start = Space.s).enter(0),
        )
        Text(
            "It has to move with your head, so it goes against the side of your face.",
            style = DsType.body,
            color = c.muted,
            modifier = Modifier.padding(start = Space.s, bottom = Space.s).enter(1),
        )
        Box(Modifier.enter(2)) {
            ChoiceCard(
                title = "Against your cheek",
                body = "Needs nothing. Hold it flat on your cheekbone, screen facing out, top of " +
                    "the phone toward the top of your head.",
                onClick = { onMode(MountMode.CHEEK) },
            )
        }
        Box(Modifier.enter(3)) {
            ChoiceCard(
                title = "In a headband or cap",
                body = "Most accurate, and both hands stay free. Tuck it against the side of " +
                    "your head.",
                onClick = { onMode(MountMode.HEADBAND) },
            )
        }
    }
}

/** A tappable option: the whole card is the target, not a button underneath a paragraph. */
@Composable
private fun ChoiceCard(title: String, body: String, onClick: () -> Unit) {
    val c = Ds
    DsCard(padding = Space.l, minHeight = 104.dp, onClick = onClick) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Space.m),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                Text(title, style = DsType.cardTitle, color = c.ink)
                Text(body, style = DsType.label, color = c.muted)
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = c.muted,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Calibrate and learn direction
// ---------------------------------------------------------------------------------------------

/**
 * Capture the reference pose.
 *
 * The button is pressed while the screen can still be seen; the pose is taken afterwards, once
 * the sensor agrees the person has stopped moving. Asking someone to press a button on a screen
 * that is flat against their own cheek does not work — that was found by trying it.
 */
@Composable
fun CalibrateScreen(
    step: Int,
    total: Int,
    stepLabel: String,
    mode: MountMode,
    message: String?,
    countdown: Int?,
    onCalibrate: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Ds
    DsScreen(
        top = { SetupProgressHeader(stepLabel, step, total, onBack) },
        bottom = {
            if (countdown == null) {
                DsButton("Start, then get into position", onCalibrate)
            } else {
                CountdownCardV2(countdown)
            }
        },
    ) {
        Text(
            if (mode.tracksTheHead) "Sit up straight and look ahead" else "Hold the phone upright",
            style = DsType.title,
            color = c.ink,
            modifier = Modifier.padding(start = Space.s).enter(0),
        )
        Box(Modifier.enter(1)) {
            DsCard(radius = Radius.hero) {
                Text(mode.instruction, style = DsType.body, color = c.ink)
            }
        }
        Text(
            if (mode.tracksTheHead) {
                "Tap now, while you can still see the screen. The app counts you down out loud, " +
                    "then captures the position once you are still, so you never have to press " +
                    "anything with the phone against your face."
            } else {
                "Screen toward your face, like reading it. Tap, then hold it still."
            },
            style = DsType.body,
            color = c.muted,
            modifier = Modifier.padding(start = Space.s).enter(2),
        )
        if (message != null) {
            Box(Modifier.enter(3)) { DsWarning("Try again", message) }
        }
    }
}

/**
 * Learn which way is which, by asking for one turn toward the identified ear.
 *
 * The sensor reports a signed rotation, but nothing in it says which sign is the user's right.
 * One deliberate turn settles it; the app cannot work it out alone.
 */
@Composable
fun DirectionScreen(
    step: Int,
    total: Int,
    stepLabel: String,
    side: Side,
    practice: Boolean,
    message: String?,
    countdown: Int?,
    onLearn: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Ds
    val word = side.word
    DsScreen(
        top = { SetupProgressHeader(stepLabel, step, total, onBack) },
        bottom = {
            if (countdown == null) {
                DsButton("Start, then turn to my $word", onLearn)
            } else {
                CountdownCardV2(countdown)
            }
        },
    ) {
        Text(
            if (practice) "Turn the phone to your $word" else "Turn your head to your $word",
            style = DsType.title,
            color = c.ink,
            modifier = Modifier.padding(start = Space.s).enter(0),
        )
        Text(
            if (practice) {
                "Tap now, then turn it like a key, about halfway, and hold it there."
            } else {
                "Tap now, then turn about halfway to your shoulder and hold it there. The app " +
                    "counts you down out loud and captures it once you are still."
            },
            style = DsType.body,
            color = c.ink,
            modifier = Modifier.padding(start = Space.s).enter(1),
        )
        Box(Modifier.enter(2)) {
            DsCard(radius = Radius.hero) {
                // The design's own figure: its first position is exactly this move.
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FigureView(
                        step = 1,
                        ear = if (side == Side.LEFT) 'L' else 'R',
                        night = c.night,
                        playing = true,
                        size = 208,
                        description = "Figure: head turned 45° toward the ${side.word} ear",
                    )
                }
            }
        }
        Box(Modifier.enter(3)) {
            DsNote("This teaches the app which direction is which. It can't work that out on its own.")
        }
        if (message != null) {
            Box(Modifier.enter(4)) { DsWarning("Try again", message) }
        }
    }
}

/** The spoken count is the real one; this is for anyone who can still see the screen. */
@Composable
private fun CountdownCardV2(countdown: Int) {
    val c = Ds
    DsCard(fill = c.lilac, tint = c.lilacTint, outline = c.lilac, radius = Radius.hero) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Space.xs),
        ) {
            Text(
                if (countdown > 0) "$countdown" else "Hold still",
                style = if (countdown > 0) DsType.count(64.sp) else DsType.title,
                color = if (c.night) c.ink else Color(0xFF17161C),
            )
            Text(
                if (countdown > 0) "Get into position" else "Capturing when you stop moving",
                style = DsType.label,
                color = if (c.night) c.soft else Color(0xCC17161C),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Ready
// ---------------------------------------------------------------------------------------------

/** Everything confirmed, the stop gesture explained, and one button. */
@Composable
fun ReadyScreen(
    step: Int,
    total: Int,
    stepLabel: String,
    side: Side,
    mode: MountMode,
    practice: Boolean,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Ds
    DsScreen(
        top = { SetupProgressHeader(stepLabel, step, total, onBack) },
        bottom = { DsButton("Start", onStart, trailing = Icons.AutoMirrored.Rounded.ArrowForward) },
    ) {
        Text(
            "Ready",
            style = DsType.display,
            color = c.ink,
            modifier = Modifier.padding(start = Space.s).enter(0),
        )
        Box(Modifier.enter(1)) {
            DsCard(padding = Space.l) {
                ConfirmedLine(
                    if (practice) {
                        "Practice · ${side.word} ear"
                    } else {
                        "${side.word.replaceFirstChar { it.uppercase() }} ear · posterior canal"
                    },
                )
                DsDivider()
                ConfirmedLine(
                    when {
                        practice -> "Phone in your hand"
                        mode == MountMode.HEADBAND -> "Phone in a headband"
                        else -> "Phone against your cheek"
                    },
                )
                DsDivider()
                ConfirmedLine("Direction learned")
            }
        }
        if (!practice) {
            Text(
                "Sit on the bed so that when you lie back, your head can hang over the end.",
                style = DsType.body,
                color = c.muted,
                modifier = Modifier.padding(start = Space.s).enter(2),
            )
        }
        Text(
            "The voice will guide you through five positions. You can keep your eyes shut.",
            style = DsType.body,
            color = c.ink,
            modifier = Modifier.padding(start = Space.s).enter(3),
        )
        Box(Modifier.enter(4)) {
            DsNote(
                "Hold either volume button for a moment to stop. A quick press still changes " +
                    "the volume.",
            )
        }
    }
}

@Composable
private fun ConfirmedLine(text: String) {
    val c = Ds
    Row(
        Modifier.fillMaxWidth().heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Icon(
            Icons.Rounded.Check,
            contentDescription = null,
            tint = c.mint,
            modifier = Modifier.size(22.dp),
        )
        Text(text, style = DsType.rowTitle, color = c.ink)
    }
}
