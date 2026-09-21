package health.epley.app

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import health.epley.core.AngleUnwrapper
import health.epley.core.HeadAngles
import health.epley.core.HeadPose
import health.epley.core.MountCalibration
import health.epley.core.MountMode
import health.epley.core.MountMonitor
import health.epley.core.MountResidual
import health.epley.core.Quaternion
import health.epley.core.Vector3
import health.epley.core.UprightCheck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

/**
 * Streams head angles from the phone's orientation sensor.
 *
 * ## Sensor choice
 *
 * [Sensor.TYPE_GAME_ROTATION_VECTOR], not [Sensor.TYPE_ROTATION_VECTOR]. The difference is the
 * magnetometer: the plain rotation vector fuses it to get absolute heading, and a magnetometer
 * beside a steel bed frame, a laptop and a second phone is not a heading reference. The game
 * rotation vector omits it and instead lets yaw drift slowly, which is a problem we can bound by
 * zeroing everything against a calibration captured seconds earlier.
 *
 * ## What drifts and what does not
 *
 * Neck extension is an angle to **gravity**, so the accelerometer supplies a permanent absolute
 * reference and fusion bounds that error forever. It does not drift. Head rotation while seated is
 * rotation *about* gravity, which gravity cannot see, so it depends on gyro integration between
 * calibration and the supine position — roughly a minute. That is the one number to be sceptical
 * of, and the one the drift test exists to quantify.
 */
class HeadTracker(
    private val sensorManager: SensorManager,
) : SensorEventListener {

    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    /** True when this device can report orientation at all. Some budget phones have no gyroscope. */
    val isSupported: Boolean get() = sensor != null

    /** Which sensor we actually got, for the record. Reported in the UI and logged to CSV. */
    val sensorName: String
        get() = when (sensor?.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR -> "GAME_ROTATION_VECTOR (no magnetometer)"
            Sensor.TYPE_ROTATION_VECTOR -> "ROTATION_VECTOR (fuses magnetometer)"
            else -> "none available"
        }

    private val _state = MutableStateFlow(TrackerState())
    val state: StateFlow<TrackerState> = _state

    // Head rotation arrives folded into (-180, 180]. Unwrapped here so a pass through the
    // boundary reads as continuous motion instead of a 320 degree jump, which is what the
    // first hardware recording actually produced.
    private val rotationUnwrapper = AngleUnwrapper()

    private val mountMonitor = MountMonitor()
    private val mountResidual = MountResidual()

    private var calibration: MountCalibration? = null
    private var lastQuaternion: Quaternion? = null
    private var lastTimestampNanos: Long = 0L

    /**
     * How the phone is currently being held.
     *
     * Changing it invalidates the calibration, because the mounting transform is derived from a
     * mode-specific assumption about which way the face points. Carrying a cheek calibration into
     * practice mode would silently measure the wrong axis.
     */
    fun setMode(mode: MountMode) {
        if (mode == _state.value.mode) return
        calibration = null
        rotationUnwrapper.reset()
        mountMonitor.clear()
        mountResidual.reset()
        _state.value = TrackerState(
            mode = mode,
            sampleCount = _state.value.sampleCount,
            calibrationGeneration = _state.value.calibrationGeneration + 1,
        )
    }

    fun start() {
        val s = sensor ?: return
        // SENSOR_DELAY_GAME is ~50 Hz. Head repositioning happens well under 2 Hz, so FASTEST buys
        // nothing but battery drain and noise. maxReportLatency 0 disables batching, which would
        // otherwise deliver samples in bursts and ruin the stillness detector.
        sensorManager.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME, 0)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /**
     * Capture the phone-to-head relationship.
     *
     * Called while the user sits upright, faces forward, and holds still. Everything measured
     * afterwards is relative to this, which is what makes display rotation, device natural
     * orientation and absolute heading all irrelevant.
     */
    fun calibrate(): CalibrationResult {
        val q = lastQuaternion ?: return CalibrationResult.NO_SAMPLES

        val candidate = MountCalibration.fromUprightSample(q, _state.value.mode)
        if (!candidate.isUsable) {
            // The phone is lying too flat for its forward axis to be determined. Accepting this
            // would produce a mounting transform built out of numerical noise, and every angle for
            // the rest of the session would inherit it.
            _state.value = _state.value.copy(lastCalibrationRejected = true)
            return CalibrationResult.PHONE_TOO_FLAT
        }

        calibration = candidate
        // The reference has changed, so continuity with samples taken against the old one is
        // meaningless, and a jolt that predates it no longer says anything about the new mount.
        rotationUnwrapper.reset()
        mountMonitor.clear()
        _state.value = _state.value.copy(
            isCalibrated = true,
            samplesSinceCalibration = 0,
            lastCalibrationRejected = false,
            isJolted = false,
            calibrationGeneration = _state.value.calibrationGeneration + 1,
        )
        return CalibrationResult.OK
    }

    /**
     * Check the mount against a pose whose true answer is known.
     *
     * Called when the user is back sitting upright and facing forward — the pose the calibration
     * was taken in, where both angles should read zero. Whatever they actually read is the drift
     * accumulated since, and it is the only direct evidence we have that a hand-held phone stayed
     * where it was put.
     */
    fun recordUprightCheck(): UprightCheck? {
        val pose = _state.value.pose ?: return null
        val check = mountResidual.record(pose)
        _state.value = _state.value.copy(
            lastResidualDegrees = check.residualDegrees,
            worstResidualDegrees = mountResidual.worstDegrees,
            residualChecks = mountResidual.count,
        )
        return check
    }

    fun clearCalibration() {
        calibration = null
        rotationUnwrapper.reset()
        _state.value = _state.value.copy(
            isCalibrated = false,
            calibrationGeneration = _state.value.calibrationGeneration + 1,
        )
    }

    override fun onSensorChanged(event: SensorEvent) {
        val q = Quaternion.fromRotationVector(event.values)

        // Angular rate from consecutive orientations. Used to tell "held still at the target" from
        // "passing through the target", which is the difference between a real hold and a lie.
        val previous = lastQuaternion
        val degreesPerSecond = if (previous != null && lastTimestampNanos != 0L) {
            val dtSeconds = (event.timestamp - lastTimestampNanos) / 1_000_000_000.0
            if (dtSeconds > 1e-6) {
                Math.toDegrees(previous.angularDistanceTo(q)) / dtSeconds
            } else {
                _state.value.angularRateDegPerSec
            }
        } else {
            0.0
        }

        lastQuaternion = q
        lastTimestampNanos = event.timestamp

        // The phone's own tilt, independent of any calibration: the angle of its top edge below
        // horizontal. Used by the accuracy self-check, which needs the raw instrument rather than
        // a head angle derived from it.
        val deviceTilt = Math.toDegrees(
            kotlin.math.asin((q.rotate(Vector3(0.0, 1.0, 0.0)).normalized() dot Vector3.DOWN).coerceIn(-1.0, 1.0)),
        )

        // A phone cannot slip without moving fast. Watching the rate is the only handle a single
        // orientation sensor gives us on the mount coming loose.
        mountMonitor.onSample(degreesPerSecond)

        val cal = calibration
        val rawPose = if (cal != null) HeadAngles.compute(q, cal) else null
        val pose = rawPose?.copy(
            headRotationDegrees = rotationUnwrapper.unwrap(rawPose.headRotationDegrees),
        )

        _state.value = _state.value.copy(
            pose = pose,
            angularRateDegPerSec = degreesPerSecond,
            isStill = abs(degreesPerSecond) < STILLNESS_THRESHOLD_DEG_PER_SEC,
            sampleCount = _state.value.sampleCount + 1,
            samplesSinceCalibration = if (cal != null) _state.value.samplesSinceCalibration + 1 else 0,
            lastTimestampNanos = event.timestamp,
            devicePitchDegrees = deviceTilt,
            isJolted = mountMonitor.isJolted,
            peakRateDegPerSec = mountMonitor.peakRateDegPerSec,
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        _state.value = _state.value.copy(sensorAccuracy = accuracy)
    }

    companion object {
        /**
         * Below this the head counts as held rather than moving.
         *
         * Chosen so that the hold timer cannot start while the head is merely sweeping through the
         * target angle. Without this gate the app would confidently report a completed hold that
         * never happened.
         */
        const val STILLNESS_THRESHOLD_DEG_PER_SEC = 5.0
    }
}

/** What came of asking for a calibration. */
enum class CalibrationResult {
    OK,

    /** No sensor sample has arrived yet, so there is nothing to calibrate against. */
    NO_SAMPLES,

    /**
     * The phone was too close to flat for its forward axis to be determined.
     *
     * Refused rather than accepted, because the resulting transform would be noise and every
     * angle for the rest of the session would be built on it.
     */
    PHONE_TOO_FLAT,
}

/** Everything the UI and the logger need, in one immutable snapshot. */
data class TrackerState(
    val pose: HeadPose? = null,
    val mode: MountMode = MountMode.DEFAULT,
    val angularRateDegPerSec: Double = 0.0,
    val isStill: Boolean = false,
    val isCalibrated: Boolean = false,
    val lastCalibrationRejected: Boolean = false,
    /** Latched once the phone has moved faster than a neck can. Cleared by recalibrating. */
    val isJolted: Boolean = false,
    val peakRateDegPerSec: Double = 0.0,
    val lastResidualDegrees: Double? = null,
    val worstResidualDegrees: Double = 0.0,
    val residualChecks: Int = 0,
    val sampleCount: Long = 0,
    val samplesSinceCalibration: Long = 0,
    /** The phone's own top-edge tilt below horizontal, before any calibration. */
    val devicePitchDegrees: Double = 0.0,
    val sensorAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val lastTimestampNanos: Long = 0,
    /**
     * Bumped every time the mounting reference changes — a new calibration, a cleared one, or a
     * new mount mode. Anything learned relative to the old reference, such as which way counts
     * as a turn toward the affected ear, is void once this moves.
     */
    val calibrationGeneration: Int = 0,
) {

    /** Whether the current pose is close enough to a target for the mount in use. */
    fun isInPosition(target: health.epley.core.TargetPose): Boolean =
        pose?.isWithin(target, mode.toleranceDegrees) == true
}
