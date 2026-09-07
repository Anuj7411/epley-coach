package health.epley.core

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AngleUnwrapperTest {

    @Test
    fun `passes ordinary angles through untouched`() {
        val u = AngleUnwrapper()
        for (a in listOf(0.0, 10.0, 45.0, 90.0, -30.0, -90.0)) {
            assertEquals(a, u.unwrap(a), 1e-9)
        }
    }

    @Test
    fun `removes the wrap that produced a 320 degree jump on hardware`() {
        // The real recording stepped +166.4 to -169.7. Raw difference: 336 degrees.
        val u = AngleUnwrapper()
        val first = u.unwrap(166.4)
        val second = u.unwrap(-169.7)
        val step = abs(second - first)
        assertTrue(step < 30.0, "unwrapped step was $step degrees, expected the true ~24")
        assertEquals(190.3, second, 1e-6)
    }

    @Test
    fun `handles a wrap in the other direction`() {
        val u = AngleUnwrapper()
        u.unwrap(-175.0)
        val next = u.unwrap(178.0)
        assertTrue(abs(next - (-175.0)) < 30.0)
        assertEquals(-182.0, next, 1e-6)
    }

    @Test
    fun `stays continuous across a full turn`() {
        val u = AngleUnwrapper()
        var previous = u.unwrap(0.0)
        var worstStep = 0.0
        // Sweep a whole turn in 2 degree steps, wrapping the input as a quaternion would.
        for (trueAngle in 2..360 step 2) {
            val wrapped = HeadAngles.normalizeSigned(Math.toRadians(trueAngle.toDouble()))
            val out = u.unwrap(Math.toDegrees(wrapped))
            worstStep = maxOf(worstStep, abs(out - previous))
            previous = out
        }
        assertTrue(worstStep < 5.0, "worst step across a full turn was $worstStep degrees")
        assertEquals(360.0, previous, 1e-6)
    }

    @Test
    fun `tracks several turns without losing count`() {
        val u = AngleUnwrapper()
        u.unwrap(0.0)
        var last = 0.0
        for (trueAngle in 5..1080 step 5) {
            val wrapped = Math.toDegrees(HeadAngles.normalizeSigned(Math.toRadians(trueAngle.toDouble())))
            last = u.unwrap(wrapped)
        }
        assertEquals(1080.0, last, 1e-6)
        assertEquals(3.0, u.accumulatedTurns, 1e-9)
    }

    @Test
    fun `unwinds back to zero when the motion reverses`() {
        val u = AngleUnwrapper()
        u.unwrap(0.0)
        for (a in 10..720 step 10) {
            u.unwrap(Math.toDegrees(HeadAngles.normalizeSigned(Math.toRadians(a.toDouble()))))
        }
        var last = 0.0
        for (a in 710 downTo 0 step 10) {
            last = u.unwrap(Math.toDegrees(HeadAngles.normalizeSigned(Math.toRadians(a.toDouble()))))
        }
        assertEquals(0.0, last, 1e-6)
        assertEquals(0.0, u.accumulatedTurns, 1e-9)
    }

    @Test
    fun `reset forgets history so a new calibration starts clean`() {
        val u = AngleUnwrapper()
        u.unwrap(170.0)
        u.unwrap(-170.0)
        assertTrue(u.accumulatedTurns != 0.0)
        u.reset()
        assertEquals(0.0, u.accumulatedTurns, 1e-9)
        assertEquals(-170.0, u.unwrap(-170.0), 1e-9)
    }

    @Test
    fun `flags a step no head could physically make`() {
        assertTrue(isImplausibleStep(0.0, 45.0))
        assertTrue(isImplausibleStep(10.0, -40.0))
        assertTrue(!isImplausibleStep(10.0, 20.0))
        assertTrue(!isImplausibleStep(0.0, -6.09))  // the worst real step we measured
    }
}

class RotationReliabilityTest {

    @Test
    fun `a normal manoeuvre pose is reported reliable`() {
        // Epley position one: neck extended 30, head turned 45.
        val pose = HeadPose(neckExtensionDegrees = 30.0, headRotationDegrees = 45.0, swingDegrees = 30.0)
        assertTrue(pose.isRotationReliable)
    }

    @Test
    fun `a half turn swing is reported unreliable`() {
        // The full flip in the hardware test. The twist decomposition degenerates here.
        val pose = HeadPose(neckExtensionDegrees = 0.0, headRotationDegrees = 166.0, swingDegrees = 178.0)
        assertTrue(!pose.isRotationReliable)
    }

    @Test
    fun `the boundary sits above anything a real Epley produces`() {
        // Worst-case combined swing in a correctly performed manoeuvre is around 110 degrees.
        assertTrue(HeadPose(0.0, 0.0, swingDegrees = 110.0).isRotationReliable)
        assertTrue(!HeadPose(0.0, 0.0, swingDegrees = 151.0).isRotationReliable)
    }

    @Test
    fun `computed poses carry a swing value`() {
        val upright = Quaternion.IDENTITY
        val mount = MountCalibration.fromUprightSample(upright)
        val tipped = Quaternion.fromAxisAngle(Vector3(1.0, 0.0, 0.0), Math.toRadians(30.0))
        val pose = HeadAngles.compute(tipped, mount)
        assertTrue(pose.swingDegrees >= 0.0)
        assertTrue(pose.isRotationReliable)
    }
}
