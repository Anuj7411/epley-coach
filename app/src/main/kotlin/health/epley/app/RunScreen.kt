package health.epley.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import health.epley.core.CuePlanner
import health.epley.core.Guidance
import health.epley.core.HeadAngles
import health.epley.core.Phrasing
import health.epley.core.Side
import kotlin.math.abs
import kotlin.math.ceil

/**
 * The guided run (handoff §4.8 Find, §4.9 Hold), fed by the live engine.
 *
 * The screens are the design's (RunViews); what they show comes from the engine: its targets and
 * tolerances, not the design's placeholder ranges (README §4b says to use the app's thresholds).
 * The direction line is the engine's correction, from [Phrasing] — the same source the voice
 * reads, so the screen and the speaker can never give different instructions.
 */
@Composable
fun RunScreen(
    run: RunUiState,
    onRepeat: () -> Unit,
    onStop: () -> Unit,
) {
    val engineState = run.engineState
    val step = engineState?.step
    val guidance = engineState?.guidance ?: Guidance.SEEKING
    val polarity = run.polarity
    val holding = guidance == Guidance.HOLDING || guidance == Guidance.STEP_COMPLETE
    val position = ((engineState?.stepIndex ?: 0) + 1).coerceIn(1, 5)
    val ear = if (run.side == Side.LEFT) 'L' else 'R'
    val copy = PositionCopies.getValue(position)

    // Live values in the frame the engine judges them in.
    val pose = run.pose
    val liveTurn = pose?.let { p ->
        if (polarity?.towardAffectedSideIsPositive == false) -p.headRotationDegrees else p.headRotationDegrees
    }
    val livePitch = pose?.pitchDegrees

    // Signal lost (README §14): no reading for 5 s while holding. The engine only advances on
    // readings, so the count has already stopped; this says why instead of looking frozen.
    var lastReadingAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(run.pose) { lastReadingAt = System.currentTimeMillis() }
    LaunchedEffect(holding) {
        while (holding) {
            now = System.currentTimeMillis()
            kotlinx.coroutines.delay(500)
        }
    }
    val signalLost = holding && now - lastReadingAt > 5_000

    if (holding) {
        val required = engineState?.holdSecondsRequired ?: 0
        val held = (engineState?.heldSeconds ?: 0.0).coerceAtMost(required.toDouble())
        HoldView(
            HoldModel(
                position = position,
                ear = ear,
                short = mirrorFor(ear, copy.short),
                count = ceil(required - held).toInt().coerceAtLeast(0),
                total = required,
                blocks = holdBlocks(required, held.toFloat()),
                turn = liveTurn?.let(::deg) ?: "--",
                tip = livePitch?.let(::deg) ?: "--",
                practice = run.practice,
                paused = signalLost,
            ),
            onStop = onStop,
        )
        return
    }

    var figurePlaying by remember(position) { mutableStateOf(true) }

    if (step == null) {
        FindView(
            FindModel(position, ear, mirrorFor(ear, copy.title), mirrorFor(ear, copy.sub), mirrorFor(ear, copy.dir),
                "--", "now --", "--", 0f, 0f, 0.5f, practice = run.practice, moved = run.mountMoved),
            figurePlaying, { figurePlaying = !figurePlaying }, onStop,
        )
        return
    }

    // Positions 1, 3 and 4 are led by the turn; 2 and 5 by the tip (§4b).
    val turnLed = position == 1 || position == 3 || position == 4
    val target = if (turnLed) step.target.headRotationDegrees else step.target.pitchDegrees
    val tolerance = step.toleranceDegrees
    val live = if (turnLed) liveTurn else livePitch
    // Wrap-safe, exactly as the engine judges it: a reading a whole turn out must not show in
    // range while the voice is saying move.
    val offset = live?.let { if (turnLed) HeadAngles.shortestDegrees(it - target) else it - target }
    val beyond = offset?.let { if (abs(it) <= tolerance) 0.0 else it - (if (it > 0) tolerance else -tolerance) }

    val phrases = if (engineState.correction != null && polarity != null) {
        Phrasing.corrections(
            engineState.correction!!, polarity, run.side,
            toleranceDegrees = tolerance,
            seated = step.target.pitchDegrees < CuePlanner.SEATED_BELOW_PITCH,
            practice = run.practice,
        )
    } else {
        emptyList()
    }
    val direction = phrases.firstOrNull()?.let { Phrasing.onScreen(it) } ?: mirrorFor(ear, copy.dir)

    val lo = target - tolerance
    val hi = target + tolerance
    val span = if (turnLed) 90.0 else 80.0
    val low = target - span
    val high = target + span
    fun frac(x: Double) = ((x.coerceIn(low, high) - low) / (high - low)).toFloat()

    FindView(
        FindModel(
            position = position,
            ear = ear,
            title = mirrorFor(ear, copy.title),
            // In practice the phone is the head, so the engine's own practice wording is kept.
            sub = if (run.practice) step.instruction(run.side, true) else mirrorFor(ear, copy.sub),
            direction = direction,
            remaining = when {
                beyond == null -> "--"
                beyond == 0.0 -> "In range"
                beyond < 0 -> "+" + abs(beyond).toInt().coerceAtLeast(1) + "°"
                else -> "−" + abs(beyond).toInt().coerceAtLeast(1) + "°"
            },
            now = "now " + (live?.let(::deg) ?: "--"),
            aim = if (turnLed) "${lo.toInt()}–${hi.toInt()}°" else deg(hi) + " to " + deg(lo),
            zoneStart = frac(lo),
            zoneWidth = frac(hi) - frac(lo),
            marker = live?.let { frac(if (turnLed) target + (offset ?: 0.0) else it) } ?: 0.5f,
            practice = run.practice,
            moved = run.mountMoved,
        ),
        figurePlaying = figurePlaying,
        onToggleFigure = { figurePlaying = !figurePlaying },
        onStop = onStop,
    )
}
