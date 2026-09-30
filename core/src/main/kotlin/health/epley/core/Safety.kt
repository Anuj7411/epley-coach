package health.epley.core

/** Where the safety screen has got to. */
sealed interface SafetyOutcome {
    /** [nextQuestion] is 1 (emergency signs) or 2 (reasons not to treat). */
    data class Incomplete(val nextQuestion: Int) : SafetyOutcome

    /** A stroke warning sign. No manoeuvre; emergency care now. */
    data object Emergency : SafetyOutcome

    /** Not an emergency, but a reason this person should not self-treat today. */
    data object SeeDoctor : SafetyOutcome

    /** Both questions answered no. The only outcome that lets a run start. */
    data object Clear : SafetyOutcome
}

/**
 * The check before every run. Unskippable, and never behind the paywall.
 *
 * ## Why two questions and not a checklist
 *
 * The person answering is dizzy and possibly nauseous. Each question shows a short list and asks
 * whether *any* of it applies — two taps in total. A ten-item checklist would be skimmed, and a
 * skimmed safety screen is worse than none because it looks like diligence.
 *
 * ## Where the items come from
 *
 * Emergency signs: the red flags that separate a posterior circulation stroke from benign
 * positional vertigo — one-sided weakness, falling to one side, sudden hearing loss, skew
 * deviation (felt as double vision) — plus the speech and headache signs of stroke generally.
 * 20-25% of posterior circulation strokes are misdiagnosed at first presentation, with dizziness
 * the main confounder. See docs/RESEARCH.md section 7.
 *
 * Reasons not to treat: the JAMA Neurology 2023 self-treatment trial enrolled only patients with
 * a confirmed BPPV diagnosis and excluded those who could not perform the manoeuvre because of
 * spinal problems. A recent head injury and an attack unlike previous ones are the same idea: a
 * reason to think this is not the diagnosed condition.
 */
object Safety {

    /** Question 1: "Do you have any of these right now?" */
    // The design's wording, verbatim (handoff README §1.2): one short line each, so a frightened
    // person can scan six rows rather than read six sentences.
    val emergencySigns = listOf(
        "Weakness or numbness",
        "Trouble speaking",
        "Double vision",
        "Can’t walk",
        "Sudden severe headache",
        "Sudden hearing loss",
    )

    /** Question 2: "Do any of these apply to you?" */
    val reasonsNotToTreat = listOf(
        "A doctor has never diagnosed you with BPPV",
        "A neck or back problem that makes moving your head hard",
        "A recent head injury",
        "This attack feels different from the ones you were diagnosed with",
    )

    fun assess(emergencySigns: Boolean?, reasonsNotToTreat: Boolean?): SafetyOutcome = when {
        emergencySigns == null -> SafetyOutcome.Incomplete(1)
        emergencySigns -> SafetyOutcome.Emergency
        reasonsNotToTreat == null -> SafetyOutcome.Incomplete(2)
        reasonsNotToTreat -> SafetyOutcome.SeeDoctor
        else -> SafetyOutcome.Clear
    }
}
