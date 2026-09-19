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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.HeadPose
import health.epley.core.ManeuverStep
import health.epley.core.RotationPolarity
import health.epley.core.Side
import health.epley.core.word
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val Ink = Color(0xFFE8E8E8)
private val Faint = Color(0xFF555555)
private val Good = Color(0xFF6BCB77)
private val Move = Color(0xFFFFD93D)
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
        Text("$pitchText · $turnText", color = Color(0xFFBBBBBB), fontSize = 14.sp)
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
 * Two live dials — the turn from above, the tilt from the side — with the target drawn dashed.
 *
 * The solid head moves with the real one; getting in position means laying it over the dashed
 * one, and each dial turns green once its own axis is inside the band. Readable at arm's length by
 * a helper holding the phone, by someone practising with it in their hand, and in a demo video,
 * where "measured, not guessed" has to land in a few seconds without narration.
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
    val turnOk = liveTurn != null && abs(liveTurn - targetTurn) <= band
    val pitchOk = livePitch != null && abs(livePitch - targetPitch) <= band

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Dial(label = "TURN", ok = turnOk) { centre, r, stroke ->
            drawTurnHead(centre, r, screenTurnDegrees(targetTurn, side), Faint, dashed = true, stroke = stroke)
            if (liveTurn != null) {
                drawTurnHead(centre, r, screenTurnDegrees(liveTurn, side), if (turnOk) Good else Move, false, stroke)
            }
        }
        Dial(label = "TILT", ok = pitchOk) { centre, r, stroke ->
            drawTiltHead(centre, r, targetPitch, Faint, dashed = true, stroke = stroke)
            if (livePitch != null) {
                drawTiltHead(centre, r, livePitch, if (pitchOk) Good else Move, false, stroke)
            }
        }
    }
}

@Composable
private fun Dial(
    label: String,
    ok: Boolean,
    draw: DrawScope.(centre: Offset, radius: Float, stroke: Float) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(140.dp)) {
            val r = size.minDimension * 0.24f
            draw(center, r, size.minDimension * 0.03f)
        }
        Text(label, color = if (ok) Good else Color(0xFF888888), fontSize = 12.sp)
    }
}

/**
 * A head from the side: the line is the crown direction. Straight up is sitting, pointing left is
 * lying flat with the head toward the bed's edge, below that is hanging.
 */
private fun DrawScope.drawTiltHead(
    centre: Offset,
    radius: Float,
    pitchDegrees: Double,
    colour: Color,
    dashed: Boolean,
    stroke: Float,
) {
    val effect = if (dashed) Dashed else null
    drawCircle(colour, radius, centre, style = Stroke(stroke, pathEffect = effect))
    val a = Math.toRadians(pitchDegrees)
    val dir = Offset((-cos(a)).toFloat(), sin(a).toFloat())
    drawLine(
        colour,
        Offset(centre.x + dir.x * radius * 0.2f, centre.y + dir.y * radius * 0.2f),
        Offset(centre.x + dir.x * radius * 1.45f, centre.y + dir.y * radius * 1.45f),
        stroke * 1.4f,
        StrokeCap.Round,
        pathEffect = effect,
    )
}
