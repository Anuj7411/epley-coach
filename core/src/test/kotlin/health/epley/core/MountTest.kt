package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MountModeTest {

    @Test
    fun `every mode states a usable tolerance and a unit forward hint`() {
        for (mode in MountMode.entries) {
            assertTrue(mode.toleranceDegrees > 0.0, "${mode.name} has no tolerance")
            assertTrue(
                mode.toleranceDegrees < 15.0,
                "${mode.name} tolerance ${mode.toleranceDegrees} is looser than the error people " +
                    "already make unaided, which would make the guidance pointless",
            )
            assertEquals(1.0, mode.phoneForwardHint.length, 1e-9, "${mode.name} hint is not a unit vector")
        }
    }

    @Test
    fun `practice mode is marked as not tracking the head`() {
        // The UI keys the "this is not a treatment" wording off this flag. If it ever flips, the
        // app starts claiming to measure a head while measuring a hand.
        assertFalse(MountMode.IN_HAND.tracksTheHead)
        assertTrue(MountMode.CHEEK.tracksTheHead)
        assertTrue(MountMode.HEADBAND.tracksTheHead)
    }

    @Test
    fun `tolerance widens as the mount gets less rigid`() {
        assertTrue(MountMode.HEADBAND.toleranceDegrees < MountMode.CHEEK.toleranceDegrees)
        assertTrue(MountMode.CHEEK.toleranceDegrees < MountMode.IN_HAND.toleranceDegrees)
    }

    @Test
    fun `the default needs no equipment`() {
        assertEquals(MountMode.CHEEK, MountMode.DEFAULT)
    }
}

class MountMonitorTest {

    @Test
    fun `manoeuvre speed motion is not a jolt`() {
        val monitor = MountMonitor()
        // A deliberately performed repositioning tops out around 60 deg/s.
        for (rate in listOf(0.0, 5.0, 22.5, 60.0, -75.0, 120.0)) {
            assertFalse(monitor.onSample(rate), "$rate deg/s was wrongly called a jolt")
        }
        assertFalse(monitor.isJolted)
    }

    @Test
    fun `a slip past the threshold latches`() {
        val monitor = MountMonitor()
        monitor.onSample(40.0)
        assertTrue(monitor.onSample(400.0))
        assertTrue(monitor.isJolted)

        // Still latched after the motion settles: the calibration is stale from here on, and
        // going quiet again would hide that.
        monitor.onSample(2.0)
        assertTrue(monitor.isJolted)
    }

    @Test
    fun `only a recalibration clears the latch`() {
        val monitor = MountMonitor()
        monitor.onSample(999.0)
        assertTrue(monitor.isJolted)
        monitor.clear()
        assertFalse(monitor.isJolted)
        // The count survives, because the session log should still say it happened.
        assertEquals(1, monitor.jolts)
    }

    @Test
    fun `sign of the rate does not matter`() {
        val monitor = MountMonitor()
        assertTrue(monitor.onSample(-300.0))
    }

    @Test
    fun `peak rate is kept for the record`() {
        val monitor = MountMonitor()
        listOf(10.0, 88.0, -140.0, 33.0).forEach(monitor::onSample)
        assertEquals(140.0, monitor.peakRateDegPerSec, 1e-9)
        assertFalse(monitor.isJolted)
    }
}

class MountResidualTest {

    private fun pose(neck: Double, rotation: Double) = HeadPose(neck, rotation)

    @Test
    fun `a perfect return to upright reads zero`() {
        val residual = MountResidual()
        val check = residual.record(pose(0.0, 0.0))
        assertEquals(0.0, check.residualDegrees, 1e-9)
        assertEquals(1, residual.count)
    }

    @Test
    fun `the residual is the worse of the two angles`() {
        val residual = MountResidual()
        val check = residual.record(pose(neck = -1.8, rotation = 4.2))
        assertEquals(4.2, check.residualDegrees, 1e-9)
    }

    @Test
    fun `rms does not let opposite errors cancel`() {
        val residual = MountResidual()
        residual.record(pose(0.0, 6.0))
        residual.record(pose(0.0, -6.0))
        // A mean of the signed errors would report a flattering 0.0 here.
        assertEquals(6.0, residual.rmsDegrees, 1e-9)
        assertEquals(6.0, residual.worstDegrees, 1e-9)
    }

    @Test
    fun `worst tracks the single bad cycle rather than the average`() {
        val residual = MountResidual()
        residual.record(pose(0.5, 0.3))
        residual.record(pose(0.2, 11.0))
        residual.record(pose(0.4, 0.1))
        assertEquals(11.0, residual.worstDegrees, 1e-9)
        assertEquals(3, residual.count)
        assertEquals(0.1, residual.last?.headRotationDegrees ?: -1.0, 1e-9)
    }

    @Test
    fun `an empty record reports nothing rather than dividing by zero`() {
        val residual = MountResidual()
        assertEquals(0.0, residual.rmsDegrees, 1e-9)
        assertEquals(0.0, residual.worstDegrees, 1e-9)
        assertEquals(null, residual.last)
    }

    @Test
    fun `acceptability is judged against the mode that made the promise`() {
        val small = UprightCheck(index = 1, neckExtensionDegrees = 3.0, headRotationDegrees = 0.0)
        // Cheek promises 7 degrees, so half a band is 3.5.
        assertTrue(small.isAcceptableFor(MountMode.CHEEK))
        // Headband promises 5, so the same drift is no longer good enough for that promise.
        assertFalse(small.isAcceptableFor(MountMode.HEADBAND))
        assertTrue(small.isAcceptableFor(MountMode.IN_HAND))
    }

    @Test
    fun `reset clears the history between sessions`() {
        val residual = MountResidual()
        residual.record(pose(9.0, 9.0))
        residual.reset()
        assertEquals(0, residual.count)
        assertEquals(0.0, residual.worstDegrees, 1e-9)
    }
}

class MountCalibrationQualityTest {

    private fun aboutAxis(x: Double, y: Double, z: Double, degrees: Double) =
        Quaternion.fromAxisAngle(Vector3(x, y, z), Math.toRadians(degrees))

    /** Phone standing on its short edge, screen facing sideways: any of the three holds. */
    private val heldUpright = aboutAxis(1.0, 0.0, 0.0, 90.0)

    @Test
    fun `a phone held upright against the cheek calibrates cleanly`() {
        val calibration = MountCalibration.fromUprightSample(heldUpright, MountMode.CHEEK)
        assertTrue(calibration.isUsable)
        assertEquals(1.0, calibration.hintHorizontality, 1e-6)
        assertEquals(1.0, calibration.screenUprightness, 1e-6)
    }

    @Test
    fun `a phone lying face up on a desk is refused for every mount`() {
        // The mistake people actually make. Identity is a phone flat on a table, screen at the
        // ceiling. Its forward axis is perfectly well determined — the long edge points somewhere
        // horizontal — so a check that only asked about forward would wave this through, and the
        // app would spend the session reporting angles for a desk.
        for (mode in MountMode.entries) {
            val calibration = MountCalibration.fromUprightSample(Quaternion.IDENTITY, mode)
            assertFalse(
                calibration.isUsable,
                "${mode.name} accepted a phone lying flat: uprightness " +
                    "${calibration.screenUprightness}, horizontality ${calibration.hintHorizontality}",
            )
        }
    }

    @Test
    fun `screen uprightness is judged separately from the forward axis`() {
        // Flat on a desk: forward is fine, the screen is not.
        val flat = MountCalibration.fromUprightSample(Quaternion.IDENTITY, MountMode.CHEEK)
        assertEquals(1.0, flat.hintHorizontality, 1e-6)
        assertEquals(0.0, flat.screenUprightness, 1e-6)

        // Tipped fully back: the screen is on edge, but the face now points at the ceiling.
        val tippedBack = MountCalibration.fromUprightSample(
            aboutAxis(0.0, 1.0, 0.0, 90.0),
            MountMode.CHEEK,
        )
        assertEquals(0.0, tippedBack.hintHorizontality, 1e-6)
        assertEquals(1.0, tippedBack.screenUprightness, 1e-6)

        // Two independent ways to be wrong, and both are refused.
        assertFalse(flat.isUsable)
        assertFalse(tippedBack.isUsable)
    }

    @Test
    fun `a cheek calibration with the face pointing at the ceiling is refused`() {
        // Rotating 90 degrees about world Y sends the phone's negative X axis straight up, so
        // there is no horizontal component left to define "forward" with.
        val phone = aboutAxis(0.0, 1.0, 0.0, 90.0)
        val calibration = MountCalibration.fromUprightSample(phone, MountMode.CHEEK)
        assertFalse(
            calibration.isUsable,
            "horizontality was ${calibration.hintHorizontality}, expected a refusal",
        )
    }

    @Test
    fun `practice mode wants the phone stood up, not lying flat`() {
        // Flat on a table the screen faces the ceiling, so the practice mode's forward axis is
        // vertical and undetermined.
        val flat = MountCalibration.fromUprightSample(Quaternion.IDENTITY, MountMode.IN_HAND)
        assertFalse(flat.isUsable)

        // Stood upright with the screen toward the user, it is fully determined.
        val upright = MountCalibration.fromUprightSample(heldUpright, MountMode.IN_HAND)
        assertTrue(upright.isUsable)
        assertEquals(1.0, upright.hintHorizontality, 1e-6)
    }

    @Test
    fun `a usable calibration still measures zero at the pose it was taken in`() {
        val phone = heldUpright
        val calibration = MountCalibration.fromUprightSample(phone, MountMode.CHEEK)
        assertTrue(calibration.isUsable)
        val pose = HeadAngles.compute(phone, calibration)
        assertEquals(0.0, pose.neckExtensionDegrees, 1e-6)
        assertEquals(0.0, pose.headRotationDegrees, 1e-6)
    }

    @Test
    fun `the borderline sits where the documentation says it does`() {
        // 30 degrees off vertical is the stated cut-off. Just inside it must pass, just outside
        // must fail, or the constant and the prose have drifted apart.
        val justInside = MountCalibration.fromUprightSample(
            aboutAxis(0.0, 1.0, 0.0, 59.0),
            MountMode.CHEEK,
        )
        val justOutside = MountCalibration.fromUprightSample(
            aboutAxis(0.0, 1.0, 0.0, 61.0),
            MountMode.CHEEK,
        )
        assertTrue(justInside.isUsable, "horizontality ${justInside.hintHorizontality}")
        assertFalse(justOutside.isUsable, "horizontality ${justOutside.hintHorizontality}")
    }
}

/**
 * Head pitch: the angle of the head's long axis (the crown direction) below horizontal. This is the
 * measure the Epley is written in — "head hanging about 20-30 degrees below horizontal" — and it
 * reads -90 sitting up, 0 lying flat, positive once the head hangs off the bed.
 */
class HeadPitchTest {

    private fun aboutAxis(x: Double, y: Double, z: Double, degrees: Double) =
        Quaternion.fromAxisAngle(Vector3(x, y, z), Math.toRadians(degrees))

    // Phone upright against the cheek. Its negative X faces forward (world -X), so lying back
    // rotates the long axis toward world +X: a rotation about world Y.
    private val upright = aboutAxis(1.0, 0.0, 0.0, 90.0)
    private val mount = MountCalibration.fromUprightSample(upright, MountMode.CHEEK)
    private fun lieBack(degrees: Double, from: Quaternion = upright) = aboutAxis(0.0, 1.0, 0.0, degrees) * from

    @Test
    fun `sitting upright is minus ninety`() {
        kotlin.test.assertEquals(-90.0, HeadAngles.compute(upright, mount).pitchDegrees, 1e-6)
    }

    @Test
    fun `lying flat is zero`() {
        kotlin.test.assertEquals(0.0, HeadAngles.compute(lieBack(90.0), mount).pitchDegrees, 1e-6)
    }

    @Test
    fun `hanging twenty five degrees off the bed reads plus twenty five`() {
        // The position this bug made unreachable: the old face-based measure reads about -65 here.
        val pose = HeadAngles.compute(lieBack(115.0), mount)
        kotlin.test.assertEquals(25.0, pose.pitchDegrees, 1e-6)
        kotlin.test.assertEquals(0.0, pose.headRotationDegrees, 1e-6, "lying back is not a turn")
    }

    @Test
    fun `a head turn made sitting up survives lying back`() {
        // Epley step one to step two: turn 45, then lie back with the turn held.
        val turned = aboutAxis(0.0, 0.0, 1.0, 45.0) * upright
        val pose = HeadAngles.compute(lieBack(115.0, from = turned), mount)
        kotlin.test.assertEquals(25.0, pose.pitchDegrees, 1e-6)
        kotlin.test.assertEquals(45.0, kotlin.math.abs(pose.headRotationDegrees), 1e-6)
    }
}
