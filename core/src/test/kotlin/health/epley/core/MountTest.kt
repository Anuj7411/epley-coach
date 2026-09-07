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

    @Test
    fun `a phone held upright against the cheek calibrates cleanly`() {
        // Identity: the phone's negative X axis lies in the horizontal plane, which is what the
        // cheek hold produces when someone sits up and faces forward.
        val calibration = MountCalibration.fromUprightSample(Quaternion.IDENTITY, MountMode.CHEEK)
        assertTrue(calibration.isUsable)
        assertEquals(1.0, calibration.hintHorizontality, 1e-6)
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
        val upright = MountCalibration.fromUprightSample(
            aboutAxis(1.0, 0.0, 0.0, 90.0),
            MountMode.IN_HAND,
        )
        assertTrue(upright.isUsable)
        assertEquals(1.0, upright.hintHorizontality, 1e-6)
    }

    @Test
    fun `a usable calibration still measures zero at the pose it was taken in`() {
        val phone = Quaternion.IDENTITY
        val calibration = MountCalibration.fromUprightSample(phone, MountMode.CHEEK)
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
