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
/** Back circle, caption and the step segments, for the four numbered steps. */
// ---------------------------------------------------------------------------------------------
// Practice: which side to rehearse (README §4.12)
// ---------------------------------------------------------------------------------------------

/**
 * The only screen in the app where anyone picks an ear (§4.12).
 *
 * Nothing is measured here and nothing is treated, so the pick costs nothing if it is wrong.
 * Everywhere else the ear comes from the six questions, because a person choosing their own side
 * resolved 42.9% of the time against 72.4% in the trial this app follows. It starts with nothing
 * selected: a pre-ticked side is the habit the rest of the app exists to break.
 */
@Composable
fun PracticeSideScreen(
    onSide: (Side) -> Unit,
    onTakeQuestions: () -> Unit,
    onBack: () -> Unit,
) {
    var chosen by remember { mutableStateOf<Side?>(null) }
    PracticeSideView(chosen, onChoose = { chosen = it }, onTakeQuestions, onStart = { chosen?.let(onSide) }, onBack)
}

/** Flex region: the spacer above Start practice. */
@Composable
fun PracticeSideView(
    chosen: Side?,
    onChoose: (Side) -> Unit,
    onTakeQuestions: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        Row(
            Modifier.padding(end = 8.dp).enter(0),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(48.dp).pressable(onClick = onBack).box(c.surface, 24.dp), contentAlignment = Alignment.Center) {
                Sym("arrow_back", 24f, c.ink)
            }
            Txt("Practice mode", type(16f, 600), c.muted)
        }
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 24.dp).enter(1),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Txt("Which side to rehearse?", type(34f, 800, lineHeight = 1.05f, letterSpacing = -0.03f, wrap = Wrap.Balance), c.ink)
            Txt(
                "Nothing is measured. Walk through the moves with the phone in your hand before you try them lying down.",
                type(17f, 500, lineHeight = 1.45f, wrap = Wrap.Pretty),
                c.muted,
            )
        }
        Row(Modifier.enter(2), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SideChoice("L", "Left side", chosen == Side.LEFT, Modifier.weight(1f)) { onChoose(Side.LEFT) }
            SideChoice("R", "Right side", chosen == Side.RIGHT, Modifier.weight(1f)) { onChoose(Side.RIGHT) }
        }
        Row(
            Modifier.enter(3).heightIn(min = 80.dp).pressable(onClick = onTakeQuestions).box(c.surface, 28.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(48.dp).box(if (n) c.surface2 else c.ground, 16.dp), contentAlignment = Alignment.Center) {
                Sym("help", 24f, c.ink)
            }
            Txt("Not sure which ear? Take the 6 questions", type(17f, 700), c.ink, modifier = Modifier.weight(1f))
            Sym("chevron_right", 24f, c.muted)
        }
        Spacer(Modifier.flex())
        // Start practice: text left, arrow right (padding 0 24 0 32). Muted until a side is picked.
        val ready = chosen != null
        Row(
            Modifier
                .heightIn(min = 64.dp)
                .pressable(enabled = ready, onClick = onStart)
                .box(if (!ready) c.surface2 else if (n) c.lilac else Ink, 32.dp)
                .padding(start = 32.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val fg = if (!ready) c.muted else if (n) Ink else Color.White
            Txt("Start practice", type(20f, 700), fg)
            Sym("arrow_forward", 28f, fg, weight = 700)
        }
    }
}

/** L / R: the letter large enough to read at arm's length, the word underneath for certainty. */
@Composable
private fun SideChoice(letter: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Ds
    val n = c.night
    // M5: selection cross-fades its colours over 200 ms; nothing moves.
    val reduced = LocalReducedMotion.current
    val spec = if (reduced) androidx.compose.animation.core.snap() else androidx.compose.animation.core.tween<Color>(Motion.TOGGLE, easing = StandardEase)
    val fill by androidx.compose.animation.animateColorAsState(if (!selected) c.surface else if (n) c.lilacTint else c.lilac, spec, label = "fill")
    val ring by androidx.compose.animation.animateColorAsState(if (!selected) Color.Transparent else if (n) c.lilac else Ink, spec, label = "ring")
    Column(
        modifier
            .heightIn(min = 280.dp)
            .pressable(onClick = onClick)
            .box(fill, 32.dp, ring = ring, ringWidth = 3.dp)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (selected) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sym("radio_button_checked", 28f, if (n) c.lilac else Ink, fill = true, weight = 700)
                Txt("Selected", type(16f, 700), if (n) c.lilac else Ink, maxLines = 1)
            }
        } else {
            Sym("radio_button_unchecked", 28f, c.muted)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Txt(
                letter,
                type(112f, 800, lineHeight = 0.8f, letterSpacing = -0.06f),
                when {
                    !n -> Ink
                    selected -> c.lilac
                    else -> c.muted
                },
                maxLines = 1,
            )
            Txt(label, type(20f, 700), if (n) c.ink else Ink, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// How the phone is held (app-only; built in the design's language: the Question layout)
// ---------------------------------------------------------------------------------------------

/** The header of the numbered setup steps: back circle, "Step N of 6", then the segments. */
@Composable
private fun StepHeader(caption: String, step: Int, total: Int, onBack: () -> Unit) {
    val c = Ds
    Column(Modifier.enter(0), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(48.dp).pressable(onClick = onBack).box(c.surface, 24.dp), contentAlignment = Alignment.Center) {
                Sym("arrow_back", 24f, c.ink)
            }
            Txt(caption, type(16f, 600), c.muted, maxLines = 1)
        }
        Box(Modifier.padding(horizontal = 8.dp)) { ProgressBars(total = total, current = step) }
    }
}

/** Title and sub-text as the question screens set them. */
@Composable
private fun StepTitle(title: String, sub: String?, index: Int = 1) {
    val c = Ds
    Column(
        Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp).enter(index),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Txt(title, type(34f, 800, lineHeight = 1.05f, letterSpacing = -0.03f, wrap = Wrap.Balance), c.ink)
        if (sub != null) Txt(sub, type(17f, 500, lineHeight = 1.45f, wrap = Wrap.Pretty), c.muted)
    }
}

/** The ring note, as on the question screens. */
@Composable
private fun StepNote(text: String, icon: String = "info") {
    val c = Ds
    Row(
        Modifier.box(Color.Transparent, 24.dp, ring = c.line, ringWidth = 1.5.dp).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Sym(icon, 24f, c.muted)
        Txt(text, type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), c.ink, modifier = Modifier.weight(1f))
    }
}

/** A failed capture: coral card, as the design's warning family. */
@Composable
private fun StepWarning(text: String) {
    val c = Ds
    val n = c.night
    Row(
        Modifier.box(if (n) c.coralTint else c.coral, 28.dp, ring = if (n) c.coral else null).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(48.dp).box(if (n) c.coral else Color.White.copy(alpha = 0.6f), 16.dp), contentAlignment = Alignment.Center) {
            Sym("warning", 24f, Ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Txt("TRY AGAIN", type(14f, 800, letterSpacing = 0.06f), if (n) c.coral else Ink)
            Txt(text, type(16f, 600, lineHeight = 1.35f, wrap = Wrap.Pretty), if (n) c.ink else Ink)
        }
    }
}

/**
 * Only the two head-mounted choices. Practice sets its own mount, because there the phone is the
 * head and there is nothing to strap it to. Laid out as the two-option questions (§4.6).
 */
@Composable
fun HoldScreen(step: Int, total: Int, onMode: (MountMode) -> Unit, onBack: () -> Unit) {
    val c = Ds
    DScreen(bg = c.ground) {
        StepHeader("Step $step of $total", step, total, onBack)
        StepTitle("How will you hold the phone?", "It has to move with your head, so it goes against the side of your face.")
        Column(Modifier.enter(2), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceCard(
                "Against your cheek",
                "Needs nothing. Flat on your cheekbone, screen facing out, top of the phone toward the top of your head.",
                "back_hand",
            ) { onMode(MountMode.CHEEK) }
            ChoiceCard(
                "In a headband or cap",
                "Most accurate, and both hands stay free. Tuck it against the side of your head.",
                "check_circle",
            ) { onMode(MountMode.HEADBAND) }
        }
        Spacer(Modifier.flex())
        StepNote("Either way, the voice guides you. You can keep your eyes shut.")
    }
}

@Composable
private fun ChoiceCard(title: String, body: String, icon: String, onClick: () -> Unit) {
    val c = Ds
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .pressable(onClick = onClick)
            .box(c.surface, 28.dp)
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(48.dp).box(if (c.night) c.surface2 else c.ground, 16.dp), contentAlignment = Alignment.Center) {
            Sym(icon, 24f, c.ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Txt(title, type(22f, 800, lineHeight = 1.15f, letterSpacing = -0.01f), c.ink)
            Txt(body, type(16f, 600, lineHeight = 1.35f, wrap = Wrap.Pretty), c.muted)
        }
        Sym("chevron_right", 24f, c.muted)
    }
}

// ---------------------------------------------------------------------------------------------
// Calibrate and learn direction
// ---------------------------------------------------------------------------------------------

/**
 * Capture the reference pose. The button is pressed while the screen can still be seen; the pose
 * is taken afterwards, once the sensor agrees the person has stopped moving. Asking someone to
 * press a button on a screen flat against their own cheek does not work — found by trying it.
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
    ear: Char = 'R',
) {
    val c = Ds
    DScreen(bg = c.ground) {
        StepHeader(stepLabel, step, total, onBack)
        StepTitle(
            if (mode.tracksTheHead) "Sit up straight and look ahead" else "Hold the phone upright",
            null,
        )
        Column(
            Modifier.enter(2).box(c.surface, 32.dp).padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // The reference pose, from the design's own renderer: position 5's end pose is seated,
            // facing forward, head level, phone on the cheek — exactly what is captured here. In
            // practice the phone is the head, so no head is drawn.
            if (mode.tracksTheHead) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    FigureView(
                        step = 5, ear = ear, night = c.night, playing = false, size = 208,
                        description = "Figure: sitting up, facing forward, head level, phone on the cheek",
                    )
                }
            }
            Txt(mode.instruction, type(17f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), c.ink)
        }
        if (message != null) Box(Modifier.enter(3)) { StepWarning(message) }
        Spacer(Modifier.flex())
        if (countdown == null) {
            StepNote(
                if (mode.tracksTheHead) {
                    "Tap while you can still see the screen. The app counts you down out loud, then " +
                        "captures the position once you are still."
                } else {
                    "Screen toward your face, like reading it. Tap, then hold it still."
                },
            )
            PillButton("Start, then get into position", onCalibrate, fill = if (c.night) c.lilac else Ink, content = if (c.night) Ink else Color.White)
        } else {
            CountdownCardV2(countdown)
        }
    }
}

/**
 * Learn which way is which, by asking for one turn toward the identified ear. The sensor reports
 * a signed rotation, but nothing in it says which sign is the user's right; one turn settles it.
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
    DScreen(bg = c.ground) {
        StepHeader(stepLabel, step, total, onBack)
        StepTitle(
            if (practice) "Turn the phone to your $word" else "Turn your head to your $word",
            if (practice) "Turn it like a key, about halfway, and hold it there."
            else "About halfway to your shoulder, and hold it there.",
        )
        Box(Modifier.enter(2).box(c.surface, 32.dp).padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 16.dp)) {
            // The design's own figure: its first position is exactly this move.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FigureView(
                    step = 1,
                    ear = if (side == Side.LEFT) 'L' else 'R',
                    night = c.night,
                    playing = true,
                    size = 208,
                    description = "Figure: head turned 45° toward the $word ear",
                )
            }
        }
        if (message != null) Box(Modifier.enter(3)) { StepWarning(message) }
        Spacer(Modifier.flex())
        if (countdown == null) {
            StepNote("This teaches the app which direction is which. It can’t work that out on its own.")
            PillButton("Start, then turn to my $word", onLearn, fill = if (c.night) c.lilac else Ink, content = if (c.night) Ink else Color.White)
        } else {
            CountdownCardV2(countdown)
        }
    }
}

/** The spoken count is the real one; this is for anyone who can still see the screen. */
@Composable
private fun CountdownCardV2(countdown: Int) {
    val c = Ds
    val n = c.night
    Column(
        Modifier.fillMaxWidth().box(if (n) c.lilacTint else c.lilac, 32.dp, ring = if (n) c.lilac else null).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (countdown > 0) {
            Txt("$countdown", type(96f, 800, lineHeight = 0.86f, letterSpacing = -0.055f, tnum = true), if (n) c.ink else Ink, maxLines = 1)
            Txt("Get into position", type(20f, 700), if (n) c.soft else Ink, maxLines = 1)
        } else {
            Txt("Hold still", type(44f, 800, lineHeight = 1f, letterSpacing = -0.035f), if (n) c.ink else Ink, maxLines = 1)
            Txt("Capturing when you stop moving", type(17f, 600, align = TextAlign.Center), if (n) c.soft else Ink)
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
    val n = c.night
    DScreen(bg = c.ground) {
        StepHeader(stepLabel, step, total, onBack)
        Column(
            Modifier.flex().enter(1).box(if (n) c.lilacTint else c.lilac, 32.dp, ring = if (n) c.lilac else null).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier
                    .box(if (n) Color.Transparent else Color.White.copy(alpha = 0.55f), 999.dp, ring = if (n) c.lilac else null)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("check_circle", 20f, if (n) c.lilac else Ink, weight = 600)
                Txt("All set", type(16f, 600), if (n) c.lilac else Ink, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Txt("Ready", type(96f, 800, lineHeight = 0.86f, letterSpacing = -0.055f), if (n) c.ink else Ink, maxLines = 1)
            Txt(
                "The voice will guide you through five positions. You can keep your eyes shut.",
                type(17f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty),
                if (n) c.soft else Ink,
            )
        }
        Column(Modifier.enter(2).box(c.surface, 28.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
            ConfirmedLine(
                if (practice) "Practice · ${side.word} side"
                else "${side.word.replaceFirstChar { it.uppercase() }} ear · posterior canal",
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(if (n) c.surface2 else c.ground))
            ConfirmedLine(
                when {
                    practice -> "Phone in your hand"
                    mode == MountMode.HEADBAND -> "Phone in a headband"
                    else -> "Phone against your cheek"
                },
            )
            Box(Modifier.fillMaxWidth().height(1.dp).background(if (n) c.surface2 else c.ground))
            ConfirmedLine("Direction learned")
        }
        if (!practice) StepNote("Sit on the bed so that when you lie back, your head can hang over the end.", icon = "bed")
        StepNote("Hold either volume button for a moment to stop. A quick press still changes the volume.")
        PillButton("Start", onStart, fill = if (n) c.lilac else Ink, content = if (n) Ink else Color.White, icon = "arrow_forward")
    }
}

@Composable
private fun ConfirmedLine(text: String) {
    val c = Ds
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Sym("check_circle", 22f, if (c.night) c.mint else Ink, fill = true)
        Txt(text, type(17f, 600), c.ink, modifier = Modifier.weight(1f))
    }
}
