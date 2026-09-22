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

class ReversalSanityTest {

    @Test
    fun `two near-identical readings mean the phone was not turned`() {
        // Seen on hardware: -10.83 then -9.61, which the maths reports as a 10 degree sensor
        // error. A gravity-referenced tilt sensor cannot be 10 degrees wrong, so the readings are
        // the thing at fault — the phone was flipped over, or never turned at all.
        assertTrue(ReversalCheck.looksUnturned(-10.83, -9.61))
        assertTrue(ReversalCheck.looksUnturned(20.0, 20.4))
    }

    @Test
    fun `a real reversal is not flagged`() {
        assertFalse(ReversalCheck.looksUnturned(0.6, -0.6))
        assertFalse(ReversalCheck.looksUnturned(45.4, -44.6))
        // A level surface with an honest sensor: both readings near zero, nothing to complain of.
        assertFalse(ReversalCheck.looksUnturned(0.2, -0.1))
    }

    @Test
    fun `a genuinely small bias on a flat surface still passes`() {
        // Readings 1.0 and -0.4: surface 0.7, bias 0.3. Sensible, and must not be flagged.
        assertFalse(ReversalCheck.looksUnturned(1.0, -0.4))
    }
}
