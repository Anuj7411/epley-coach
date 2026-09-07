package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RotationPolarityTest {

    @Test
    fun `a turn one way makes that way positive`() {
        val learned = RotationPolarity.learnFrom(HeadPose(0.0, 42.0))
        assertNotNull(learned)
        assertTrue(learned.towardAffectedSideIsPositive)
        assertEquals(45.0, learned.toSensor(45.0), 1e-9)
    }

    @Test
    fun `a turn the other way mirrors every target`() {
        val learned = RotationPolarity.learnFrom(HeadPose(0.0, -42.0))
        assertNotNull(learned)
        assertFalse(learned.towardAffectedSideIsPositive)
        assertEquals(-45.0, learned.toSensor(45.0), 1e-9)
        assertEquals(135.0, learned.toSensor(-135.0), 1e-9)
    }

    @Test
    fun `a head that barely moved teaches nothing`() {
        // Noise at rest measured under a tenth of a degree, so this is not about noise — it is
        // about refusing to fix a convention for the whole session off an ambiguous gesture.
        assertNull(RotationPolarity.learnFrom(HeadPose(0.0, 3.0)))
        assertNull(RotationPolarity.learnFrom(HeadPose(0.0, -19.9)))
        assertNotNull(RotationPolarity.learnFrom(HeadPose(0.0, -20.1)))
    }

    @Test
    fun `extension is never mirrored`() {
        // Lying back is lying back whichever ear is affected. Only the turn has a handedness.
        val left = RotationPolarity(true)
        val right = RotationPolarity(false)
        for (p in listOf(left, right)) {
            val engine = engineWith(p)
            assertEquals(25.0, engine.sensorTarget(TargetPose(25.0, 45.0)).neckExtensionDegrees, 1e-9)
        }
    }

    private fun engineWith(polarity: RotationPolarity) = ManeuverEngine(
        steps = Epley.steps(),
        polarity = polarity,
        toleranceDegrees = 7.0,
        stillnessThresholdDegPerSec = 5.0,
    )
}

class EpleyStepsTest {

    @Test
    fun `the manoeuvre has the five positions it is described with`() {
        val steps = Epley.steps()
        assertEquals(5, steps.size)
        assertEquals(
            listOf("prepare", "lie-back", "turn-across", "roll", "sit-up"),
            steps.map { it.id },
        )
    }

    @Test
    fun `the three therapeutic holds are long enough to be therapeutic`() {
        val holds = Epley.steps().filter { it.id in setOf("lie-back", "turn-across", "roll") }
        assertEquals(3, holds.size)
        for (step in holds) {
            assertTrue(step.holdSeconds >= 30, "${step.id} holds only ${step.holdSeconds}s")
        }
    }

    @Test
    fun `the head stays extended through the middle of the manoeuvre`() {
        // Letting the head come up between positions is the classic self-treatment error: it
        // lets the crystals fall back. The targets have to keep it down.
        val byId = Epley.steps().associateBy { it.id }
        assertTrue(byId.getValue("lie-back").target.neckExtensionDegrees >= 20.0)
        assertTrue(byId.getValue("turn-across").target.neckExtensionDegrees >= 20.0)
        assertTrue(byId.getValue("roll").target.neckExtensionDegrees >= 20.0)
    }

    @Test
    fun `each turn is a ninety degree change from the one before`() {
        val byId = Epley.steps().associateBy { it.id }
        val prepare = byId.getValue("prepare").target.headRotationDegrees
        val across = byId.getValue("turn-across").target.headRotationDegrees
        val roll = byId.getValue("roll").target.headRotationDegrees
        assertEquals(90.0, prepare - across, 1e-9)
        assertEquals(90.0, across - roll, 1e-9)
    }

    @Test
    fun `the last step returns to the calibration pose`() {
        // Which is what lets it double as the drift check: the true answer here is zero.
        val last = Epley.steps().last().target
        assertEquals(0.0, last.neckExtensionDegrees, 1e-9)
        assertEquals(0.0, last.headRotationDegrees, 1e-9)
    }

    @Test
    fun `no target asks for a pose the sensor cannot report reliably`() {
        // Every target must sit inside the region where the twist decomposition still means
        // something, or the engine would be asking for a position it then refuses to credit.
        for (step in Epley.steps()) {
            val worst = maxOf(
                kotlin.math.abs(step.target.neckExtensionDegrees),
                kotlin.math.abs(step.target.headRotationDegrees),
            )
            assertTrue(
                worst < MAX_RELIABLE_SWING_DEGREES,
                "${step.id} asks for $worst degrees, past the reliable limit",
            )
        }
    }
}

class ManeuverEngineTest {

    private val tolerance = 7.0
    private val stillness = 5.0

    private fun engine(polarity: Boolean = true) = ManeuverEngine(
        steps = Epley.steps(),
        polarity = RotationPolarity(polarity),
        toleranceDegrees = tolerance,
        stillnessThresholdDegPerSec = stillness,
    )

    private fun pose(neck: Double, rotation: Double, swing: Double = 30.0) =
        HeadPose(neck, rotation, swing)

    /** Feed the same pose for a number of seconds at 50 Hz. */
    private fun ManeuverEngine.feed(
        pose: HeadPose,
        seconds: Double,
        rate: Double,
    ): EngineState {
        var last: EngineState? = null
        val dt = 0.02
        repeat((seconds / dt).toInt()) { last = onSample(pose, rate, dt) }
        return last!!
    }

    @Test
    fun `an out of position head is told which way to move`() {
        val e = engine()
        // Step one wants 0 extension and 45 rotation. The head is at 0 and 10.
        val state = e.onSample(pose(0.0, 10.0), 0.0, 0.02)
        assertEquals(Guidance.SEEKING, state.guidance)
        val correction = state.correction!!
        assertEquals(35.0, correction.headRotationDegrees, 1e-9)
        assertEquals(0.0, correction.neckExtensionDegrees, 1e-9)
        assertFalse(correction.worstAxisIsExtension)
    }

    @Test
    fun `sweeping through the target does not start the hold`() {
        // The failure this whole class exists to prevent. The head is exactly on target, but
        // moving at 60 deg/s, which is a head passing through rather than a head holding.
        val e = engine()
        val state = e.onSample(pose(0.0, 45.0), 60.0, 0.02)
        assertEquals(Guidance.SETTLING, state.guidance)
        assertEquals(0.0, state.heldSeconds, 1e-9)
    }

    @Test
    fun `a still head in position accrues hold time`() {
        val e = engine()
        val state = e.feed(pose(0.0, 45.0), seconds = 1.0, rate = 0.5)
        assertEquals(Guidance.HOLDING, state.guidance)
        assertEquals(1.0, state.heldSeconds, 0.05)
    }

    @Test
    fun `the step completes only after the full hold`() {
        val e = engine()
        // Step one asks for three seconds.
        val early = e.feed(pose(0.0, 45.0), seconds = 2.0, rate = 0.0)
        assertEquals(Guidance.HOLDING, early.guidance)
        val done = e.feed(pose(0.0, 45.0), seconds = 1.5, rate = 0.0)
        assertEquals(Guidance.STEP_COMPLETE, done.guidance)
    }

    @Test
    fun `a brief wobble pauses the hold instead of resetting it`() {
        val e = engine()
        e.feed(pose(0.0, 45.0), seconds = 2.0, rate = 0.0)
        // Half a second well outside the band, then back.
        e.feed(pose(0.0, 20.0), seconds = 0.5, rate = 0.0)
        val back = e.onSample(pose(0.0, 45.0), 0.0, 0.02)
        assertTrue(
            back.heldSeconds > 1.9,
            "a half second wobble cost ${2.0 - back.heldSeconds} seconds of hold",
        )
    }

    @Test
    fun `a sustained departure resets the hold`() {
        val e = engine()
        e.feed(pose(0.0, 45.0), seconds = 2.0, rate = 0.0)
        val out = e.feed(pose(0.0, 0.0), seconds = 3.0, rate = 0.0)
        assertEquals(Guidance.SEEKING, out.guidance)
        assertEquals(0.0, out.heldSeconds, 1e-9)
    }

    @Test
    fun `an unreliable rotation reading cannot satisfy a hold`() {
        val e = engine()
        // Exactly on target by the numbers, but the decomposition has degenerated, so the
        // numbers do not mean anything.
        val state = e.onSample(pose(0.0, 45.0, swing = 170.0), 0.0, 0.02)
        assertEquals(Guidance.SEEKING, state.guidance)
        assertEquals(0.0, state.heldSeconds, 1e-9)
    }

    @Test
    fun `advancing moves to the next position and clears the timer`() {
        val e = engine()
        e.feed(pose(0.0, 45.0), seconds = 3.5, rate = 0.0)
        e.advance()
        val state = e.onSample(pose(25.0, 45.0), 0.0, 0.02)
        assertEquals("lie-back", state.step?.id)
        // The one sample just fed is in position, so it has already earned its own 0.02s. What
        // matters is that the 3.5 seconds banked on the previous step did not come with it.
        assertEquals(0.02, state.heldSeconds, 1e-6)
        assertEquals(1, state.completedSteps)
    }

    @Test
    fun `the manoeuvre finishes after the last step`() {
        val e = engine()
        repeat(Epley.steps().size) { e.advance() }
        val state = e.onSample(pose(0.0, 0.0), 0.0, 0.02)
        assertEquals(Guidance.FINISHED, state.guidance)
        assertNull(state.step)
    }

    @Test
    fun `a mirrored polarity mirrors the position the user is asked for`() {
        val mirrored = engine(polarity = false)
        // Step one still asks for 45 toward the affected side, which is now -45 on the sensor.
        val wrongWay = mirrored.onSample(pose(0.0, 45.0), 0.0, 0.02)
        assertEquals(Guidance.SEEKING, wrongWay.guidance)
        val rightWay = mirrored.onSample(pose(0.0, -45.0), 0.0, 0.02)
        assertEquals(Guidance.HOLDING, rightWay.guidance)
    }

    @Test
    fun `hold progress reports how far through the hold the user is`() {
        val e = engine()
        val state = e.feed(pose(0.0, 45.0), seconds = 1.5, rate = 0.0)
        assertEquals(0.5, state.holdProgress, 0.05)
    }

    @Test
    fun `restarting a step clears the timer without losing the position`() {
        val e = engine()
        e.feed(pose(0.0, 45.0), seconds = 2.0, rate = 0.0)
        e.restartStep()
        val state = e.onSample(pose(0.0, 45.0), 0.0, 0.02)
        assertEquals("prepare", state.step?.id)
        assertEquals(0.02, state.heldSeconds, 1e-6)
    }

    @Test
    fun `a whole manoeuvre can be walked through end to end`() {
        val e = engine()
        for (step in Epley.steps()) {
            val target = e.sensorTarget(step.target)
            val at = pose(target.neckExtensionDegrees, target.headRotationDegrees)
            val state = e.feed(at, seconds = step.holdSeconds + 1.0, rate = 0.0)
            assertEquals(Guidance.STEP_COMPLETE, state.guidance, "stalled on ${step.id}")
            e.advance()
        }
        assertEquals(Guidance.FINISHED, e.onSample(pose(0.0, 0.0), 0.0, 0.02).guidance)
    }

    @Test
    fun `time comes from the sample deltas, not from a count of samples`() {
        // Sensor delivery is not perfectly regular, and a hold counted in samples would run long
        // or short whenever the rate wandered. Two seconds of hold must be two seconds whether it
        // arrives as 100 samples or 40.
        val fast = engine()
        var last: EngineState? = null
        repeat(100) { last = fast.onSample(pose(0.0, 45.0), 0.0, 0.02) }
        assertEquals(2.0, last!!.heldSeconds, 1e-6)

        val slow = engine()
        repeat(40) { last = slow.onSample(pose(0.0, 45.0), 0.0, 0.05) }
        assertEquals(2.0, last!!.heldSeconds, 1e-6)
    }
}
