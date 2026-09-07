package health.epley.app

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import health.epley.core.MountMode
import health.epley.core.Side
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

    private val runController = RunController()

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
                tracker.state.collect { state ->
                    logger?.append(state)
                    runController.onTrackerState(state)
                }
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    val run by runController.state.collectAsState()
                    val trackerState by tracker.state.collectAsState()

                    if (run.running) {
                        RunScreen(
                            run = run,
                            toleranceDegrees = trackerState.mode.toleranceDegrees,
                            onStop = runController::stop,
                        )
                    } else {
                        ProbeScreen(
                            tracker = tracker,
                            run = run,
                            controller = runController,
                            onToggleLogging = ::toggleLogging,
                            isLogging = logger != null,
                            logDirectory = getExternalFilesDir(null)?.absolutePath ?: "",
                        )
                    }
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
    run: RunUiState,
    controller: RunController,
    onToggleLogging: () -> Boolean,
    isLogging: Boolean,
    logDirectory: String,
) {
    val state by tracker.state.collectAsState()
    var logging by remember { mutableStateOf(isLogging) }
    var calibrationMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (tracker.isSupported) tracker.sensorName else "NO ORIENTATION SENSOR",
            color = if (tracker.isSupported) Color(0xFF7FB3FF) else Color(0xFFFF6B6B),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
        )

        MountPicker(
            selected = state.mode,
            onSelect = {
                tracker.setMode(it)
                calibrationMessage = null
            },
        )

        Text(
            text = state.mode.instruction,
            color = Color(0xFFCCCCCC),
            fontSize = 14.sp,
        )

        if (!state.mode.tracksTheHead) {
            // This mode measures a hand. Saying so once, plainly, is the difference between a
            // practice feature and a false claim about someone's treatment.
            Text(
                text = "PRACTICE ONLY — this reads the phone, not your head. Nothing here is a treatment.",
                color = Color(0xFFFFD93D),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        if (state.isJolted) {
            Text(
                text = "MOUNT MOVED — the phone turned faster than a neck can (peak " +
                    "${state.peakRateDegPerSec.toInt()}°/s). Recalibrate before trusting these numbers.",
                color = Color(0xFFFF6B6B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        calibrationMessage?.let {
            Text(text = it, color = Color(0xFFFF6B6B), fontSize = 13.sp)
        }

        if (!state.isCalibrated) {
            Text(
                text = "Get into that position, hold still, then calibrate.",
                color = Color(0xFF888888),
                fontSize = 14.sp,
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
            onClick = {
                calibrationMessage = when (tracker.calibrate()) {
                    CalibrationResult.OK -> null
                    CalibrationResult.NO_SAMPLES -> "No sensor data yet — wait a second and retry."
                    CalibrationResult.PHONE_TOO_FLAT ->
                        "The phone is not being held the way this mount needs. It has to be on " +
                            "edge — not lying flat, not tipped fully back — so it can tell which " +
                            "way you are facing. Position it and try again."
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.isCalibrated) "RE-CALIBRATE" else "CALIBRATE") }

        if (state.isCalibrated) {
            OutlinedButton(
                onClick = { tracker.recordUprightCheck() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("BACK UPRIGHT — CHECK DRIFT") }

            // The one honest measurement of whether the mount held. Truth here is zero on both
            // angles, so whatever it reads is the error, and it costs nothing to take.
            Text(
                text = if (state.residualChecks == 0) {
                    "Return to the calibration pose and tap the button: whatever it reads then " +
                        "is how far the mount has drifted."
                } else {
                    "drift  last %.1f°   worst %.1f°   over %d check(s)   tolerance %.0f°".format(
                        state.lastResidualDegrees ?: 0.0,
                        state.worstResidualDegrees,
                        state.residualChecks,
                        state.mode.toleranceDegrees,
                    )
                },
                color = when {
                    state.residualChecks == 0 -> Color(0xFF888888)
                    state.worstResidualDegrees <= state.mode.toleranceDegrees / 2 -> Color(0xFF6BCB77)
                    else -> Color(0xFFFF6B6B)
                },
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

        if (state.isCalibrated) {
            GuidedRunSetup(
                run = run,
                controller = controller,
                trackerState = state,
                onStart = {
                    controller.start(
                        toleranceDegrees = state.mode.toleranceDegrees,
                        stillnessThresholdDegPerSec = HeadTracker.STILLNESS_THRESHOLD_DEG_PER_SEC,
                    )
                },
            )
        }

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

/**
 * The two things a guided run needs that calibration does not supply.
 *
 * Which ear the user reports as affected, and — separately — which way the sensor's positive
 * rotation points. The second cannot be derived from the first: the sign falls out of which cheek
 * the phone is against and which way round it sits, so it is learned by asking for one turn and
 * watching what happens. Assuming it would run the whole manoeuvre mirrored.
 */
@Composable
private fun GuidedRunSetup(
    run: RunUiState,
    controller: RunController,
    trackerState: TrackerState,
    onStart: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "WHICH EAR WERE YOU TOLD IS AFFECTED",
            color = Color(0xFF888888),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (side in Side.entries) {
                val selected = side == run.side
                Button(
                    onClick = { controller.setSide(side) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFF2A4A7F) else Color(0xFF1A1A1A),
                        contentColor = if (selected) Color.White else Color(0xFF999999),
                    ),
                ) { Text(if (side == Side.LEFT) "LEFT" else "RIGHT") }
            }
        }

        Text(
            text = if (run.polarity == null) {
                "Now turn your head toward that side, hold it there, and tap below. This teaches " +
                    "the app which direction is which — it cannot work that out on its own."
            } else {
                "Direction learned."
            },
            color = if (run.polarity == null) Color(0xFFCCCCCC) else Color(0xFF6BCB77),
            fontSize = 13.sp,
        )

        run.polarityMessage?.let {
            Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp)
        }

        OutlinedButton(
            onClick = { controller.learnPolarity(trackerState) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (run.polarity == null) "I AM TURNED THAT WAY" else "LEARN DIRECTION AGAIN") }

        Button(
            onClick = onStart,
            enabled = run.canStart,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2E6B3E),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFF1A1A1A),
                disabledContentColor = Color(0xFF666666),
            ),
        ) { Text("START GUIDED RUN") }
    }
}

/**
 * Choose how the phone is being held.
 *
 * On screen before anything else, because the mount decides both the tolerance the app can promise
 * and which way it thinks the face points. A user with no headband picks the first option and
 * loses nothing but two degrees of tolerance; a reviewer with no intention of lying on a bed picks
 * the last and sees the whole thing work in their hand.
 */
@Composable
private fun MountPicker(
    selected: MountMode,
    onSelect: (MountMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "HOW ARE YOU HOLDING THE PHONE",
            color = Color(0xFF888888),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )
        for (mode in MountMode.entries) {
            val isSelected = mode == selected
            Button(
                onClick = { onSelect(mode) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFF2A4A7F) else Color(0xFF1A1A1A),
                    contentColor = if (isSelected) Color.White else Color(0xFF999999),
                ),
            ) {
                Text(
                    text = "${mode.displayName}   ±${mode.toleranceDegrees.toInt()}°",
                    fontSize = 14.sp,
                )
            }
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
        // mount_mode and is_jolted turn an unexplained recording into an explained one: a run
        // full of nonsense is very different evidence if the mount was already flagged as moved.
        writer.write(
            "timestamp_nanos,mount_mode,neck_extension_deg,head_rotation_deg,swing_deg," +
                "rotation_reliable,rate_deg_per_sec,is_still,is_jolted\n",
        )
    }

    fun append(state: TrackerState) {
        val pose = state.pose ?: return
        writer.write(
            "${state.lastTimestampNanos}," +
                "${state.mode.name}," +
                "%.3f,".format(pose.neckExtensionDegrees) +
                "%.3f,".format(pose.headRotationDegrees) +
                "%.3f,".format(pose.swingDegrees) +
                "${pose.isRotationReliable}," +
                "%.3f,".format(state.angularRateDegPerSec) +
                "${state.isStill}," +
                "${state.isJolted}\n",
        )
    }

    fun close() {
        runCatching {
            writer.flush()
            writer.close()
        }
    }
}
