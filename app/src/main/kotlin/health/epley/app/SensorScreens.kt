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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import health.epley.core.MountMode
import health.epley.core.ReversalCheck
import kotlin.math.abs

/*
 * The app's own pages the design never drew — Check sensors, the accuracy self-check, Settings and
 * About, and the "sensor unavailable" state (README §14) — built from the same parts as the design
 * screens, so nothing in the app changes language.
 */

// ---------------------------------------------------------------------------------------------
// Shared pieces
// ---------------------------------------------------------------------------------------------

@Composable
private fun PageHeader(caption: String, onBack: () -> Unit) {
    val c = Ds
    Row(Modifier.enter(0), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.size(48.dp).pressable(onClick = onBack).box(c.surface, 24.dp), contentAlignment = Alignment.Center) {
            Sym("arrow_back", 24f, c.ink)
        }
        Txt(caption, type(16f, 600), c.muted, maxLines = 1)
    }
}

@Composable
private fun PageTitle(title: String, sub: String?, index: Int = 1) {
    val c = Ds
    Column(
        Modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 8.dp).enter(index),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Txt(title, type(34f, 800, lineHeight = 1.05f, letterSpacing = -0.03f, wrap = Wrap.Balance), c.ink)
        if (sub != null) Txt(sub, type(17f, 500, lineHeight = 1.45f, wrap = Wrap.Pretty), c.muted)
    }
}

@Composable
private fun Note(text: String, icon: String = "info") {
    val c = Ds
    Row(
        Modifier.box(Color.Transparent, 24.dp, ring = c.line, ringWidth = 1.5.dp).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Sym(icon, 24f, c.muted)
        Txt(text, type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty), c.ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Warning(caption: String, text: String) {
    val c = Ds
    val n = c.night
    Row(
        Modifier.box(if (n) c.coralTint else c.coral, 28.dp, ring = if (n) c.coral else null).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(48.dp).box(if (n) c.coral else Color.White.copy(alpha = 0.6f), 16.dp), contentAlignment = Alignment.Center) {
            Sym("warning", 24f, Ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Txt(caption, type(14f, 800, letterSpacing = 0.06f), if (n) c.coral else Ink)
            Txt(text, type(16f, 600, lineHeight = 1.35f, wrap = Wrap.Pretty), if (n) c.ink else Ink)
        }
    }
}

/** A tappable row in a white card: icon tile, title, subtitle, chevron. */
@Composable
private fun NavRow(icon: String, title: String, sub: String?, onClick: () -> Unit) {
    val c = Ds
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp).pressable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(48.dp).box(if (c.night) c.surface2 else c.ground, 16.dp), contentAlignment = Alignment.Center) {
            Sym(icon, 24f, c.ink)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Txt(title, type(17f, 700), c.ink)
            if (sub != null) Txt(sub, type(16f, 600, lineHeight = 1.3f), c.muted)
        }
        Sym("chevron_right", 24f, c.muted)
    }
}

@Composable
private fun Divider() {
    val c = Ds
    Box(Modifier.fillMaxWidth().height(1.dp).background(if (c.night) c.surface2 else c.ground))
}

@Composable
private fun SecondaryPill(label: String, onClick: () -> Unit, icon: String? = null) {
    val c = Ds
    PillButton(label, onClick, fill = c.surface, content = c.ink, icon = icon)
}

@Composable
private fun PrimaryPill(label: String, onClick: () -> Unit, icon: String? = null, enabled: Boolean = true) {
    val c = Ds
    val n = c.night
    PillButton(
        label, onClick,
        fill = if (!enabled) c.surface2 else if (n) c.lilac else Ink,
        content = if (!enabled) c.muted else if (n) Ink else Color.White,
        icon = icon, enabled = enabled,
    )
}

// ---------------------------------------------------------------------------------------------
// Check sensors
// ---------------------------------------------------------------------------------------------

/**
 * What the phone's orientation sensor sees, in the terms the run uses: tip and turn, relative to a
 * calibration, plus the one honest measurement of whether a mount held — return to the calibration
 * pose, where the truth is zero, and read the error.
 */
@Composable
fun CheckSensorsScreen(
    tracker: HeadTracker,
    onAccuracyCheck: () -> Unit,
    onBack: () -> Unit,
    onToggleLogging: () -> Boolean,
    isLogging: Boolean,
    logDirectory: String,
) {
    val c = Ds
    val n = c.night
    val state by tracker.state.collectAsState()
    var logging by remember { mutableStateOf(isLogging) }
    var message by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf(false) }

    DScreen(bg = c.ground) {
        PageHeader("Check sensors", onBack)
        PageTitle(
            "What the phone sees",
            "Hold it the way you would for a run, calibrate, then move. These are the angles the app guides by.",
        )

        // Sensor and stillness
        Row(
            Modifier.enter(2).box(c.surface, 28.dp).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(48.dp).box(
                    if (tracker.isSupported) (if (n) c.mintTint else c.mint) else (if (n) c.coralTint else c.coral), 16.dp,
                ),
                contentAlignment = Alignment.Center,
            ) { Sym("sensors", 24f, if (!n) Ink else if (tracker.isSupported) c.mint else c.coral) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Txt(if (tracker.isSupported) "Orientation sensor" else "No orientation sensor", type(17f, 700), c.ink, maxLines = 1)
                Txt("%.0f°/s".format(state.angularRateDegPerSec), type(16f, 600, tnum = true), c.muted, maxLines = 1)
            }
            Row(
                Modifier
                    .box(if (state.isStill) (if (n) c.mint else Ink) else Color.Transparent, 999.dp, ring = if (state.isStill) null else c.butter)
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val fg = if (state.isStill) (if (n) Ink else c.mint) else (if (n) c.butter else Ink)
                Sym(if (state.isStill) "check_circle" else "explore", 20f, fg, fill = state.isStill, weight = 700)
                Txt(if (state.isStill) "Still" else "Moving", type(16f, 700), fg, maxLines = 1)
            }
        }

        // How it is held
        Column(Modifier.enter(3).box(c.surface, 28.dp).padding(horizontal = 16.dp, vertical = 4.dp)) {
            MountMode.entries.forEachIndexed { i, mode ->
                if (i > 0) Divider()
                val selected = mode == state.mode
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).pressable {
                        tracker.setMode(mode)
                        message = null
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Sym(if (selected) "radio_button_checked" else "radio_button_unchecked", 24f, if (selected) c.ink else c.muted, fill = selected)
                    Txt(mode.displayName, type(17f, if (selected) 700 else 600), c.ink, modifier = Modifier.weight(1f))
                    Txt("±${mode.toleranceDegrees.toInt()}°", type(16f, 600, tnum = true), c.muted)
                }
            }
        }
        if (!state.mode.tracksTheHead) {
            Note("Practice only. This reads the phone, not your head. Nothing here is a treatment.", icon = "back_hand")
        }

        // Live angles
        Row(Modifier.enter(4), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Reading("Tip", state.pose?.pitchDegrees, Modifier.weight(1f))
            Reading("Turn", state.pose?.headRotationDegrees, Modifier.weight(1f))
        }
        if (state.pose?.isRotationReliable == false) {
            Warning("TURN UNRELIABLE", "The phone has swung ${state.pose?.swingDegrees?.toInt()}° from where it was calibrated. Recalibrate.")
        }
        if (state.isJolted) {
            Warning(
                "THE PHONE MOVED",
                "It turned faster than a neck can (${state.peakRateDegPerSec.toInt()}°/s). Recalibrate before trusting these numbers.",
            )
        }
        message?.let { Warning("TRY AGAIN", it) }
        if (!state.isCalibrated) Note("Get into position, hold still, then calibrate.")

        PrimaryPill(
            if (state.isCalibrated) "Calibrate again" else "Calibrate",
            onClick = {
                message = when (tracker.calibrate()) {
                    CalibrationResult.OK -> null
                    CalibrationResult.NO_SAMPLES -> "No sensor reading yet. Wait a second and try again."
                    CalibrationResult.PHONE_TOO_FLAT ->
                        "The phone is lying too flat to tell which way you're facing. Hold it on its edge, as described, and try again."
                }
            },
        )
        if (state.isCalibrated) {
            SecondaryPill("Back upright — check drift", { tracker.recordUprightCheck() })
            Note(
                if (state.residualChecks == 0) {
                    "Return to the pose you calibrated in and tap above: the truth there is zero, so whatever it reads is how far the mount has drifted."
                } else {
                    "Drift: last %.1f°, worst %.1f° over %d check%s. Tolerance ±%.0f°.".format(
                        state.lastResidualDegrees ?: 0.0, state.worstResidualDegrees, state.residualChecks,
                        if (state.residualChecks == 1) "" else "s", state.mode.toleranceDegrees,
                    )
                },
                icon = if (state.residualChecks > 0 && state.worstResidualDegrees <= state.mode.toleranceDegrees / 2) "check_circle" else "info",
            )
        }

        Column(Modifier.box(c.surface, 28.dp).padding(horizontal = 16.dp, vertical = 4.dp)) {
            NavRow("check", "Accuracy self-check", "Measure this phone's own error on any flat surface", onAccuracyCheck)
            Divider()
            NavRow(
                if (logging) "pause" else "play_arrow",
                if (logging) "Stop recording" else "Record a log",
                if (logging) "Writing to $logDirectory" else "Every sample, for checking afterwards",
            ) { logging = onToggleLogging() }
            Divider()
            NavRow("info", if (details) "Hide raw readings" else "Raw readings", "The sensor's own numbers") { details = !details }
        }
        if (details) {
            val q = state.quaternion
            Box(Modifier.box(c.surface, 28.dp).padding(16.dp)) {
                Txt(
                    buildString {
                        appendLine(tracker.sensorName)
                        if (q != null) appendLine("q  %+.3f %+.3f %+.3f %+.3f".format(q.x, q.y, q.z, q.w))
                        appendLine("fused %+.2f  gravity %+.2f".format(state.devicePitchDegrees, state.gravityTiltDegrees))
                        appendLine("accel %+.2f  |a| %.3f".format(state.accelTiltDegrees, state.accelMagnitude))
                        append("samples ${state.sampleCount}  since calib ${state.samplesSinceCalibration}  accuracy ${state.sensorAccuracy}")
                    },
                    type(14f, 600, lineHeight = 1.4f).copy(fontFamily = FontFamily.Monospace),
                    c.muted,
                )
            }
        }
    }
}

@Composable
private fun Reading(label: String, degrees: Double?, modifier: Modifier) {
    val c = Ds
    Column(modifier.box(c.surface, 28.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Txt(label, type(16f, 600), c.muted, maxLines = 1)
        Txt(degrees?.let(::deg) ?: "--", type(44f, 800, lineHeight = 1f, letterSpacing = -0.035f, tnum = true), c.ink, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------
// Accuracy self-check
// ---------------------------------------------------------------------------------------------

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
    val c = Ds
    val n = c.night
    var first by remember { mutableStateOf<Double?>(null) }
    var second by remember { mutableStateOf<Double?>(null) }
    var firstFacedUp by remember { mutableStateOf(true) }
    var secondFacedUp by remember { mutableStateOf(true) }
    var rotationAtFirst by remember { mutableStateOf(0.0) }
    var turnedDegrees by remember { mutableStateOf(0.0) }

    val a = first
    val b = second
    // Two ways the measurement can be invalid rather than the sensor being bad; both were found on
    // hardware, where a flip produced an impossible 10 degree "sensor error".
    val flipped = a != null && b != null && firstFacedUp != secondFacedUp
    val unturned = a != null && b != null && !ReversalCheck.turnedEnough(turnedDegrees)
    val result = if (a != null && b != null && !flipped && !unturned) ReversalCheck.analyse(a, b) else null
    val restart = { first = null; second = null }

    DScreen(bg = c.ground) {
        PageHeader("Accuracy self-check", onBack)
        PageTitle(
            "How accurate is this phone?",
            "Put it flat on any surface — it doesn’t need to be level. Take a reading, spin the phone 180° on the spot, and take another.",
        )
        if (storedOffsetDegrees != 0.0) {
            Note("This phone is already corrected by %+.2f°. Readings below include that.".format(-storedOffsetDegrees))
        }
        Column(Modifier.enter(2).box(c.surface, 28.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt("Live", type(16f, 600), c.muted, modifier = Modifier.weight(1f))
                Txt(if (isStill) "Still" else "Moving", type(16f, 700), if (isStill) c.ink else (if (n) c.butter else c.muted))
            }
            Txt("%+.2f°".format(devicePitchDegrees), type(56f, 800, lineHeight = 0.9f, letterSpacing = -0.045f, tnum = true), c.ink, maxLines = 1)
            if (a != null) Txt("Reading 1   %+.2f°".format(a), type(17f, 600, tnum = true), c.muted)
            if (b != null) Txt("Reading 2   %+.2f°".format(b), type(17f, 600, tnum = true), c.muted)
        }
        if (a != null && b == null) {
            Note("Now spin the phone 180° flat on the surface, like turning a plate, so the top edge points the other way. Keep the screen facing up.")
        }
        if (flipped || unturned) {
            Warning(
                if (flipped) "TURNED OVER" else "NOT TURNED",
                if (flipped) "Reading 2 was taken face-down, so the two can’t be compared. Keep the screen up and spin it round."
                else "It turned only %.0f° between readings. A reversal needs a half turn.".format(turnedDegrees),
            )
        }
        if (result != null) {
            val pass = result.isWithinTolerance
            Column(
                Modifier.box(if (pass) (if (n) c.mintTint else c.mint) else (if (n) c.coralTint else c.coral), 28.dp,
                    ring = if (n) (if (pass) c.mint else c.coral) else null).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Txt(if (pass) "Passed" else "Outside tolerance", type(34f, 800, lineHeight = 1.05f, letterSpacing = -0.03f), if (n) c.ink else Ink)
                Txt("Sensor error  %+.2f°".format(result.sensorErrorDegrees), type(20f, 700, tnum = true), if (n) c.ink else Ink)
                Txt("Surface tilt  %+.2f°".format(result.surfaceTiltDegrees), type(17f, 600, tnum = true), if (n) c.soft else Ink)
                Txt(
                    "Passing means the sensor’s own error is under ${ReversalCheck.MAX_ERROR_DEGREES}°, half of what the app promises." +
                        if (pass) "" else " A fixed error like this doesn’t affect the manoeuvre: every angle is measured against the calibration on your head, so it cancels.",
                    type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty),
                    if (n) c.soft else Ink,
                )
            }
            if (abs(result.surfaceTiltDegrees) > 1.0) {
                Note("For a known angle: fold paper corner-to-edge for exactly 45°, rest the phone on it and repeat. The tilt should read 45° plus your surface’s.")
            }
        }
        Spacer(Modifier.flex())
        when {
            result != null && !result.isWithinTolerance -> {
                PrimaryPill("Correct this phone by %+.2f°".format(-result.sensorErrorDegrees), { onSaveOffset(result); restart() })
                SecondaryPill("Start again", restart)
            }
            a != null && b != null -> SecondaryPill("Start again", restart)
            a == null -> PrimaryPill("Take reading 1", {
                first = devicePitchDegrees; firstFacedUp = screenFacingUp; rotationAtFirst = rotationTravelledDegrees
            }, enabled = isStill)
            else -> PrimaryPill("Take reading 2", {
                second = devicePitchDegrees; secondFacedUp = screenFacingUp; turnedDegrees = rotationTravelledDegrees - rotationAtFirst
            }, enabled = isStill)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Settings and About (README §5: the model credit lives in Settings › About)
// ---------------------------------------------------------------------------------------------

@Composable
fun SettingsScreen(onRuns: () -> Unit, onSensors: () -> Unit, onBack: () -> Unit) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        PageHeader("Settings", onBack)
        Txt(
            "Settings",
            type(44f, 800, lineHeight = 1f, letterSpacing = -0.035f),
            c.ink,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 16.dp).enter(1),
        )
        Column(Modifier.enter(2).box(c.surface, 28.dp).padding(horizontal = 16.dp, vertical = 4.dp)) {
            NavRow("hourglass_top", "Your runs", "Every run, and the PDF for your doctor", onRuns)
            Divider()
            NavRow("sensors", "Check sensors", "See the angles the app measures", onSensors)
        }
        Txt("About", type(16f, 600), c.muted, modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp).enter(3))
        Column(
            Modifier.enter(3).box(c.surface, 28.dp).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RailWordmark(width = (28f * 683f / 244f).dp, colour = c.ink)
                Txt("coach", type(17f, 700, lineHeight = 1f), c.muted, modifier = Modifier.padding(bottom = 2.dp))
            }
            Txt(
                "Guides the Epley manoeuvre for posterior canal BPPV, one position at a time, measuring each head angle with the phone’s sensor.",
                type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty),
                c.ink,
            )
            Divider()
            Credit("Triage questions", "Kim HJ et al., Neurology 2020.")
            Credit("After-care", "AAO-HNS Clinical Practice Guideline: BPPV, 2017.")
            // Required by the model's licence (CC BY 4.0).
            Credit("Head model", "“Human head” by ADAMA on Sketchfab, CC BY 4.0.")
            Credit("Type", "Bricolage Grotesque and Material Symbols, SIL OFL / Apache 2.0.")
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
                Txt("NOT A MEDICAL DEVICE", type(14f, 800, letterSpacing = 0.06f), if (n) c.coral else Ink)
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
        Txt(source, type(16f, 600, lineHeight = 1.35f), c.ink)
    }
}

// ---------------------------------------------------------------------------------------------
// Sensor unavailable (README §14)
// ---------------------------------------------------------------------------------------------

/** No orientation sensor: the whole app depends on one, so it says so rather than pretend. */
@Composable
fun SensorUnavailableScreen() {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        Column(
            Modifier.flex().enter(0).box(if (n) c.coralTint else c.coral, 32.dp, ring = if (n) c.coral else null).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                Modifier.box(if (n) c.coral else Ink, 999.dp).padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("sensors", 20f, if (n) Ink else c.coral, weight = 700)
                Txt("No sensor", type(16f, 700), if (n) Ink else c.coral, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Txt("This phone can’t measure head angles", type(44f, 800, lineHeight = 1f, letterSpacing = -0.035f, wrap = Wrap.Balance), c.ink)
            Txt(
                "Your phone’s motion sensor isn’t responding, and every angle in this app comes from it. Nothing would be measured, so it won’t pretend to guide you. Restart the app, or try another phone.",
                type(17f, 600, lineHeight = 1.45f, wrap = Wrap.Pretty),
                if (n) c.soft else Ink,
            )
        }
    }
}
