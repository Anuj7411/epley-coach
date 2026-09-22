package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Correcting a phone whose absolute tilt is out. The test device reads about 9° off; the reversal
 * measures that without any reference instrument, and storing it makes later readings right.
 */
class TiltOffsetTest {

    @Test
    fun `the measured error becomes the correction`() {
        // Readings taken on the test phone: a level floor and a 9.23 degree device offset.
        val measured = ReversalCheck.analyse(-10.16, -8.30)
        val offset = TiltOffset.from(current = 0.0, measured = measured)
        assertEquals(-9.23, offset, 0.01)
    }

    @Test
    fun `applying the offset zeroes the error on a re-run`() {
        val first = ReversalCheck.analyse(-10.16, -8.30)
        val offset = TiltOffset.from(current = 0.0, measured = first)

        // The same physical situation, now read through the correction.
        val corrected = ReversalCheck.analyse(-10.16 - offset, -8.30 - offset)
        assertEquals(0.0, corrected.sensorErrorDegrees, 1e-9)
        assertTrue(corrected.isWithinTolerance)
        // The floor is still the floor: correcting the phone must not flatten the world.
        assertEquals(first.surfaceTiltDegrees, corrected.surfaceTiltDegrees, 1e-9)
    }

    @Test
    fun `a second measurement refines the offset rather than replacing it`() {
        // Running the check again on a corrected phone finds a small residual, and that adds to
        // what is already stored instead of overwriting it with a near-zero number.
        val stored = -9.0
        val residual = ReversalCheck.analyse(-0.4, 0.0) // half-sum -0.2
        assertEquals(-9.2, TiltOffset.from(current = stored, measured = residual), 1e-9)
    }
}
