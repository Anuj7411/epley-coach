package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CuePlannerTest {

    private val steps = Epley.steps()
    private val lieBack = steps[1]

    private fun planner(practice: Boolean = false) = CuePlanner(
        polarity = RotationPolarity(towardAffectedSideIsPositive = true),
        side = Side.RIGHT,
        practice = practice,
    )

    /** When corrections may start after the lie-back instruction, however long it is. */
    private val grace = CuePlanner.graceSeconds(lieBack.instruction(Side.RIGHT, practice = false))

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
        assertEquals(listOf(lieBack.instruction(Side.RIGHT, practice = false)), first.spoken())
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
            p.onState(state(Guidance.SEEKING, correction = off), grace).spoken(),
        )
        // Not again straight away: a voice that never stops is a voice people stop hearing.
        assertTrue(p.onState(state(Guidance.SEEKING, correction = off), grace + 2.0).spoken().isEmpty())
        val again = p.onState(
            state(Guidance.SEEKING, correction = off),
            grace + CuePlanner.CORRECTION_REPEAT_SECONDS,
        )
        assertEquals(listOf("Let your head hang lower"), again.spoken())
    }

    @Test
    fun `only the worst axis is spoken, so each cue is one instruction`() {
        val p = planner()
        p.onState(state(Guidance.SEEKING, correction = Correction(10.0, -35.0)), 0.0)
        val cues = p.onState(
            state(Guidance.SEEKING, correction = Correction(10.0, -35.0)),
            grace,
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
        assertEquals(listOf("Hold still"), p.onState(state(Guidance.SETTLING), 1.0).spoken())
        assertTrue(p.onState(state(Guidance.SETTLING), 1.5).spoken().isEmpty())
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

    @Test
    fun `a second hold in the same position is acknowledged briefly`() {
        // Reported from hardware: "hold for 45 seconds" over and over is irritating. The full
        // announcement is for the first time; after a wobble, a buzz and one word is enough.
        val p = planner()
        p.onState(state(Guidance.SEEKING), 0.0)
        p.onState(state(Guidance.HOLDING, held = 0.02), 1.0)
        p.onState(state(Guidance.SEEKING, correction = Correction(0.0, 35.0)), 5.0)
        val again = p.onState(state(Guidance.HOLDING, held = 0.02), 8.0)
        assertEquals(listOf(Haptic.IN_POSITION), again.buzzes())
        assertEquals(listOf("Hold"), again.spoken())
    }

    @Test
    fun `an instruction nobody has acted on is repeated`() {
        val p = planner()
        val off = Correction(40.0, 0.0)
        p.onState(state(Guidance.SEEKING, correction = off), 0.0)
        val later = p.onState(state(Guidance.SEEKING, correction = off), CuePlanner.REANNOUNCE_SECONDS)
        assertEquals(lieBack.instruction(Side.RIGHT, practice = false), later.spoken().first())
    }

    @Test
    fun `the instruction names the actual side, not a category`() {
        // "Your affected side" makes a dizzy person work out which that is. "Your right" does not.
        val spoken = planner().onState(state(Guidance.SEEKING, step = steps[0], index = 0), 0.0).spoken().single()
        assertTrue("right" in spoken, spoken)
        assertTrue("affected" !in spoken && "{" !in spoken, spoken)
    }

    @Test
    fun `practice mode talks about the phone, not a bed`() {
        // Reported from hardware: practice mode told the user to lie on a bed while they held a
        // phone. In practice the phone is the head, so the instruction has to say what to do to it.
        val spoken = planner(practice = true).onState(state(Guidance.SEEKING), 0.0).spoken().single()
        assertEquals(lieBack.instruction(Side.RIGHT, practice = true), spoken)
        assertTrue("phone" in spoken, spoken)
        assertTrue("bed" !in spoken, spoken)
    }

    @Test
    fun `practice corrections describe moving the phone`() {
        val p = planner(practice = true)
        val off = Correction(40.0, 0.0)
        p.onState(state(Guidance.SEEKING, correction = off), 0.0)
        val g = CuePlanner.graceSeconds(lieBack.instruction(Side.RIGHT, practice = true))
        assertEquals(listOf("Tip the top further down"), p.onState(state(Guidance.SEEKING, correction = off), g).spoken())
    }

    @Test
    fun `longer instructions get longer before corrections start`() {
        assertTrue(CuePlanner.graceSeconds("Sit up.") >= CuePlanner.INSTRUCTION_GRACE_SECONDS)
        val long = "word ".repeat(40)
        assertTrue(CuePlanner.graceSeconds(long) > CuePlanner.INSTRUCTION_GRACE_SECONDS)
    }
}
