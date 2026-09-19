package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CuePlannerTest {

    private val steps = Epley.steps()
    private val lieBack = steps[1]

    private fun planner() = CuePlanner(
        polarity = RotationPolarity(towardAffectedSideIsPositive = true),
        side = Side.RIGHT,
    )

    private fun state(
        guidance: Guidance,
        step: ManeuverStep? = lieBack,
        index: Int = 1,
        held: Double = 0.0,
        correction: Correction? = Correction(0.0, 0.0),
    ) = EngineState(
        stepIndex = index,
        step = step,
        guidance = guidance,
        correction = correction,
        heldSeconds = held,
        holdSecondsRequired = step?.holdSeconds ?: 0,
        completedSteps = index,
    )

    private fun List<Cue>.spoken() = filterIsInstance<Cue.Speak>().map { it.text }
    private fun List<Cue>.buzzes() = filterIsInstance<Cue.Buzz>().map { it.pattern }

    @Test
    fun `a new position is announced once, with its instruction`() {
        val p = planner()
        val first = p.onState(state(Guidance.SEEKING, correction = Correction(40.0, 0.0)), 0.0)
        assertEquals(listOf(lieBack.spoken), first.spoken())
        assertTrue(p.onState(state(Guidance.SEEKING, correction = Correction(40.0, 0.0)), 0.5).isEmpty())
    }

    @Test
    fun `reaching the position buzzes once and says how long to hold`() {
        val p = planner()
        p.onState(state(Guidance.SEEKING), 0.0)
        val cues = p.onState(state(Guidance.HOLDING, held = 0.02), 3.0)
        assertEquals(listOf(Haptic.IN_POSITION), cues.buzzes())
        assertEquals(listOf("Good. Hold still for 45 seconds."), cues.spoken())
        assertTrue(p.onState(state(Guidance.HOLDING, held = 1.0), 4.0).isEmpty())
    }

    @Test
    fun `a finished position gives two buzzes once, however many samples report it`() {
        val p = planner()
        p.onState(state(Guidance.HOLDING, held = 44.0), 0.0)
        val done = p.onState(state(Guidance.STEP_COMPLETE, held = 45.0), 1.0)
        assertEquals(listOf(Haptic.STEP_DONE), done.buzzes())
        assertTrue(p.onState(state(Guidance.STEP_COMPLETE, held = 45.0), 1.5).isEmpty())
    }

    @Test
    fun `corrections wait for the instruction to be heard, then repeat at a calm pace`() {
        val p = planner()
        val off = Correction(40.0, 0.0)
        p.onState(state(Guidance.SEEKING, correction = off), 0.0)
        // Talking over the instruction would drown it out.
        assertTrue(p.onState(state(Guidance.SEEKING, correction = off), 3.0).spoken().isEmpty())
        assertEquals(
            listOf("Let your head hang lower"),
            p.onState(state(Guidance.SEEKING, correction = off), CuePlanner.INSTRUCTION_GRACE_SECONDS).spoken(),
        )
        // Not again straight away: a voice that never stops is a voice people stop hearing.
        assertTrue(p.onState(state(Guidance.SEEKING, correction = off), 8.0).spoken().isEmpty())
        val again = p.onState(
            state(Guidance.SEEKING, correction = off),
            CuePlanner.INSTRUCTION_GRACE_SECONDS + CuePlanner.CORRECTION_REPEAT_SECONDS,
        )
        assertEquals(listOf("Let your head hang lower"), again.spoken())
    }

    @Test
    fun `only the worst axis is spoken, so each cue is one instruction`() {
        val p = planner()
        p.onState(state(Guidance.SEEKING, correction = Correction(10.0, -35.0)), 0.0)
        val cues = p.onState(
            state(Guidance.SEEKING, correction = Correction(10.0, -35.0)),
            CuePlanner.INSTRUCTION_GRACE_SECONDS,
        )
        assertEquals(listOf("Turn toward your left"), cues.spoken())
    }

    @Test
    fun `moving out of a hold is corrected at once`() {
        val p = planner()
        p.onState(state(Guidance.SEEKING), 0.0)
        p.onState(state(Guidance.HOLDING, held = 5.0), 10.0)
        val cues = p.onState(state(Guidance.SEEKING, held = 5.0, correction = Correction(0.0, 35.0)), 11.0)
        assertEquals(listOf("You moved. Turn toward your right"), cues.spoken())
    }

    @Test
    fun `settling is prompted once, not on every sample`() {
        val p = planner()
        p.onState(state(Guidance.SEEKING), 0.0)
        assertEquals(listOf("Hold still"), p.onState(state(Guidance.SETTLING), 7.0).spoken())
        assertTrue(p.onState(state(Guidance.SETTLING), 7.5).spoken().isEmpty())
    }

    @Test
    fun `ten seconds left is announced on long holds`() {
        val p = planner()
        p.onState(state(Guidance.HOLDING, held = 30.0), 0.0)
        assertTrue(p.onState(state(Guidance.HOLDING, held = 34.9), 5.0).spoken().isEmpty())
        assertEquals(listOf("10 seconds"), p.onState(state(Guidance.HOLDING, held = 35.0), 5.1).spoken())
        assertTrue(p.onState(state(Guidance.HOLDING, held = 36.0), 6.0).spoken().isEmpty())
    }

    @Test
    fun `the end of the manoeuvre is announced once`() {
        val p = planner()
        p.onState(state(Guidance.HOLDING), 0.0)
        val end = p.onState(state(Guidance.FINISHED, step = null, index = 5, correction = null), 1.0)
        assertEquals(listOf(Haptic.FINISHED), end.buzzes())
        assertTrue(end.spoken().single().startsWith("Manoeuvre complete"))
        assertTrue(p.onState(state(Guidance.FINISHED, step = null, index = 5, correction = null), 2.0).isEmpty())
    }
}
