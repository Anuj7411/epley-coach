package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SafetyTest {

    @Test
    fun `nothing answered asks the emergency question first`() {
        assertEquals(SafetyOutcome.Incomplete(nextQuestion = 1), Safety.assess(null, null))
    }

    @Test
    fun `any emergency sign stops everything, whatever else is answered`() {
        // Checked first and wins outright: a stroke sign is never outranked by anything.
        assertEquals(SafetyOutcome.Emergency, Safety.assess(emergencySigns = true, reasonsNotToTreat = null))
        assertEquals(SafetyOutcome.Emergency, Safety.assess(emergencySigns = true, reasonsNotToTreat = false))
    }

    @Test
    fun `no emergency signs moves on to the reasons not to treat`() {
        assertEquals(SafetyOutcome.Incomplete(nextQuestion = 2), Safety.assess(false, null))
    }

    @Test
    fun `a reason not to treat sends the user to a doctor instead`() {
        assertEquals(SafetyOutcome.SeeDoctor, Safety.assess(false, true))
    }

    @Test
    fun `only two clear answers clear the screen`() {
        assertEquals(SafetyOutcome.Clear, Safety.assess(false, false))
    }

    @Test
    fun `the emergency list names the stroke signs the research found`() {
        // docs/RESEARCH.md section 7. If one is dropped from the list this should fail loudly.
        val text = Safety.emergencySigns.joinToString(" ").lowercase()
        for (sign in listOf("weakness", "speaking", "double vision", "walk", "headache", "hearing")) {
            assertTrue(sign in text, "emergency list lost '$sign'")
        }
    }

    @Test
    fun `the reasons not to treat include the trial's exclusions`() {
        val text = Safety.reasonsNotToTreat.joinToString(" ").lowercase()
        for (reason in listOf("diagnosed", "neck", "head injury", "different")) {
            assertTrue(reason in text, "reasons list lost '$reason'")
        }
    }
}
