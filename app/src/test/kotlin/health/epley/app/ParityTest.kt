package health.epley.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

/**
 * README ★P3: one screenshot per screen state × theme × device, compared against the design's own
 * references by `design_handoff_epley_coach_v2/tools/compare.mjs`.
 *
 * Output: app/build/outputs/roborazzi/<device>/<theme>/<id>.png — the names compare.mjs expects.
 * Animations resolve to their end state (reduced motion), and the figure shows its held frame.
 *
 *   ./gradlew :app:testDebugUnitTest --tests "*ParityTest*" -Dparity.device=phone-432x960   (or =all)
 *   node design_handoff_epley_coach_v2/tools/compare.mjs --device phone-432x960 \
 *       --actual app/build/outputs/roborazzi/phone-432x960
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ParityTest(private val device: String, private val theme: String, private val id: String) {

    private val compose = createComposeRule()

    // The device has to be set before the compose rule starts its activity.
    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(object : TestWatcher() {
            override fun starting(description: Description) {
                RuntimeEnvironment.setQualifiers(qualifiers())
            }
        })
        .around(compose)

    /**
     * Scrolling screens (§16: Settings, Check sensors, Accuracy, Paywall) are compared full length:
     * the window is made as tall as the design's own full-page render, read from its PNG header.
     */
    private fun qualifiers(): String {
        val base = DEVICES.getValue(device)
        val ref = java.io.File("../design_handoff_epley_coach_v2/reference/$device/$theme/$id.png")
        if (!ref.exists()) return base
        val dpi = Regex("""(\d+)dpi""").find(base)!!.groupValues[1].toInt()
        val heightPx = ref.inputStream().use { input ->
            val header = ByteArray(24).also { input.read(it) }
            java.nio.ByteBuffer.wrap(header, 20, 4).int
        }
        val heightDp = Math.round(heightPx / (dpi / 160f))
        val deviceDp = Regex("""h(\d+)dp""").find(base)!!.groupValues[1].toInt()
        return if (heightDp > deviceDp) base.replace("h${deviceDp}dp", "h${heightDp}dp") else base
    }

    @Test
    fun capture() {
        val night = theme == "night"
        compose.setContent {
            EpleyTheme {
                CompositionLocalProvider(
                    LocalEpley provides if (night) NightColors else DayColors,
                    LocalReducedMotion provides true,
                ) {
                    Surface(Modifier.fillMaxSize(), color = Ds.ground) {
                        DesignFrame { ParityScreen(id, night) }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(3_000)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$device/$theme/$id.png")
    }

    companion object {
        /** tools/screens.json devices, as Android resource qualifiers (density as dpi). */
        val DEVICES = linkedMapOf(
            "phone-432x960" to "w432dp-h960dp-400dpi",
            "pixel-412x915" to "w412dp-h915dp-420dpi",
            "base-390x844" to "w390dp-h844dp-480dpi",
            "small-360x800" to "w360dp-h800dp-480dpi",
            "large-480x1040" to "w480dp-h1040dp-480dpi",
        )

        val SCREENS = listOf(
            "splash",
            "welcome",
            "home",
            "safety",
            "emergency",
            "q1",
            "q2",
            "q3",
            "q4",
            "q5",
            "q6",
            "result-right",
            "result-left",
            "horizontal",
            "notbppv",
            "find-p1",
            "find-p2",
            "find-p3",
            "find-p4",
            "find-p5",
            "find-p4-left",
            "hold-p1",
            "hold-p2",
            "hold-p3",
            "hold-p4",
            "hold-p5",
            "done-right",
            "done-left",
            "history",
            "practice",
            "setup-mount",
            "calibrate",
            "calibrate-count",
            "calibrate-capture",
            "calibrate-failnone",
            "calibrate-failflat",
            "calibrate-band-left",
            "direction",
            "direction-count",
            "ready",
            "ready-band-left",
            "practice-picker-none",
            "practice-picker-right",
            "practice-calibrate",
            "practice-direction",
            "practice-ready",
            "practice-find-p2",
            "practice-hold-p2",
            "practice-done",
            "safety2",
            "seedoctor",
            "settings",
            "sensors-uncal",
            "sensors",
            "sensors-turnwarn",
            "sensors-moved",
            "accuracy",
            "accuracy-after1",
            "accuracy-turnedover",
            "accuracy-notturned",
            "accuracy-pass",
            "accuracy-moving",
            "accuracy-fail",
            "paywall",
            "paywall-test",
            "paywall-errcharge",
            "paywall-errrestore",
            "sensorfail",
            "history-empty",
            "emergency-callfail",
            "hold-paused",
            "find-moved",
        )

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} {1} {2}")
        fun params(): List<Array<Any>> {
            // Opt-in: 300 screenshots would slow every ordinary test run. -Dparity.device=all runs
            // every device; a device name runs just that one.
            val onlyDevice = System.getProperty("parity.device").orEmpty()
            if (onlyDevice.isEmpty()) return emptyList()
            val only = System.getProperty("parity.only").orEmpty().split(',').filter { it.isNotBlank() }
            return DEVICES.keys
                .filter { onlyDevice == "all" || it == onlyDevice }
                .flatMap { d ->
                    listOf("day", "night").flatMap { t ->
                        // Named states outside the design's list (previews of app-only additions) run too.
                        (if (only.isEmpty()) SCREENS else only).map { arrayOf<Any>(d, t, it) }
                    }
                }
        }
    }
}

/** Each screen state from tools/screens.json, with the design's sample values. */
@Composable
fun ParityScreen(id: String, night: Boolean) {
    when (id) {
        "splash" -> SplashScreenV2(onDone = {})
        "welcome" -> WelcomeScreenV2(onGetStarted = {}, figurePlaying = false)
        "home" -> HomeScreenV2(
            onStart = {}, onPractice = {}, onInstrument = {}, onRuns = {},
            date = if (night) "Wednesday 1 October" else "Tuesday 30 September",
        )
        "safety" -> SafetyScreen(onFinished = {}, onCancel = {})
        "emergency" -> EmergencyStop(onCall = {})
        "q1", "q2", "q3", "q4", "q5", "q6" -> {
            val n = id.drop(1).toInt()
            TriageQuestion(n, onUndo = if (n > 1) ({}) else null, onAnswer = {})
        }
        "result-right" -> EarResult(health.epley.core.Side.RIGHT, onStart = {})
        "result-left" -> EarResult(health.epley.core.Side.LEFT, onStart = {})
        "horizontal" -> HorizontalResult(onUndo = {}, onHome = {})
        "notbppv" -> NotBppvResult(onUndo = {}, onCall = {}, onHome = {})
        "find-p1", "find-p2", "find-p3", "find-p4", "find-p5" ->
            FindView(sampleFind(id.last().digitToInt(), 'R'), figurePlaying = true, onToggleFigure = {}, onStop = {})
        "find-p4-left" -> FindView(sampleFind(4, 'L'), figurePlaying = true, onToggleFigure = {}, onStop = {})
        "hold-p1", "hold-p2", "hold-p3", "hold-p4", "hold-p5" -> HoldView(sampleHold(id.last().digitToInt(), 'R'), onStop = {})
        "done-right" -> DoneView(ear = 'R', showAfterCare = true, onFinish = {})
        "done-left" -> DoneView(ear = 'L', showAfterCare = true, onFinish = {})
        "history" -> RunsView(
            // The design dates its sample runs from the moment it was rendered (§16 J); these
            // match the reference render.
            listOf(
                RunGroup("This week", listOf(
                    RunRow(true, "Tonight, 05:03", "Right ear · all 5 held", "6 min"),
                    RunRow(true, "Tue 22 Sep", "Right ear · all 5 held", "6 min"),
                    RunRow(false, "Mon 21 Sep", "Right ear · stopped at 2", "1 min"),
                )),
                RunGroup("July", listOf(RunRow(true, "Sat 4 Jul", "Left ear · all 5 held", "7 min"))),
            ),
            onShare = {}, onBack = {},
        )
        "practice" -> PracticeSideView(health.epley.core.Side.RIGHT, onChoose = {}, onTakeQuestions = {}, onStart = {}, onBack = {})
        "setup-mount" -> HoldScreen(step = 3, total = 6, onMode = {}, onBack = {})
        "calibrate" -> CalibrateView("Step 4 of 6", 4, 6, practice = false, band = false, ear = 'R', stage = CaptureStage.Idle, onStart = {}, onBack = {})
        "calibrate-count" -> CalibrateView("Step 4 of 6", 4, 6, practice = false, band = false, ear = 'R', stage = CaptureStage.Count(2), onStart = {}, onBack = {})
        "calibrate-capture" -> CalibrateView("Step 4 of 6", 4, 6, practice = false, band = false, ear = 'R', stage = CaptureStage.Capture, onStart = {}, onBack = {})
        "calibrate-failnone" -> CalibrateView("Step 4 of 6", 4, 6, practice = false, band = false, ear = 'R',
            stage = CaptureStage.Fail("No sensor reading yet. Wait a second and try again."), onStart = {}, onBack = {})
        "calibrate-failflat" -> CalibrateView("Step 4 of 6", 4, 6, practice = false, band = false, ear = 'R',
            stage = CaptureStage.Fail("The phone is lying too flat to tell which way you’re facing. Hold it on its edge, as described, and try again."), onStart = {}, onBack = {})
        "calibrate-band-left" -> CalibrateView("Step 4 of 6", 4, 6, practice = false, band = true, ear = 'L', stage = CaptureStage.Idle, onStart = {}, onBack = {})
        "direction" -> DirectionView("Step 5 of 6", 5, 6, practice = false, ear = 'R', stage = CaptureStage.Idle, onStart = {}, onBack = {})
        "direction-count" -> DirectionView("Step 5 of 6", 5, 6, practice = false, ear = 'R', stage = CaptureStage.Count(1), onStart = {}, onBack = {})
        "ready" -> ReadyView("Step 6 of 6", 6, 6, practice = false, band = false, ear = 'R', onStart = {}, onBack = {})
        "ready-band-left" -> ReadyView("Step 6 of 6", 6, 6, practice = false, band = true, ear = 'L', onStart = {}, onBack = {})
        "practice-calibrate" -> CalibrateView("Practice · step 2 of 4", 2, 4, practice = true, band = false, ear = 'R', stage = CaptureStage.Idle, onStart = {}, onBack = {})
        "practice-direction" -> DirectionView("Practice · step 3 of 4", 3, 4, practice = true, ear = 'R', stage = CaptureStage.Idle, onStart = {}, onBack = {})
        "practice-ready" -> ReadyView("Practice · step 4 of 4", 4, 4, practice = true, band = false, ear = 'R', onStart = {}, onBack = {})
        "practice-picker-none" -> PracticeSideView(null, onChoose = {}, onTakeQuestions = {}, onStart = {}, onBack = {})
        "practice-picker-right" -> PracticeSideView(health.epley.core.Side.RIGHT, onChoose = {}, onTakeQuestions = {}, onStart = {}, onBack = {})
        "practice-find-p2" -> FindView(sampleFind(2, 'R').copy(practice = true), figurePlaying = true, onToggleFigure = {}, onStop = {})
        "practice-hold-p2" -> HoldView(sampleHold(2, 'R').copy(practice = true), onStop = {})
        "practice-done" -> DoneView(ear = 'R', showAfterCare = false, onFinish = {})
        // App-only: the second angle under the main meter (requested after v2.1; not in the design).
        "find-p2-second" -> FindView(
            sampleFind(2, 'R').copy(
                second = AxisModel("Turn", "now 38°", "26–64°", 0.36f, 0.29f, 0.47f, inRange = true),
            ),
            figurePlaying = false, onToggleFigure = {}, onStop = {},
        )
        "find-p4-second" -> FindView(
            sampleFind(4, 'R').copy(
                second = AxisModel("Tip", "now −2°", "−8° to −46°", 0.38f, 0.24f, 0.62f, inRange = false),
            ),
            figurePlaying = false, onToggleFigure = {}, onStop = {},
        )
        "find-moved" -> FindView(sampleFind(4, 'R').copy(moved = true), figurePlaying = true, onToggleFigure = {}, onStop = {})
        "hold-paused" -> HoldView(sampleHold(4, 'R').copy(paused = true), onStop = {})
        "safety2" -> SafetyQuestion(
            caption = "One more", question = "Do any of these apply to you?",
            items = health.epley.core.Safety.reasonsNotToTreat, yes = "Yes, one applies", no = "No, none apply",
            onYes = {}, onNo = {}, tall = true,
        )
        "seedoctor" -> SeeDoctorStop(onHome = {})
        "emergency-callfail" -> EmergencyStop(onCall = {}, callFailed = true)
        "settings" -> SettingsScreen(onRuns = {}, onSensors = {}, onBack = {})
        "sensors-uncal", "sensors", "sensors-turnwarn", "sensors-moved" -> {
            val v = id.removePrefix("sensors").removePrefix("-")
            val calibrated = v != "uncal"
            SensorsView(
                SensorsModel(
                    rate = if (v == "moved") "84 °/s" else "0.6 °/s",
                    moving = v == "moved",
                    mode = health.epley.core.MountMode.CHEEK,
                    tip = if (calibrated) "−3°" else "--",
                    turn = if (calibrated) "12°" else "--",
                    turnWarning = if (v == "turnwarn") "Swung 38° from where it was calibrated." else null,
                    moved = v == "moved",
                    calibrated = calibrated,
                    drift = if (calibrated) "Drift: last 1.2°, worst 2.0° over 3 checks. Tolerance ±7°." else null,
                    logging = false,
                    raw = "q  0.012  −0.694  0.031  0.719\npitch −3.1   roll 88.4   yaw 131.0\nrate 4.2 °/s   acc 3   t 21:51:07.412",
                ),
            )
        }
        "accuracy", "accuracy-after1", "accuracy-turnedover", "accuracy-notturned", "accuracy-pass", "accuracy-moving", "accuracy-fail" -> {
            val v = id.removePrefix("accuracy").removePrefix("-")
            val done = v in setOf("pass", "fail", "moving")
            AccuracyView(
                AccuracyModel(
                    live = "+0.42°",
                    moving = v == "moving" || v == "fail",
                    reading1 = if (v.isEmpty()) "—" else "+0.42°",
                    reading2 = if (!done) "—" else if (v == "fail") "−5.88°" else "−0.42°",
                    stage = when (v) {
                        "after1" -> AccuracyStage.After1
                        "turnedover" -> AccuracyStage.TurnedOver
                        "notturned" -> AccuracyStage.NotTurned
                        "pass", "moving" -> AccuracyStage.Passed
                        "fail" -> AccuracyStage.Failed
                        else -> AccuracyStage.Start
                    },
                    sensorError = if (v == "fail") "6.3°" else "0.84°",
                    surfaceTilt = "1.1°",
                    correctBy = "−0.84°",
                    correctEnabled = v == "pass",
                ),
            )
        }
        "paywall", "paywall-test", "paywall-errcharge", "paywall-errrestore" -> PaywallView(
            price = "₹299",
            simulated = id == "paywall-test",
            error = when (id) {
                "paywall-errcharge" -> PaywallError.Charge
                "paywall-errrestore" -> PaywallError.Restore
                else -> null
            },
        )
        "sensorfail" -> SensorUnavailableScreen()
        "history-empty" -> RunsView(emptyList(), onShare = {}, onBack = {})
        else -> Unit
    }
}
