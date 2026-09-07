package health.epley.app

import health.epley.core.EngineState
import health.epley.core.Epley
import health.epley.core.Guidance
import health.epley.core.ManeuverEngine
import health.epley.core.RotationPolarity
import health.epley.core.Side
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Drives [ManeuverEngine] from the sensor stream.
 *
 * Kept out of the engine so the engine stays a pure function of samples and remains testable on
 * the JVM. Everything here is about turning a stream of [TrackerState] into one, which means
 * timekeeping and knowing when to move on.
 */
class RunController {

    private val _state = MutableStateFlow(RunUiState())
    val state: StateFlow<RunUiState> = _state

    private var engine: ManeuverEngine? = null
    private var lastTimestampNanos: Long = 0L
    private var secondsSinceStepCompleted = 0.0

    /** Record which side the user reports as affected. Their report, not a diagnosis. */
    fun setSide(side: Side) {
        _state.value = _state.value.copy(side = side, polarity = null, polarityMessage = null)
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
            _state.value.copy(polarity = learned, polarityMessage = null)
        }
    }

    /** Begin a guided run. Does nothing without a learned direction. */
    fun start(toleranceDegrees: Double, stillnessThresholdDegPerSec: Double) {
        val polarity = _state.value.polarity ?: return
        engine = ManeuverEngine(
            steps = Epley.steps(),
            polarity = polarity,
            toleranceDegrees = toleranceDegrees,
            stillnessThresholdDegPerSec = stillnessThresholdDegPerSec,
        )
        lastTimestampNanos = 0L
        secondsSinceStepCompleted = 0.0
        _state.value = _state.value.copy(running = true, engineState = null)
    }

    fun stop() {
        engine = null
        _state.value = _state.value.copy(running = false, engineState = null)
    }

    /** Feed one sensor sample into the run. */
    fun onTrackerState(tracker: TrackerState) {
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

        _state.value = _state.value.copy(engineState = engineState)
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
) {
    val canStart: Boolean get() = polarity != null
}
