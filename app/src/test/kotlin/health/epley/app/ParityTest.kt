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
                RuntimeEnvironment.setQualifiers(DEVICES.getValue(device))
            }
        })
        .around(compose)

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
            "splash", "welcome", "home", "safety", "emergency", "q1", "q2", "q3", "q4", "q5", "q6",
            "result-right", "result-left", "horizontal", "notbppv",
            "find-p1", "find-p2", "find-p3", "find-p4", "find-p5", "find-p4-left",
            "hold-p1", "hold-p2", "hold-p3", "hold-p4", "hold-p5",
            "done-right", "done-left", "history", "practice",
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
                        SCREENS.filter { only.isEmpty() || it in only }.map { arrayOf<Any>(d, t, it) }
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
        else -> Unit
    }
}
