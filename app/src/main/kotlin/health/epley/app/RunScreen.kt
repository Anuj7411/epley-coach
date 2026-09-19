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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import health.epley.core.CuePlanner
import health.epley.core.Guidance
import health.epley.core.Phrasing
import kotlin.math.roundToInt

/**
 * The guided run.
 *
 * Top to bottom: what position this is, a picture of it, the live dials against the target, and
 * one block of feedback. The picture is for the moments the screen can be seen — before lying back,
 * or by a helper; the voice and vibration carry the rest. Colour carries the state on its own:
 * amber move, blue settle, green hold.
 *
 * Corrections come from [Phrasing], the same source the voice uses, so the screen and the speaker
 * can never give different instructions.
 */
@Composable
fun RunScreen(
    run: RunUiState,
    onStop: () -> Unit,
) {
    val engineState = run.engineState
    val step = engineState?.step
    val guidance = engineState?.guidance ?: Guidance.SEEKING
    val holding = guidance == Guidance.HOLDING || guidance == Guidance.STEP_COMPLETE
    val polarity = run.polarity

    val accent = when (guidance) {
        Guidance.HOLDING, Guidance.STEP_COMPLETE, Guidance.FINISHED -> Color(0xFF6BCB77)
        Guidance.SETTLING -> Color(0xFF7FB3FF)
        Guidance.SEEKING -> Color(0xFFFFD93D)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )

        if (step != null) PoseIllustration(step, run.side)

        if (step != null && polarity != null) {
            HeadDials(pose = run.pose, step = step, polarity = polarity, side = run.side)
        }

        // The whole of the feedback, in one block, in one colour.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF111111))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            val correction = engineState?.correction
            if (!holding && correction != null && step != null && polarity != null) {
                val phrases = Phrasing.corrections(
                    correction, polarity, run.side,
                    toleranceDegrees = step.toleranceDegrees,
                    seated = step.target.pitchDegrees < CuePlanner.SEATED_BELOW_PITCH,
                )
                if (phrases.isEmpty()) {
                    Text("In position — hold still", color = Color(0xFF7FB3FF), fontSize = 18.sp)
                }
                for (phrase in phrases) CorrectionRow(phrase.text, phrase.degrees)
            }

            if (holding && engineState != null) {
                val remaining = (engineState.holdSecondsRequired - engineState.heldSeconds).coerceAtLeast(0.0)
                Text(
                    text = "${remaining.roundToInt()}s",
                    color = accent,
                    fontSize = 64.sp,
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
            text = step?.spoken ?: "",
            color = Color(0xFF999999),
            fontSize = 14.sp,
        )

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

@Composable
private fun CorrectionRow(text: String, degrees: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Color.White, fontSize = 20.sp)
        Text(
            "$degrees°",
            color = Color(0xFFFFD93D),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun FinishedPanel(onStop: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
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
