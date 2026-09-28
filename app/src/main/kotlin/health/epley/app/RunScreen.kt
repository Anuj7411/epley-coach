package health.epley.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.CuePlanner
import health.epley.core.Guidance
import health.epley.core.HeadAngles
import health.epley.core.Phrasing
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The guided run.
 *
 * Two states, and they look nothing alike. **Searching** shows a ruler per axis, with the target
 * band hatched and a marker for the head. **Holding** collapses both rulers to a line each and
 * gives the whole screen to the countdown, because once you are in position the only thing that
 * matters is how long you have left.
 *
 * Corrections come from [Phrasing], the same source the voice reads, so the screen and the speaker
 * can never give different instructions. Nothing on this screen animates except the hold ring,
 * which steps once a second with no easing: WCAG 2.3.3 exists because moving interfaces make
 * vestibular patients dizzy, and they are the whole audience.
 */
@Composable
fun RunScreen(
    run: RunUiState,
    onRepeat: () -> Unit,
    onStop: () -> Unit,
) {
    val engineState = run.engineState
    val step = engineState?.step
    val guidance = engineState?.guidance ?: Guidance.SEEKING
    val polarity = run.polarity
    val holding = guidance == Guidance.HOLDING || guidance == Guidance.STEP_COMPLETE

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

    FlowFrame(
        stepLabel = "Position ${(engineState?.stepIndex ?: 0) + 1} of 5" + if (run.practice) ", practice" else "",
        progress = null,
        onBack = null,
        bottom = {
            SecondaryButton("Say it again", onRepeat)
            StopButton(onStop)
        },
    ) {
        RunProgress(
            completed = engineState?.completedSteps ?: 0,
            current = engineState?.stepIndex ?: 0,
            total = 5,
        )

        StateChip(
            holding = holding,
            text = if (holding) "In position, hold still" else "Finding the position",
        )

        Text(
            step?.title ?: "Getting ready",
            color = Palette.Ink,
            fontSize = AppType.RunTitleSize,
            lineHeight = AppType.RunTitleLine,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = AppType.Sans,
        )
        step?.let {
            Text(
                if (holding) "Stay exactly like this. Dizziness now is normal." else it.instruction(run.side, run.practice),
                color = Palette.InkMuted,
                fontSize = AppType.BodySize,
                lineHeight = AppType.BodyLine,
                fontFamily = AppType.Sans,
            )
        }

        if (run.mountMoved) MountMovedNote()

        if (step != null && polarity != null) {
            val pose = run.pose
            val liveTurn = pose?.let { p ->
                if (polarity.towardAffectedSideIsPositive) p.headRotationDegrees else -p.headRotationDegrees
            }
            val livePitch = pose?.pitchDegrees
            val targetTurn = step.target.headRotationDegrees
            val targetPitch = step.target.pitchDegrees

            if (holding) {
                CheckRow("Turn", liveTurn)
                CheckRow("Tilt", livePitch)
                Spacer(Modifier.height(4.dp))
                HoldRing(
                    remaining = engineState?.let { (it.holdSecondsRequired - it.heldSeconds).coerceAtLeast(0.0) } ?: 0.0,
                    required = engineState?.holdSecondsRequired ?: 0,
                    progress = engineState?.holdProgress?.toFloat() ?: 0f,
                )
            } else {
                // Wrap-safe, exactly as the engine judges it: a reading a whole turn out must not
                // draw on target while the app says move.
                val turnOffset = liveTurn?.let { HeadAngles.shortestDegrees(it - targetTurn) }
                AngleGauge(
                    label = "Turn",
                    valueDegrees = turnOffset?.let { targetTurn + it },
                    targetDegrees = targetTurn,
                    bandDegrees = step.toleranceDegrees,
                    minDegrees = targetTurn - 90,
                    maxDegrees = targetTurn + 90,
                    guidance = phrases.firstOrNull { it.text.startsWith("Turn") }?.let { Phrasing.spoken(it) },
                )
                AngleGauge(
                    label = "Tilt",
                    valueDegrees = livePitch,
                    targetDegrees = targetPitch,
                    bandDegrees = step.toleranceDegrees,
                    minDegrees = -90.0,
                    maxDegrees = 45.0,
                    guidance = phrases.firstOrNull { !it.text.startsWith("Turn") }?.let { Phrasing.spoken(it) },
                )
            }
        }
    }
}

/**
 * The countdown, and nothing else.
 *
 * Set at 72sp because it is read with one eye, at arm's length, by someone whose head is tipped
 * back off the edge of a bed. The ring is the only thing in the app that changes over time, and it
 * steps once a second rather than tweening.
 */
@Composable
private fun HoldRing(remaining: Double, required: Int, progress: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.Surface)
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier.size(220.dp),
            color = Palette.Holding,
            trackColor = Palette.Divider,
            strokeWidth = 16.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${remaining.roundToInt()}",
                color = Palette.Ink,
                fontSize = 72.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = AppType.Mono,
            )
            Text(
                "seconds left of $required",
                color = Palette.InkMuted,
                fontSize = AppType.LabelSize,
                fontWeight = FontWeight.Bold,
                fontFamily = AppType.Sans,
            )
        }
    }
}

/** The mount has been jolted, so the angles are measured against a reference that may have moved. */
@Composable
private fun MountMovedNote() {
    androidx.compose.foundation.layout.Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.StopFill)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Palette.Urgent, modifier = Modifier.size(22.dp))
        Text(
            "The phone moved on your head. These angles may be wrong, so stop and set it up again.",
            color = Palette.Urgent,
            fontSize = AppType.ReadingFloor,
            lineHeight = 22.sp,
            fontFamily = AppType.Sans,
        )
    }
}
