package health.epley.app

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import health.epley.core.AngleUnwrapper
import health.epley.core.HeadAngles
import health.epley.core.HeadPose
import health.epley.core.MountCalibration
import health.epley.core.Quaternion
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

    private var calibration: MountCalibration? = null
    private var lastQuaternion: Quaternion? = null
    private var lastTimestampNanos: Long = 0L

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
    fun calibrate() {
        val q = lastQuaternion ?: return
        calibration = MountCalibration.fromUprightSample(q)
        // The reference has changed, so continuity with samples taken against the old one is
        // meaningless.
        rotationUnwrapper.reset()
        _state.value = _state.value.copy(isCalibrated = true, samplesSinceCalibration = 0)
    }

    fun clearCalibration() {
        calibration = null
        rotationUnwrapper.reset()
        _state.value = _state.value.copy(isCalibrated = false)
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

/** Everything the UI and the logger need, in one immutable snapshot. */
data class TrackerState(
    val pose: HeadPose? = null,
    val angularRateDegPerSec: Double = 0.0,
    val isStill: Boolean = false,
    val isCalibrated: Boolean = false,
    val sampleCount: Long = 0,
    val samplesSinceCalibration: Long = 0,
    val sensorAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE,
    val lastTimestampNanos: Long = 0,
)
