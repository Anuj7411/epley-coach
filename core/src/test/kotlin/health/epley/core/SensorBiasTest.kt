package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A phone whose sensor is miscalibrated must still be guided correctly.
 *
 * Measured on the test device: absolute tilt is offset by about 9-10 degrees, fixed in the phone's
 * own frame — the reversal check separated a level floor (-0.9) from a sensor offset (-9.2). The
 * app's angles are all relative to a calibration captured on the user, so the offset appears in
 * both the reference and the reading and cancels. This proves that rather than asserting it.
 */
class SensorBiasTest {

    private fun aboutAxis(x: Double, y: Double, z: Double, degrees: Double) =
        Quaternion.fromAxisAngle(Vector3(x, y, z), Math.toRadians(degrees))

    /** The phone held upright against a cheek. */
    private val upright = aboutAxis(1.0, 0.0, 0.0, 90.0)

    /** Lying back with the head hanging 25 degrees below horizontal. */
    private val hanging = aboutAxis(0.0, 1.0, 0.0, 115.0) * upright

    /**
     * A fixed error in the phone's own frame: whatever the phone's true orientation, the sensor
     * reports it rotated by this much. 9.2 degrees is what the test device actually shows.
     */
    private fun asReportedBy(bias: Quaternion, trueOrientation: Quaternion) = trueOrientation * bias

    @Test
    fun `a nine degree sensor offset does not move the measured head angles`() {
        val bias = aboutAxis(1.0, 0.0, 0.0, 9.2)

        val honest = MountCalibration.fromUprightSample(upright, MountMode.CHEEK)
        val biased = MountCalibration.fromUprightSample(asReportedBy(bias, upright), MountMode.CHEEK)

        val honestPose = HeadAngles.compute(hanging, honest)
        val biasedPose = HeadAngles.compute(asReportedBy(bias, hanging), biased)

        assertEquals(honestPose.pitchDegrees, biasedPose.pitchDegrees, 1e-6)
        assertEquals(honestPose.headRotationDegrees, biasedPose.headRotationDegrees, 1e-6)
        assertEquals(25.0, biasedPose.pitchDegrees, 1e-6, "the hanging position must still read 25")
    }

    @Test
    fun `the offset cancels whichever way it is oriented`() {
        // Not just a pitch offset: a mounting error can be about any axis.
        for (axis in listOf(Vector3(1.0, 0.0, 0.0), Vector3(0.0, 1.0, 0.0), Vector3(0.0, 0.0, 1.0))) {
            for (degrees in listOf(3.0, 9.2, 15.0)) {
                val bias = aboutAxis(axis.x, axis.y, axis.z, degrees)
                val mount = MountCalibration.fromUprightSample(asReportedBy(bias, upright), MountMode.CHEEK)
                val pose = HeadAngles.compute(asReportedBy(bias, hanging), mount)
                assertEquals(25.0, pose.pitchDegrees, 1e-6, "axis $axis, $degrees degrees")
            }
        }
    }

    @Test
    fun `an uncalibrated absolute tilt does carry the offset — which is why the self-check exists`() {
        // The one number a device offset does reach is the raw tilt the accuracy screen reads.
        // Measuring it is the point of the reversal check, not a defect in it.
        val bias = aboutAxis(1.0, 0.0, 0.0, 9.2)
        val reported = asReportedBy(bias, Quaternion.IDENTITY)
        val tilt = Math.toDegrees(kotlin.math.asin(reported.rotate(Vector3(0.0, 1.0, 0.0)).normalized() dot Vector3.DOWN))
        assertTrue(kotlin.math.abs(kotlin.math.abs(tilt) - 9.2) < 1e-6, "raw tilt was $tilt")
    }
}
