package health.epley.core

import kotlin.math.abs

/** What two readings 180° apart tell you about the surface and about the sensor. */
data class ReversalResult(
    /** The surface's true tilt, with the sensor's own error removed. */
    val surfaceTiltDegrees: Double,
    /** The sensor's own error, with the surface's tilt removed. Should be near zero. */
    val sensorErrorDegrees: Double,
) {
    val isWithinTolerance: Boolean get() = abs(sensorErrorDegrees) <= ReversalCheck.MAX_ERROR_DEGREES

    /** How far the measured tilt is from an angle you constructed and therefore know. */
    fun errorAgainst(knownAngleDegrees: Double): Double = surfaceTiltDegrees - knownAngleDegrees
}

/**
 * Measuring the instrument with no reference instrument.
 *
 * Take a reading, turn the phone 180° on the same spot, take another. The surface's unknown tilt
 * appears with the opposite sign the second time, because the phone is facing the other way; the
 * sensor's own error does not, because it travels with the phone. So half the difference is the
 * surface, and half the sum is the sensor.
 *
 * This is the standard reversal technique used to check a precision level without a calibrated
 * master — and it means the accuracy claim in the README needs no bought hardware, only a table.
 *
 * Known angles come just as cheaply: folding a sheet of paper corner-to-edge gives exactly 45°,
 * and an equilateral fold gives 30° and 60°. Gravity supplies the rest: face-up and face-down are
 * exactly 180° apart by definition.
 */
object ReversalCheck {

    /**
     * The most sensor error that still counts as passing.
     *
     * Half of the app's own 5° static-accuracy requirement (NFR-1). Anything under this is small
     * against the 40–51° error the app exists to correct; anything over it means the claim in the
     * README is not supported on this device.
     */
    const val MAX_ERROR_DEGREES = 2.5

    /**
     * Whether the two readings say the phone was never actually turned.
     *
     * A gravity-referenced tilt sensor cannot be several degrees wrong, so a large apparent
     * "sensor error" with almost no difference between the readings means the phone kept the same
     * orientation: flipped face-down rather than spun flat, or not moved at all. Caught on
     * hardware, where -10.83 and -9.61 were reported as a 10 degree sensor error.
     */
    fun looksUnturned(firstReadingDegrees: Double, reversedReadingDegrees: Double): Boolean =
        abs(firstReadingDegrees - reversedReadingDegrees) < MIN_REVERSAL_DIFFERENCE &&
            abs((firstReadingDegrees + reversedReadingDegrees) / 2.0) > MAX_ERROR_DEGREES

    /**
     * Below this difference between the two readings, nothing was meaningfully reversed.
     *
     * A true reversal produces readings that differ by twice the surface's tilt. Five degrees is
     * generous: it only ever matters alongside an implausible bias, and the pair seen on hardware
     * differed by 1.2 while claiming a 10 degree error.
     */
    const val MIN_REVERSAL_DIFFERENCE = 5.0

    fun analyse(firstReadingDegrees: Double, reversedReadingDegrees: Double) = ReversalResult(
        surfaceTiltDegrees = (firstReadingDegrees - reversedReadingDegrees) / 2.0,
        sensorErrorDegrees = (firstReadingDegrees + reversedReadingDegrees) / 2.0,
    )
}
