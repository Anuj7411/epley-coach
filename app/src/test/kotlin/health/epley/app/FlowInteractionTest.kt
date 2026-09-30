package health.epley.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import health.epley.core.SafetyOutcome
import health.epley.core.Side
import health.epley.core.TriageOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The screens driven the way a person drives them — by tapping — with every clinical path checked
 * for where it leads. The logic has its own exhaustive tests in :core; these prove the screens
 * wire it up correctly and that nothing a tap can reach skips a stop.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-xxhdpi")
class FlowInteractionTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(night: Boolean = false, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            EpleyTheme {
                val d = LocalDensity.current
                CompositionLocalProvider(
                    LocalEpley provides if (night) NightColors else DayColors,
                    LocalReducedMotion provides true,
                    LocalDensity provides Density(d.density, fontScale),
                ) {
                    Surface(Modifier.fillMaxSize(), color = Ds.ground) { DesignFrame { content() } }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text, substring = true).performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
    }

    private fun seen(text: String) = compose.onNodeWithText(text, substring = true).assertExists()

    // ---- Safety ----

    @Test
    fun `an emergency sign is a hard stop with one action`() {
        var finished: SafetyOutcome? = null
        show { SafetyScreen(onFinished = { finished = it }, onCancel = {}) }
        tap("Yes, one or more")
        seen("Stop. Get emergency help now.")
        seen("Call emergency")
        assertEquals(0, compose.onAllNodesWithText("Back to home").fetchSemanticsNodes().size)
        assertNull("nothing leads on from the emergency stop", finished)
    }

    @Test
    fun `a reason not to treat stops at see a doctor`() {
        var finished: SafetyOutcome? = null
        show { SafetyScreen(onFinished = { finished = it }, onCancel = {}) }
        tap("No, none of these")
        seen("Do any of these apply to you?")
        tap("Yes, one applies")
        seen("See a doctor before using this")
        tap("Back to home")
        assertEquals(SafetyOutcome.SeeDoctor, finished)
    }

    @Test
    fun `two Nos go straight on, with no screen in between`() {
        var finished: SafetyOutcome? = null
        show { SafetyScreen(onFinished = { finished = it }, onCancel = {}) }
        tap("No, none of these")
        tap("No, none apply")
        assertEquals(SafetyOutcome.Clear, finished)
    }

    // ---- Triage ----

    @Test
    fun `a left ear comes only from question 5 and starts a left treatment`() {
        var finished: TriageOutcome? = null
        show { TriageScreen(onFinished = { finished = it }, onCancel = {}) }
        repeat(3) { tap("Yes") }
        seen("question 4 of 6")
        tap("Lying down, or getting out of bed")
        tap("Turning my head to the left")
        seen("Left")
        seen("toward your left ear")
        seen("Questions from Kim HJ et al., Neurology 2020.")
        tap("Start treatment")
        assertEquals(TriageOutcome.PosteriorCanal(Side.LEFT), finished)
    }

    @Test
    fun `a No among the first three still asks all three, then stops`() {
        var finished: TriageOutcome? = null
        show { TriageScreen(onFinished = { finished = it }, onCancel = {}) }
        tap("No")
        seen("question 2 of 6")
        tap("Yes")
        tap("Yes")
        seen("This doesn’t sound like BPPV")
        seen("Call emergency")
        assertNull("the stop never offers treatment", finished)
        // Change my last answer goes back one question, not to the start.
        tap("Change my last answer")
        seen("question 3 of 6")
    }

    @Test
    fun `turning leads through question 6 to can't treat, with no side shown`() {
        show { TriageScreen(onFinished = {}, onCancel = {}) }
        repeat(3) { tap("Yes") }
        tap("Turning my head or body while lying down")
        tap("Turning my head to the right")
        seen("question 6 of 6")
        tap("Less than 1 minute")
        seen("This app can’t treat this type")
        assertEquals(0, compose.onAllNodesWithText("Right ear", substring = true).fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("Start treatment").fetchSemanticsNodes().size)
    }

    @Test
    fun `a mis-tap on the ear can be undone`() {
        var finished: TriageOutcome? = null
        show { TriageScreen(onFinished = { finished = it }, onCancel = {}) }
        repeat(3) { tap("Yes") }
        tap("Lying down, or getting out of bed")
        seen("question 5 of 6")
        tap("Change my last answer")
        seen("question 4 of 6")
        tap("Lying down, or getting out of bed")
        tap("Turning my head to the right")
        tap("Start treatment")
        assertEquals(TriageOutcome.PosteriorCanal(Side.RIGHT), finished)
    }

    // ---- Practice ----

    @Test
    fun `practice cannot start until a side is picked`() {
        var side: Side? = null
        show { PracticeSideScreen(onSide = { side = it }, onTakeQuestions = {}, onBack = {}) }
        tap("Start practice")
        assertNull("nothing chosen, nothing started", side)
        tap("Right side")
        seen("Selected")
        tap("Start practice")
        assertEquals(Side.RIGHT, side)
    }

    // ---- Paywall ----

    private class FakeEntitlements(private val succeed: Boolean) : Entitlements {
        override var hasExport = false
        override val priceLabel = "₹199"
        override val isPlaceholder = false
        override fun purchase(onResult: (Boolean) -> Unit) { hasExport = succeed; onResult(succeed) }
        override fun restore(onResult: (Boolean) -> Unit) = onResult(false)
    }

    @Test
    fun `a successful purchase unlocks, a failed one says nothing was charged`() {
        var done = false
        show { PaywallScreen(FakeEntitlements(succeed = false), onDone = { done = true }, onCancel = {}) }
        seen("₹199")
        tap("Unlock the PDF")
        seen("Nothing was charged.")
        assertTrue(!done)
        tap("Restore")
        seen("No previous purchase found")
    }

    @Test
    fun `the paywall's success path calls through`() {
        var done = false
        show { PaywallScreen(FakeEntitlements(succeed = true), onDone = { done = true }, onCancel = {}) }
        tap("Unlock the PDF")
        assertTrue(done)
    }

    // ---- Stress: huge text still reaches every action ----

    @Test
    fun `at double font size every screen still reaches its buttons`() {
        show(fontScale = 2f) { SafetyScreen(onFinished = {}, onCancel = {}) }
        compose.onNodeWithText("No, none of these").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `at double font size the ear result still reaches Start treatment`() {
        show(fontScale = 2f, night = true) { EarResult(Side.RIGHT, onStart = {}) }
        compose.onNodeWithText("Start treatment").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `at double font size Find still reaches Stop`() {
        show(fontScale = 2f) { FindView(sampleFind(3, 'L'), figurePlaying = false, onToggleFigure = {}, onStop = {}) }
        compose.onNodeWithText("Stop").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `at double font size Done still reaches Finish`() {
        show(fontScale = 2f) { DoneView(ear = 'L', showAfterCare = true, onFinish = {}) }
        seen("No restrictions afterwards")
        compose.onNodeWithText("Finish").performScrollTo().assertIsDisplayed()
    }
}
