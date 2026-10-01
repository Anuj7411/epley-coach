package health.epley.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import health.epley.core.MountMode
import health.epley.core.ReversalCheck
import kotlin.math.abs

/*
 * The app's own pages, from the v2.1 handoff (README §16 D, E, F, I): Settings and About, Check
 * sensors, the accuracy self-check, and the sensor-unavailable state. Each is a stateless view over
 * a small model — the app fills it from the live sensor, the parity tests from the design's samples.
 * Settings, Check sensors and Accuracy scroll, with no flex region.
 */

// ---------------------------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun BackRow(onBack: () -> Unit) {
    val c = Ds
    Row(Modifier.enter(0), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).label("Back").pressable(onClick = onBack).box(c.surface, 24.dp), contentAlignment = Alignment.Center) {
            Sym("arrow_back", 24f, c.ink)
        }
    }
}

@Composable
private fun PageTitle(title: String, sub: String?) {
    val c = Ds
    Column(
        Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 16.dp).enter(1),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Txt(title, type(34f, 800, lineHeight = 1.05f, letterSpacing = -0.03f, wrap = Wrap.Balance), c.ink)
        if (sub != null) Txt(sub, type(17f, 500, lineHeight = 1.45f, wrap = Wrap.Pretty), c.muted)
    }
}

@Composable
private fun Divider() {
    val c = Ds
    Box(Modifier.fillMaxWidth().height(1.dp).background(if (c.night) c.surface2 else c.ground))
}

@Composable
private fun Tile48(icon: String, fill: Boolean = false) {
    val c = Ds
    Box(Modifier.size(48.dp).box(if (c.night) c.surface2 else c.ground, 16.dp), contentAlignment = Alignment.Center) {
        Sym(icon, 24f, c.ink, fill = fill)
    }
}

/** A 72 dp row: icon tile, title and optional subtitle, optional chevron. */
@Composable
private fun NavRow(icon: String, title: String, sub: String?, chevron: Boolean = true, iconFill: Boolean = false, onClick: (() -> Unit)?) {
    val c = Ds
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Tile48(icon, fill = iconFill)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(title, type(17f, 700), c.ink)
            if (sub != null) Txt(sub, type(16f, 600, lineHeight = 1.35f, wrap = Wrap.Pretty), c.muted)
        }
        if (chevron) Sym("chevron_right", 24f, c.muted)
    }
}

/** Coral warning card with an optional caps title, as the design's warning family. */
@Composable
private fun WarnCard(title: String?, text: String) {
    val c = Ds
    val n = c.night
    Row(
        Modifier.box(if (n) c.coralTint else c.coral, 24.dp, ring = if (n) c.coral else null).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Sym("warning", 24f, if (n) c.coral else Ink, fill = true)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (title != null) Txt(title, type(14f, 800, lineHeight = 1.3f, letterSpacing = 0.06f), if (n) c.coral else Ink)
            Txt(text, type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), if (n) c.ink else Ink)
        }
    }
}

@Composable
private fun RingNote(text: String) {
    val c = Ds
    Row(
        Modifier.box(Color.Transparent, 24.dp, ring = c.line, ringWidth = 1.5.dp).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Sym("info", 24f, c.muted)
        Txt(text, type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), c.ink, modifier = Modifier.weight(1f))
    }
}

/** Still (mint, check) or Moving (coral, vibration), as on Check sensors and Accuracy. */
@Composable
private fun StillChip(moving: Boolean) {
    val c = Ds
    val n = c.night
    val bg = if (moving) c.coral else if (n) c.mintTint else c.mint
    val fg = if (n && !moving) c.mint else Ink
    Row(
        Modifier.box(bg, 999.dp).padding(start = 8.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Sym(if (moving) "vibration" else "check", 20f, fg, weight = 700)
        Txt(if (moving) "Moving" else "Still", type(16f, 700), fg, maxLines = 1)
    }
}

@Composable
private fun Primary(label: String, onClick: () -> Unit, enabled: Boolean = true, icon: String? = null) {
    val c = Ds
    val n = c.night
    if (enabled) {
        PillButton(label, onClick, fill = if (n) c.lilac else Ink, content = if (n) Ink else Color.White, icon = icon)
    } else {
        // Disabled (§16): line fill, muted text, no press effect.
        Row(
            Modifier.heightIn(min = 64.dp).box(c.line, 32.dp).semantics { disabled() },
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) Sym(icon, 28f, c.muted, weight = 700)
            Txt(label, type(20f, 700), c.muted)
        }
    }
}

@Composable
private fun Ghost(label: String, onClick: () -> Unit) {
    val c = Ds
    PillButton(label, onClick, fill = Color.Transparent, content = c.ink, ring = c.ink)
}

// ---------------------------------------------------------------------------------------------
// Settings and About (§16 D). The model credit is required by its CC BY 4.0 licence.
// ---------------------------------------------------------------------------------------------

@Composable
fun SettingsScreen(onRuns: () -> Unit, onSensors: () -> Unit, onBack: () -> Unit) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        BackRow(onBack)
        Txt(
            "Settings", type(44f, 800, lineHeight = 1f, letterSpacing = -0.035f), c.ink,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 16.dp).enter(1),
        )
        Column(Modifier.enter(2).box(c.surface, 28.dp).padding(horizontal = 16.dp)) {
            NavRow("history", "Your runs", "Every run, and the PDF for your doctor", onClick = onRuns)
            Divider()
            NavRow("sensors", "Check sensors", "See the angles the app measures", onClick = onSensors)
        }
        Column(Modifier.enter(3).box(c.surface, 28.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RailWordmark(width = (28f * 683f / 244f).dp, colour = c.ink)
                Txt("coach", type(17f, 700, lineHeight = 1f), c.muted, modifier = Modifier.padding(bottom = 2.dp))
            }
            Txt(
                "Guides the Epley manoeuvre for posterior canal BPPV, one position at a time, measuring each head angle with the phone’s sensor.",
                type(17f, 500, lineHeight = 1.45f, wrap = Wrap.Pretty), c.ink,
            )
            Column {
                Divider()
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Credit("Triage questions", "Kim HJ et al., Neurology 2020.")
                    Credit("After-care", "AAO-HNS Clinical Practice Guideline: BPPV, 2017.")
                    Credit("Head model", "“Human head” by ADAMA on Sketchfab, CC BY 4.0.")
                    Credit("Type", "Bricolage Grotesque and Material Symbols.")
                }
            }
        }
        Row(
            Modifier.enter(4).box(if (n) c.coralTint else c.coral, 28.dp, ring = if (n) c.coral else null).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(48.dp).box(if (n) c.coral else Color.White.copy(alpha = 0.6f), 16.dp), contentAlignment = Alignment.Center) {
                Sym("warning", 24f, Ink)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt("NOT A MEDICAL DEVICE", type(14f, 800, lineHeight = 1.3f, letterSpacing = 0.06f), if (n) c.coral else Ink)
                Txt("An unregulated prototype. It must never be used on a patient.", type(16f, 600, lineHeight = 1.35f, wrap = Wrap.Pretty), if (n) c.ink else Ink)
            }
        }
    }
}

@Composable
private fun Credit(what: String, source: String) {
    val c = Ds
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Txt(what, type(14f, 700), c.muted)
        Txt(source, type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), c.ink)
    }
}

// ---------------------------------------------------------------------------------------------
// Check sensors (§16 E)
// ---------------------------------------------------------------------------------------------

data class SensorsModel(
    val rate: String,
    val moving: Boolean,
    val mode: MountMode,
    val tip: String,
    val turn: String,
    val turnWarning: String?,
    val moved: Boolean,
    val calibrated: Boolean,
    val drift: String?,
    val logging: Boolean,
    val raw: String?,
)

@Composable
fun CheckSensorsScreen(
    tracker: HeadTracker,
    onAccuracyCheck: () -> Unit,
    onBack: () -> Unit,
    onToggleLogging: () -> Boolean,
    isLogging: Boolean,
    logDirectory: String,
) {
    val state by tracker.state.collectAsState()
    var logging by remember { mutableStateOf(isLogging) }
    var message by remember { mutableStateOf<String?>(null) }
    // Open by default, as the design shows it; the row folds it away.
    var details by remember { mutableStateOf(true) }
    val pose = state.pose
    val q = state.quaternion
    fun signed(s: String) = s.replace('-', '−')
    SensorsView(
        SensorsModel(
            rate = "%.1f °/s".format(state.angularRateDegPerSec),
            moving = !state.isStill,
            mode = state.mode,
            tip = if (state.isCalibrated) pose?.pitchDegrees?.let(::deg) ?: "--" else "--",
            turn = if (state.isCalibrated) pose?.headRotationDegrees?.let(::deg) ?: "--" else "--",
            turnWarning = if (pose?.isRotationReliable == false) "Swung ${pose.swingDegrees.toInt()}° from where it was calibrated." else null,
            moved = state.isJolted,
            calibrated = state.isCalibrated,
            drift = when {
                !state.isCalibrated -> null
                state.residualChecks == 0 ->
                    "Back in the pose you calibrated in, tap above: the truth there is zero, so what it reads is the drift."
                else -> "Drift: last %.1f°, worst %.1f° over %d check%s. Tolerance ±%.0f°.".format(
                    state.lastResidualDegrees ?: 0.0, state.worstResidualDegrees, state.residualChecks,
                    if (state.residualChecks == 1) "" else "s", state.mode.toleranceDegrees,
                )
            },
            logging = logging,
            raw = if (!details) null else buildString {
                if (q != null) {
                    appendLine(signed("q  %.3f  %.3f  %.3f  %.3f".format(q.x, q.y, q.z, q.w)))
                    val roll = Math.toDegrees(kotlin.math.atan2(2 * (q.w * q.x + q.y * q.z), 1 - 2 * (q.x * q.x + q.y * q.y)))
                    val yaw = Math.toDegrees(kotlin.math.atan2(2 * (q.w * q.z + q.x * q.y), 1 - 2 * (q.y * q.y + q.z * q.z)))
                    appendLine(signed("pitch %.1f   roll %.1f   yaw %.1f".format(state.devicePitchDegrees, roll, yaw)))
                }
                append("rate %.1f °/s   acc %d   t %s".format(
                    state.angularRateDegPerSec, state.sensorAccuracy,
                    java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.ROOT).format(java.util.Date()),
                ))
                if (logging) append("\n$logDirectory")
            },
        ),
        message = message,
        onBack = onBack,
        onMode = { tracker.setMode(it); message = null },
        onCalibrate = {
            message = when (tracker.calibrate()) {
                CalibrationResult.OK -> null
                CalibrationResult.NO_SAMPLES -> "No sensor reading yet. Wait a second and try again."
                CalibrationResult.PHONE_TOO_FLAT -> "The phone is lying too flat to tell which way you’re facing. Hold it on its edge, as described, and try again."
            }
        },
        onDrift = { tracker.recordUprightCheck() },
        onAccuracy = onAccuracyCheck,
        onLog = { logging = onToggleLogging() },
        onRaw = { details = !details },
    )
}

@Composable
fun SensorsView(
    m: SensorsModel,
    message: String? = null,
    onBack: () -> Unit = {},
    onMode: (MountMode) -> Unit = {},
    onCalibrate: () -> Unit = {},
    onDrift: () -> Unit = {},
    onAccuracy: () -> Unit = {},
    onLog: () -> Unit = {},
    onRaw: () -> Unit = {},
) {
    val c = Ds
    DScreen(bg = c.ground) {
        BackRow(onBack)
        PageTitle("What the phone sees", "Hold it the way you would for a run, calibrate, then move. These are the angles the app guides by.")
        Row(
            Modifier.enter(2).box(c.surface, 28.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Tile48("sensors")
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Txt("Orientation sensor", type(17f, 700), c.ink)
                Txt(m.rate, type(16f, 600, tnum = true), c.muted, maxLines = 1)
            }
            StillChip(m.moving)
        }
        Column(Modifier.enter(3).box(c.surface, 28.dp).padding(horizontal = 16.dp, vertical = 4.dp)) {
            MountMode.entries.forEachIndexed { i, mode ->
                if (i > 0) Divider()
                val on = mode == m.mode
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).pressable { onMode(mode) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Sym(if (on) "radio_button_checked" else "radio_button_unchecked", 26f, if (on) c.ink else c.muted, fill = on)
                    Txt(mountLabel(mode), type(17f, 700), c.ink, modifier = Modifier.weight(1f))
                    Txt("±${mode.toleranceDegrees.toInt()}°", type(16f, 700, tnum = true), c.muted)
                }
            }
        }
        if (!m.mode.tracksTheHead) RingNote("Practice only. This reads the phone, not your head. Nothing here is a treatment.")
        Row(Modifier.enter(4), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Reading("Tip", m.tip, Modifier.weight(1f))
            Reading("Turn", m.turn, Modifier.weight(1f))
        }
        if (m.turnWarning != null) WarnCard("TURN UNRELIABLE", m.turnWarning)
        if (m.moved) WarnCard("THE PHONE MOVED", "It turned faster than a neck can.")
        if (message != null) WarnCard(null, message)
        Primary(if (m.calibrated) "Calibrate again" else "Calibrate", onCalibrate)
        if (m.calibrated) {
            Ghost("Back upright — check drift", onDrift)
            if (m.drift != null) {
                Txt(m.drift, type(16f, 600, lineHeight = 1.4f, tnum = true, align = TextAlign.Center), c.muted,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp))
            }
        }
        Column(Modifier.box(c.surface, 28.dp).padding(horizontal = 16.dp)) {
            NavRow("verified", "Accuracy self-check", "Test this phone on a flat surface", onClick = onAccuracy)
            Divider()
            NavRow("fiber_manual_record", if (m.logging) "Stop recording" else "Record a log", null, chevron = false, iconFill = true, onClick = onLog)
            Divider()
            Row(
                Modifier.fillMaxWidth().heightIn(min = 72.dp).pressable(onClick = onRaw),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Tile48("data_object")
                Txt("Raw readings", type(17f, 700), c.ink, modifier = Modifier.weight(1f))
                Sym("expand_more", 24f, c.muted)
            }
            if (m.raw != null) {
                Box(Modifier.padding(bottom = 16.dp).box(if (c.night) c.surface2 else c.ground, 16.dp).padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Txt(m.raw, type(14f, 400, lineHeight = 1.5f, tnum = true).copy(fontFamily = FontFamily.Monospace), c.ink)
                }
            }
        }
    }
}

private fun mountLabel(mode: MountMode) = when (mode) {
    MountMode.CHEEK -> "Against your cheek"
    MountMode.HEADBAND -> "In a headband or cap"
    else -> "In your hand (practice)"
}

@Composable
private fun Reading(label: String, value: String, modifier: Modifier) {
    val c = Ds
    Column(modifier.box(c.surface, 28.dp).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt(label, type(16f, 600), c.muted, maxLines = 1)
        Txt(value, type(56f, 800, lineHeight = 0.9f, letterSpacing = -0.045f, tnum = true), c.ink, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// Accuracy self-check (§16 F)
// ---------------------------------------------------------------------------------------------

enum class AccuracyStage { Start, After1, TurnedOver, NotTurned, Passed, Failed }

data class AccuracyModel(
    val live: String,
    val moving: Boolean,
    val reading1: String,
    val reading2: String,
    val stage: AccuracyStage,
    val sensorError: String = "",
    val surfaceTilt: String = "",
    val correctBy: String = "",
    val correctEnabled: Boolean = false,
)

/**
 * The app measuring its own error with no reference instrument: two readings 180° apart on any
 * flat surface. The surface's tilt flips sign, the sensor's error does not, so half the difference
 * is the surface and half the sum is the sensor — the reversal technique for checking a precision
 * level without a master. Maths and tolerance live in [ReversalCheck].
 */
@Composable
fun AccuracyCheckScreen(
    devicePitchDegrees: Double,
    screenFacingUp: Boolean,
    isStill: Boolean,
    rotationTravelledDegrees: Double,
    storedOffsetDegrees: Double,
    onSaveOffset: (health.epley.core.ReversalResult) -> Unit,
    onBack: () -> Unit,
) {
    var first by remember { mutableStateOf<Double?>(null) }
    var second by remember { mutableStateOf<Double?>(null) }
    var firstFacedUp by remember { mutableStateOf(true) }
    var secondFacedUp by remember { mutableStateOf(true) }
    var rotationAtFirst by remember { mutableStateOf(0.0) }
    var turnedDegrees by remember { mutableStateOf(0.0) }
    val a = first
    val b = second
    // Two ways the measurement is invalid rather than the sensor bad; both found on hardware,
    // where a flip produced an impossible 10 degree "sensor error".
    val flipped = a != null && b != null && firstFacedUp != secondFacedUp
    val unturned = a != null && b != null && !ReversalCheck.turnedEnough(turnedDegrees)
    val result = if (a != null && b != null && !flipped && !unturned) ReversalCheck.analyse(a, b) else null
    fun fmt(v: Double) = "%+.2f°".format(v).replace('-', '−')
    val stage = when {
        a == null -> AccuracyStage.Start
        b == null -> AccuracyStage.After1
        flipped -> AccuracyStage.TurnedOver
        unturned -> AccuracyStage.NotTurned
        result != null && result.isWithinTolerance -> AccuracyStage.Passed
        else -> AccuracyStage.Failed
    }
    val takeSecond = {
        second = devicePitchDegrees
        secondFacedUp = screenFacingUp
        turnedDegrees = rotationTravelledDegrees - rotationAtFirst
    }
    AccuracyView(
        AccuracyModel(
            live = fmt(devicePitchDegrees),
            moving = !isStill,
            reading1 = a?.let(::fmt) ?: "—",
            reading2 = b?.let(::fmt) ?: "—",
            stage = stage,
            sensorError = result?.let { "%.2f°".format(abs(it.sensorErrorDegrees)) } ?: "",
            surfaceTilt = result?.let { "%.1f°".format(abs(it.surfaceTiltDegrees)) } ?: "",
            correctBy = result?.let { "%+.2f°".format(-it.sensorErrorDegrees).replace('-', '−') } ?: "",
            // Offered whenever there is a result and the phone is still (§16 F: disabled while moving).
            correctEnabled = result != null && isStill,
        ),
        onBack = onBack,
        onReading = {
            when (stage) {
                AccuracyStage.Start -> {
                    first = devicePitchDegrees
                    firstFacedUp = screenFacingUp
                    rotationAtFirst = rotationTravelledDegrees
                }
                // Reading 2, or a retake of it after a flip or a turn that was too small.
                AccuracyStage.After1, AccuracyStage.TurnedOver, AccuracyStage.NotTurned -> takeSecond()
                else -> { first = null; second = null }
            }
        },
        onCorrect = { result?.let { onSaveOffset(it); first = null; second = null } },
        onRestart = { first = null; second = null },
        storedOffset = if (storedOffsetDegrees != 0.0) "%+.2f°".format(-storedOffsetDegrees).replace('-', '−') else null,
    )
}

@Composable
fun AccuracyView(
    m: AccuracyModel,
    onBack: () -> Unit = {},
    onReading: () -> Unit = {},
    onCorrect: () -> Unit = {},
    onRestart: () -> Unit = {},
    storedOffset: String? = null,
) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        BackRow(onBack)
        PageTitle(
            "How accurate is this phone?",
            "Put it flat on any surface — it doesn’t need to be level. Take a reading, spin the phone 180° on the spot, and take another.",
        )
        if (storedOffset != null) RingNote("This phone is already corrected by $storedOffset. Readings include that.")
        Column(Modifier.enter(2).box(c.surface, 32.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Txt(m.live, type(72f, 800, lineHeight = 0.85f, letterSpacing = -0.05f, tnum = true), c.ink, modifier = Modifier.weight(1f), maxLines = 1)
                StillChip(m.moving)
            }
            Column {
                Divider()
                ReadingRow("Reading 1", m.reading1)
                Divider()
                ReadingRow("Reading 2", m.reading2)
            }
        }
        when (m.stage) {
            AccuracyStage.After1 -> RingNote("Now spin the phone 180° flat on the surface, like turning a plate…")
            AccuracyStage.TurnedOver -> WarnCard("TURNED OVER", "Keep the screen facing up. Spin it flat, don’t flip it.")
            AccuracyStage.NotTurned -> WarnCard("NOT TURNED", "It hasn’t been spun 180° yet. Turn it like a plate, then take reading 2.")
            AccuracyStage.Passed, AccuracyStage.Failed -> {
                val pass = m.stage == AccuracyStage.Passed
                Column(
                    Modifier.box(
                        if (pass) (if (n) c.mintTint else c.mint) else (if (n) c.coralTint else c.coral), 28.dp,
                        ring = if (!n) null else if (pass) c.mint else c.coral,
                    ).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val chipBg = if (n) (if (pass) c.mint else c.coral) else Ink
                    val chipFg = if (n) Ink else if (pass) c.mint else c.coral
                    Row(
                        Modifier.box(chipBg, 999.dp).padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Sym(if (pass) "check_circle" else "error", 20f, chipFg, weight = 700)
                        Txt(if (pass) "Passed" else "Outside tolerance", type(16f, 700), chipFg, maxLines = 1)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ResultCell("Sensor error", m.sensorError, Modifier.weight(1f))
                        ResultCell("Surface tilt", m.surfaceTilt, Modifier.weight(1f))
                    }
                }
            }
            AccuracyStage.Start -> Unit
        }
        Spacer(Modifier.flex())
        when (m.stage) {
            AccuracyStage.Start -> Primary("Take reading 1", onReading, enabled = !m.moving)
            AccuracyStage.After1, AccuracyStage.TurnedOver, AccuracyStage.NotTurned -> Primary("Take reading 2", onReading, enabled = !m.moving)
            AccuracyStage.Passed, AccuracyStage.Failed -> {
                Primary("Correct this phone by ${m.correctBy}", onCorrect, enabled = m.correctEnabled)
                Ghost("Start again", onRestart)
            }
        }
    }
}

@Composable
private fun ReadingRow(label: String, value: String) {
    val c = Ds
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Txt(label, type(17f, 600), c.muted, modifier = Modifier.weight(1f))
        Txt(value, type(20f, 800, tnum = true), c.ink, maxLines = 1)
    }
}

@Composable
private fun ResultCell(label: String, value: String, modifier: Modifier) {
    val c = Ds
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Txt(label, type(16f, 600), if (c.night) c.soft else Ink)
        Txt(value, type(28f, 800, tnum = true), if (c.night) c.ink else Ink, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// Sensor unavailable (§16 I)
// ---------------------------------------------------------------------------------------------

/** No orientation sensor: every angle in the app comes from one, so it says so rather than pretend. */
@Composable
fun SensorUnavailableScreen(onHome: () -> Unit = {}) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        Column(
            // Day rings it in ink; night in coral (§16 I).
            Modifier.flex().enter(0).box(c.surface, 32.dp, ring = if (n) c.coral else c.ink).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier.box(Color.Transparent, 999.dp, ring = if (n) c.coral else c.ink).padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("sensors_off", 20f, if (n) c.coral else c.ink, weight = 700)
                Txt("No sensor", type(16f, 700), if (n) c.coral else c.ink, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Txt("This phone can’t measure head angles", type(44f, 800, lineHeight = 1f, letterSpacing = -0.035f, wrap = Wrap.Balance), c.ink)
            Txt(
                "Your phone’s motion sensor isn’t responding. Restart the app, or try another phone.",
                type(17f, 500, lineHeight = 1.45f, wrap = Wrap.Pretty), if (n) c.soft else Ink,
            )
        }
        PillButton("Back to home", onHome, fill = if (n) c.lilac else Ink, content = if (n) Ink else Color.White)
    }
}
