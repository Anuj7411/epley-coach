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
enum class Screen { SPLASH, WELCOME, HOME, RUNS, SAFETY, TRIAGE, PRACTICE_SIDE, HOLD, CALIBRATE, DIRECTION, READY, PAYWALL, INSTRUMENT, ACCURACY }

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
        entitlements = if (BuildConfig.REVENUECAT_API_KEY.isNotBlank()) {
            RevenueCatEntitlements(this, BuildConfig.REVENUECAT_API_KEY)
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
            // FR-1. Some budget phones ship without a gyroscope, and every angle in this app comes
            // from one. Saying so here is the only honest option; the flow behind it would measure
            // nothing and claim a completed manoeuvre.
            FlowFrame(stepLabel = null, progress = null, onBack = null, bottom = {}) {
                Title("This phone can't measure head angles", color = Palette.Move)
                Body(
                    "It has no orientation sensor, which is what the whole app depends on. " +
                        "Nothing here would be measured, so it won't pretend to guide you.",
                )
                Body("Most phones from the last decade do have one.", secondary = true)
            }
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
                    onRuns = { screen = Screen.RUNS },
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

                Screen.INSTRUMENT -> ProbeScreen(
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

@Composable
private fun ProbeScreen(
    tracker: HeadTracker,
    onAccuracyCheck: () -> Unit,
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

        SecondaryButton("Accuracy self-check", onAccuracyCheck)

        // Raw diagnostic: the orientation as the sensor gives it, and the phone's own axes in
        // world coordinates. Where a wrong angle gets traced back to its cause.
        state.quaternion?.let { q ->
            val y = q.rotate(health.epley.core.Vector3(0.0, 1.0, 0.0)).normalized()
            val z = q.rotate(health.epley.core.Vector3(0.0, 0.0, 1.0)).normalized()
            Text(
                "q  %+.3f %+.3f %+.3f %+.3f".format(q.x, q.y, q.z, q.w) +
                    "  Yw %+.3f %+.3f %+.3f".format(y.x, y.y, y.z) +
                    "  Zw %+.3f %+.3f %+.3f".format(z.x, z.y, z.z) +
                    "  fused %+.2f  gravity %+.2f  accel %+.2f  |a| %.3f".format(
                        state.devicePitchDegrees, state.gravityTiltDegrees,
                        state.accelTiltDegrees, state.accelMagnitude,
                    ),
                color = Color(0xFF9E9E9E),
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
            )
        }

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
