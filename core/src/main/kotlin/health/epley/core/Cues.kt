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
) {

    private var lastStepIndex: Int? = null
    private var lastGuidance: Guidance? = null
    private var stepStartedAt = 0.0
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
            stepStartedAt = nowSeconds
            lastCorrectionAt = null
            tenSecondsAnnounced = false
            state.step?.let { cues += Cue.Speak(it.spoken, interrupt = true) }
        }
        val previous = if (newStep) null else lastGuidance
        val remaining = ceil(state.holdSecondsRequired - state.heldSeconds).toInt().coerceAtLeast(0)

        when (state.guidance) {
            Guidance.HOLDING -> {
                if (previous != Guidance.HOLDING) {
                    cues += Cue.Buzz(Haptic.IN_POSITION)
                    cues += Cue.Speak("Good. Hold still for $remaining seconds.")
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
                val phrase = state.correction?.takeIf { step != null }?.let {
                    Phrasing.corrections(
                        it, polarity, side,
                        toleranceDegrees = step!!.toleranceDegrees,
                        seated = step.target.pitchDegrees < SEATED_BELOW_PITCH,
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
                        nowSeconds - stepStartedAt >= INSTRUCTION_GRACE_SECONDS &&
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

    companion object {
        /**
         * Quiet time after a new position is announced. The instructions run to around five
         * seconds when spoken; correcting before they finish drowns them out.
         */
        const val INSTRUCTION_GRACE_SECONDS = 6.0

        /** Minimum gap between repeated corrections. Long enough to act on one before the next. */
        const val CORRECTION_REPEAT_SECONDS = 4.0

        /** Holds at least this long get a "10 seconds" cue. The therapeutic holds are 45. */
        const val LONG_HOLD_SECONDS = 30

        /** A target pitch below this is a sitting position. Sitting is -90; lying is 0 or more. */
        const val SEATED_BELOW_PITCH = -45.0
    }
}
