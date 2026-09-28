package health.epley.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Theme A, "warm dark", from the design handoff.
 *
 * ## Why these values and not the previous ones
 *
 * The first system was picked for how it looked on a desk. This one is picked for a person who is
 * dizzy, often past sixty, lying in a dark room at three in the morning, reading at arm's length.
 * Three rules drive every token below:
 *
 * 1. **Warm dark, low glare.** Off-black and off-white, never pure black or pure white, because
 *    pure black under pure white causes halation for exactly the low-vision readers most likely to
 *    need this.
 * 2. **Every state carries four signals**: a word, an icon, a shape and a colour, in that order of
 *    reliability. Colour is always the last of the four and never the only one, because red-green
 *    colour blindness affects roughly one man in twelve.
 * 3. **Every text colour reaches at least 7:1 against Ground**, which is AAA rather than AA. The
 *    audience is the reason.
 */
object Palette {

    /** Screen background. */
    val Ground = Color(0xFF121311)

    /** Cards, list rows, the back button. */
    val Surface = Color(0xFF1C1D1A)

    /** The fill of a selected option. */
    val SurfaceSelected = Color(0xFF221F2E)

    /** The fill behind the Stop button, so it reads as destructive before the text is read. */
    val StopFill = Color(0xFF2A1C19)

    /** Hairlines inside cards. */
    val Divider = Color(0xFF2A2B27)

    /** Gauge track, inactive progress segment. */
    val Track = Color(0xFF34352F)

    /** Secondary button border. */
    val Outline = Color(0xFF4A4B45)

    /** Primary text, and the fill of the primary button. */
    val Ink = Color(0xFFF3F1EA)

    /** Long body text, set slightly softer than [Ink] to reduce glare over paragraphs. */
    val InkBody = Color(0xFFD9D6CD)

    /** Secondary text, unselected radio. */
    val InkMuted = Color(0xFFB9B6AC)

    /** Tick labels and meta. Only ever used at 14sp or larger. */
    val InkFaint = Color(0xFF9C998F)

    /** Searching for the position. Always paired with an arrow icon and a dashed outline. */
    val Seeking = Color(0xFFFFC857)

    /** Holding, done, completed. Always paired with a check icon and a solid fill. */
    val Holding = Color(0xFF7AD9E0)

    /** Stop, emergency, a run that was stopped. Always paired with a cross or warning icon. */
    val Urgent = Color(0xFFFF8F7A)

    /** Selection and links. */
    val Selected = Color(0xFFC4B5FF)

    // Names the older screens still use. They resolve to the tokens above so the app keeps
    // building while each screen is moved across, and they go away once it is done.
    val Background get() = Ground
    val Raised get() = Track
    val TextPrimary get() = Ink
    val TextSecondary get() = InkMuted
    val Action get() = Holding
    val OnAction get() = Ground
    val ActionTint get() = Color(0xFF16302F)
    val ActionTrack get() = Divider
    val Move get() = Seeking
    val Danger get() = Urgent
}

/**
 * Atkinson Hyperlegible, and a mono cut for live numbers.
 *
 * Drawn by the Braille Institute for low-vision readers: open counters, and 1 / l / I and 0 / O
 * that cannot be confused. That is a functional choice for this audience rather than an aesthetic
 * one. The mono cut is used only for values that change while you watch them, so the digits keep
 * their positions instead of jittering as the sensor moves.
 */
object AppType {
    val Sans: FontFamily = FontFamily(
        Font(R.font.atkinson_regular, FontWeight.Normal),
        Font(R.font.atkinson_bold, FontWeight.Bold),
        Font(R.font.atkinson_extrabold, FontWeight.ExtraBold),
    )

    val Mono: FontFamily = FontFamily(
        Font(R.font.atkinson_mono_medium, FontWeight.Medium),
        Font(R.font.atkinson_mono_bold, FontWeight.Bold),
    )

    /** Screen hero. */
    val DisplaySize = 34.sp
    val DisplayLine = 40.sp

    /** The one-word answer on a result screen. */
    val HeroSize = 48.sp

    /** A question, or the instruction during a run. */
    val TitleSize = 26.sp
    val TitleLine = 32.sp
    val RunTitleSize = 28.sp
    val RunTitleLine = 34.sp

    /** Card titles. */
    val SubtitleSize = 22.sp

    /** Long body text. */
    val BodyLargeSize = 18.sp
    val BodyLargeLine = 26.sp
    val BodySize = 17.sp
    val BodyLine = 24.sp

    /** Anything that labels something else. */
    val LabelSize = 16.sp
    val LabelLine = 22.sp

    /**
     * The floor. Nothing meant to be read is smaller than this, and 14sp is allowed only for the
     * tick labels under a gauge, which are reference marks rather than reading.
     */
    val ReadingFloor = 16.sp
    val TickSize = 14.sp
}

@Composable
fun EpleyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Ink,
            onPrimary = Palette.Ground,
            background = Palette.Ground,
            onBackground = Palette.Ink,
            surface = Palette.Surface,
            onSurface = Palette.Ink,
            outline = Palette.Outline,
            error = Palette.Urgent,
        ),
        content = content,
    )
}

/** Pill, the full width, 64dp. Everything primary is in the thumb zone at the bottom. */
@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(percent = 50),
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Ink,
            contentColor = Palette.Ground,
            disabledContainerColor = Palette.Ink.copy(alpha = 0.38f),
            disabledContentColor = Palette.Ground,
        ),
    ) { Text(label, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = AppType.Sans) }
}

/** The same pill, hollow, 56dp. A secondary action is still a full-width target. */
@Composable
fun SecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(1.5.dp, Palette.Outline),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.Ink),
    ) { Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppType.Sans) }
}

/**
 * The answer that stops the flow, such as "Yes, I have one of these".
 *
 * Outlined rather than filled, and never the same shape as the safe answer, so the dangerous
 * choice cannot be hit by muscle memory in the dark.
 */
@Composable
fun DangerOutlineButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(2.dp, Palette.Urgent),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.Urgent),
    ) { Text(label, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = AppType.Sans) }
}

/**
 * Stop, present on every run screen, in the thumb zone.
 *
 * Filled dark red rather than outlined like the other secondary actions, because during a run it
 * is the only control and it has to be findable without reading. It is also the one button a
 * person might reach for while genuinely unwell.
 */
@Composable
fun StopButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(2.dp, Palette.Urgent),
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Palette.StopFill, contentColor = Palette.Urgent),
    ) {
        Text("Stop", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = AppType.Sans)
    }
}

/** A large answer choice, for one-question-at-a-time screens. */
@Composable
fun ChoiceButton(label: String, onClick: () -> Unit, container: Color = Palette.Surface) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Palette.Ink),
    ) { Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = AppType.Sans) }
}
