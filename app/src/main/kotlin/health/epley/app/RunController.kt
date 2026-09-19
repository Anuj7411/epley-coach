package health.epley.app

import health.epley.core.Cue
import health.epley.core.CuePlanner
import health.epley.core.EngineState
import health.epley.core.Epley
import health.epley.core.Guidance
import health.epley.core.HeadPose
import health.epley.core.ManeuverEngine
import health.epley.core.RotationPolarity
import health.epley.core.SafetyOutcome
import health.epley.core.Side
import health.epley.core.TriageOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Drives [ManeuverEngine] from the sensor stream.
 *
 * Kept out of the engine so the engine stays a pure function of samples and remains testable on
 * the JVM. Everything here is about turning a stream of [TrackerState] into one, which means
 * timekeeping and knowing when to move on.
 */
class RunController(
    /** Where spoken and haptic cues go. The Android side speaks and vibrates; tests collect them. */
    private val onCue: (Cue) -> Unit = {},
) {

    private val _state = MutableStateFlow(RunUiState())
    val state: StateFlow<RunUiState> = _state

    private var engine: ManeuverEngine? = null
    private var lastTimestampNanos: Long = 0L
    private var secondsSinceStepCompleted = 0.0
    private var planner: CuePlanner? = null
    private var runSeconds = 0.0

    /** The calibration the learned direction belongs to. See [TrackerState.calibrationGeneration]. */
    private var polarityGeneration = -1

    /**
     * Record the safety screen's result. Only [SafetyOutcome.Clear] lets a run start, and it is
     * cleared again when the run ends: symptoms change between attacks, so a check passed
     * yesterday says nothing about today.
     */
    fun confirmSafety(outcome: SafetyOutcome) {
        _state.value = _state.value.copy(safety = outcome)
    }

    /**
     * Record what the six-question triage concluded.
     *
     * The affected side comes from here and nowhere else. Asking the user which ear was affected
     * last time is exactly the control arm of the JAMA Neurology 2023 trial, which resolved 42.9%
     * against 72.4% for the questionnaire — recurrences often move. Any new triage discards the
     * learned turn direction, because that was learned toward the old side.
     */
    fun confirmTriage(outcome: TriageOutcome) {
        val side = (outcome as? TriageOutcome.PosteriorCanal)?.side ?: _state.value.side
        _state.value = _state.value.copy(
            triage = outcome,
            side = side,
            polarity = null,
            polarityMessage = null,
        )
    }

    /**
     * Learn which sensor sign corresponds to turning toward the affected side.
     *
     * Called while the user is holding that turn. A turn too small to read a direction from is
     * rejected with an explanation rather than resolved by guessing.
     */
    fun learnPolarity(state: TrackerState) {
        val pose = state.pose
        if (pose == null) {
            _state.value = _state.value.copy(polarityMessage = "No reading yet — wait a moment.")
            return
        }
        val learned = RotationPolarity.learnFrom(pose)
        _state.value = if (learned == null) {
            _state.value.copy(
                polarity = null,
                polarityMessage = "Turn further — at least " +
                    "${RotationPolarity.MIN_LEARNING_TURN_DEGREES.toInt()}° — then tap again.",
            )
        } else {
            polarityGeneration = state.calibrationGeneration
            _state.value.copy(polarity = learned, polarityMessage = null)
        }
    }

    /**
     * Begin a guided run. Does nothing unless [RunUiState.canStart] — a posterior-canal triage and
     * a learned direction. The check lives here and not only on the button, so no other caller can
     * start an Epley the questionnaire ruled out.
     */
    fun start(stillnessThresholdDegPerSec: Double, practice: Boolean = false) {
        if (!_state.value.canStart) return
        val polarity = _state.value.polarity ?: return
        engine = ManeuverEngine(
            steps = Epley.steps(),
            polarity = polarity,
            stillnessThresholdDegPerSec = stillnessThresholdDegPerSec,
        )
        lastTimestampNanos = 0L
        secondsSinceStepCompleted = 0.0
        runSeconds = 0.0
        planner = CuePlanner(polarity, _state.value.side, practice)
        _state.value = _state.value.copy(running = true, engineState = null, practice = practice)
    }

    /**
     * End the run, finished or abandoned.
     *
     * The triage and the learned direction go with it. The next attack may be in the other ear or
     * a different canal, and reusing today's answers is exactly the habit the questionnaire exists
     * to replace.
     */
    /**
     * Say the current position's instruction again, on request.
     *
     * A practice run on hardware found the first hearing is easy to miss. A button that repeats it
     * costs nothing and beats guessing.
     */
    fun repeatInstruction() {
        val step = _state.value.engineState?.step ?: return
        val text = planner?.instructionFor(step) ?: return
        onCue(Cue.Speak(text, interrupt = true))
    }

    fun stop() {
        engine = null
        planner = null
        _state.value = _state.value.copy(
            running = false,
            engineState = null,
            triage = null,
            safety = null,
            polarity = null,
            polarityMessage = null,
        )
    }

    /** Feed one sensor sample into the run. */
    fun onTrackerState(tracker: TrackerState) {
        // The direction is only meaningful against the calibration it was learned under. Moving
        // the phone to the other cheek and recalibrating can invert it, and a stale one would run
        // the whole manoeuvre mirrored toward the healthy ear.
        if (_state.value.polarity != null &&
            (!tracker.isCalibrated || tracker.calibrationGeneration != polarityGeneration)
        ) {
            val side = if (_state.value.side == Side.LEFT) "left" else "right"
            _state.value = _state.value.copy(
                polarity = null,
                polarityMessage = "The phone was recalibrated, so turn toward your $side again and tap.",
            )
        }

        val e = engine ?: return
        val pose = tracker.pose ?: return

        // Elapsed time comes from the sensor's own clock. Using the wall clock would let a busy
        // UI thread or a dropped frame shorten a therapeutic hold.
        val previous = lastTimestampNanos
        lastTimestampNanos = tracker.lastTimestampNanos
        if (previous == 0L) return
        val deltaSeconds = (tracker.lastTimestampNanos - previous) / 1_000_000_000.0
        if (deltaSeconds <= 0.0 || deltaSeconds > MAX_PLAUSIBLE_GAP_SECONDS) return

        val engineState = e.onSample(pose, tracker.angularRateDegPerSec, deltaSeconds)
        runSeconds += deltaSeconds
        planner?.onState(engineState, runSeconds)?.forEach(onCue)

        // Hold on the completed position for a moment before moving on, so the user has time to
        // register that it finished. Counted in sample time like everything else.
        if (engineState.guidance == Guidance.STEP_COMPLETE) {
            secondsSinceStepCompleted += deltaSeconds
            if (secondsSinceStepCompleted >= STEP_COMPLETE_PAUSE_SECONDS) {
                secondsSinceStepCompleted = 0.0
                e.advance()
            }
        } else {
            secondsSinceStepCompleted = 0.0
        }

        _state.value = _state.value.copy(engineState = engineState, pose = pose)
    }

    private companion object {
        /**
         * A gap longer than this is not elapsed time we can credit.
         *
         * The app was backgrounded, or the sensor stalled. Counting it would hand the user a
         * completed hold for time the phone spent asleep.
         */
        const val MAX_PLAUSIBLE_GAP_SECONDS = 0.5

        /** How long a finished position stays on screen before the next instruction. */
        const val STEP_COMPLETE_PAUSE_SECONDS = 2.0
    }
}

data class RunUiState(
    val running: Boolean = false,
    val side: Side = Side.LEFT,
    val polarity: RotationPolarity? = null,
    val polarityMessage: String? = null,
    val engineState: EngineState? = null,
    val triage: TriageOutcome? = null,
    /** The safety screen's result for this run. Unskippable, never paywalled. */
    val safety: SafetyOutcome? = null,
    /** The latest head pose during a run, for the live head dials. */
    val pose: HeadPose? = null,
    /** Practice mode: the phone stands in for the head, and the words say so. */
    val practice: Boolean = false,
) {
    /**
     * A run needs all three: a cleared safety check, a posterior-canal triage (the only type the
     * Epley treats), and a learned turn direction.
     */
    val canStart: Boolean
        get() = safety == SafetyOutcome.Clear &&
            triage is TriageOutcome.PosteriorCanal &&
            polarity != null
}
