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
    /** Read aloud when the step begins. The screen is secondary; this is the real interface. */
    val spoken: String,
    /** Said when the user is out of position, after the direction of correction. */
    val target: TargetPose,
    val holdSeconds: Int,
)

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
 * Extension is the angle of the face below horizontal, measured against gravity, which is the one
 * angle that cannot drift.
 */
object Epley {

    fun steps(): List<ManeuverStep> = listOf(
        ManeuverStep(
            id = "prepare",
            title = "Sit up and turn",
            spoken = "Sit upright on the edge of the bed. Turn your head forty five degrees " +
                "toward your affected side.",
            target = TargetPose(neckExtensionDegrees = 0.0, headRotationDegrees = 45.0),
            // Short: this only confirms the starting position, it is not a therapeutic hold.
            holdSeconds = 3,
        ),
        ManeuverStep(
            id = "lie-back",
            title = "Lie back, head hanging",
            spoken = "Keeping your head turned, lie back quickly so your head hangs off the edge " +
                "of the bed. You may feel the spinning start. That is expected. Stay still.",
            target = TargetPose(neckExtensionDegrees = 25.0, headRotationDegrees = 45.0),
            holdSeconds = 45,
        ),
        ManeuverStep(
            id = "turn-across",
            title = "Turn to the other side",
            spoken = "Slowly turn your head ninety degrees, to face the other way. Keep your head " +
                "hanging back.",
            target = TargetPose(neckExtensionDegrees = 25.0, headRotationDegrees = -45.0),
            holdSeconds = 45,
        ),
        ManeuverStep(
            id = "roll",
            title = "Roll onto your shoulder",
            spoken = "Roll onto that shoulder and turn your head further, until you are looking " +
                "down at the floor.",
            // Face down at 45 degrees, head turned a further 90 from the previous step.
            target = TargetPose(neckExtensionDegrees = 45.0, headRotationDegrees = -135.0),
            holdSeconds = 45,
        ),
        ManeuverStep(
            id = "sit-up",
            title = "Sit up slowly",
            spoken = "Slowly sit up, and bring your head back to facing forward.",
            // Also the drift check: this is the pose the calibration was taken in, so the true
            // reading here is zero and anything else is measured mount error.
            target = TargetPose(neckExtensionDegrees = 0.0, headRotationDegrees = 0.0),
            holdSeconds = 5,
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
    val neckExtensionDegrees: Double,
    val headRotationDegrees: Double,
) {
    /** The axis that is furthest out, which is the one worth speaking about first. */
    val worstAxisIsExtension: Boolean
        get() = abs(neckExtensionDegrees) >= abs(headRotationDegrees)
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
    private val toleranceDegrees: Double,
    private val stillnessThresholdDegPerSec: Double,
) {

    private var index = 0
    private var heldSeconds = 0.0
    private var secondsOutOfPosition = 0.0
    private var finished = false

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

        val extensionError = target.neckExtensionDegrees - pose.neckExtensionDegrees
        val rotationError = target.headRotationDegrees - pose.headRotationDegrees
        val correction = Correction(extensionError, rotationError)

        val inPosition = maxOf(abs(extensionError), abs(rotationError)) <= toleranceDegrees
        val isStill = abs(angularRateDegPerSec) < stillnessThresholdDegPerSec

        // A rotation reading we do not trust cannot be allowed to satisfy a hold. Reporting
        // SEEKING is the honest response: the user is told to reposition rather than credited for
        // a position we cannot confirm.
        if (!pose.isRotationReliable) {
            secondsOutOfPosition += deltaSeconds
            if (secondsOutOfPosition >= RESET_AFTER_SECONDS_OUT) heldSeconds = 0.0
            return state(Guidance.SEEKING, correction)
        }

        if (!inPosition) {
            secondsOutOfPosition += deltaSeconds
            if (secondsOutOfPosition >= RESET_AFTER_SECONDS_OUT) heldSeconds = 0.0
            return state(Guidance.SEEKING, correction)
        }

        secondsOutOfPosition = 0.0

        if (!isStill) return state(Guidance.SETTLING, correction)

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
        heldSeconds = 0.0
        secondsOutOfPosition = 0.0
    }

    /** Express a step's target, written in degrees toward the affected side, in sensor terms. */
    fun sensorTarget(target: TargetPose): TargetPose = TargetPose(
        neckExtensionDegrees = target.neckExtensionDegrees,
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
    }
}
