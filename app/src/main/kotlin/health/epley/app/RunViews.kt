package health.epley.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/*
 * The run screens (handoff §4.8 Find, §4.9 Hold, §4b), as pure views over a display model: the
 * app fills the model from the live engine (RunScreen), the parity tests from the design's own
 * sample values. The README is explicit that the design's angle ranges are layout placeholders and
 * the app's thresholds are the ones to show, so only the model differs between the two.
 */

/** What Find shows. Angles are already formatted, with the design's minus sign (U+2212). */
data class FindModel(
    val position: Int,
    val ear: Char,
    val title: String,
    val sub: String,
    val direction: String,
    val remaining: String,
    val now: String,
    val aim: String,
    val zoneStart: Float,
    val zoneWidth: Float,
    val marker: Float,
    /** Practice: a pill on the instruction card, the stand-in sub-text, and "Stop practice" (§16 B). */
    val practice: Boolean = false,
    /** The phone turned faster than a head can: the instruction card becomes a warning (§16 I). */
    val moved: Boolean = false,
    /** The other angle — the one this position is not led by — so both can be seen at once. */
    val second: AxisModel? = null,
)

/** One angle on its own scale: where it is now, where it has to be, and whether it is there. */
data class AxisModel(
    val label: String,
    val now: String,
    val aim: String,
    val zoneStart: Float,
    val zoneWidth: Float,
    val marker: Float,
    val inRange: Boolean,
)

/** What Hold shows. */
data class HoldModel(
    val position: Int,
    val ear: Char,
    val short: String,
    val count: Int,
    val total: Int,
    val blocks: List<Float>,
    val turn: String,
    val tip: String,
    val practice: Boolean = false,
    /** Signal lost mid-hold: the count pauses and says so (§16 I). */
    val paused: Boolean = false,
)

/** The design's copy per position (§4b), for a right ear. A left ear swaps every left/right word. */
internal class PositionCopy(val title: String, val sub: String, val dir: String, val short: String, val fig: String)

internal val PositionCopies = mapOf(
    1 to PositionCopy("Turn your head toward your right ear", "Stay sitting up. Turn about 45°.", "Keep turning right", "head turned right", "seated, head turned 45° toward the right ear"),
    2 to PositionCopy("Lie back", "Keep your head turned. Let it hang just past the pillow.", "Tip your head back", "lying back", "lying back, head hanging 25° below horizontal, still turned right"),
    3 to PositionCopy("Turn your head to the left", "Slowly, about 90°. Keep it hanging back.", "Keep turning left", "head turned left", "lying back, head turned 90° to the left, still hanging back"),
    4 to PositionCopy("Roll onto your left side", "Keep your head turned, nose to the bed.", "Keep rolling left", "left side", "rolled onto the left shoulder, face angled 45° toward the floor"),
    5 to PositionCopy("Sit up slowly", "Face forward. Keep your chin level.", "Lift your chin a little", "sitting up", "sitting up, facing forward, head level"),
)

/** The design's `mirror()`: every right/left word swapped for a left ear. */
internal fun mirrorFor(ear: Char, text: String): String = if (ear != 'L') text else
    Regex("\\b(right|left|Right|Left)\\b").replace(text) {
        when (it.value) { "right" -> "left"; "left" -> "right"; "Right" -> "Left"; else -> "Right" }
    }

/** Degrees as the design writes them: a true minus sign, rounded. */
internal fun deg(v: Double): String = (if (v < 0) "−" else "") + abs(v).roundToInt() + "°"

// ---------------------------------------------------------------------------------------------
// Find (§4.8). Flex region: the spacer above Stop.
// ---------------------------------------------------------------------------------------------

@Composable
fun FindView(
    m: FindModel,
    figurePlaying: Boolean,
    onToggleFigure: () -> Unit,
    onStop: () -> Unit,
    warning: (@Composable () -> Unit)? = null,
) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        Column(
            Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp).enter(0),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProgressBars(total = 5, current = m.position)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Txt("Position ${m.position} of 5", type(16f, 600), c.muted, maxLines = 1, modifier = Modifier.weight(1f))
                Row(
                    Modifier
                        .box(Color.Transparent, 999.dp, ring = if (n) c.butter else Ink)
                        .padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Sym("explore", 20f, if (n) c.butter else Ink, weight = 700)
                    Txt("Finding position", type(16f, 700), if (n) c.butter else Ink, maxLines = 1)
                }
            }
        }
        if (m.moved) {
            Column(
                Modifier.enter(1).box(if (n) c.coralTint else c.coral, 32.dp, ring = if (n) c.coral else null).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Txt("THE PHONE MOVED", type(14f, 800, lineHeight = 1.3f, letterSpacing = 0.06f), if (n) c.coral else Ink)
                Txt("It turned faster than a head can.", type(28f, 800, lineHeight = 1.05f, letterSpacing = -0.02f, wrap = Wrap.Balance), if (n) c.ink else Ink)
                Txt("Recalibrate before trusting the reading.", type(17f, 600, lineHeight = 1.4f), if (n) c.soft else Ink)
            }
        } else {
            Column(
                Modifier.enter(1).box(if (n) c.butterTint else c.butter, 32.dp, ring = if (n) c.butter else null).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (m.practice) {
                    Row(
                        Modifier.box(if (n) c.lilac else Ink, 999.dp).padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Sym("back_hand", 18f, if (n) Ink else c.butter, weight = 800)
                        Txt("PRACTICE \u00b7 NOT A TREATMENT", type(14f, 800, letterSpacing = 0.06f), if (n) Ink else c.butter, maxLines = 1)
                    }
                }
                Txt(m.title, type(34f, 800, lineHeight = 1.02f, letterSpacing = -0.03f, wrap = Wrap.Balance), if (n) c.ink else Ink)
                Txt(if (m.practice) "The phone stands in for your head." else m.sub, type(17f, 500, lineHeight = 1.4f), if (n) c.soft else Ink)
            }
        }
        if (warning != null) warning()
        Column(
            Modifier.enter(2).box(c.surface, 32.dp).padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FigureView(
                    step = m.position, ear = m.ear, night = n, playing = figurePlaying, size = 248,
                    description = "Figure: " + mirrorFor(m.ear, PositionCopies.getValue(m.position).fig),
                )
                // Still / Play: pauses the figure. The pill names what it will do next.
                Row(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp)
                        .height(48.dp)
                        .pressable(onClick = onToggleFigure)
                        .box(if (n) c.surface2 else c.ground, 24.dp)
                        .padding(start = 12.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Sym(if (figurePlaying) "pause" else "play_arrow", 22f, c.ink, fill = true, weight = 700)
                    Txt(if (figurePlaying) "Still" else "Play", type(16f, 700), c.ink, maxLines = 1)
                }
            }
            Column {
                Box(Modifier.fillMaxWidth().height(1.dp).background(if (n) c.surface2 else c.ground))
                Row(
                    Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Txt(m.remaining, type(56f, 800, lineHeight = 0.9f, letterSpacing = -0.045f, tnum = true), c.ink, maxLines = 1)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Txt(m.direction, type(20f, 800, letterSpacing = -0.01f), c.ink, maxLines = 1)
                        Txt("${m.now} · aim ${m.aim}", type(16f, 600), c.muted, maxLines = 1)
                    }
                }
            }
            RangeMeter(m.zoneStart, m.zoneWidth, m.marker)
            m.second?.let { SecondAxis(it) }
        }
        Spacer(Modifier.flex())
        PillButton(if (m.practice) "Stop practice" else "Stop", onStop, fill = c.coral, content = Ink, icon = "close")
    }
}

/**
 * The angle the position is not led by, on its own smaller scale, so a run can be watched on both
 * at once: the label and its reading, then the same zone-and-marker meter at 8 dp.
 */
@Composable
private fun SecondAxis(a: AxisModel) {
    val c = Ds
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            // Colour and icon cross-fade as the angle enters or leaves its range (M5).
            val reduced = LocalReducedMotion.current
            val tint by androidx.compose.animation.animateColorAsState(
                if (a.inRange) c.mint else c.muted,
                if (reduced) snap() else tween(Motion.TOGGLE, easing = StandardEase), label = "axis",
            )
            Sym(if (a.inRange) "check" else "radio_button_unchecked", 20f, tint, weight = 700)
            Txt(a.label, type(16f, 700), c.ink, maxLines = 1)
            Spacer(Modifier.weight(1f))
            Txt("${a.now} · aim ${a.aim}", type(16f, 600, tnum = true), c.muted, maxLines = 1)
        }
        RangeMeter(a.zoneStart, a.zoneWidth, a.marker, track = 8.dp, markerHeight = 16.dp)
    }
}

/** 12 dp track, the in-range zone, and a 4 × 24 dp marker centred on the value. */
@Composable
private fun RangeMeter(zoneStart: Float, zoneWidth: Float, marker: Float, track: Dp = 12.dp, markerHeight: Dp = 24.dp) {
    val c = Ds
    val n = c.night
    // M7: the marker follows each new reading over 240 ms; the numeral beside it swaps instantly.
    val reduced = LocalReducedMotion.current
    // A critically damped spring rather than a fixed tween: it follows a reading that keeps moving
    // without restarting each time, and it never overshoots (no bounce).
    val at by animateFloatAsState(
        marker,
        if (reduced) snap() else androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 300f),
        label = "marker",
    )
    BoxWithConstraints(Modifier.fillMaxWidth().height(track)) {
        val w = maxWidth
        Box(Modifier.fillMaxWidth().fillMaxHeight().box(if (n) c.surface2 else c.ground, track / 2))
        Box(
            Modifier
                .offset(x = w * zoneStart)
                .width(w * zoneWidth)
                .fillMaxHeight()
                .box(if (n) c.zone else c.mint, track / 2),
        )
        // 24 dp tall across a 12 dp track: requiredSize lets it overflow, centred, which is the
        // design's top: -6px. A plain size() was clamped to the track and cut the marker in half.
        Box(
            Modifier
                // Lambda offset: the marker moves every frame of its glide without recomposing.
                .offset { androidx.compose.ui.unit.IntOffset(((w * at - 2.dp).roundToPx()), 0) }
                .requiredSize(4.dp, markerHeight)
                .box(c.ink, 2.dp),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Hold (§4.9). Flex region: between "Stay still." and the count (night: inside the mint card).
// ---------------------------------------------------------------------------------------------

@Composable
fun HoldView(m: HoldModel, onStop: () -> Unit) {
    val c = Ds
    val n = c.night
    // Signal lost: the ground goes back to bone (M2), because nothing is being held.
    DScreen(bg = if (n || m.paused) c.ground else c.mint) {
        Column(
            Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp).enter(0),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProgressBars(total = 5, current = m.position, empty = if (n) c.line else if (m.paused) c.line else Color(0x2E17161C))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (m.practice) {
                    Row(
                        Modifier.box(if (n) c.lilac else Ink, 999.dp).padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Sym("back_hand", 18f, if (n) Ink else c.lilac, weight = 800)
                        Txt("PRACTICE", type(14f, 800, letterSpacing = 0.04f), if (n) Ink else c.lilac, maxLines = 1)
                    }
                }
                Txt(
                    if (m.practice) "Position ${m.position} of 5" else "Position ${m.position} of 5 \u00b7 ${m.short}",
                    type(16f, 600), if (n) c.muted else Ink,
                )
            }
        }
        val chip: @Composable () -> Unit = {
            Row(
                Modifier
                    .box(if (n) c.mint else Ink, 999.dp)
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("check_circle", 20f, if (n) Ink else c.mint, fill = true, weight = 700)
                Txt("Holding", type(16f, 700), if (n) Ink else c.mint, maxLines = 1)
            }
        }
        val stay: @Composable () -> Unit = {
            Txt(if (m.paused) "Signal lost." else "Stay still.", type(44f, 800, lineHeight = 1f, letterSpacing = -0.035f), if (n) c.ink else Ink)
        }
        val pausedCard: @Composable () -> Unit = {
            Column(
                Modifier.box(if (n) c.coralTint else c.surface, 28.dp, ring = if (n) c.coral else null).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.box(if (n) c.coral else Ink, 999.dp).padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Sym("pause_circle", 20f, if (n) Ink else Color.White, weight = 700)
                    Txt("Paused", type(16f, 700), if (n) Ink else Color.White, maxLines = 1)
                }
                Txt(
                    "The phone isn\u2019t sending readings. Stay where you are \u2014 the count carries on when it does.",
                    type(17f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), c.ink,
                )
            }
        }
        val count: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Txt(
                    "${m.count}",
                    type(if (n) 200f else 232f, 800, lineHeight = 0.72f, letterSpacing = -0.075f, tnum = true),
                    if (n) c.mint else Ink,
                    modifier = Modifier.offset(x = if (n) (-10).dp else (-12).dp),
                    maxLines = 1,
                )
                Txt("seconds left of ${m.total}", type(20f, 700), if (n) c.ink else Ink, maxLines = 1)
            }
        }
        val blocks: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                m.blocks.forEach { target ->
                    // Each block fills continuously between readings (M9), not in 0.1 s steps.
                    val f by animateFloatAsState(target, tween(120, easing = androidx.compose.animation.core.LinearEasing), label = "block")
                    val ring = when {
                        n && f == 0f -> c.mintDim
                        n -> c.mint
                        else -> Ink
                    }
                    Box(Modifier.weight(1f).height(16.dp).box(Color.Transparent, 4.dp, ring = ring)) {
                        if (f > 0f) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(f).background(if (n) c.mint else Ink))
                        }
                    }
                }
            }
        }
        if (n) {
            Column(
                Modifier.flex().enter(1).box(c.mintTint, 32.dp, ring = c.mint).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!m.paused) Box { chip() }
                stay()
                Spacer(Modifier.weight(1f))
                if (m.paused) pausedCard() else {
                    count()
                    blocks()
                }
            }
        } else {
            Column(
                Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp).enter(1),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (!m.paused) Box { chip() }
                stay()
            }
            Spacer(Modifier.flex())
            if (m.paused) {
                Box(Modifier.enter(2)) { pausedCard() }
            } else {
                Box(Modifier.padding(horizontal = 8.dp).enter(2)) { count() }
                Box(Modifier.padding(horizontal = 8.dp, vertical = 16.dp)) { blocks() }
            }
        }
        Row(
            Modifier.enter(3).box(if (n) c.surface else Color.White.copy(alpha = 0.6f), 28.dp)
                .padding(start = 8.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FigureView(
                step = m.position, ear = m.ear, night = n, playing = false, size = 144,
                description = "Figure: " + mirrorFor(m.ear, PositionCopies.getValue(m.position).fig),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HeldAxis("Turn", m.turn)
                Box(Modifier.fillMaxWidth().height(1.dp).background(if (n) c.surface2 else Color(0x1F17161C)))
                HeldAxis("Tip", m.tip)
            }
        }
        PillButton(if (m.practice) "Stop practice" else "Stop", onStop, fill = c.coral, content = Ink, icon = "close")
    }
}

@Composable
private fun HeldAxis(label: String, value: String) {
    val c = Ds
    val n = c.night
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Sym("check", 20f, if (n) c.mint else Ink, weight = 600)
            Txt(label, type(16f, 600), if (n) c.muted else Ink, maxLines = 1)
        }
        Txt(value, type(28f, 800, lineHeight = 1f, letterSpacing = -0.02f, tnum = true), if (n) c.ink else Ink, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// The design's sample values (its POS table and renderVals), for the parity tests.
// ---------------------------------------------------------------------------------------------

private class Sample(val start: Double, val tip: Boolean, val min: Double, val max: Double, val aim: Pair<Double, Double>, val hold: Int, val endTurn: Double, val endTip: Double)

private val Samples = mapOf(
    1 to Sample(12.0, false, 0.0, 90.0, 30.0 to 60.0, 3, 44.0, 0.0),
    2 to Sample(0.0, true, -90.0, 90.0, -35.0 to -15.0, 45, 45.0, -26.0),
    3 to Sample(20.0, false, 0.0, 180.0, 75.0 to 105.0, 45, 92.0, -25.0),
    4 to Sample(96.0, false, 0.0, 180.0, 120.0 to 150.0, 45, 131.0, -27.0),
    5 to Sample(-30.0, true, -90.0, 90.0, -10.0 to 10.0, 5, 0.0, -2.0),
)

fun sampleFind(position: Int, ear: Char): FindModel {
    val p = Samples.getValue(position)
    val copy = PositionCopies.getValue(position)
    val v = p.start
    val (lo, hi) = p.aim
    val inRange = v in lo..hi
    val delta = if (v < lo) lo - v else v - hi
    val span = p.max - p.min
    fun frac(x: Double) = ((x.coerceIn(p.min, p.max) - p.min) / span).toFloat()
    return FindModel(
        position = position,
        ear = ear,
        title = mirrorFor(ear, copy.title),
        sub = mirrorFor(ear, copy.sub),
        direction = mirrorFor(ear, copy.dir),
        remaining = if (inRange) "In range" else (if (v < lo) "+" else "−") + delta.roundToInt() + "°",
        now = "now " + deg(v),
        aim = if (p.tip) deg(hi) + " to " + deg(lo) else "${lo.roundToInt()}–${hi.roundToInt()}°",
        zoneStart = frac(lo),
        zoneWidth = ((hi - lo) / span).toFloat(),
        marker = frac(v),
    )
}

fun sampleHold(position: Int, ear: Char): HoldModel {
    val p = Samples.getValue(position)
    val total = p.hold
    val count = ceil(total * 0.4).toInt()
    return HoldModel(
        position = position,
        ear = ear,
        short = mirrorFor(ear, PositionCopies.getValue(position).short),
        count = count,
        total = total,
        blocks = holdBlocks(total, (total - count).toFloat()),
        turn = deg(p.endTurn),
        tip = deg(p.endTip),
    )
}

/** Blocks fill left to right: 5 s blocks for the 45 s holds, 1 s blocks for the 3 s and 5 s. */
fun holdBlocks(total: Int, elapsed: Float): List<Float> {
    val size = if (total <= 5) 1 else 5
    val count = ceil(total / size.toFloat()).toInt()
    return List(count) { i -> ((elapsed - i * size) / size).coerceIn(0f, 1f) }
}
