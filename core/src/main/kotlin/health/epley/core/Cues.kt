package health.epley.core

import kotlin.math.ceil

/** Vibration patterns. Distinct enough to tell apart with the eyes shut. */
enum class Haptic {
    /** One short buzz: you are in position, the hold has started. */
    IN_POSITION,

    /** Two buzzes: this position is done, the next instruction follows. */
    STEP_DONE,

    /** One long buzz: the manoeuvre is over. */
    FINISHED,
}

/** Something to say or feel. The app decides how; the planner decides when. */
sealed interface Cue {
    /** [interrupt] cuts off whatever is being said; otherwise the phrase queues behind it. */
    data class Speak(val text: String, val interrupt: Boolean = false) : Cue
    data class Buzz(val pattern: Haptic) : Cue
}

/**
 * Decides when to speak and when to vibrate, from the stream of engine states.
 *
 * ## Why this is its own class
 *
 * The engine emits a state fifty times a second. Speaking on every sample would be noise, and a
 * voice that never stops is a voice people stop listening to. The rules for *when* — announce a
 * position once, let the instruction be heard before correcting, repeat corrections at a calm
 * pace, react at once when a hold is broken — are the whole of the audio design, and they are
 * testable only if they live apart from Android's speech engine.
 *
 * Only the axis furthest out is spoken, so each cue is a single instruction. The screen shows both.
 */
class CuePlanner(
    private val polarity: RotationPolarity,
    private val side: Side,
    /** Practice mode: the phone stands in for the head, so every word is about the phone. */
    private val practice: Boolean = false,
) {

    private var lastStepIndex: Int? = null
    private var lastGuidance: Guidance? = null
    private var graceUntil = 0.0
    private var lastInstructionAt = 0.0
    private var holdAnnouncedThisStep = false
    private var lastCorrectionAt: Double? = null
    private var tenSecondsAnnounced = false
    private var finishedAnnounced = false

    /** The cues this state calls for, possibly none. [nowSeconds] is sensor time. */
    fun onState(state: EngineState, nowSeconds: Double): List<Cue> {
        val cues = mutableListOf<Cue>()

        if (state.guidance == Guidance.FINISHED) {
            if (!finishedAnnounced) {
                finishedAnnounced = true
                cues += Cue.Buzz(Haptic.FINISHED)
                cues += Cue.Speak(
                    "Manoeuvre complete. Stay sitting upright for a minute before you stand.",
                    interrupt = true,
                )
            }
            lastGuidance = state.guidance
            return cues
        }

        val newStep = state.stepIndex != lastStepIndex
        if (newStep) {
            lastStepIndex = state.stepIndex
            lastCorrectionAt = null
            tenSecondsAnnounced = false
            holdAnnouncedThisStep = false
            state.step?.let { announce(it, nowSeconds, cues) }
        }
        val previous = if (newStep) null else lastGuidance
        val remaining = ceil(state.holdSecondsRequired - state.heldSeconds).toInt().coerceAtLeast(0)

        when (state.guidance) {
            Guidance.HOLDING -> {
                if (previous != Guidance.HOLDING) {
                    cues += Cue.Buzz(Haptic.IN_POSITION)
                    // The full sentence once per position. Back in position after a wobble, one
                    // word is enough: hearing "hold for 45 seconds" on every re-entry is what a
                    // practice run on hardware found irritating.
                    cues += Cue.Speak(
                        if (holdAnnouncedThisStep) "Hold" else "Good. Hold still for $remaining seconds.",
                    )
                    holdAnnouncedThisStep = true
                    // Resuming a hold that is already nearly done: no separate countdown cue.
                    if (remaining <= 10) tenSecondsAnnounced = true
                } else if (!tenSecondsAnnounced &&
                    state.holdSecondsRequired >= LONG_HOLD_SECONDS &&
                    remaining <= 10
                ) {
                    tenSecondsAnnounced = true
                    cues += Cue.Speak("10 seconds")
                }
            }

            Guidance.STEP_COMPLETE -> if (previous != Guidance.STEP_COMPLETE) {
                cues += Cue.Buzz(Haptic.STEP_DONE)
                cues += Cue.Speak("Done.")
            }

            Guidance.SETTLING -> if (previous != Guidance.SETTLING) {
                cues += Cue.Speak("Hold still")
            }

            Guidance.SEEKING -> {
                val step = state.step
                // Nobody has managed this position for a while: say the instruction again, rather
                // than keep correcting someone who may not have understood it.
                if (step != null && previous != Guidance.HOLDING &&
                    nowSeconds - lastInstructionAt >= REANNOUNCE_SECONDS
                ) {
                    announce(step, nowSeconds, cues)
                    lastGuidance = state.guidance
                    return cues
                }
                val phrase = state.correction?.takeIf { step != null }?.let {
                    Phrasing.corrections(
                        it, polarity, side,
                        toleranceDegrees = step!!.toleranceDegrees,
                        seated = step.target.pitchDegrees < SEATED_BELOW_PITCH,
                        practice = practice,
                    ).firstOrNull()
                }
                if (phrase != null) {
                    val lastSpoken = lastCorrectionAt
                    when {
                        // A broken hold is the one moment worth interrupting for.
                        previous == Guidance.HOLDING -> {
                            cues += Cue.Speak("You moved. ${phrase.text}", interrupt = true)
                            lastCorrectionAt = nowSeconds
                        }
                        nowSeconds >= graceUntil &&
                            (lastSpoken == null || nowSeconds - lastSpoken >= CORRECTION_REPEAT_SECONDS) -> {
                            cues += Cue.Speak(phrase.text)
                            lastCorrectionAt = nowSeconds
                        }
                    }
                }
            }

            Guidance.FINISHED -> Unit
        }

        lastGuidance = state.guidance
        return cues
    }

    /** Speak a step's instruction and give it time to be heard before any correction. */
    private fun announce(step: ManeuverStep, nowSeconds: Double, cues: MutableList<Cue>) {
        val text = step.instruction(side, practice)
        cues += Cue.Speak(text, interrupt = true)
        lastInstructionAt = nowSeconds
        graceUntil = nowSeconds + graceSeconds(text)
        lastCorrectionAt = null
    }

    /** The current step's instruction as it would be spoken, for a "say it again" button. */
    fun instructionFor(step: ManeuverStep): String = step.instruction(side, practice)

    companion object {
        /**
         * How long to wait after an instruction before correcting, scaled to its length.
         *
         * At the slowed speech rate a word takes about half a second, plus a second to act. A flat
         * wait either cuts long instructions off or leaves short ones hanging.
         */
        fun graceSeconds(text: String): Double {
            val words = text.split(' ').count { it.isNotBlank() }
            return maxOf(INSTRUCTION_GRACE_SECONDS, words * SECONDS_PER_WORD + 1.0)
        }

        /** Spoken pace at the app's slowed speech rate. */
        const val SECONDS_PER_WORD = 0.5

        /** Seeking this long since the instruction was last said: say it again. */
        const val REANNOUNCE_SECONDS = 20.0

        /** The least quiet time after any instruction; correcting sooner drowns it out. */
        const val INSTRUCTION_GRACE_SECONDS = 6.0

        /** Minimum gap between repeated corrections. Long enough to act on one before the next. */
        const val CORRECTION_REPEAT_SECONDS = 4.0

        /** Holds at least this long get a "10 seconds" cue. The therapeutic holds are 45. */
        const val LONG_HOLD_SECONDS = 30

        /** A target pitch below this is a sitting position. Sitting is -90; lying is 0 or more. */
        const val SEATED_BELOW_PITCH = -45.0
    }
}
