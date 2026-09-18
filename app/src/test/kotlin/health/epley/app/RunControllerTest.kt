package health.epley.app

import health.epley.core.Epley
import health.epley.core.Guidance
import health.epley.core.HeadPose
import health.epley.core.MountMode
import health.epley.core.RotationPolarity
import health.epley.core.HorizontalType
import health.epley.core.Side
import health.epley.core.TriageOutcome
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the bridge between the sensor stream and the engine.
 *
 * Everything here is reachable on a device only by someone physically holding a phone against
 * their head and lying down, which makes it the part of the app least likely to get retested by
 * hand and most likely to break silently.
 */
class RunControllerTest {

    private val tolerance = MountMode.CHEEK.toleranceDegrees
    private val stillness = HeadTracker.STILLNESS_THRESHOLD_DEG_PER_SEC

    private fun controllerReadyToRun(): RunController = RunController().apply {
        confirmTriage(TriageOutcome.PosteriorCanal(Side.LEFT))
        learnPolarity(sample(HeadPose(0.0, 45.0), nanos = 0))
        start(tolerance, stillness)
    }

    private fun sample(
        pose: HeadPose,
        nanos: Long,
        rate: Double = 0.0,
    ) = TrackerState(
        pose = pose,
        mode = MountMode.CHEEK,
        angularRateDegPerSec = rate,
        isCalibrated = true,
        lastTimestampNanos = nanos,
    )

    /** Feed steady samples at 50 Hz starting from [fromNanos]; returns the next free timestamp. */
    private fun RunController.feed(
        pose: HeadPose,
        seconds: Double,
        fromNanos: Long,
        rate: Double = 0.0,
    ): Long {
        val stepNanos = 20_000_000L
        var t = fromNanos
        repeat((seconds / 0.02).toInt()) {
            t += stepNanos
            onTrackerState(sample(pose, t, rate))
        }
        return t
    }

    @Test
    fun `a run cannot start without a posterior canal triage`() {
        // The Epley treats the posterior canal only. Anything else the questionnaire concludes
        // must leave the start button unreachable, however the direction step went.
        for (outcome in listOf(
            null,
            TriageOutcome.HorizontalCanal(Side.LEFT, HorizontalType.CANALITHIASIS),
            TriageOutcome.NotConsistentWithBppv(listOf(3)),
            TriageOutcome.Incomplete(4),
        )) {
            val c = RunController()
            if (outcome != null) c.confirmTriage(outcome)
            c.learnPolarity(sample(HeadPose(0.0, 45.0), nanos = 0))
            assertFalse(c.state.value.canStart, "could start after $outcome")
            c.start(tolerance, stillness)
            assertFalse(c.state.value.running)
        }
    }

    @Test
    fun `the side comes from the triage, not from a guess`() {
        val c = RunController()
        c.confirmTriage(TriageOutcome.PosteriorCanal(Side.RIGHT))
        assertEquals(Side.RIGHT, c.state.value.side)
    }

    @Test
    fun `a run cannot start before the direction is learned`() {
        val c = RunController()
        c.confirmTriage(TriageOutcome.PosteriorCanal(Side.LEFT))
        assertFalse(c.state.value.canStart)
        c.start(tolerance, stillness)
        assertFalse(c.state.value.running)
    }

    @Test
    fun `too small a turn is refused with an explanation rather than guessed`() {
        val c = RunController()
        c.learnPolarity(sample(HeadPose(0.0, 5.0), nanos = 0))
        assertNull(c.state.value.polarity)
        assertNotNull(c.state.value.polarityMessage)
        assertFalse(c.state.value.canStart)
    }

    @Test
    fun `a clear turn teaches the direction and clears the complaint`() {
        val c = RunController()
        c.confirmTriage(TriageOutcome.PosteriorCanal(Side.LEFT))
        c.learnPolarity(sample(HeadPose(0.0, -40.0), nanos = 0))
        assertNotNull(c.state.value.polarity)
        assertNull(c.state.value.polarityMessage)
        assertTrue(c.state.value.canStart)
        // Turning that way was toward the affected side, so the targets mirror.
        assertFalse(c.state.value.polarity!!.towardAffectedSideIsPositive)
    }

    @Test
    fun `a new triage discards the learned direction`() {
        // The turn was learned for the old side. Carrying it over would mirror the manoeuvre.
        val c = RunController()
        c.confirmTriage(TriageOutcome.PosteriorCanal(Side.LEFT))
        c.learnPolarity(sample(HeadPose(0.0, 40.0), nanos = 0))
        assertNotNull(c.state.value.polarity)
        c.confirmTriage(TriageOutcome.PosteriorCanal(Side.RIGHT))
        assertNull(c.state.value.polarity)
        assertFalse(c.state.value.canStart)
    }

    @Test
    fun `no pose means nothing to learn from`() {
        val c = RunController()
        c.onTrackerState(TrackerState(pose = null, lastTimestampNanos = 1))
        c.learnPolarity(TrackerState(pose = null))
        assertNull(c.state.value.polarity)
        assertNotNull(c.state.value.polarityMessage)
    }

    @Test
    fun `the first sample sets the clock without crediting any hold`() {
        val c = controllerReadyToRun()
        c.onTrackerState(sample(HeadPose(0.0, 45.0), nanos = 5_000_000_000))
        // Nothing to measure elapsed time against yet, so no time may be counted.
        assertNull(c.state.value.engineState)
    }

    @Test
    fun `holding the first position drives the timer`() {
        val c = controllerReadyToRun()
        c.feed(HeadPose(0.0, 45.0), seconds = 2.0, fromNanos = 0)
        val engineState = c.state.value.engineState!!
        assertEquals(Guidance.HOLDING, engineState.guidance)
        assertEquals(2.0, engineState.heldSeconds, 0.05)
    }

    @Test
    fun `being out of position reports a correction instead of a countdown`() {
        val c = controllerReadyToRun()
        c.feed(HeadPose(0.0, 0.0), seconds = 0.5, fromNanos = 0)
        val engineState = c.state.value.engineState!!
        assertEquals(Guidance.SEEKING, engineState.guidance)
        assertEquals(45.0, engineState.correction!!.headRotationDegrees, 1e-6)
        assertEquals(0.0, engineState.heldSeconds, 1e-9)
    }

    @Test
    fun `a completed position advances on its own after the pause`() {
        val c = controllerReadyToRun()
        // Step one asks for three seconds; the pause before advancing is two more.
        c.feed(HeadPose(0.0, 45.0), seconds = 3.5, fromNanos = 0)
        assertEquals("prepare", c.state.value.engineState!!.step?.id)
        c.feed(HeadPose(0.0, 45.0), seconds = 2.5, fromNanos = 3_500_000_000)
        assertEquals("lie-back", c.state.value.engineState!!.step?.id)
    }

    @Test
    fun `time spent backgrounded is not credited as a hold`() {
        val c = controllerReadyToRun()
        c.onTrackerState(sample(HeadPose(0.0, 45.0), nanos = 1_000_000_000))
        // The screen went off for a minute. Counting the gap would hand over a finished hold for
        // time the phone spent asleep.
        c.onTrackerState(sample(HeadPose(0.0, 45.0), nanos = 61_000_000_000))
        assertNull(c.state.value.engineState)

        // The stream resumes normally afterwards rather than staying wedged.
        c.feed(HeadPose(0.0, 45.0), seconds = 1.0, fromNanos = 61_000_000_000)
        assertEquals(1.0, c.state.value.engineState!!.heldSeconds, 0.05)
    }

    @Test
    fun `a non-advancing timestamp is ignored`() {
        val c = controllerReadyToRun()
        c.onTrackerState(sample(HeadPose(0.0, 45.0), nanos = 1_000_000_000))
        c.onTrackerState(sample(HeadPose(0.0, 45.0), nanos = 1_000_000_000))
        assertNull(c.state.value.engineState)
    }

    @Test
    fun `stopping mid-run ends it and drops the engine`() {
        val c = controllerReadyToRun()
        c.feed(HeadPose(0.0, 45.0), seconds = 1.0, fromNanos = 0)
        c.stop()
        assertFalse(c.state.value.running)
        assertNull(c.state.value.engineState)
        // Samples arriving after a stop must not resurrect the run.
        c.feed(HeadPose(0.0, 45.0), seconds = 1.0, fromNanos = 5_000_000_000)
        assertNull(c.state.value.engineState)
    }

    @Test
    fun `a whole run can be driven from the sensor stream`() {
        val c = controllerReadyToRun()
        val polarity = c.state.value.polarity!!
        var t = 0L
        for (step in Epley.steps()) {
            val rotation = polarity.toSensor(step.target.headRotationDegrees)
            val pose = HeadPose(step.target.neckExtensionDegrees, rotation)
            // Hold long enough to finish the step and clear the pause before the next one.
            t = c.feed(pose, seconds = step.holdSeconds + 3.0, fromNanos = t)
        }
        assertEquals(Guidance.FINISHED, c.state.value.engineState!!.guidance)
    }

    @Test
    fun `the polarity learned is the one the run actually uses`() {
        val c = RunController()
        c.confirmTriage(TriageOutcome.PosteriorCanal(Side.RIGHT))
        // The user turned right and the sensor read negative, so targets must be mirrored.
        c.learnPolarity(sample(HeadPose(0.0, -38.0), nanos = 0))
        c.start(tolerance, stillness)

        // The unmirrored pose is wrong ...
        c.feed(HeadPose(0.0, 45.0), seconds = 1.0, fromNanos = 0)
        assertEquals(Guidance.SEEKING, c.state.value.engineState!!.guidance)

        // ... and the mirrored one is right.
        c.feed(HeadPose(0.0, -45.0), seconds = 1.0, fromNanos = 2_000_000_000)
        assertEquals(Guidance.HOLDING, c.state.value.engineState!!.guidance)
    }

    @Test
    fun `a sweep through the target does not earn hold time`() {
        val c = controllerReadyToRun()
        // Dead on the target the whole way, but moving at 40 deg/s.
        c.feed(HeadPose(0.0, 45.0), seconds = 2.0, fromNanos = 0, rate = 40.0)
        val engineState = c.state.value.engineState!!
        assertEquals(Guidance.SETTLING, engineState.guidance)
        assertEquals(0.0, engineState.heldSeconds, 1e-9)
    }
}
