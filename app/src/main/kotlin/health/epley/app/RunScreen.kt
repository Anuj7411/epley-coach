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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.CuePlanner
import health.epley.core.Guidance
import health.epley.core.HeadAngles
import health.epley.core.Phrasing
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The guided run, in the v2 system (handoff §4.8 Find, §4.9 Hold).
 *
 * Two states that look nothing alike. **Finding** is butter: the instruction, one big signed
 * remaining angle, the direction in words, and a meter with the tolerance band drawn on it.
 * **Holding** floods mint and gives the screen to the countdown, because once you are in position
 * the only thing that matters is how long is left.
 *
 * Corrections come from [Phrasing], the same source the voice reads, so the screen and the
 * speaker can never give different instructions.
 *
 * Nothing here animates except the hold blocks and the marker. WCAG 2.3.3 exists because moving
 * interfaces make vestibular patients dizzy, and they are the whole audience.
 */
@Composable
fun RunScreen(
    run: RunUiState,
    onRepeat: () -> Unit,
    onStop: () -> Unit,
) {
    val c = Ds
    val engineState = run.engineState
    val step = engineState?.step
    val guidance = engineState?.guidance ?: Guidance.SEEKING
    val polarity = run.polarity
    val holding = guidance == Guidance.HOLDING || guidance == Guidance.STEP_COMPLETE
    val position = (engineState?.stepIndex ?: 0) + 1

    val phrases = if (engineState?.correction != null && step != null && polarity != null) {
        Phrasing.corrections(
            engineState.correction!!, polarity, run.side,
            toleranceDegrees = step.toleranceDegrees,
            seated = step.target.pitchDegrees < CuePlanner.SEATED_BELOW_PITCH,
            practice = run.practice,
        )
    } else {
        emptyList()
    }

    // Live values in the same frame the engine judges them in.
    val pose = run.pose
    val liveTurn = pose?.let { p ->
        if (polarity?.towardAffectedSideIsPositive == false) -p.headRotationDegrees else p.headRotationDegrees
    }
    val livePitch = pose?.pitchDegrees

    // Positions 1, 3 and 4 are led by the turn; 2 and 5 by the tilt (handoff §4b). The other axis
    // is still shown, smaller — the engine gates on both, so hiding one would guide badly.
    val turnLed = position == 1 || position == 3 || position == 4

    DsScreen(
        background = if (holding && !c.night) c.mint else c.ground,
        top = {
            Column(
                Modifier.fillMaxWidth().padding(vertical = Space.s),
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                DsProgressSegments(total = 5, current = position)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.m)) {
                    Text(
                        "Position $position of 5" + if (run.practice) " · practice" else "",
                        style = DsType.label,
                        color = if (holding && !c.night) Color(0xCC17161C) else c.muted,
                    )
                    Spacer(Modifier.weight(1f))
                    if (holding) {
                        DsChip("Holding", Icons.Rounded.Check, ChipStyle.Filled)
                    } else {
                        DsChip("Finding position", Icons.Rounded.Warning, ChipStyle.Outlined, c.butter)
                    }
                }
            }
        },
        bottom = {
            DsButton(
                label = "Say it again",
                onClick = onRepeat,
                fill = if (holding && !c.night) Color(0x33FFFFFF) else c.surface,
                contentColor = if (holding && !c.night) Color(0xFF17161C) else c.ink,
                leading = Icons.Rounded.Refresh,
            )
            DsStopButton(onStop)
        },
    ) {
        if (holding) {
            HoldBody(run, liveTurn, livePitch, engineState?.heldSeconds ?: 0.0, engineState?.holdSecondsRequired ?: 0)
        } else {
            FindBody(run, step, phrases, liveTurn, livePitch, turnLed)
        }
    }
}

/** Finding: the instruction on butter, then one big signed angle and a meter. */
@Composable
private fun FindBody(
    run: RunUiState,
    step: health.epley.core.ManeuverStep?,
    phrases: List<health.epley.core.CorrectionPhrase>,
    liveTurn: Double?,
    livePitch: Double?,
    turnLed: Boolean,
) {
    val c = Ds
    if (step == null) {
        Text("Getting ready", style = DsType.title, color = c.ink)
        return
    }
    DsCard(
        modifier = Modifier.enter(0),
        fill = c.butter,
        tint = c.butterTint,
        outline = c.butter,
    ) {
        Text(
            step.title,
            style = DsType.cardTitle,
            color = if (c.night) c.ink else Color(0xFF17161C),
        )
        Text(
            step.instruction(run.side, run.practice),
            style = DsType.body,
            color = if (c.night) c.soft else Color(0xCC17161C),
        )
    }

    if (run.mountMoved) {
        Box(Modifier.enter(1)) {
            DsWarning(
                caption = "THE PHONE MOVED",
                body = "It turned faster than a head can. Recalibrate before trusting the reading.",
            )
        }
    }

    val target = if (turnLed) step.target.headRotationDegrees else step.target.pitchDegrees
    val live = if (turnLed) liveTurn else livePitch
    val label = if (turnLed) "Turn" else "Tip"
    val tolerance = step.toleranceDegrees
    // Wrap-safe, exactly as the engine judges it: a reading a whole turn out must not draw on
    // target while the app is saying move.
    val offset = live?.let { if (turnLed) HeadAngles.shortestDegrees(it - target) else it - target }
    val remaining = offset?.let { if (abs(it) <= tolerance) 0.0 else it - (if (it > 0) tolerance else -tolerance) }
    val direction = phrases.firstOrNull { it.text.startsWith(label, ignoreCase = true) }
        ?: phrases.firstOrNull()

    DsCard(modifier = Modifier.enter(2)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                when {
                    remaining == null -> "--"
                    remaining == 0.0 -> "In range"
                    else -> (if (remaining > 0) "+" else "") + (-remaining).roundToInt().toString() + "°"
                },
                style = DsType.live,
                color = if (remaining == 0.0) c.mint else c.ink,
            )
            Spacer(Modifier.width(Space.m))
            Column(Modifier.weight(1f)) {
                Text(
                    direction?.let { Phrasing.onScreen(it) } ?: "Hold still",
                    style = DsType.rowTitle,
                    color = c.ink,
                )
                Text(
                    buildString {
                        append("now ")
                        append(live?.roundToInt()?.toString() ?: "--")
                        append("° · aim ")
                        append((target - tolerance).roundToInt())
                        append("–")
                        append((target + tolerance).roundToInt())
                        append("°")
                    },
                    style = DsType.label,
                    color = c.muted,
                )
            }
        }
        val span = if (turnLed) 90.0 else 80.0
        val low = target - span
        val high = target + span
        DsRangeMeter(
            value = live?.let { (((if (turnLed) target + (offset ?: 0.0) else it) - low) / (high - low)).toFloat() } ?: 0.5f,
            zoneStart = (((target - tolerance) - low) / (high - low)).toFloat(),
            zoneEnd = (((target + tolerance) - low) / (high - low)).toFloat(),
        )
    }

    // The engine gates on both axes, so the second one is always visible even when not leading.
    val otherLive = if (turnLed) livePitch else liveTurn
    val otherTarget = if (turnLed) step.target.pitchDegrees else step.target.headRotationDegrees
    val otherLabel = if (turnLed) "Tip" else "Turn"
    Box(Modifier.enter(3)) {
        DsCard(padding = Space.l) {
            AxisRow(otherLabel, otherLive, otherTarget, tolerance)
        }
    }
}

/** Holding: the count owns the screen. */
@Composable
private fun HoldBody(
    run: RunUiState,
    liveTurn: Double?,
    livePitch: Double?,
    heldSeconds: Double,
    required: Int,
) {
    val c = Ds
    val remaining = (required - heldSeconds).coerceAtLeast(0.0)
    val onMint = !c.night
    val ink = if (onMint) Color(0xFF17161C) else c.ink
    val body: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Text("Stay still.", style = DsType.title, color = ink, modifier = Modifier.enter(0))
            Text(
                remaining.roundToInt().toString(),
                style = DsType.count((LocalViewportHeight.current.value * 0.26f).coerceIn(120f, 232f).sp),
                color = if (onMint) Color(0xFF17161C) else c.mint,
                modifier = Modifier.enter(1),
            )
            Text(
                "seconds left of $required",
                style = DsType.cardTitle,
                color = if (onMint) Color(0xCC17161C) else c.muted,
                modifier = Modifier.enter(2),
            )
            Spacer(Modifier.height(Space.s))
            Box(Modifier.enter(3)) {
                DsHoldBlocks(
                    totalSeconds = required,
                    elapsedSeconds = heldSeconds.toFloat(),
                    fill = if (onMint) Color(0xFF17161C) else c.mint,
                    empty = if (onMint) Color(0x2217161C) else c.mintTint,
                )
            }
            Spacer(Modifier.height(Space.s))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.note))
                    .background(if (onMint) Color(0x66FFFFFF) else c.mintTint)
                    .padding(Space.l)
                    .enter(4),
                verticalArrangement = Arrangement.spacedBy(Space.s),
            ) {
                HeldReading("Turn", liveTurn, ink)
                HeldReading("Tip", livePitch, ink)
            }
        }
    }
    Spacer(Modifier.height(Space.s))
    if (c.night) {
        DsCard(fill = c.mintTint, tint = c.mintTint, outline = c.mint, radius = Radius.hero) { body() }
    } else {
        body()
    }
}

@Composable
private fun HeldReading(label: String, value: Double?, ink: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Space.s))
        Text(label, style = DsType.label, color = ink, modifier = Modifier.weight(1f))
        Text(
            value?.let { it.roundToInt().toString() + "°" } ?: "--",
            style = DsType.cardTitle,
            color = ink,
        )
    }
}

/** The axis that is not leading this position: label, live value, and whether it is in range. */
@Composable
private fun AxisRow(label: String, live: Double?, target: Double, tolerance: Double) {
    val c = Ds
    val off = live?.let { abs(HeadAngles.shortestDegrees(it - target)) }
    val inRange = off != null && off <= tolerance
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (inRange) Icons.Rounded.Check else Icons.Rounded.Warning,
            contentDescription = null,
            tint = if (inRange) c.mint else c.butter,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(Space.s))
        Text(label, style = DsType.label, color = c.muted, modifier = Modifier.weight(1f))
        Text(
            (live?.roundToInt()?.toString() ?: "--") + "° · aim " +
                (target - tolerance).roundToInt() + "–" + (target + tolerance).roundToInt() + "°",
            style = DsType.label,
            color = c.ink,
        )
    }
}
