package health.epley.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The visual system from docs/DESIGN.md, in one place. Every value there has a source; change it
 * there first, then here.
 */
object Palette {
    /** Material dark theme: pure black under off-white text causes halation. */
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1E1E1E)
    val Raised = Color(0xFF2A2A2A)
    val TextPrimary = Color(0xFFE6E6E6)
    val TextSecondary = Color(0xFFB3B3B3)
    val Outline = Color(0xFF6F6F6F)

    /** Okabe-Ito sky blue: the one action colour, and "in position". */
    val Action = Color(0xFF56B4E9)
    val OnAction = Color(0xFF0B1A24)

    /** A dark tint of [Action] for a whole card that is in position. */
    val ActionTint = Color(0xFF0F2C40)
    val ActionTrack = Color(0xFF1C4560)

    /** Okabe-Ito orange: "move". Told apart from blue by every common colour-vision deficiency. */
    val Move = Color(0xFFE69F00)

    /** Okabe-Ito vermillion: emergency only. */
    val Danger = Color(0xFFD55E00)
}

@Composable
fun EpleyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Action,
            onPrimary = Palette.OnAction,
            background = Palette.Background,
            onBackground = Palette.TextPrimary,
            surface = Palette.Background,
            onSurface = Palette.TextPrimary,
            outline = Palette.Outline,
        ),
        content = content,
    )
}

private val ButtonShape = RoundedCornerShape(16.dp)

/** The one main action on a screen. At least 56 dp tall; sentence case. */
@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        modifier = modifier.fillMaxWidth().heightIn(min = 60.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Palette.Action,
            contentColor = Palette.OnAction,
            disabledContainerColor = Palette.Raised,
            disabledContentColor = Palette.TextSecondary,
        ),
    ) { Text(label, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
}

/** Secondary actions: outlined, same height, so they are just as easy to hit. */
@Composable
fun SecondaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = ButtonShape,
        border = BorderStroke(1.dp, Palette.Outline),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Palette.TextPrimary),
    ) { Text(label, fontSize = 17.sp) }
}

/** A large answer choice, for one-question-at-a-time screens. */
@Composable
fun ChoiceButton(label: String, onClick: () -> Unit, container: Color = Palette.Surface) {
    Button(
        onClick = onClick,
        shape = ButtonShape,
        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Palette.TextPrimary),
    ) { Text(label, fontSize = 18.sp) }
}
