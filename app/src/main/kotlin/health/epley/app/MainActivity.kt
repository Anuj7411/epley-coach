package health.epley.app

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
import health.epley.core.spokenSetupPrompt
import health.epley.core.word
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** Where the user is in the app when no run is in progress. */
enum class Screen { SPLASH, WELCOME, HOME, SETTINGS, RUNS, SAFETY, TRIAGE, PRACTICE_SIDE, HOLD, CALIBRATE, DIRECTION, READY, PAYWALL, INSTRUMENT, ACCURACY }

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

    private lateinit var deviceCalibration: DeviceCalibrationStore

    /**
     * RevenueCat when a key is configured, a local stand-in when it is not. Nothing clinical
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

    /**
     * Held while the app is in front.
     *
     * Keeping the screen on is not enough: a phone against a cheek looks like a pocket, and the
     * display sleeps anyway — seen on this hardware with the phone face-down. Non-wakeup sensors
     * stop delivering when the processor suspends, so a manoeuvre would silently stall.
     */
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private var screen by mutableStateOf(Screen.SPLASH)

    /**
     * Welcome is shown once (README §11). Read before the splash finishes, written the moment
     * "Get started" is pressed, so a kill during Welcome still shows it again — the three facts
     * on it are worth repeating, and nothing is lost by repeating them.
     */
    private val welcomePrefs by lazy { getSharedPreferences("welcome", Context.MODE_PRIVATE) }
    private var welcomePending by mutableStateOf(true)

    private var episodes by mutableStateOf<List<Episode>>(emptyList())

    /**
     * Seconds left before the app captures a setup pose, or null when it is not counting.
     *
     * The setup steps ask for the phone against a cheekbone, screen facing out — and then asked
     * the user to press a button on that screen. Found by holding the phone to a face: it cannot
     * be done. The button is now pressed while the screen is still visible, and the pose is
     * captured after a spoken countdown, once the sensor agrees the person has stopped moving.
     */
    private var setupCountdown by mutableStateOf<Int?>(null)
    private var calibrationMessage by mutableStateOf<String?>(null)

    /** When the current run started, for its duration in the history. */
    private var runStartedAt = 0L

    /** Head angles at the end of each held position of the current run (turn, tip). */
    private val heldAngles = mutableListOf<Pair<Double, Double>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Non-wakeup sensors stop delivering when the application processor suspends, and the
        // manoeuvre is performed with the phone against the face and the eyes shut. The screen has
        // to stay on for the samples to keep arriving at all.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        wakeLock = (getSystemService(Context.POWER_SERVICE) as android.os.PowerManager)
            .newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "EpleyCoach:run")

        tracker = HeadTracker(getSystemService(Context.SENSOR_SERVICE) as SensorManager)
        guidance = GuidanceOutput(this)
        episodeStore = EpisodeStore(this)
        deviceCalibration = DeviceCalibrationStore(this)
        tracker.tiltOffsetDegrees = deviceCalibration.tiltOffsetDegrees
        // A clone with no RevenueCat key of its own still builds and runs; it just gets the
        // stand-in, which says so on the paywall rather than pretending to take money.
        //
        // A Test Store key crashes any non-debuggable build, by RevenueCat's own documentation, and
        // must never ship. So a release build uses RevenueCat only with a real store key; with a
        // test key it gets the stand-in too. Debug builds keep the Test Store.
        val key = BuildConfig.REVENUECAT_API_KEY
        val usable = key.isNotBlank() && (BuildConfig.DEBUG || !key.startsWith("test_"))
        entitlements = if (usable) {
            RevenueCatEntitlements(this, key)
        } else {
            PlaceholderEntitlements(this)
        }
        episodes = episodeStore.load()

        // Drain the sensor stream into the CSV whenever logging is active. Without this the file
        // gets its header and nothing else — which is exactly the bug this comment exists to stop
        // anyone reintroducing.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                tracker.state.collect { state ->
                    // The engine runs first so the row can be labelled with the step it belongs
                    // to. Analysing the first run meant inferring the step boundaries from the
                    // angles, which is guesswork dressed up as evidence.
                    runController.onTrackerState(state)
                    val engine = runController.state.value.engineState
                    // A jolt is only meaningful where the head is supposed to be still.
                    tracker.stillnessExpected =
                        engine?.guidance == Guidance.HOLDING || engine?.guidance == Guidance.SETTLING
                    logger?.append(state, engine?.step?.id, engine?.guidance?.name)
                }
            }
        }

        welcomePending = !welcomePrefs.getBoolean("seen", false)

        // Hands the system splash over to the app without a blank frame between them.
        installSplashScreen()

        // The design draws edge to edge into its own 48 dp status and 32 dp gesture zones
        // (README ★P1). Bar icons are dark on the day ground and light on the night one.
        val nightAtLaunch = isNightNow(
            resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES,
        )
        val bars = if (nightAtLaunch) {
            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)

        setContent {
            // Night when the system is dark, or between 21:00 and 06:00. Read once, so the
            // colours cannot change under someone lying with their head hanging back.
            // uiMode, not Configuration.isNightModeActive — that getter is API 30 and minSdk
            // here is 24, so it would throw on anything older.
            val systemDark = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            val night = remember { isNightNow(systemDark) }
            EpleyTheme {
                EpleyDesign(night = night) {
                    Surface(modifier = Modifier.fillMaxSize(), color = Ds.ground) {
                        // README ★P1: every screen is written in design dp inside this frame.
                        DesignFrame { App() }
                    }
                }
            }
        }
    }

    @Composable
    private fun App() {
        val run by runController.state.collectAsState()
        val trackerState by tracker.state.collectAsState()
        val practice = run.practice

        // Each held position leaves its measured angles for the doctor's PDF: the pose at the
        // moment the engine marks the position complete, in the frame it judged it in.
        LaunchedEffect(run.engineState?.guidance, run.engineState?.stepIndex) {
            val state = run.engineState ?: return@LaunchedEffect
            val pose = run.pose ?: return@LaunchedEffect
            if (state.guidance == Guidance.STEP_COMPLETE && heldAngles.size == state.stepIndex) {
                val turn = if (run.polarity?.towardAffectedSideIsPositive == false) -pose.headRotationDegrees else pose.headRotationDegrees
                heldAngles += turn to pose.pitchDegrees
            }
        }

        // Once the direction is learned there is nothing to confirm; move on by itself.
        LaunchedEffect(run.polarity, screen) {
            if (screen == Screen.DIRECTION && run.polarity != null) screen = Screen.READY
        }
        // Back from Splash or Welcome leaves the app. Sending it to Home instead would be a way
        // past the one screen that has to be read.
        val backable = screen != Screen.HOME && screen != Screen.SPLASH && screen != Screen.WELCOME
        BackHandler(enabled = backable && !run.running) { screen = Screen.HOME }

        // Practice skips the safety check, the questions and the mount choice — the phone stands
        // in for the head, so there is nothing to screen and nothing to strap on.
        val totalSteps = if (practice) 4 else 6
        fun step(treatment: Int, practiceStep: Int) = if (practice) practiceStep else treatment
        fun label(treatment: Int, practiceStep: Int) =
            if (practice) "Practice · step $practiceStep of 4" else "Step $treatment of 6"

        if (!tracker.isSupported) {
            // FR-1 / README §14. Every angle comes from this sensor; without it the flow would
            // measure nothing and claim a completed manoeuvre.
            SensorUnavailableScreen()
            return
        }

        when {
            run.running && run.engineState?.guidance == Guidance.FINISHED -> AfterCareScreen(
                practice = practice,
                runsBefore = EpisodeLog.runsThisEpisode(episodes, System.currentTimeMillis()),
                // The last position returns to the pose the calibration was taken in, where the
                // true reading is zero. Whatever it actually reads is measured mount drift (FR-17).
                driftDegrees = run.pose?.let {
                    maxOf(kotlin.math.abs(it.pitchDegrees + 90.0), kotlin.math.abs(it.headRotationDegrees))
                },
                onDone = { feeling -> endRun(completed = true, feeling = feeling) },
                ear = if (run.side == Side.LEFT) 'L' else 'R',
            )

            run.running -> RunScreen(
                run = run,
                onRepeat = runController::repeatInstruction,
                onStop = { endRun(completed = false, feeling = null) },
            )

            else -> when (screen) {
                Screen.SPLASH -> SplashScreenV2(
                    onDone = { screen = if (welcomePending) Screen.WELCOME else Screen.HOME },
                )

                Screen.WELCOME -> WelcomeScreenV2(
                    onGetStarted = {
                        welcomePrefs.edit().putBoolean("seen", true).apply()
                        welcomePending = false
                        screen = Screen.HOME
                    },
                )

                Screen.HOME -> HomeScreenV2(
                    onStart = { screen = Screen.SAFETY },
                    onPractice = { screen = Screen.PRACTICE_SIDE },
                    onInstrument = { screen = Screen.INSTRUMENT },
                    onRuns = { screen = Screen.SETTINGS },
                )

                Screen.SETTINGS -> SettingsScreen(
                    onRuns = { screen = Screen.RUNS },
                    onSensors = { screen = Screen.INSTRUMENT },
                    onBack = { screen = Screen.HOME },
                )

                Screen.RUNS -> RunsScreenV2(
                    episodes = episodes,
                    onExport = if (EpisodeLog.treatments(episodes).isEmpty()) {
                        null
                    } else {
                        { if (entitlements.hasExport) shareHistory() else screen = Screen.PAYWALL }
                    },
                    onBack = { screen = Screen.HOME },
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
                    onTakeQuestions = { screen = Screen.SAFETY },
                    onSide = {
                        runController.preparePractice(it)
                        tracker.setMode(MountMode.IN_HAND)
                        calibrationMessage = null
                        screen = Screen.CALIBRATE
                    },
                    onBack = { screen = Screen.HOME },
                )

                Screen.HOLD -> HoldScreen(
                    step = step(3, 1),
                    total = totalSteps,
                    onMode = {
                        tracker.setMode(it)
                        calibrationMessage = null
                        screen = Screen.CALIBRATE
                    },
                    onBack = { screen = Screen.HOME },
                )

                Screen.CALIBRATE -> CalibrateScreen(
                    step = step(4, 2),
                    total = totalSteps,
                    stepLabel = label(4, 2),
                    mode = trackerState.mode,
                    message = calibrationMessage,
                    countdown = setupCountdown,
                    ear = if (run.side == Side.LEFT) 'L' else 'R',
                    onCalibrate = {
                        calibrationMessage = null
                        captureWhenStill(trackerState.mode.spokenSetupPrompt) {
                            when (tracker.calibrate()) {
                                CalibrationResult.OK -> {
                                    calibrationMessage = null
                                    guidance.play(Cue.Speak("Set.", interrupt = true))
                                    screen = Screen.DIRECTION
                                }
                                CalibrationResult.NO_SAMPLES -> fail(
                                    "No sensor reading yet. Wait a second and try again.",
                                ) { calibrationMessage = it }
                                CalibrationResult.PHONE_TOO_FLAT -> fail(
                                    "The phone is lying too flat to tell which way you're facing. " +
                                        "Hold it on its edge, as described, and try again.",
                                ) { calibrationMessage = it }
                            }
                        }
                    },
                    onBack = { screen = if (practice) Screen.PRACTICE_SIDE else Screen.HOLD },
                )

                Screen.DIRECTION -> DirectionScreen(
                    step = step(5, 3),
                    total = totalSteps,
                    stepLabel = label(5, 3),
                    side = run.side,
                    practice = practice,
                    message = run.polarityMessage,
                    countdown = setupCountdown,
                    onLearn = {
                        val word = run.side.word
                        val prompt = if (practice) {
                            "Turn the phone toward your $word and hold it there."
                        } else {
                            "Turn your head toward your $word, about halfway to your shoulder, and hold it."
                        }
                        captureWhenStill(prompt) {
                            runController.learnPolarity(trackerState)
                            val failure = runController.state.value.polarityMessage
                            if (failure != null) {
                                guidance.play(Cue.Speak(failure, interrupt = true))
                            } else {
                                guidance.play(Cue.Speak("Got it.", interrupt = true))
                            }
                        }
                    },
                    onBack = { screen = Screen.CALIBRATE },
                )

                Screen.READY -> ReadyScreen(
                    step = step(6, 4),
                    total = totalSteps,
                    stepLabel = label(6, 4),
                    side = run.side,
                    mode = trackerState.mode,
                    practice = practice,
                    onStart = {
                        runStartedAt = System.currentTimeMillis()
                        heldAngles.clear()
                        startLoggingForRun()
                        runController.start(HeadTracker.STILLNESS_THRESHOLD_DEG_PER_SEC)
                    },
                    onBack = {
                        // Without clearing it, the direction is still learned and the screen would
                        // bounce straight back to Ready, making Back look broken.
                        runController.clearDirection()
                        screen = Screen.DIRECTION
                    },
                )

                Screen.ACCURACY -> AccuracyCheckScreen(
                    devicePitchDegrees = trackerState.devicePitchDegrees,
                    screenFacingUp = trackerState.screenFacingUp,
                    isStill = trackerState.isStill,
                    rotationTravelledDegrees = trackerState.rotationTravelledDegrees,
                    storedOffsetDegrees = tracker.tiltOffsetDegrees,
                    onSaveOffset = { measured ->
                        val updated = health.epley.core.TiltOffset.from(tracker.tiltOffsetDegrees, measured)
                        tracker.tiltOffsetDegrees = updated
                        deviceCalibration.tiltOffsetDegrees = updated
                    },
                    onBack = { screen = Screen.INSTRUMENT },
                )

                Screen.INSTRUMENT -> CheckSensorsScreen(
                    tracker = tracker,
                    onAccuracyCheck = { screen = Screen.ACCURACY },
                    onBack = { screen = Screen.HOME },
                    onToggleLogging = ::toggleLogging,
                    isLogging = logger != null,
                    logDirectory = getExternalFilesDir(null)?.absolutePath ?: "",
                )
            }
        }
    }

    /**
     * Speak a countdown, wait for the person to be still, then capture.
     *
     * Stillness is a requirement rather than a courtesy: a pose captured mid-movement becomes the
     * reference every later angle is measured against, so an error here is silently carried
     * through the whole manoeuvre. If they are still moving when the count ends, nothing is
     * captured and the app says so out loud — on this screen the user cannot see a message.
     */
    private fun fail(message: String, show: (String) -> Unit) {
        show(message)
        // Spoken as well as shown, for the same reason the countdown exists: at this point in the
        // flow the screen is against a cheekbone and cannot be read.
        guidance.play(Cue.Speak(message, interrupt = true))
    }

    private fun captureWhenStill(prompt: String, onCapture: () -> Unit) {
        if (setupCountdown != null) return
        lifecycleScope.launch {
            guidance.play(Cue.Speak(prompt, interrupt = true))
            for (remaining in SETUP_COUNTDOWN_SECONDS downTo 1) {
                setupCountdown = remaining
                if (remaining <= 3) guidance.play(Cue.Speak("$remaining"))
                delay(1000)
            }
            setupCountdown = 0
            val settled = withTimeoutOrNull(STILLNESS_TIMEOUT_MILLIS) {
                tracker.state.first { it.isStill }
            }
            setupCountdown = null
            if (settled == null) {
                guidance.play(Cue.Speak("Still moving. Hold steady and try again.", interrupt = true))
            } else {
                onCapture()
            }
        }
    }

    /**
     * Hand the history to whatever the user picks — mail, messages, printing.
     *
     * A chooser, so sharing only ever happens to a destination they chose. The app itself sends
     * nothing anywhere.
     */
    /** The doctor's PDF: every treatment run, its duration, and each position's hold and angles. */
    private fun shareHistory() {
        val file = HistoryPdf.write(this, episodes)
        val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.share", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Epley Coach: my history")
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(android.content.Intent.createChooser(intent, "Share with your doctor"))
    }

    /** Every run ends in the history, finished or not, so the record is honest about stops too. */
    /**
     * Every run records itself. Not a developer toggle — the evidence is the point.
     *
     * The first on-head run produced twenty-six minutes of a phone lying still and not one
     * measured head angle, because logging was a switch on a screen nobody thinks to visit before
     * lying down, and because it closes when the app goes to the background. A run that leaves no
     * trace cannot be checked afterwards, and an app whose whole claim is measurement should
     * never perform one.
     */
    private fun startLoggingForRun() {
        if (logger != null) return
        val dir = getExternalFilesDir(null) ?: return
        logger = CsvLogger(dir)
    }

    private fun endRun(completed: Boolean, feeling: Feeling?) {
        val run = runController.state.value
        logger?.close()
        logger = null
        episodes = episodeStore.append(
            Episode(
                epochMillis = System.currentTimeMillis(),
                side = run.side,
                completed = completed,
                feeling = feeling,
                practice = run.practice,
                positionsCompleted = run.engineState?.completedSteps ?: 0,
                durationMillis = if (runStartedAt > 0) System.currentTimeMillis() - runStartedAt else 0,
                heldAngles = heldAngles.toList(),
            ),
        )
        runStartedAt = 0L
        heldAngles.clear()
        guidance.silence()
        runController.stop()
        screen = Screen.HOME
    }

    /**
     * Holding either volume button stops a run.
     *
     * Someone face-down with their eyes shut cannot find a STOP button on a screen pressed to
     * their cheek. A physical button they can feel is the one control that works in every
     * position (spec FR-8). Outside a run the buttons change the volume as normal.
     *
     * It has to be a *hold*, not a press. A single press was the original design and it was wrong
     * for an obvious reason nobody noticed until the app was used on a head: the moment a person
     * most wants the volume button is mid-run, when the voice is too quiet to follow. Reaching for
     * the volume then cancelled their treatment. A short press now does what it has always done.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val volumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        val run = runController.state.value
        val midManoeuvre = run.running && run.engineState?.guidance != Guidance.FINISHED
        // Android repeats a held key, so eventTime advances while downTime does not.
        val heldLongEnough = event != null && event.eventTime - event.downTime >= STOP_HOLD_MILLIS
        if (volumeKey && midManoeuvre && heldLongEnough) {
            endRun(completed = false, feeling = null)
            guidance.play(Cue.Speak("Stopped.", interrupt = true))
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()
        tracker.start()
        @Suppress("WakelockTimeout")
        if (wakeLock?.isHeld == false) wakeLock?.acquire(30 * 60 * 1000L)

        // A run that is still going gets its recording back. Found in the data: a four minute run
        // left 2.7 seconds of log, because onPause closes the file and nothing ever reopened it,
        // so a single glance at the notification shade destroyed the evidence for everything that
        // followed. The run itself survives a pause; the record of it has to as well.
        if (runController.state.value.running) startLoggingForRun()
    }

    override fun onDestroy() {
        super.onDestroy()
        guidance.release()
    }

    override fun onPause() {
        super.onPause()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        tracker.stop()
        logger?.close()
        logger = null
    }

    private companion object {
        /**
         * How long a volume button must be held to stop a run.
         *
         * Long enough that changing the volume never stops a treatment, short enough to find with
         * the eyes shut and the head hanging off a bed.
         */
        const val STOP_HOLD_MILLIS = 1200L

        /** Time to get the phone onto a cheekbone after tapping, while the screen is still visible. */
        const val SETUP_COUNTDOWN_SECONDS = 5

        /** How long to wait for the person to settle before giving up and saying so. */
        const val STILLNESS_TIMEOUT_MILLIS = 6000L
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
            "timestamp_nanos,mount_mode,device_tilt_deg,gravity_tilt_deg,accel_tilt_deg,accel_mag," +
                "neck_extension_deg," +
                "pitch_deg,head_rotation_deg,swing_deg,rotation_reliable,rate_deg_per_sec," +
                "is_still,is_jolted,step_id,guidance\n",
        )
    }

    fun append(state: TrackerState, stepId: String? = null, guidance: String? = null) {
        val pose = state.pose
        // Written whether or not a calibration exists: the device and gravity tilts are the
        // instrument's own measurement, and they are exactly what is needed when the screen
        // cannot be read — face-down, or pressed against a cheek.
        writer.write(
            "${state.lastTimestampNanos}," +
                "${state.mode.name}," +
                "%.3f,".format(state.devicePitchDegrees) +
                "%.3f,".format(state.gravityTiltDegrees) +
                "%.3f,".format(state.accelTiltDegrees) +
                "%.3f,".format(state.accelMagnitude) +
                "%.3f,".format(pose?.neckExtensionDegrees ?: Double.NaN) +
                "%.3f,".format(pose?.pitchDegrees ?: Double.NaN) +
                "%.3f,".format(pose?.headRotationDegrees ?: Double.NaN) +
                "%.3f,".format(pose?.swingDegrees ?: Double.NaN) +
                "${pose?.isRotationReliable ?: false}," +
                "%.3f,".format(state.angularRateDegPerSec) +
                "${state.isStill}," +
                "${state.isJolted}," +
                "${stepId ?: ""}," +
                "${guidance ?: ""}\n",
        )
    }

    fun close() {
        runCatching {
            writer.flush()
            writer.close()
        }
    }
}
