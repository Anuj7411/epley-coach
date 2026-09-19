package health.epley.core

import kotlin.math.abs

/** Which ear the user reports as affected. Their own report, never a diagnosis. */
enum class Side { LEFT, RIGHT }

/**
 * Which direction of head turn the sensor calls positive.
 *
 * The sign of [HeadPose.headRotationDegrees] falls out of the calibration, not out of anatomy — it
 * depends on which cheek the phone is against and which way round it is. So the manoeuvre cannot
 * assume that "positive means left". Instead the app asks once, at the start: turn your head
 * toward the affected side. Whatever sign that produces is what "toward the affected side" means
 * for the rest of the session.
 *
 * Learning it beats guessing it. A guess that is wrong by a sign sends the user through the whole
 * manoeuvre mirrored, which does nothing for the affected ear and might provoke the other one.
 */
data class RotationPolarity(val towardAffectedSideIsPositive: Boolean) {

    /** Convert an angle expressed as "degrees toward the affected side" into sensor degrees. */
    fun toSensor(degreesTowardAffectedSide: Double): Double =
        if (towardAffectedSideIsPositive) degreesTowardAffectedSide else -degreesTowardAffectedSide

    companion object {
        /**
         * Learn the polarity from a sample taken while the user turns toward the affected side.
         *
         * Returns null if the turn was too small to read a sign from — better to ask again than
         * to fix the convention off a couple of degrees of noise.
         */
        fun learnFrom(pose: HeadPose): RotationPolarity? {
            if (abs(pose.headRotationDegrees) < MIN_LEARNING_TURN_DEGREES) return null
            return RotationPolarity(pose.headRotationDegrees > 0)
        }

        /**
         * A turn smaller than this does not establish a direction.
         *
         * 20 degrees is well above sensor noise (under a tenth of a degree at rest, measured) and
         * well under the 45 the manoeuvre asks for, so someone making a genuine attempt clears it
         * easily while someone who did not move at all does not.
         */
        const val MIN_LEARNING_TURN_DEGREES = 20.0
    }
}

/** One position in the manoeuvre. */
data class ManeuverStep(
    val id: String,
    val title: String,
    /**
     * Read aloud when the step begins, for someone doing the manoeuvre. Short sentences, no
     * numbers a dizzy person must convert: "halfway to your shoulder", not "45 degrees".
     * `{affected}` and `{other}` become "right" and "left" -- see [instruction].
     */
    val spoken: String,
    /**
     * The same step for practice mode, where the phone stands in for the head. A practice run
     * that tells someone holding a phone to lie on a bed teaches nothing, and a practice run on
     * hardware showed exactly that.
     */
    val practiceSpoken: String,
    /** Said when the user is out of position, after the direction of correction. */
    val target: TargetPose,
    val holdSeconds: Int,
    /**
     * How far from [target] still counts as in position, on either axis.
     *
     * For the three therapeutic positions this is the acceptable range Kwon et al. (Sci Rep 2023)
     * derived from specialists' own accuracy — mean error plus half a standard deviation. Holding
     * a patient to a tighter band than an experienced specialist achieves would leave them hunting
     * for a position that was already good enough.
     */
    val toleranceDegrees: Double,
) {
    /** The words to say, with the sides filled in. "Your right" beats "your affected side". */
    fun instruction(side: Side, practice: Boolean): String =
        (if (practice) practiceSpoken else spoken)
            .replace("{affected}", side.word)
            .replace("{other}", side.other().word)
}

/**
 * The four-position Epley for the posterior canal, plus the sit-up.
 *
 * ## Where these angles come from
 *
 * The manoeuvre as normally described: head turned 45 degrees toward the affected ear; lie back
 * with the head extended 20-30 degrees below horizontal; turn 90 degrees to the other side; roll
 * onto that shoulder and turn a further 90 so the face points at the floor; sit up. Every target
 * below is that description written in the two angles this app can actually measure, with the
 * extension taken at the middle of each published range.
 *
 * ## What the numbers mean here
 *
 * Rotation is expressed as **degrees toward the affected side** and converted to sensor sign by
 * [RotationPolarity]. So step three's -45 means 45 degrees away from the affected side, which is
 * the 90-degree turn the manoeuvre calls for, measured from where the head already was.
 *
 * Pitch is the head's long axis below horizontal, measured against gravity so it cannot drift:
 * -90 sitting, 0 lying flat, +25 hanging off the bed. It is the crown, not the face, that the
 * guideline's "20-30 degrees below horizontal" describes — lying back, the face points at the
 * ceiling.
 */
object Epley {

    fun steps(): List<ManeuverStep> = listOf(
        ManeuverStep(
            id = "prepare",
            title = "Sit up and turn",
            spoken = "Sit on the edge of the bed. Turn your head to the {affected}, halfway to " +
                "your shoulder.",
            practiceSpoken = "Hold the phone upright, screen facing you. It stands in for your " +
                "head. Turn it like a key, halfway to the {affected}.",
            target = TargetPose(pitchDegrees = -90.0, headRotationDegrees = 45.0),
            // Short: this only confirms the starting position, it is not a therapeutic hold.
            holdSeconds = 3,
            // No published range for the seated steps. Sitting and turning is the easy part, so
            // this is narrower than any therapeutic band while still wider than the sensor error.
            toleranceDegrees = 15.0,
        ),
        ManeuverStep(
            id = "lie-back",
            title = "Lie back, head hanging",
            spoken = "Keep your head turned. Lie back quickly, with your head hanging over the " +
                "edge of the bed. You may feel dizzy. That is expected. Stay still.",
            practiceSpoken = "Keep it turned. Tip the top of the phone away from you, until it " +
                "lies flat. Then tip it a little further.",
            target = TargetPose(pitchDegrees = 25.0, headRotationDegrees = 45.0),
            holdSeconds = 45,
            toleranceDegrees = 26.1,
        ),
        ManeuverStep(
            id = "turn-across",
            title = "Turn to the other side",
            spoken = "Now slowly turn your head all the way to the {other}. Keep it hanging back.",
            practiceSpoken = "Keep it tipped back. Turn it like a key the other way, to the {other}.",
            target = TargetPose(pitchDegrees = 25.0, headRotationDegrees = -45.0),
            holdSeconds = 45,
            toleranceDegrees = 31.0,
        ),
        ManeuverStep(
            id = "roll",
            title = "Roll onto your shoulder",
            spoken = "Roll onto your {other} side. Turn your head further, so you look down at " +
                "the floor.",
            practiceSpoken = "Keep turning it like a key to the {other}, until the screen faces " +
                "down. Bring the top back up level.",
            // Head turned a further 90 from the previous step, so the face points toward the floor.
            // Rolled onto the side with the neck angle kept, the hang that pointed downward now
            // points sideways, so the long axis lies level: pitch 0.
            target = TargetPose(pitchDegrees = 0.0, headRotationDegrees = -135.0),
            holdSeconds = 45,
            toleranceDegrees = 18.9,
        ),
        ManeuverStep(
            id = "sit-up",
            title = "Sit up slowly",
            spoken = "Slowly sit up. Face straight ahead.",
            practiceSpoken = "Bring the phone back upright, screen facing you.",
            // Also the drift check: this is the pose the calibration was taken in, so the true
            // reading here is zero and anything else is measured mount error.
            target = TargetPose(pitchDegrees = -90.0, headRotationDegrees = 0.0),
            holdSeconds = 5,
            toleranceDegrees = 15.0,
        ),
    )
}

/** What the engine wants the user to do about the current sample. */
enum class Guidance {
    /** Out of position. [EngineState.correction] says which way to move. */
    SEEKING,

    /** In position but still moving. The hold cannot start until the head settles. */
    SETTLING,

    /** In position and still. The hold timer is running. */
    HOLDING,

    /** This step is finished. */
    STEP_COMPLETE,

    /** Every step is finished. */
    FINISHED,
}

/**
 * Which way to move, and by how much, per axis.
 *
 * Signed in the same terms as the pose, so the UI and the voice can both say "further back" or
 * "turn toward your affected side" without re-deriving anything.
 */
data class Correction(
    val pitchDegrees: Double,
    val headRotationDegrees: Double,
) {
    /** The axis that is furthest out, which is the one worth speaking about first. */
    val worstAxisIsPitch: Boolean
        get() = abs(pitchDegrees) >= abs(headRotationDegrees)
}

/** Everything the UI, the voice and the log need about where the manoeuvre has got to. */
data class EngineState(
    val stepIndex: Int,
    val step: ManeuverStep?,
    val guidance: Guidance,
    val correction: Correction?,
    val heldSeconds: Double,
    val holdSecondsRequired: Int,
    val completedSteps: Int,
) {
    val holdProgress: Double
        get() = if (holdSecondsRequired <= 0) 1.0 else (heldSeconds / holdSecondsRequired).coerceIn(0.0, 1.0)
}

/**
 * Runs the manoeuvre.
 *
 * ## The rule that makes this worth building
 *
 * The hold timer advances only while the head is inside the tolerance band **and** below the
 * stillness threshold. A countdown that runs regardless is a stopwatch with extra steps: it will
 * happily certify a forty-five second hold that the user spent sweeping through the target. The
 * whole clinical argument for this app is that the angle was actually reached and actually held,
 * so the timer has to be unable to lie about it.
 *
 * ## Leaving position mid-hold
 *
 * A brief excursion pauses the timer rather than resetting it — real necks wobble, and punishing a
 * half-second of wobble by restarting a forty-five second hold would make the app unusable and
 * push people to give up. A sustained departure does reset, because at that point the crystals
 * have not been where they needed to be and pretending otherwise is the one thing we must not do.
 * [RESET_AFTER_SECONDS_OUT] is where one becomes the other.
 */
class ManeuverEngine(
    private val steps: List<ManeuverStep>,
    private val polarity: RotationPolarity,
    private val stillnessThresholdDegPerSec: Double,
) {

    private var index = 0
    private var heldSeconds = 0.0
    private var secondsOutOfPosition = 0.0
    private var finished = false

    /** True while a hold is running. Keeping one takes less stillness than starting one. */
    private var holding = false

    /**
     * Advance the manoeuvre by one sample.
     *
     * @param deltaSeconds time since the previous sample, from the sensor timestamps rather than
     *   the wall clock, so a hold cannot be shortened by a stalled UI thread
     */
    fun onSample(pose: HeadPose, angularRateDegPerSec: Double, deltaSeconds: Double): EngineState {
        if (finished) return state(Guidance.FINISHED, null)

        val step = steps[index]
        val target = sensorTarget(step.target)

        val pitchError = target.pitchDegrees - pose.pitchDegrees
        val rotationError = HeadAngles.shortestDegrees(target.headRotationDegrees - pose.headRotationDegrees)
        val correction = Correction(pitchError, rotationError)

        val inPosition = maxOf(abs(pitchError), abs(rotationError)) <= step.toleranceDegrees
        // Hysteresis. Starting a hold needs real stillness, so a head sweeping through the target
        // is never credited. Keeping one only needs the absence of real movement: a hand-held
        // phone trembles around the start threshold, and without this the state flipped between
        // holding and settling several times a second on hardware.
        val threshold = if (holding) stillnessThresholdDegPerSec * HOLD_HYSTERESIS else stillnessThresholdDegPerSec
        val isStill = abs(angularRateDegPerSec) < threshold

        // A rotation reading we do not trust cannot be allowed to satisfy a hold. Reporting
        // SEEKING is the honest response: the user is told to reposition rather than credited for
        // a position we cannot confirm.
        if (!pose.isRotationReliable) {
            holding = false
            secondsOutOfPosition += deltaSeconds
            if (secondsOutOfPosition >= RESET_AFTER_SECONDS_OUT) heldSeconds = 0.0
            return state(Guidance.SEEKING, correction)
        }

        if (!inPosition) {
            holding = false
            secondsOutOfPosition += deltaSeconds
            if (secondsOutOfPosition >= RESET_AFTER_SECONDS_OUT) heldSeconds = 0.0
            return state(Guidance.SEEKING, correction)
        }

        secondsOutOfPosition = 0.0

        if (!isStill) {
            holding = false
            return state(Guidance.SETTLING, correction)
        }

        holding = true
        heldSeconds += deltaSeconds
        if (heldSeconds >= step.holdSeconds) {
            return state(Guidance.STEP_COMPLETE, correction)
        }
        return state(Guidance.HOLDING, correction)
    }

    /**
     * Move to the next step.
     *
     * Called by the app once it has announced the completion, rather than automatically, so the
     * spoken cue for the next position is never cut off by the engine racing ahead of the voice.
     */
    fun advance() {
        if (finished) return
        holding = false
        heldSeconds = 0.0
        secondsOutOfPosition = 0.0
        if (index >= steps.lastIndex) {
            finished = true
            index = steps.size
        } else {
            index++
        }
    }

    /** Start the current step again, keeping the manoeuvre where it is. */
    fun restartStep() {
        holding = false
        heldSeconds = 0.0
        secondsOutOfPosition = 0.0
    }

    /** Express a step's target, written in degrees toward the affected side, in sensor terms. */
    fun sensorTarget(target: TargetPose): TargetPose = TargetPose(
        pitchDegrees = target.pitchDegrees,
        headRotationDegrees = polarity.toSensor(target.headRotationDegrees),
    )

    private fun state(guidance: Guidance, correction: Correction?) = EngineState(
        stepIndex = index,
        step = steps.getOrNull(index),
        guidance = guidance,
        correction = correction,
        heldSeconds = heldSeconds,
        holdSecondsRequired = steps.getOrNull(index)?.holdSeconds ?: 0,
        completedSteps = index,
    )

    companion object {
        /**
         * How long the head may stray before the hold restarts rather than resumes.
         *
         * Two seconds is longer than a wobble and shorter than a repositioning. Below about one
         * second ordinary tremor would reset the timer and nobody would ever finish a hold; much
         * above three and someone could leave the position entirely, come back, and be credited
         * for time the crystals spent going the wrong way.
         */
        const val RESET_AFTER_SECONDS_OUT = 2.0

        /**
         * Once holding, how many times the start threshold it takes to count as moving.
         *
         * Three times 5 deg/s is 15: above hand and head tremor, and still a quarter of the
         * roughly 60 deg/s of someone deliberately repositioning.
         */
        const val HOLD_HYSTERESIS = 3.0
    }
}
