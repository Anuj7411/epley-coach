package health.epley.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.HeadAngles
import health.epley.core.HeadPose
import health.epley.core.ManeuverStep
import health.epley.core.RotationPolarity
import health.epley.core.Side
import health.epley.core.word
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.min
import kotlin.math.sin

private val Ink = Palette.TextPrimary
private val Faint = Palette.Outline
private val Dashed = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))

/**
 * Where a head turn of [towardAffected] degrees points on screen, seen from above and behind.
 * Clockwise is the user's right, so a turn toward a left ear goes anticlockwise.
 */
private fun screenTurnDegrees(towardAffected: Double, side: Side): Double =
    if (side == Side.RIGHT) towardAffected else -towardAffected

/** The live sensor turn, re-expressed as degrees toward the affected ear. */
private fun towardAffected(pose: HeadPose, polarity: RotationPolarity): Double =
    if (polarity.towardAffectedSideIsPositive) pose.headRotationDegrees else -pose.headRotationDegrees

/**
 * A picture of the position to get into.
 *
 * Shown while the user is still able to see the screen, which is the whole reason to draw it: a
 * figure lying back with its head over the edge says in a glance what a paragraph struggles to.
 * The side view shows the body and the head's tilt; the inset shows the turn from above.
 */
@Composable
fun PoseIllustration(step: ManeuverStep, side: Side) {
    val pitch = step.target.pitchDegrees
    val sitting = pitch < -45
    val turn = screenTurnDegrees(step.target.headRotationDegrees, side)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(2.2f)) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.018f
            val bedTop = h * 0.66f
            val bedLeft = w * 0.26f
            val bedRight = w * 0.70f
            val headR = w * 0.042f

            // The bed. Its left edge is where the head hangs over.
            drawRoundRect(
                color = Faint,
                topLeft = Offset(bedLeft, bedTop),
                size = Size(bedRight - bedLeft, h * 0.09f),
                cornerRadius = CornerRadius(8f, 8f),
            )

            if (sitting) {
                val hip = Offset(bedLeft + w * 0.10f, bedTop - stroke)
                val shoulder = Offset(hip.x, hip.y - h * 0.34f)
                val knee = Offset(hip.x - w * 0.10f, hip.y)
                drawLine(Ink, hip, shoulder, stroke, StrokeCap.Round)
                drawLine(Ink, hip, knee, stroke, StrokeCap.Round)
                drawLine(Ink, knee, Offset(knee.x, knee.y + h * 0.26f), stroke, StrokeCap.Round)
                drawCircle(Ink, headR, Offset(shoulder.x, shoulder.y - headR * 1.4f), style = Stroke(stroke))
            } else {
                // Lying on the back (or side), shoulders at the bed's edge, head beyond it.
                val shoulder = Offset(bedLeft + w * 0.02f, bedTop - stroke)
                val feet = Offset(bedRight - w * 0.02f, bedTop - stroke)
                drawLine(Ink, shoulder, feet, stroke, StrokeCap.Round)
                // The neck follows the target pitch: 0 level, positive hanging below the edge.
                val a = Math.toRadians(pitch)
                val neckLen = w * 0.05f
                val neckEnd = Offset(
                    shoulder.x - (neckLen * cos(a)).toFloat(),
                    shoulder.y + (neckLen * sin(a)).toFloat(),
                )
                drawLine(Ink, shoulder, neckEnd, stroke, StrokeCap.Round)
                val headCentre = Offset(
                    neckEnd.x - (headR * 1.2f * cos(a)).toFloat(),
                    neckEnd.y + (headR * 1.2f * sin(a)).toFloat(),
                )
                drawCircle(Ink, headR, headCentre, style = Stroke(stroke))
                if (pitch > 5) {
                    // Show the hang against a dashed horizontal, so "below the bed" is visible.
                    drawLine(
                        Faint, shoulder, Offset(shoulder.x - w * 0.16f, shoulder.y),
                        stroke / 2, pathEffect = Dashed,
                    )
                }
            }

            // Inset: the turn, seen from above. Nose up is facing straight ahead.
            drawTurnHead(
                centre = Offset(w * 0.86f, h * 0.40f),
                radius = h * 0.24f,
                turnDegrees = turn,
                colour = Ink,
                dashed = false,
                stroke = stroke,
            )
        }

        val turnText = when {
            abs(step.target.headRotationDegrees) < 1 -> "Face straight ahead"
            else -> {
                val towardSide = if (step.target.headRotationDegrees > 0) side else side.otherSide()
                "Head turned ${abs(step.target.headRotationDegrees).roundToInt()}° to your ${towardSide.word}"
            }
        }
        val pitchText = when {
            sitting -> "Sitting upright"
            pitch > 5 -> "Head hanging ${pitch.roundToInt()}° below the bed"
            else -> "Lying on your side, head level, looking at the floor"
        }
        Text("$pitchText · $turnText", color = Palette.TextSecondary, fontSize = 16.sp)
    }
}

private fun Side.otherSide(): Side = if (this == Side.LEFT) Side.RIGHT else Side.LEFT

/** A head from above: a circle with a nose pointing [turnDegrees] clockwise from straight ahead. */
private fun DrawScope.drawTurnHead(
    centre: Offset,
    radius: Float,
    turnDegrees: Double,
    colour: Color,
    dashed: Boolean,
    stroke: Float,
) {
    val effect = if (dashed) Dashed else null
    drawCircle(colour, radius, centre, style = Stroke(stroke, pathEffect = effect))
    val a = Math.toRadians(turnDegrees)
    val tip = Offset(
        centre.x + (radius * 1.35f * sin(a)).toFloat(),
        centre.y - (radius * 1.35f * cos(a)).toFloat(),
    )
    val base = Offset(
        centre.x + (radius * 0.85f * sin(a)).toFloat(),
        centre.y - (radius * 0.85f * cos(a)).toFloat(),
    )
    drawLine(colour, base, tip, stroke * 1.4f, StrokeCap.Round, pathEffect = effect)
}

/**
 * The live head gauges: a head seen from above for the turn, and in profile for the tilt.
 *
 * Designed from three patterns (docs/DESIGN.md): the rehabilitation biofeedback target zone — a
 * shaded "good range" with a marker for where you are, which measurably improves exercise
 * correctness; the phone spirit level, where lining two things up turns the state colour; and
 * physiotherapy apps' use of a recognisable body rather than abstract shapes. The first version,
 * two circles with a stick, was hard to read on the phone.
 *
 * The shaded zone is exactly the band the engine judges by, so "the head is in the zone" and
 * "the app counts it" can never disagree. Nothing animates except the head following the sensor.
 */
@Composable
fun HeadDials(
    pose: HeadPose?,
    step: ManeuverStep,
    polarity: RotationPolarity,
    side: Side,
) {
    val targetTurn = step.target.headRotationDegrees
    val targetPitch = step.target.pitchDegrees
    val liveTurn = pose?.let { towardAffected(it, polarity) }
    val livePitch = pose?.pitchDegrees
    val band = step.toleranceDegrees
    // Short way round, exactly as the engine judges it. A reading a whole turn out would
    // otherwise draw on target while showing orange, which is the bug a practice run found.
    val turnOk = liveTurn != null && abs(HeadAngles.shortestDegrees(liveTurn - targetTurn)) <= band
    val pitchOk = livePitch != null && abs(livePitch - targetPitch) <= band

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Gauge("From above", ok = turnOk, modifier = Modifier.weight(1f), aspect = 1f) {
            drawTurnGauge(
                target = screenTurnDegrees(targetTurn, side),
                live = liveTurn?.let { screenTurnDegrees(it, side) },
                band = band,
                ok = turnOk,
            )
        }
        Gauge("From the side", ok = pitchOk, modifier = Modifier.weight(1.3f), aspect = 1.3f) {
            drawTiltGauge(target = targetPitch, live = livePitch, band = band, ok = pitchOk)
        }
    }
}

@Composable
private fun Gauge(
    label: String,
    ok: Boolean,
    modifier: Modifier,
    aspect: Float,
    draw: DrawScope.() -> Unit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(aspect)) { draw() }
        Text(
            text = if (ok) "$label  ✓" else label,
            color = if (ok) Palette.Action else Palette.TextSecondary,
            fontSize = 14.sp,
        )
    }
}

private fun point(c: Offset, r: Float, degreesClockwiseFromUp: Double): Offset {
    val a = Math.toRadians(degreesClockwiseFromUp)
    return Offset(c.x + r * sin(a).toFloat(), c.y - r * cos(a).toFloat())
}

/** From above: a head with ears and a nose, turned [live] degrees; the target zone on a ring. */
private fun DrawScope.drawTurnGauge(target: Double, live: Double?, band: Double, ok: Boolean) {
    val c = center
    val r = size.minDimension * 0.38f
    val track = size.minDimension * 0.07f
    val colour = if (ok) Palette.Action else Palette.Move
    val fill = if (ok) Palette.ActionTint else Palette.Surface

    drawCircle(if (ok) Palette.ActionTrack else Palette.Raised, r, c, style = Stroke(track))
    // drawArc measures from 3 o'clock; "straight ahead" is 12 o'clock, hence the -90.
    drawArc(
        color = Palette.Action.copy(alpha = if (ok) 0.8f else 0.5f),
        startAngle = (-90.0 + target - band).toFloat(),
        sweepAngle = (2 * band).toFloat(),
        useCenter = false,
        topLeft = Offset(c.x - r, c.y - r),
        size = Size(2 * r, 2 * r),
        style = Stroke(track, cap = StrokeCap.Round),
    )
    if (!ok) {
        // Where the nose should point, dashed, so the goal is visible before it is reached.
        drawLine(
            Palette.Action, point(c, r * 0.62f, target), point(c, r * 0.84f, target),
            strokeWidth = track * 0.3f, cap = StrokeCap.Round, pathEffect = Dashed,
        )
    }
    if (live == null) return

    rotate(live.toFloat(), pivot = c) {
        val hw = r * 0.43f
        val hh = r * 0.50f
        val ear = Size(r * 0.16f, r * 0.32f)
        drawOval(colour, topLeft = Offset(c.x - hw - r * 0.10f, c.y - ear.height / 2), size = ear)
        drawOval(colour, topLeft = Offset(c.x + hw - r * 0.06f, c.y - ear.height / 2), size = ear)
        drawOval(fill, topLeft = Offset(c.x - hw, c.y - hh), size = Size(2 * hw, 2 * hh))
        drawOval(colour, topLeft = Offset(c.x - hw, c.y - hh), size = Size(2 * hw, 2 * hh), style = Stroke(track * 0.32f))
        val nose = Path().apply {
            moveTo(c.x - r * 0.12f, c.y - hh + r * 0.03f)
            lineTo(c.x, c.y - hh - r * 0.20f)
            lineTo(c.x + r * 0.12f, c.y - hh + r * 0.03f)
            close()
        }
        drawPath(nose, colour)
    }
}

/**
 * In profile: the body on the bed (or upright, for the sitting positions), the neck, and the head
 * with its nose. The target zone is an arc around the neck; a dashed line marks level, so "hanging
 * below the bed" is something you can see, not a number you have to trust.
 */
private fun DrawScope.drawTiltGauge(target: Double, live: Double?, band: Double, ok: Boolean) {
    val w = size.width
    val h = size.height
    val u = min(w, h)
    val seated = target < -45
    val pivot = if (seated) Offset(w * 0.50f, h * 0.55f) else Offset(w * 0.58f, h * 0.42f)
    val line = u * 0.035f
    val colour = if (ok) Palette.Action else Palette.Move
    val fill = if (ok) Palette.ActionTint else Palette.Surface

    if (seated) {
        drawLine(Palette.Outline, pivot, Offset(pivot.x, h * 0.97f), line, StrokeCap.Round)
    } else {
        drawRoundRect(
            color = Palette.Raised,
            topLeft = Offset(pivot.x + u * 0.02f, pivot.y + line),
            size = Size(w * 0.97f - pivot.x, u * 0.07f),
            cornerRadius = CornerRadius(6f, 6f),
        )
        drawLine(Palette.Outline, pivot, Offset(w * 0.97f, pivot.y), line, StrokeCap.Round)
        drawLine(Palette.Raised, Offset(w * 0.03f, pivot.y), pivot, line * 0.35f, pathEffect = Dashed)
    }

    // Pitch p points p degrees below "toward the head end" (left). In drawArc's clockwise-from-
    // 3-o'clock terms that is 180 - p: level is 180, hanging is past it, sitting upright is 270.
    val arcR = u * 0.44f
    val track = u * 0.07f
    drawArc(
        color = Palette.Action.copy(alpha = if (ok) 0.8f else 0.5f),
        startAngle = (180.0 - (target + band)).toFloat(),
        sweepAngle = (2 * band).toFloat(),
        useCenter = false,
        topLeft = Offset(pivot.x - arcR, pivot.y - arcR),
        size = Size(2 * arcR, 2 * arcR),
        style = Stroke(track, cap = StrokeCap.Round),
    )
    if (live == null) return

    val theta = Math.toRadians(180.0 - live)
    val d = Offset(cos(theta).toFloat(), sin(theta).toFloat())
    val neck = u * 0.14f
    val headR = u * 0.13f
    val headC = pivot + d * (neck + headR)
    // The face is a quarter turn from the crown: up when lying on the back, forward when sitting.
    val face = Offset(-d.y, d.x)
    drawLine(colour, pivot, pivot + d * neck, line, StrokeCap.Round)
    drawCircle(fill, headR, headC)
    drawCircle(colour, headR, headC, style = Stroke(line * 0.8f))
    drawLine(colour, headC + face * (headR * 0.85f), headC + face * (headR * 1.45f), line, StrokeCap.Round)
}
