package health.epley.app

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import health.epley.core.Cue
import health.epley.core.Episode
import health.epley.core.Feeling
import health.epley.core.EpisodeLog
import health.epley.core.Guidance
import health.epley.core.MountMode
import health.epley.core.SafetyOutcome
import health.epley.core.Side
import health.epley.core.TriageOutcome
import kotlinx.coroutines.launch
import java.io.File

/** Where the user is in the app when no run is in progress. */
enum class Screen { HOME, SAFETY, TRIAGE, PRACTICE_SIDE, HOLD, CALIBRATE, DIRECTION, READY, PAYWALL, INSTRUMENT }

/**
 * The app: a home screen, a six-step flow into a guided run, and the after-care that follows.
 *
 * Treatment: safety check, six questions, how the phone is held, calibrate, learn direction,
 * ready. Practice skips the first three — the phone stands in for the head, so there is nothing
 * to screen and no ear to identify — and asks only which side to rehearse.
 *
 * The engineering probe that proved the sensor works is still here, behind "Instrument" on the
 * home screen: it is what the angle-finder accuracy test uses.
 */
class MainActivity : ComponentActivity() {

    private lateinit var tracker: HeadTracker

    private lateinit var guidance: GuidanceOutput

    private lateinit var episodeStore: EpisodeStore

    /**
     * Swapped for the RevenueCat-backed implementation once the key exists. Nothing clinical
     * reads it — see [Entitlements].
     */
    private lateinit var entitlements: Entitlements

    private val runController = RunController(onCue = { cue ->
        if (::guidance.isInitialized) guidance.play(cue)
    })

    /**
     * Written from a single collector coroutine, read from the UI thread on toggle.
     *
     * Volatile because the collector runs on the main dispatcher but the reference is swapped by a
     * button tap; without it the collector could keep writing to a closed file.
     */
    @Volatile
    private var logger: CsvLogger? = null

    private var screen by mutableStateOf(Screen.HOME)
    private var episodes by mutableStateOf<List<Episode>>(emptyList())
    private var calibrationMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Non-wakeup sensors stop delivering when the application processor suspends, and the
        // manoeuvre is performed with the phone against the face and the eyes shut. The screen has
        // to stay on for the samples to keep arriving at all.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        tracker = HeadTracker(getSystemService(Context.SENSOR_SERVICE) as SensorManager)
        guidance = GuidanceOutput(this)
        episodeStore = EpisodeStore(this)
        entitlements = PlaceholderEntitlements(this)
        episodes = episodeStore.load()

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
            EpleyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Palette.Background) {
                    App()
                }
            }
        }
    }

    @Composable
    private fun App() {
        val run by runController.state.collectAsState()
        val trackerState by tracker.state.collectAsState()
        val practice = run.practice

        // Once the direction is learned there is nothing to confirm; move on by itself.
        LaunchedEffect(run.polarity, screen) {
            if (screen == Screen.DIRECTION && run.polarity != null) screen = Screen.READY
        }
        BackHandler(enabled = screen != Screen.HOME && !run.running) { screen = Screen.HOME }

        fun label(treatment: Int, practiceStep: Int) =
            if (practice) "Practice · step $practiceStep of 4" else "Step $treatment of 6"
        fun progress(treatment: Int, practiceStep: Int) =
            if (practice) practiceStep / 4f else treatment / 6f

        when {
            run.running && run.engineState?.guidance == Guidance.FINISHED -> AfterCareScreen(
                practice = practice,
                runsBefore = EpisodeLog.runsThisEpisode(episodes, System.currentTimeMillis()),
                onDone = { feeling -> endRun(completed = true, feeling = feeling) },
            )

            run.running -> RunScreen(
                run = run,
                onRepeat = runController::repeatInstruction,
                onStop = { endRun(completed = false, feeling = null) },
            )

            else -> when (screen) {
                Screen.HOME -> HomeScreen(
                    episodes = episodes,
                    onStart = { screen = Screen.SAFETY },
                    onPractice = { screen = Screen.PRACTICE_SIDE },
                    onExport = if (EpisodeLog.treatments(episodes).isEmpty()) {
                        null
                    } else {
                        { if (entitlements.hasExport) shareHistory() else screen = Screen.PAYWALL }
                    },
                    onInstrument = { screen = Screen.INSTRUMENT },
                )

                Screen.PAYWALL -> PaywallScreen(
                    entitlements = entitlements,
                    onDone = {
                        screen = Screen.HOME
                        shareHistory()
                    },
                    onCancel = { screen = Screen.HOME },
                )

                Screen.SAFETY -> SafetyScreen(
                    onFinished = {
                        runController.confirmSafety(it)
                        screen = if (it == SafetyOutcome.Clear) Screen.TRIAGE else Screen.HOME
                    },
                    onCancel = { screen = Screen.HOME },
                )

                Screen.TRIAGE -> TriageScreen(
                    onFinished = {
                        runController.confirmTriage(it)
                        screen = if (it is TriageOutcome.PosteriorCanal) Screen.HOLD else Screen.HOME
                    },
                    onCancel = { screen = Screen.HOME },
                )

                Screen.PRACTICE_SIDE -> PracticeSideScreen(
                    onSide = {
                        runController.preparePractice(it)
                        tracker.setMode(MountMode.IN_HAND)
                        calibrationMessage = null
                        screen = Screen.CALIBRATE
                    },
                    onBack = { screen = Screen.HOME },
                )

                Screen.HOLD -> HoldScreen(
                    onMode = {
                        tracker.setMode(it)
                        calibrationMessage = null
                        screen = Screen.CALIBRATE
                    },
                    onBack = { screen = Screen.HOME },
                )

                Screen.CALIBRATE -> CalibrateScreen(
                    stepLabel = label(4, 2),
                    progress = progress(4, 2),
                    mode = trackerState.mode,
                    message = calibrationMessage,
                    onCalibrate = {
                        when (tracker.calibrate()) {
                            CalibrationResult.OK -> {
                                calibrationMessage = null
                                screen = Screen.DIRECTION
                            }
                            CalibrationResult.NO_SAMPLES ->
                                calibrationMessage = "No sensor reading yet. Wait a second and try again."
                            CalibrationResult.PHONE_TOO_FLAT ->
                                calibrationMessage = "The phone is lying too flat to tell which way " +
                                    "you're facing. Hold it on its edge, as described, and try again."
                        }
                    },
                    onBack = { screen = if (practice) Screen.PRACTICE_SIDE else Screen.HOLD },
                )

                Screen.DIRECTION -> DirectionScreen(
                    stepLabel = label(5, 3),
                    progress = progress(5, 3),
                    side = run.side,
                    practice = practice,
                    message = run.polarityMessage,
                    onLearn = { runController.learnPolarity(trackerState) },
                    onBack = { screen = Screen.CALIBRATE },
                )

                Screen.READY -> ReadyScreen(
                    stepLabel = label(6, 4),
                    progress = progress(6, 4),
                    side = run.side,
                    mode = trackerState.mode,
                    practice = practice,
                    onStart = { runController.start(HeadTracker.STILLNESS_THRESHOLD_DEG_PER_SEC) },
                    onBack = { screen = Screen.DIRECTION },
                )

                Screen.INSTRUMENT -> ProbeScreen(
                    tracker = tracker,
                    onBack = { screen = Screen.HOME },
                    onToggleLogging = ::toggleLogging,
                    isLogging = logger != null,
                    logDirectory = getExternalFilesDir(null)?.absolutePath ?: "",
                )
            }
        }
    }

    /**
     * Hand the history to whatever the user picks — mail, messages, printing.
     *
     * A chooser, so sharing only ever happens to a destination they chose. The app itself sends
     * nothing anywhere.
     */
    private fun shareHistory() {
        val format = java.text.SimpleDateFormat("d MMM yyyy, h:mm a", java.util.Locale.getDefault())
        val report = EpisodeLog.report(episodes) { format.format(java.util.Date(it)) }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Epley Coach — my history")
            putExtra(android.content.Intent.EXTRA_TEXT, report)
        }
        startActivity(android.content.Intent.createChooser(intent, "Send history"))
    }

    /** Every run ends in the history, finished or not, so the record is honest about stops too. */
    private fun endRun(completed: Boolean, feeling: Feeling?) {
        val run = runController.state.value
        episodes = episodeStore.append(
            Episode(
                epochMillis = System.currentTimeMillis(),
                side = run.side,
                completed = completed,
                feeling = feeling,
                practice = run.practice,
            ),
        )
        guidance.silence()
        runController.stop()
        screen = Screen.HOME
    }

    /**
     * Either volume button stops a run.
     *
     * Someone face-down with their eyes shut cannot find a STOP button on a screen pressed to
     * their cheek. A physical button they can feel is the one control that works in every
     * position (spec FR-8). Outside a run the buttons change the volume as normal.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val volumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (volumeKey && runController.state.value.running) {
            endRun(completed = false, feeling = null)
            guidance.play(Cue.Speak("Stopped.", interrupt = true))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        tracker.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        guidance.release()
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
    onBack: () -> Unit,
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
        SecondaryButton("Back to home", onBack)

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
            label = "HEAD PITCH",
            degrees = state.pose?.pitchDegrees,
            hint = "crown below horizontal: −90 sitting, 0 lying flat, + hanging — does not drift",
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
            "timestamp_nanos,mount_mode,neck_extension_deg,pitch_deg,head_rotation_deg,swing_deg," +
                "rotation_reliable,rate_deg_per_sec,is_still,is_jolted\n",
        )
    }

    fun append(state: TrackerState) {
        val pose = state.pose ?: return
        writer.write(
            "${state.lastTimestampNanos}," +
                "${state.mode.name}," +
                "%.3f,".format(pose.neckExtensionDegrees) +
                "%.3f,".format(pose.pitchDegrees) +
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
