package health.epley.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import health.epley.core.ReversalCheck
import kotlin.math.abs

/**
 * The app measuring its own error, with no reference instrument.
 *
 * Two readings 180° apart on any flat surface: the surface's unknown tilt flips sign, the sensor's
 * own error does not, so half the difference is the surface and half the sum is the sensor. This
 * is the reversal technique used to check a precision level without a calibrated master, and it is
 * why the accuracy claim in the README needs a table rather than bought hardware. Maths and
 * tolerance live in [ReversalCheck].
 */
@Composable
fun AccuracyCheckScreen(
    devicePitchDegrees: Double,
    screenFacingUp: Boolean,
    isStill: Boolean,
    rotationTravelledDegrees: Double,
    storedOffsetDegrees: Double,
    onSaveOffset: (health.epley.core.ReversalResult) -> Unit,
    onBack: () -> Unit,
) {
    var first by remember { mutableStateOf<Double?>(null) }
    var second by remember { mutableStateOf<Double?>(null) }
    var firstFacedUp by remember { mutableStateOf(true) }
    var secondFacedUp by remember { mutableStateOf(true) }
    var rotationAtFirst by remember { mutableStateOf(0.0) }
    var turnedDegrees by remember { mutableStateOf(0.0) }

    val a = first
    val b = second
    // Two ways the measurement can be invalid rather than the sensor being bad. Both were found
    // on hardware, where a flip produced an impossible 10 degree "sensor error".
    val flipped = a != null && b != null && firstFacedUp != secondFacedUp
    val unturned = a != null && b != null && !ReversalCheck.turnedEnough(turnedDegrees)
    val result = if (a != null && b != null && !flipped && !unturned) ReversalCheck.analyse(a, b) else null

    FlowFrame(
        stepLabel = "Accuracy self-check",
        progress = null,
        onBack = onBack,
        bottom = {
            when {
                result != null && !result.isWithinTolerance -> {
                    PrimaryButton(
                        label = "Correct this phone by %+.2f°".format(-result.sensorErrorDegrees),
                        onClick = { onSaveOffset(result); first = null; second = null },
                    )
                    SecondaryButton("Start again", { first = null; second = null })
                }
                result != null -> SecondaryButton("Start again", { first = null; second = null })
                a != null && b != null -> SecondaryButton("Start again", { first = null; second = null })
                a == null -> PrimaryButton(
                    "Take reading 1",
                    {
                        first = devicePitchDegrees
                        firstFacedUp = screenFacingUp
                        rotationAtFirst = rotationTravelledDegrees
                    },
                    enabled = isStill,
                )
                else -> PrimaryButton(
                    "Take reading 2",
                    {
                        second = devicePitchDegrees
                        secondFacedUp = screenFacingUp
                        turnedDegrees = rotationTravelledDegrees - rotationAtFirst
                    },
                    enabled = isStill,
                )
            }
        },
    ) {
        Title("How accurate is this phone?")
        if (storedOffsetDegrees != 0.0) {
            Body(
                "This phone is already corrected by %+.2f°. Readings below include that.".format(-storedOffsetDegrees),
                secondary = true,
            )
        }
        Body(
            "Put the phone flat on any surface. It doesn't need to be level. Take a reading, " +
                "turn the phone 180° on the same spot, and take another.",
        )
        Body(
            "The surface's tilt flips sign when you turn the phone. The sensor's own error " +
                "doesn't. So the two readings separate them, and no measuring tool is needed.",
            secondary = true,
        )

        Card {
            Text(
                "Live: %+.2f°".format(devicePitchDegrees) + if (isStill) "" else "  (moving)",
                color = if (isStill) Palette.TextPrimary else Palette.Move,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
            )
            if (a != null) Text("Reading 1: %+.2f°".format(a), color = Palette.TextSecondary, fontSize = 17.sp, fontFamily = FontFamily.Monospace)
            if (b != null) Text("Reading 2: %+.2f°".format(b), color = Palette.TextSecondary, fontSize = 17.sp, fontFamily = FontFamily.Monospace)
        }

        if (a != null && b == null) {
            Body(
                "Now spin the phone 180° flat on the surface, like turning a plate, so the top " +
                    "edge points the other way. Keep the screen facing up; don't turn it over.",
            )
        }

        if (flipped || unturned) {
            Card {
                Text(
                    if (flipped) "The phone was turned over" else "The phone doesn't look turned",
                    color = Palette.Move, fontSize = 20.sp, fontWeight = FontWeight.Medium,
                )
                Body(
                    if (flipped) {
                        "Reading 2 was taken face-down. Flipping keeps the same tilt against " +
                            "gravity, so the two readings can't be compared."
                    } else {
                        "The phone turned only %.0f° between the readings. A reversal needs a half turn.".format(turnedDegrees)
                    },
                )
                Body(
                    "Lay it flat, take reading 1, then spin it round on the spot — screen still up " +
                        "— and take reading 2.",
                    secondary = true,
                )
            }
        }

        if (result != null) {
            Card {
                Text(
                    if (result.isWithinTolerance) "Passed" else "Outside tolerance",
                    color = if (result.isWithinTolerance) Palette.Action else Palette.Move,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    "Sensor error: %+.2f°".format(result.sensorErrorDegrees),
                    color = Palette.TextPrimary, fontSize = 19.sp, fontFamily = FontFamily.Monospace,
                )
                Text(
                    "Surface tilt: %+.2f°".format(result.surfaceTiltDegrees),
                    color = Palette.TextSecondary, fontSize = 17.sp, fontFamily = FontFamily.Monospace,
                )
                Body(
                    "Passing means the sensor's own error is under ${ReversalCheck.MAX_ERROR_DEGREES}°, " +
                        "half of what the app promises. The manoeuvre's own bands are 18.9° to 31°.",
                    secondary = true,
                )
                if (!result.isWithinTolerance) {
                    Body(
                        "A large error here is the phone's own tilt calibration, and it does not " +
                            "affect the manoeuvre: every angle the app guides by is measured " +
                            "against the calibration taken on your head, so a fixed offset is in " +
                            "both readings and cancels. It only shows up in this absolute check.",
                        secondary = true,
                    )
                }
            }
            if (abs(result.surfaceTiltDegrees) > 1.0) {
                Body(
                    "For a known angle instead: fold a sheet of paper corner-to-edge for exactly " +
                        "45°, rest the phone on it, and run the same two readings. The measured " +
                        "tilt should come out at 45° plus whatever your surface is tilted.",
                    secondary = true,
                )
            }
        }
    }
}
