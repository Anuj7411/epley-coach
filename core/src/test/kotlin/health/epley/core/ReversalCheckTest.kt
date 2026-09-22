package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The reversal method: two readings 180° apart separate the surface's unknown tilt from the
 * sensor's own error, with no reference instrument. Expected values below are worked by hand from
 * that definition, not from the implementation.
 */
class ReversalCheckTest {

    @Test
    fun `a perfectly level surface and a perfect sensor read zero twice`() {
        val result = ReversalCheck.analyse(0.0, 0.0)
        assertEquals(0.0, result.surfaceTiltDegrees, 1e-9)
        assertEquals(0.0, result.sensorErrorDegrees, 1e-9)
        assertTrue(result.isWithinTolerance)
    }

    @Test
    fun `a sloping surface with a perfect sensor shows the slope and no error`() {
        // The surface tips 3 degrees. Turned around, the same slope reads as -3.
        val result = ReversalCheck.analyse(3.0, -3.0)
        assertEquals(3.0, result.surfaceTiltDegrees, 1e-9)
        assertEquals(0.0, result.sensorErrorDegrees, 1e-9)
        assertTrue(result.isWithinTolerance)
    }

    @Test
    fun `a level surface with a biased sensor shows the bias and no slope`() {
        // A sensor reading 2 degrees high does so in both directions: the bias does not flip.
        val result = ReversalCheck.analyse(2.0, 2.0)
        assertEquals(0.0, result.surfaceTiltDegrees, 1e-9)
        assertEquals(2.0, result.sensorErrorDegrees, 1e-9)
    }

    @Test
    fun `slope and bias together are separated`() {
        // Surface +3, sensor bias +2: readings are 5 and -1.
        val result = ReversalCheck.analyse(5.0, -1.0)
        assertEquals(3.0, result.surfaceTiltDegrees, 1e-9)
        assertEquals(2.0, result.sensorErrorDegrees, 1e-9)
    }

    @Test
    fun `an error past the app's own promise fails the check`() {
        // The tightest thing the app claims is the 5-degree static accuracy requirement, and half
        // of that is the point at which a measured bias stops being negligible.
        assertTrue(ReversalCheck.analyse(2.4, 2.4).isWithinTolerance)
        assertFalse(ReversalCheck.analyse(2.6, 2.6).isWithinTolerance)
        assertFalse(ReversalCheck.analyse(-2.6, -2.6).isWithinTolerance)
    }

    @Test
    fun `a known angle is checked against the same two readings`() {
        // On a folded-paper 45 degree wedge with a sensor reading 0.4 high: 45.4 one way, -44.6
        // the other. Reversal removes the bias, so the wedge measures 45.0 and the app is right
        // about the angle even though the sensor is not perfect.
        val result = ReversalCheck.analyse(45.4, -44.6)
        assertEquals(45.0, result.surfaceTiltDegrees, 1e-9)
        assertEquals(0.4, result.sensorErrorDegrees, 1e-9)
        assertEquals(0.0, result.errorAgainst(45.0), 1e-9)

        // The same wedge on a table that tips 1 degree: the wedge plus the table is what is
        // really there, and the check reports it as 46 rather than pretending otherwise.
        val tilted = ReversalCheck.analyse(46.4, -45.6)
        assertEquals(46.0, tilted.surfaceTiltDegrees, 1e-9)
        assertEquals(1.0, tilted.errorAgainst(45.0), 1e-9)
    }
}

class ReversalTurnEvidenceTest {

    @Test
    fun `a half turn counts, a nudge does not`() {
        assertTrue(ReversalCheck.turnedEnough(180.0))
        assertTrue(ReversalCheck.turnedEnough(214.0))
        assertFalse(ReversalCheck.turnedEnough(4.0))
        assertFalse(ReversalCheck.turnedEnough(0.0))
    }

    @Test
    fun `a real reversal on a biased phone is accepted, not rejected`() {
        // Measured on hardware: a genuine half turn gave -8.81 and -7.79. Judged by the readings
        // alone that looks like nothing moved; judged by the rotation the gyroscope saw, it is a
        // valid measurement of an 8.3 degree device offset on a nearly level floor.
        assertTrue(ReversalCheck.turnedEnough(183.0))
        val result = ReversalCheck.analyse(-8.81, -7.79)
        assertEquals(-8.30, result.sensorErrorDegrees, 0.01)
        assertEquals(-0.51, result.surfaceTiltDegrees, 0.01)
    }
}
