package health.epley.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.Correction
import health.epley.core.Guidance
import health.epley.core.RotationPolarity
import health.epley.core.Side
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The guided run.
 *
 * Deliberately not a dashboard. At any moment there is one instruction and one piece of feedback,
 * both large enough to read at arm's length from a bed, because the person using this is dizzy and
 * lying down. The angle numbers that dominate the probe screen appear here only as the size of a
 * correction — never as something to interpret.
 *
 * The colour carries the state on its own: amber means move, green means hold. That is what a user
 * with their eyes half shut will actually perceive, and it is the same signal the voice will give
 * once audio lands.
 */
@Composable
fun RunScreen(
    run: RunUiState,
    toleranceDegrees: Double,
    onStop: () -> Unit,
) {
    val engineState = run.engineState
    val step = engineState?.step
    val guidance = engineState?.guidance ?: Guidance.SEEKING
    val holding = guidance == Guidance.HOLDING || guidance == Guidance.STEP_COMPLETE

    val accent = when (guidance) {
        Guidance.HOLDING -> Color(0xFF6BCB77)
        Guidance.STEP_COMPLETE -> Color(0xFF6BCB77)
        Guidance.SETTLING -> Color(0xFF7FB3FF)
        Guidance.FINISHED -> Color(0xFF6BCB77)
        Guidance.SEEKING -> Color(0xFFFFD93D)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (guidance == Guidance.FINISHED) {
            FinishedPanel(onStop)
            return@Column
        }

        Text(
            text = "STEP ${(engineState?.stepIndex ?: 0) + 1} OF 5",
            color = Color(0xFF888888),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
        )

        Text(
            text = step?.title ?: "Getting ready",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(
            text = step?.spoken ?: "",
            color = Color(0xFFCCCCCC),
            fontSize = 17.sp,
        )

        Spacer(Modifier.height(4.dp))

        // The whole of the feedback, in one block, in one colour.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = when (guidance) {
                    Guidance.SEEKING -> "MOVE INTO POSITION"
                    Guidance.SETTLING -> "ALMOST — HOLD STILL"
                    Guidance.HOLDING -> "HOLDING"
                    Guidance.STEP_COMPLETE -> "POSITION COMPLETE"
                    Guidance.FINISHED -> "DONE"
                },
                color = accent,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )

            val correction = engineState?.correction
            if (!holding && correction != null) {
                CorrectionLines(
                    correction = correction,
                    polarity = run.polarity,
                    side = run.side,
                    toleranceDegrees = toleranceDegrees,
                )
            }

            if (holding && engineState != null) {
                val remaining = (engineState.holdSecondsRequired - engineState.heldSeconds)
                    .coerceAtLeast(0.0)
                Text(
                    text = "${remaining.roundToInt()}s",
                    color = accent,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
                LinearProgressIndicator(
                    progress = { engineState.holdProgress.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = accent,
                    trackColor = Color(0xFF333333),
                )
            }
        }

        Text(
            text = "The timer only runs while you are in position and still. If it pauses, you " +
                "moved — get back into position and it picks up where it left off.",
            color = Color(0xFF666666),
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3A1A1A),
                contentColor = Color(0xFFFF6B6B),
            ),
        ) { Text("STOP") }
    }
}

/**
 * How far out of position, per axis, in words rather than signed numbers.
 *
 * "Turn toward your left" is actionable lying on a bed with the eyes shut. "-27.4" is not, and
 * getting the user to translate a sign into a direction is exactly the work the app exists to do
 * for them.
 */
@Composable
private fun CorrectionLines(
    correction: Correction,
    polarity: RotationPolarity?,
    side: Side,
    toleranceDegrees: Double,
) {
    val sideWord = if (side == Side.LEFT) "left" else "right"

    val extensionOff = abs(correction.neckExtensionDegrees) > toleranceDegrees
    val rotationOff = abs(correction.headRotationDegrees) > toleranceDegrees

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (extensionOff) {
            CorrectionRow(
                text = if (correction.neckExtensionDegrees > 0) {
                    "Tip your head further back"
                } else {
                    "Lift your head"
                },
                degrees = abs(correction.neckExtensionDegrees),
            )
        }
        if (rotationOff) {
            // Which physical direction a positive correction means depends on the calibration,
            // which is the whole reason polarity is learned rather than assumed.
            val towardAffectedIsPositive = polarity?.towardAffectedSideIsPositive ?: true
            val moveTowardAffected = (correction.headRotationDegrees > 0) == towardAffectedIsPositive
            CorrectionRow(
                text = if (moveTowardAffected) {
                    "Turn toward your $sideWord"
                } else {
                    "Turn away from your $sideWord"
                },
                degrees = abs(correction.headRotationDegrees),
            )
        }
        if (!extensionOff && !rotationOff) {
            Text("In position — hold still", color = Color(0xFF7FB3FF), fontSize = 18.sp)
        }
    }
}

@Composable
private fun CorrectionRow(text: String, degrees: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Color.White, fontSize = 20.sp)
        Text(
            "${degrees.roundToInt()}°",
            color = Color(0xFFFFD93D),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun FinishedPanel(onStop: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Manoeuvre complete",
            color = Color(0xFF6BCB77),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Stay sitting upright for a minute before standing.",
            color = Color(0xFFCCCCCC),
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("DONE") }
    }
}
