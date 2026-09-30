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
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
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
 * The only screen in the app where anyone picks an ear (§4.12, §16 B).
 *
 * Nothing is measured here and nothing is treated, so the pick costs nothing if it is wrong.
 * Everywhere else the ear comes from the six questions, because a person choosing their own side
 * resolved 42.9% of the time against 72.4% in the trial this app follows. It opens with nothing
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
        SetupHeader("Practice · step 1 of 4", 1, 4, onBack)
        SetupTitle(
            "Which side to rehearse?",
            "Nothing is measured. Walk through the moves with the phone in your hand before you try them lying down.",
        )
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
        if (chosen == null) {
            // Disabled (§16 B): line fill, muted text, no press effect.
            Txt("Choose a side to start", type(16f, 600, align = TextAlign.Center), c.muted, modifier = Modifier.fillMaxWidth())
            Row(
                Modifier.heightIn(min = 64.dp).box(c.line, 32.dp).semantics { disabled() },
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Sym("arrow_forward", 28f, c.muted, weight = 700)
                Txt("Start practice", type(20f, 700), c.muted)
            }
        } else {
            PillButton("Start practice", onStart, fill = if (n) c.lilac else Ink, content = if (n) Ink else Color.White, icon = "arrow_forward")
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
            .heightIn(min = 240.dp)
            .pressable(onClick = onClick)
            .box(fill, 32.dp, ring = ring, ringWidth = 3.dp)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            Modifier.heightIn(min = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Sym(if (selected) "radio_button_checked" else "radio_button_unchecked", 28f, c.ink, fill = selected, weight = 700)
            if (selected) Txt("Selected", type(16f, 700), c.ink, maxLines = 1)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Txt(letter, type(112f, 800, lineHeight = 0.8f, letterSpacing = -0.06f), c.ink, maxLines = 1)
            Txt(label, type(20f, 700), c.ink, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Treatment setup, steps 3–6 (handoff v2.1 §16 A). Header: back circle beside the segments.
// ---------------------------------------------------------------------------------------------

/** Where a capture step is: waiting for the tap, counting down, capturing, or failed. */
sealed interface CaptureStage {
    data object Idle : CaptureStage
    data class Count(val n: Int) : CaptureStage
    data object Capture : CaptureStage
    data class Fail(val text: String) : CaptureStage
}

/** The app's countdown and message, as a stage: null countdown is idle, 0 is the capture. */
fun captureStage(countdown: Int?, message: String?): CaptureStage = when {
    countdown != null && countdown > 0 -> CaptureStage.Count(countdown)
    countdown == 0 -> CaptureStage.Capture
    message != null -> CaptureStage.Fail(message)
    else -> CaptureStage.Idle
}

@Composable
private fun SetupHeader(caption: String, step: Int, total: Int, onBack: () -> Unit) {
    val c = Ds
    Row(Modifier.enter(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(48.dp).label("Back").pressable(onClick = onBack).box(c.surface, 24.dp), contentAlignment = Alignment.Center) {
            Sym("arrow_back", 24f, c.ink)
        }
        Column(Modifier.weight(1f).padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressBars(total = total, current = step)
            Txt(caption, type(16f, 600), c.muted, maxLines = 1)
        }
    }
}

@Composable
private fun SetupTitle(title: String, sub: String?) {
    val c = Ds
    Column(
        Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 16.dp).enter(1),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Txt(title, type(34f, 800, lineHeight = 1.05f, letterSpacing = -0.03f, wrap = Wrap.Balance), c.ink)
        if (sub != null) Txt(sub, type(17f, 500, lineHeight = 1.45f, wrap = Wrap.Pretty), c.muted)
    }
}

/** The ring note of the question screens. */
@Composable
private fun SetupNote(text: String) {
    val c = Ds
    Row(
        Modifier.box(Color.Transparent, 24.dp, ring = c.line, ringWidth = 1.5.dp).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Sym("info", 24f, c.muted)
        Txt(text, type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), c.ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit, icon: String? = null) {
    val c = Ds
    PillButton(label, onClick, fill = if (c.night) c.lilac else Ink, content = if (c.night) Ink else Color.White, icon = icon)
}

// ---- How will you hold the phone? ----

/**
 * Only the two head-mounted choices. Practice sets its own mount, because there the phone is the
 * head and there is nothing to strap it to.
 */
@Composable
fun HoldScreen(step: Int, total: Int, onMode: (MountMode) -> Unit, onBack: () -> Unit) {
    val c = Ds
    DScreen(bg = c.ground) {
        SetupHeader("Step $step of $total", step, total, onBack)
        SetupTitle("How will you hold the phone?", "It has to move with your head, so it goes against the side of your face.")
        Box(Modifier.enter(2)) {
            MountTile(
                "face", "Against your cheek",
                "Needs nothing. Flat on your cheekbone, screen facing out, top of the phone toward the top of your head.",
            ) { onMode(MountMode.CHEEK) }
        }
        Box(Modifier.enter(3)) {
            // The design names a "headband" icon Material Symbols does not have; Chrome then draws the
            // literal letters across the tile. The nearest real head-worn icon stands in.
            MountTile(
                "head_mounted_device", "In a headband or cap",
                "Most accurate, and both hands stay free. Tuck it against the side of your head.",
            ) { onMode(MountMode.HEADBAND) }
        }
        Spacer(Modifier.flex())
        SetupNote("Either way, the voice guides you. You can keep your eyes shut.")
    }
}

@Composable
private fun MountTile(icon: String, title: String, body: String, onClick: () -> Unit) {
    val c = Ds
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .box(c.surface, 28.dp)
            .padding(start = 16.dp, end = 20.dp, top = 20.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(48.dp).box(if (c.night) c.surface2 else c.ground, 16.dp), contentAlignment = Alignment.Center) {
            SymOrBlank(icon, 26f, c.ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Txt(title, type(22f, 800, lineHeight = 1.15f, letterSpacing = -0.01f), c.ink)
            Txt(body, type(16f, 500, lineHeight = 1.4f, wrap = Wrap.Pretty), c.muted)
        }
        Sym("chevron_right", 24f, c.muted, modifier = Modifier.padding(top = 12.dp))
    }
}

// ---- Calibrate ----

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
    CalibrateView(
        caption = stepLabel, step = step, total = total,
        practice = !mode.tracksTheHead, band = mode == MountMode.HEADBAND, ear = ear,
        stage = captureStage(countdown, message),
        onStart = onCalibrate, onBack = onBack,
    )
}

@Composable
fun CalibrateView(
    caption: String,
    step: Int,
    total: Int,
    practice: Boolean,
    band: Boolean,
    ear: Char,
    stage: CaptureStage,
    onStart: () -> Unit,
    onBack: () -> Unit,
    figurePlaying: Boolean = false,
) {
    val c = Ds
    val side = if (ear == 'L') "left" else "right"
    DScreen(bg = c.ground) {
        SetupHeader(caption, step, total, onBack)
        SetupTitle(if (practice) "Hold the phone upright" else "Sit up straight and look ahead", null)
        if (!practice) {
            Column(
                Modifier.enter(2).box(c.surface, 32.dp).padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    // Position 5's end pose: seated, facing forward, head level — the pose captured here.
                    FigureView(
                        step = 5, ear = ear, night = c.night, playing = figurePlaying, size = 176,
                        description = "Figure: seated, facing forward, head level, phone on the $side cheek",
                    )
                }
                Txt(
                    if (band) "In the headband, tucked against the side of your head."
                    else "Flat on your cheekbone, screen facing out, top of the phone toward the top of your head.",
                    type(17f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty, align = TextAlign.Center),
                    c.ink,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            Row(
                Modifier.enter(2).box(c.surface, 32.dp).padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(Modifier.size(64.dp).box(if (c.night) c.surface2 else c.ground, 20.dp), contentAlignment = Alignment.Center) {
                    Sym("stay_current_portrait", 34f, c.ink)
                }
                Txt("Hold it upright in front of you, screen facing you.", type(17f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), c.ink, modifier = Modifier.weight(1f))
            }
        }
        if (stage is CaptureStage.Idle || stage is CaptureStage.Count) {
            SetupNote("Tap while you can still see the screen. The app counts you down out loud, then captures the position once you are still.")
        }
        Spacer(Modifier.flex())
        CaptureControls(stage, "Start, then get into position", onStart)
    }
}

// ---- Direction ----

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
    DirectionView(
        caption = stepLabel, step = step, total = total, practice = practice,
        ear = if (side == Side.LEFT) 'L' else 'R', stage = captureStage(countdown, message),
        onStart = onLearn, onBack = onBack, figurePlaying = true,
    )
}

@Composable
fun DirectionView(
    caption: String,
    step: Int,
    total: Int,
    practice: Boolean,
    ear: Char,
    stage: CaptureStage,
    onStart: () -> Unit,
    onBack: () -> Unit,
    figurePlaying: Boolean = false,
) {
    val c = Ds
    val side = if (ear == 'L') "left" else "right"
    DScreen(bg = c.ground) {
        SetupHeader(caption, step, total, onBack)
        SetupTitle(
            if (practice) "Turn the phone to your $side" else "Turn your head to your $side",
            "About halfway to your shoulder, and hold it there.",
        )
        Box(
            Modifier.enter(2).box(c.surface, 32.dp).padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FigureView(
                    step = 1, ear = ear, night = c.night, playing = figurePlaying, size = 176,
                    description = "Figure: seated, head turning 45° toward the $side ear",
                )
            }
        }
        if (stage is CaptureStage.Idle || stage is CaptureStage.Count) {
            SetupNote("This teaches the app which direction is which. It can’t work that out on its own.")
        }
        Spacer(Modifier.flex())
        CaptureControls(stage, "Start, then turn to my $side", onStart)
    }
}

/**
 * The button area of a capture step, by stage (§16 A). The design's min-heights here are CSS
 * content-box, so padding adds to them: 136 + 2 x 16 = 168.
 */
@Composable
private fun CaptureControls(stage: CaptureStage, idleLabel: String, onStart: () -> Unit) {
    val c = Ds
    val n = c.night
    when (stage) {
        CaptureStage.Idle -> PrimaryAction(idleLabel, onStart, icon = "play_arrow")
        is CaptureStage.Count -> Row(
            Modifier.heightIn(min = 168.dp).box(c.surface, 32.dp).padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Swaps with no tween (§16 A): a rolling numeral would be motion for its own sake.
            Txt("${stage.n}", type(112f, 800, lineHeight = 0.8f, letterSpacing = -0.06f, tnum = true, align = TextAlign.Center), c.ink,
                modifier = Modifier.width(72.dp), maxLines = 1)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt("Get into position", type(24f, 800, lineHeight = 1.1f, letterSpacing = -0.02f), c.ink)
                Txt("The voice counts down out loud", type(16f, 600), c.muted)
            }
        }
        CaptureStage.Capture -> Row(
            Modifier.heightIn(min = 168.dp).box(if (n) c.mintTint else c.mint, 32.dp, ring = if (n) c.mint else null)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Box(Modifier.size(64.dp).box(if (n) c.mint else Color.White.copy(alpha = 0.6f), 20.dp), contentAlignment = Alignment.Center) {
                Sym("front_hand", 34f, Ink)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt("Hold still", type(28f, 800, lineHeight = 1.05f, letterSpacing = -0.02f), if (n) c.ink else Ink)
                Txt("Capturing when you stop moving", type(16f, 600), if (n) c.soft else Ink)
            }
        }
        is CaptureStage.Fail -> {
            Row(
                Modifier.box(if (n) c.coralTint else c.coral, 24.dp, ring = if (n) c.coral else null).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Sym("warning", 24f, if (n) c.coral else Ink, fill = true)
                Txt(stage.text, type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), if (n) c.ink else Ink, modifier = Modifier.weight(1f))
            }
            PrimaryAction("Try again", onStart)
        }
    }
}

// ---- Ready ----

/** Everything confirmed, the stop gesture explained, and one button. Flex region: the hero. */
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
    ReadyView(stepLabel, step, total, practice, mode == MountMode.HEADBAND, if (side == Side.LEFT) 'L' else 'R', onStart, onBack)
}

@Composable
fun ReadyView(
    caption: String,
    step: Int,
    total: Int,
    practice: Boolean,
    band: Boolean,
    ear: Char,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Ds
    val n = c.night
    val side = if (ear == 'L') "left" else "right"
    DScreen(bg = c.ground) {
        SetupHeader(caption, step, total, onBack)
        Column(
            Modifier.flex().heightIn(min = 248.dp).enter(1)
                .box(if (n) c.lilacTint else c.lilac, 32.dp, ring = if (n) c.lilac else null).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier
                    .box(if (n) Color.Transparent else Color.White.copy(alpha = 0.55f), 999.dp, ring = if (n) c.lilac else null)
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("check_circle", 20f, if (n) c.lilac else Ink, weight = 700)
                Txt("All set", type(16f, 700), if (n) c.lilac else Ink, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Txt("Ready", type(96f, 800, lineHeight = 0.86f, letterSpacing = -0.055f), if (n) c.ink else Ink, maxLines = 1)
            Txt(
                "The voice will guide you through five positions. You can keep your eyes shut.",
                type(17f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty),
                if (n) c.soft else Ink,
            )
        }
        Column(Modifier.enter(2).box(c.surface, 28.dp).padding(horizontal = 16.dp, vertical = 4.dp)) {
            val rows = listOf(
                if (practice) "Practice · $side side" else (if (ear == 'L') "Left" else "Right") + " ear · posterior canal",
                if (practice) "Phone in your hand" else if (band) "Phone in a headband" else "Phone against your cheek",
                "Direction learned",
            )
            rows.forEachIndexed { i, text ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(if (n) c.surface2 else c.ground))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Sym("check_circle", 24f, if (n) c.mint else Ink, fill = true)
                    Txt(text, type(17f, 700, lineHeight = 1.3f), c.ink, modifier = Modifier.weight(1f))
                }
            }
        }
        if (!practice) SetupNote("Sit on the bed so that when you lie back, your head can hang over the end.")
        SetupNote("Hold either volume button for a moment to stop. A quick press still changes the volume.")
        PrimaryAction("Start", onStart, icon = "arrow_forward")
    }
}
