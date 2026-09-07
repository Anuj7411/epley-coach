package health.epley.core

import kotlin.math.abs

/**
 * Turns a wrapped angle into a continuous one.
 *
 * An angle recovered from a quaternion arrives folded into (-180, 180]. Rotate past the boundary
 * and it snaps from +179 to -179 — a 358 degree step for two degrees of real motion. On real
 * hardware this showed up as a **320.69 degree sample-to-sample jump** in a 22,530-sample
 * recording, while the same recording's gravity-referenced angle never jumped more than 6.09.
 *
 * The value is not wrong, it is discontinuous, and discontinuity is worse than wrongness here:
 * a tolerance check comparing against a target sees a 358 degree error and declares the head
 * wildly out of position at the exact moment it is correct.
 *
 * This is standard phase unwrapping. Each new sample is compared with the previous one; a step
 * larger than half a turn is assumed to be the boundary rather than genuine motion, and a whole
 * turn is added or subtracted to cancel it. That assumption holds as long as the head cannot
 * really move 180 degrees between samples — at 50 Hz that would be 9,000 degrees per second, so
 * the assumption is safe by three orders of magnitude.
 */
class AngleUnwrapper {

    private var lastWrapped: Double? = null
    private var turns = 0.0

    /** Continuous angle for this sample, in degrees. */
    fun unwrap(wrappedDegrees: Double): Double {
        val previous = lastWrapped
        if (previous == null) {
            lastWrapped = wrappedDegrees
            return wrappedDegrees
        }

        val step = wrappedDegrees - previous
        when {
            step > HALF_TURN -> turns -= FULL_TURN
            step < -HALF_TURN -> turns += FULL_TURN
        }

        lastWrapped = wrappedDegrees
        return wrappedDegrees + turns
    }

    /**
     * Forget the history.
     *
     * Called on every recalibration: the reference has changed, so continuity with samples taken
     * against the old reference is meaningless.
     */
    fun reset() {
        lastWrapped = null
        turns = 0.0
    }

    /** How many whole turns have accumulated. Useful for spotting a runaway. */
    val accumulatedTurns: Double get() = turns / FULL_TURN

    private companion object {
        const val HALF_TURN = 180.0
        const val FULL_TURN = 360.0
    }
}

/**
 * Largest sample-to-sample change we will believe is real motion.
 *
 * At 50 Hz, 30 degrees between samples is 1,500 degrees per second. Human head rotation peaks
 * far below that, so anything larger is an artefact — a dropped sample, a sensor glitch, or a
 * decomposition breaking down — and should be treated as suspect rather than plotted.
 */
const val MAX_PLAUSIBLE_STEP_DEGREES = 30.0

/** Whether two consecutive samples differ by more than physiology allows. */
fun isImplausibleStep(previousDegrees: Double, currentDegrees: Double): Boolean =
    abs(currentDegrees - previousDegrees) > MAX_PLAUSIBLE_STEP_DEGREES
