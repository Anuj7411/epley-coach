package health.epley.app

import android.content.Context

/**
 * This phone's measured tilt offset, kept between sessions.
 *
 * One number, on the device, never sent anywhere. It belongs to the phone rather than the person,
 * which is why it lives apart from the episode history.
 */
class DeviceCalibrationStore(context: Context) {

    private val prefs = context.getSharedPreferences("device_calibration", Context.MODE_PRIVATE)

    var tiltOffsetDegrees: Double
        get() = prefs.getFloat(KEY, 0f).toDouble()
        set(value) = prefs.edit().putFloat(KEY, value.toFloat()).apply()

    private companion object {
        const val KEY = "tilt_offset_degrees"
    }
}
