package health.epley.core

import kotlin.math.abs
import kotlin.math.roundToInt

/** One thing to tell the user, and how far it is in whole degrees. */
data class CorrectionPhrase(val text: String, val degrees: Int)

/**
 * Turns a signed correction into words a person lying on a bed can act on.
 *
 * Shared by the screen and the voice so the two can never disagree. "Turn toward your left" is
 * actionable with the eyes shut; "-27.4" is not, and translating a sensor sign into a body
 * direction is precisely the work the app exists to do on the user's behalf.
 */
object Phrasing {

    /**
     * What to say about a correction, the axis furthest out first. Empty when both axes are
     * inside [toleranceDegrees].
     *
     * A turn is always named by the side it goes toward. "Turn away from your right" makes a dizzy
     * person do a double negation; "turn toward your left" does not.
     */
    fun corrections(
        correction: Correction,
        polarity: RotationPolarity,
        side: Side,
        toleranceDegrees: Double,
        seated: Boolean = false,
        practice: Boolean = false,
    ): List<CorrectionPhrase> {
        val phrases = mutableListOf<Pair<Double, CorrectionPhrase>>()

        val pitch = correction.pitchDegrees
        if (abs(pitch) > toleranceDegrees) {
            // Sitting, any pitch error is a lean; "hang lower" would be nonsense upright.
            val text = when {
                practice && seated -> "Hold it upright"
                practice && pitch > 0 -> "Tip the top further down"
                practice -> "Tip the top back up a little"
                seated -> "Sit up straight"
                pitch > 0 -> "Let your head hang lower"
                else -> "Raise your head a little"
            }
            phrases += abs(pitch) to CorrectionPhrase(text, abs(pitch).roundToInt())
        }

        val rotation = correction.headRotationDegrees
        if (abs(rotation) > toleranceDegrees) {
            // A positive correction raises the sensor reading. Whether that is toward the affected
            // ear depends on the learned polarity, never on an assumption about anatomy.
            val towardAffected = (rotation > 0) == polarity.towardAffectedSideIsPositive
            val target = if (towardAffected) side else side.other()
            val text = if (practice) "Turn it like a key to your ${target.word}" else "Turn toward your ${target.word}"
            phrases += abs(rotation) to CorrectionPhrase(text, abs(rotation).roundToInt())
        }

        return phrases.sortedByDescending { it.first }.map { it.second }
    }

    /**
     * The correction as a sentence to speak, direction **and** distance.
     *
     * Found on a real head: "turn toward your left" sounds identical whether you are four degrees
     * out or forty, so a person who is nearly right over-corrects and a person who is far out
     * under-corrects, and both conclude the app is wrong. The distance was always computed and
     * then dropped on the way to the speaker — the screen showed it, and the screen is against
     * your cheek.
     *
     * Degrees are rounded to five. Nobody can execute "seventeen degrees", and false precision in
     * a spoken instruction reads as a machine talking to itself rather than to a person.
     */
    fun spoken(phrase: CorrectionPhrase): String = "${phrase.text}, ${distance(phrase.degrees)}"

    /**
     * How far, in words.
     *
     * Under ten degrees is inside or near every published tolerance band, so naming a number
     * there would send someone hunting for a precision the manoeuvre does not require.
     */
    fun distance(degrees: Int): String {
        val rounded = ((degrees + 2) / 5) * 5
        return when {
            degrees < 10 -> "a little"
            degrees <= 30 -> "about $rounded degrees"
            else -> "a long way, about $rounded degrees"
        }
    }
}

/** The opposite side. */
fun Side.other(): Side = if (this == Side.LEFT) Side.RIGHT else Side.LEFT

/** Lower-case word for speech and on-screen text. */
val Side.word: String get() = if (this == Side.LEFT) "left" else "right"
