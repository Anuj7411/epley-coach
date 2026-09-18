package health.epley.core

/** Which positional change brings the dizziness on more (question 4). */
enum class ProvokedBy { LYING_DOWN_OR_GETTING_UP, TURNING_WHILE_LYING }

/** How long the dizziness from turning the head lasts (question 6). */
enum class TurningDuration { UNDER_ONE_MINUTE, OVER_ONE_MINUTE }

/** The six answers, in the order they are asked. Later answers are null until asked. */
data class TriageAnswers(
    val spinning: Boolean,
    val worseWithHeadMovement: Boolean,
    val lastsUnderThreeMinutes: Boolean,
    val provokedBy: ProvokedBy?,
    val worseSide: Side?,
    val turningDizzinessLasts: TurningDuration?,
)

sealed interface TriageOutcome {
    /** Not enough answered yet. [nextQuestion] is 1-based. */
    data class Incomplete(val nextQuestion: Int) : TriageOutcome

    data class PosteriorCanal(val side: Side) : TriageOutcome

    /** One or more of questions 1-3 ruled out the BPPV picture. [failedQuestions] are 1-based. */
    data class NotConsistentWithBppv(val failedQuestions: List<Int>) : TriageOutcome

    /**
     * Horizontal canal. The Epley does not treat this — the trial assigned a barbecue roll for
     * canalithiasis and a Gufoni manoeuvre for cupulolithiasis — so the app must not run it.
     */
    data class HorizontalCanal(val side: Side, val type: HorizontalType) : TriageOutcome
}

enum class HorizontalType { CANALITHIASIS, CUPULOLITHIASIS }

object Triage {
    fun assess(answers: TriageAnswers): TriageOutcome {
        val failed = listOfNotNull(
            1.takeUnless { answers.spinning },
            2.takeUnless { answers.worseWithHeadMovement },
            3.takeUnless { answers.lastsUnderThreeMinutes },
        )
        if (failed.isNotEmpty()) return TriageOutcome.NotConsistentWithBppv(failed)
        val provokedBy = answers.provokedBy ?: return TriageOutcome.Incomplete(4)
        val side = answers.worseSide ?: return TriageOutcome.Incomplete(5)
        return when (provokedBy) {
            ProvokedBy.LYING_DOWN_OR_GETTING_UP -> TriageOutcome.PosteriorCanal(side)
            ProvokedBy.TURNING_WHILE_LYING -> {
                val duration = answers.turningDizzinessLasts ?: return TriageOutcome.Incomplete(6)
                TriageOutcome.HorizontalCanal(
                    side,
                    when (duration) {
                        TurningDuration.UNDER_ONE_MINUTE -> HorizontalType.CANALITHIASIS
                        TurningDuration.OVER_ONE_MINUTE -> HorizontalType.CUPULOLITHIASIS
                    },
                )
            }
        }
    }
}
