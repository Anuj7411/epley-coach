package health.epley.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.CorrectionPhrase
import health.epley.core.CuePlanner
import health.epley.core.Guidance
import health.epley.core.Phrasing
import kotlin.math.roundToInt

/**
 * The guided run.
 *
 * Top to bottom: which position, a picture of it, the live head gauges against the target zone,
 * and one status card. The card follows the phone spirit level: when you are in position the whole
 * card turns blue and says so; when you are not, it is outlined orange and says which way and how
 * far, in words. Colour is never the only signal. Nothing animates except the heads and the hold
 * bar (docs/DESIGN.md).
 *
 * Corrections come from [Phrasing], the same source the voice uses, so the screen and the speaker
 * can never give different instructions.
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

    FlowFrame(
        stepLabel = "Position ${(engineState?.stepIndex ?: 0) + 1} of 5" + if (run.practice) " · practice" else "",
        progress = null,
        onBack = null,
        bottom = {
            SecondaryButton("Say it again", onRepeat)
            SecondaryButton("Stop  ·  or hold a volume button", onStop)
        },
    ) {
        Title(step?.title ?: "Getting ready")
        if (run.mountMoved) {
            Text(
                "The phone moved on your head. These angles may be wrong — stop, and set it up again.",
                color = Palette.Move,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (step != null) PoseIllustration(step, run.side)
        if (step != null && polarity != null) {
            HeadDials(pose = run.pose, step = step, polarity = polarity, side = run.side)
        }

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

        when (guidance) {
            Guidance.HOLDING, Guidance.STEP_COMPLETE -> {
                val remaining = engineState?.let { (it.holdSecondsRequired - it.heldSeconds).coerceAtLeast(0.0) } ?: 0.0
                StatusCard(background = Palette.ActionTint, border = Palette.Action) {
                    Text(
                        if (guidance == Guidance.STEP_COMPLETE) "✓ Position complete" else "✓ In position",
                        color = Palette.Action, fontSize = 17.sp, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        if (guidance == Guidance.STEP_COMPLETE) "Next position coming" else "Hold still",
                        color = Palette.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium,
                    )
                    Text("${remaining.roundToInt()}s", color = Palette.TextPrimary, fontSize = 52.sp, fontWeight = FontWeight.Medium)
                    ProgressBar(engineState?.holdProgress?.toFloat() ?: 0f)
                }
            }

            Guidance.SETTLING -> StatusCard(background = Palette.Surface, border = Palette.Action) {
                Text("Almost there", color = Palette.Action, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                Text("Hold still", color = Palette.TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Medium)
            }

            Guidance.SEEKING -> StatusCard(background = Palette.Surface, border = Palette.Move) {
                Text("Move", color = Palette.Move, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                val first: CorrectionPhrase? = phrases.firstOrNull()
                if (first == null) {
                    Text("Get into the position shown", color = Palette.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                } else {
                    Text(first.text, color = Palette.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium)
                    Text("${first.degrees}° more", color = Palette.Move, fontSize = 40.sp, fontWeight = FontWeight.Medium)
                    for (other in phrases.drop(1)) {
                        Text("Also: ${other.text.lowercase()}, ${other.degrees}°", color = Palette.TextSecondary, fontSize = 16.sp)
                    }
                }
            }

            Guidance.FINISHED -> Unit
        }

        if (step != null) Body(step.instruction(run.side, run.practice), secondary = true)
    }
}

@Composable
private fun StatusCard(background: Color, border: Color, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(2.dp, border, shape)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

/** The one animation the design allows: essential feedback on how long is left. */
@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        Modifier.fillMaxWidth().padding(top = 6.dp).height(8.dp)
            .clip(RoundedCornerShape(4.dp)).background(Palette.ActionTrack),
    ) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).background(Palette.Action))
    }
}
