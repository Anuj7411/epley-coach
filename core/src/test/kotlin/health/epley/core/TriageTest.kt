package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The six-question subtype triage from Kim et al. (Neurology 2020), used in the JAMA Neurology
 * 2023 self-treatment RCT. Expected outcomes below come from the published mapping, not from the
 * implementation: see docs/RESEARCH.md section 6 and PMC10318130.
 */
class TriageTest {

    private fun answers(
        spinning: Boolean = true,
        worseWithHeadMovement: Boolean = true,
        lastsUnderThreeMinutes: Boolean = true,
        provokedBy: ProvokedBy? = ProvokedBy.LYING_DOWN_OR_GETTING_UP,
        worseSide: Side? = Side.RIGHT,
        turningDizzinessLasts: TurningDuration? = null,
    ) = TriageAnswers(
        spinning, worseWithHeadMovement, lastsUnderThreeMinutes,
        provokedBy, worseSide, turningDizzinessLasts,
    )

    @Test
    fun `dizziness from lying down, worse turning right, is right posterior canal`() {
        assertEquals(TriageOutcome.PosteriorCanal(Side.RIGHT), Triage.assess(answers()))
    }

    @Test
    fun `a failed screening question stops the manoeuvre and says which one`() {
        // Dizziness not tied to head movement and lasting over three minutes is not the BPPV
        // picture. The canal answers must not rescue it into a treatment.
        val outcome = Triage.assess(
            answers(worseWithHeadMovement = false, lastsUnderThreeMinutes = false),
        )
        assertEquals(TriageOutcome.NotConsistentWithBppv(failedQuestions = listOf(2, 3)), outcome)
    }

    @Test
    fun `dizziness from turning while lying is horizontal canal, typed by duration`() {
        val brief = Triage.assess(
            answers(
                provokedBy = ProvokedBy.TURNING_WHILE_LYING,
                worseSide = Side.LEFT,
                turningDizzinessLasts = TurningDuration.UNDER_ONE_MINUTE,
            ),
        )
        assertEquals(TriageOutcome.HorizontalCanal(Side.LEFT, HorizontalType.CANALITHIASIS), brief)

        val long = Triage.assess(
            answers(
                provokedBy = ProvokedBy.TURNING_WHILE_LYING,
                worseSide = Side.RIGHT,
                turningDizzinessLasts = TurningDuration.OVER_ONE_MINUTE,
            ),
        )
        assertEquals(TriageOutcome.HorizontalCanal(Side.RIGHT, HorizontalType.CUPULOLITHIASIS), long)
    }

    @Test
    fun `unanswered questions are reported as the next one to ask`() {
        assertEquals(
            TriageOutcome.Incomplete(nextQuestion = 4),
            Triage.assess(answers(provokedBy = null, worseSide = null)),
        )
        assertEquals(
            TriageOutcome.Incomplete(nextQuestion = 5),
            Triage.assess(answers(worseSide = null)),
        )
        // Question 6 is only asked for the horizontal canal.
        assertEquals(
            TriageOutcome.Incomplete(nextQuestion = 6),
            Triage.assess(answers(provokedBy = ProvokedBy.TURNING_WHILE_LYING)),
        )
    }

    @Test
    fun `a failed screen ends the questionnaire before the canal questions are asked`() {
        assertEquals(
            TriageOutcome.NotConsistentWithBppv(failedQuestions = listOf(1)),
            Triage.assess(answers(spinning = false, provokedBy = null, worseSide = null)),
        )
    }
}
