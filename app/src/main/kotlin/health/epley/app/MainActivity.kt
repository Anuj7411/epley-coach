package health.epley.app

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import java.io.File

/**
 * Angle probe.
 *
 * This is not the product. It is the instrument that tells us whether the product is possible: it
 * streams head angles from the phone's orientation sensor, shows them large enough to read from
 * across a room, and logs every sample to CSV.
 *
 * The test it exists for: rest the phone on a digital angle finder at 0, 20, 30, 45, 60, 90, 110
 * and 135 degrees, and compare. That comparison is the difference between believing this works and
 * knowing it does — and the resulting table is also what goes in the README.
 */
class MainActivity : ComponentActivity() {

    private lateinit var tracker: HeadTracker

    /**
     * Written from a single collector coroutine, read from the UI thread on toggle.
     *
     * Volatile because the collector runs on the main dispatcher but the reference is swapped by a
     * button tap; without it the collector could keep writing to a closed file.
     */
    @Volatile
    private var logger: CsvLogger? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Non-wakeup sensors stop delivering when the application processor suspends, and the
        // manoeuvre is performed with the phone against the face and the eyes shut. The screen has
        // to stay on for the samples to keep arriving at all.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tracker = HeadTracker(getSystemService(Context.SENSOR_SERVICE) as SensorManager)

        // Drain the sensor stream into the CSV whenever logging is active. Without this the file
        // gets its header and nothing else — which is exactly the bug this comment exists to stop
        // anyone reintroducing.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tracker.state.collect { state -> logger?.append(state) }
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    ProbeScreen(
                        tracker = tracker,
                        onToggleLogging = ::toggleLogging,
                        isLogging = logger != null,
                        logDirectory = getExternalFilesDir(null)?.absolutePath ?: "",
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tracker.start()
    }

    override fun onPause() {
        super.onPause()
        tracker.stop()
        logger?.close()
        logger = null
    }

    private fun toggleLogging(): Boolean {
        val current = logger
        return if (current != null) {
            current.close()
            logger = null
            false
        } else {
            val dir = getExternalFilesDir(null) ?: return false
            logger = CsvLogger(dir)
            true
        }
    }
}

@Composable
private fun ProbeScreen(
    tracker: HeadTracker,
    onToggleLogging: () -> Boolean,
    isLogging: Boolean,
    logDirectory: String,
) {
    val state by tracker.state.collectAsState()
    var logging by remember { mutableStateOf(isLogging) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (tracker.isSupported) tracker.sensorName else "NO ORIENTATION SENSOR",
            color = if (tracker.isSupported) Color(0xFF7FB3FF) else Color(0xFFFF6B6B),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
        )

        Spacer(Modifier.height(8.dp))

        if (!state.isCalibrated) {
            Text(
                text = "Sit upright, face forward, hold the phone to your cheek, then calibrate.",
                color = Color(0xFFCCCCCC),
                fontSize = 16.sp,
            )
        }

        AngleReadout(
            label = "NECK EXTENSION",
            degrees = state.pose?.neckExtensionDegrees,
            hint = "angle to gravity — does not drift",
        )

        AngleReadout(
            label = "HEAD ROTATION",
            degrees = state.pose?.headRotationDegrees,
            hint = if (state.pose?.isRotationReliable == false) {
                "UNRELIABLE — swung ${state.pose?.swingDegrees?.toInt()}° from the reference axis"
            } else {
                "about the body axis — gyro integrated, watch this one"
            },
            hintIsWarning = state.pose?.isRotationReliable == false,
        )

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "%.1f °/s".format(state.angularRateDegPerSec),
                color = if (state.isStill) Color(0xFF6BCB77) else Color(0xFFFFD93D),
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = if (state.isStill) "STILL" else "MOVING",
                color = if (state.isStill) Color(0xFF6BCB77) else Color(0xFFFFD93D),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Text(
            text = "samples ${state.sampleCount}   since calib ${state.samplesSinceCalibration}   " +
                "accuracy ${state.sensorAccuracy}",
            color = Color(0xFF888888),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { tracker.calibrate() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.isCalibrated) "RE-CALIBRATE" else "CALIBRATE") }

        Button(
            onClick = { logging = onToggleLogging() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (logging) "STOP LOGGING" else "START LOGGING") }

        if (logging) {
            Text(
                text = "writing to $logDirectory",
                color = Color(0xFF6BCB77),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun AngleReadout(
    label: String,
    degrees: Double?,
    hint: String,
    hintIsWarning: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .padding(14.dp),
    ) {
        Text(label, color = Color(0xFF888888), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = degrees?.let { "%+.1f".format(it) } ?: "--",
                color = Color.White,
                // Deliberately huge: this has to be readable while the phone sits on an angle
                // finder a metre away, with the reader holding a protractor in the other hand.
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text("°", color = Color(0xFF888888), fontSize = 28.sp)
        }
        Text(
            text = hint,
            color = if (hintIsWarning) Color(0xFFFF6B6B) else Color(0xFF666666),
            fontSize = 11.sp,
        )
    }
}

/**
 * Appends every sample to a CSV in the app's external files directory.
 *
 * Pulled off the device with `adb pull`. This file is the validation evidence — one row per
 * sensor sample, with the raw timestamp, so accuracy against a physical reference and drift over a
 * session are both recoverable from it afterwards.
 */
internal class CsvLogger(directory: File) {

    private val file = File(directory, "angles-${System.currentTimeMillis()}.csv")
    private val writer = file.bufferedWriter()

    init {
        // swing_deg and rotation_reliable are diagnostics, not clinical values. They record how
        // well-conditioned the twist decomposition was for each sample, so a suspicious reading
        // can be checked after the fact rather than argued about.
        writer.write(
            "timestamp_nanos,neck_extension_deg,head_rotation_deg,swing_deg," +
                "rotation_reliable,rate_deg_per_sec,is_still\n",
        )
    }

    fun append(state: TrackerState) {
        val pose = state.pose ?: return
        writer.write(
            "${state.lastTimestampNanos}," +
                "%.3f,".format(pose.neckExtensionDegrees) +
                "%.3f,".format(pose.headRotationDegrees) +
                "%.3f,".format(pose.swingDegrees) +
                "${pose.isRotationReliable}," +
                "%.3f,".format(state.angularRateDegPerSec) +
                "${state.isStill}\n",
        )
    }

    fun close() {
        runCatching {
            writer.flush()
            writer.close()
        }
    }
}
