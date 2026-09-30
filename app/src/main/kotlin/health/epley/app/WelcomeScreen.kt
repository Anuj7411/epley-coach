package health.epley.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// The wordmark (README §5): "epley" on its level line. Splash and Welcome only — never on Home
// or on a run screen, where the space belongs to the instruction.
// ---------------------------------------------------------------------------------------------

/**
 * The Rail wordmark, drawn from the handoff's own vector rather than a bitmap.
 *
 * `logo-kit/svg/epley-wordmark-ink.svg` is six stroked shapes on a 683×244 box at a stroke width
 * of 22. Keeping it as geometry means it is sharp at any size and takes its colour from the
 * theme, so one drawing serves ink on day and bone on night.
 */
private val WordmarkStrokes = listOf(
    // The "e" and the rail it sits on, which runs the full width of the mark.
    "M48 120H152A52 52 0 1 0 100 172H704",
    "M204 57V230", // p, stem
    "M360 20V172", // l
    "M412 120H516A52 52 0 1 0 500.8 156.8", // the second e
    "M568 57V120A52 52 0 0 0 672 120M672 57V186A40 40 0 0 1 632 226H608", // y
)

private const val MarkWidth = 683f
private const val MarkHeight = 244f
private const val MarkLeft = 29f
private const val MarkTop = 1f
private const val MarkStroke = 22f

@Composable
fun RailWordmark(
    width: Dp,
    colour: Color,
    modifier: Modifier = Modifier,
    reveal: Float = 1f,
) {
    val paths: List<Path> = remember {
        WordmarkStrokes.map { PathParser().parsePathString(it).toPath() }
    }
    Canvas(
        modifier
            .width(width)
            .aspectRatio(MarkWidth / MarkHeight)
            .semantics { contentDescription = "epley coach" },
    ) {
        val scale = size.width / MarkWidth
        clipRect(right = size.width * reveal.coerceIn(0f, 1f)) {
            withTransform({
                scale(scale, scale, pivot = Offset.Zero)
                translate(-MarkLeft, -MarkTop)
            }) {
                val stroke = Stroke(width = MarkStroke)
                paths.forEach { drawPath(it, colour, style = stroke) }
                // The bowl of the "p" is a circle in the source, not a path.
                drawCircle(colour, radius = 52f, center = Offset(256f, 120f), style = stroke)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Splash (README §4.1, motion M6). Flex region: the centre block.
// ---------------------------------------------------------------------------------------------

/**
 * The in-app splash the system splash hands over to. Capped at 1.4 s, and it hands over whether
 * or not anything is still loading, so it can never become a wall. The reveal runs left to right
 * along the rail — the one direction that cannot read as the world turning. Reduced motion shows
 * the finished frame.
 */
@Composable
fun SplashScreenV2(onDone: () -> Unit) {
    val c = Ds
    val n = c.night
    val reduced = LocalReducedMotion.current
    val reveal = remember { Animatable(if (reduced) 1f else 0f) }
    val line = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) line.animateTo(1f, tween(1000, easing = StandardEase))
    }
    LaunchedEffect(Unit) {
        if (reduced) delay(400) else {
            reveal.animateTo(1f, tween(700, easing = Emphasized))
            delay(700)
        }
        onDone()
    }
    DScreen(bg = if (n) c.ground else c.lilac) {
        Column(
            Modifier.flex(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
        ) {
            RailWordmark(width = 208.dp, colour = if (n) c.ink else Color(0xFF17161C), reveal = reveal.value)
            Txt(
                "coach",
                style = type(20f, 700, lineHeight = 1f, letterSpacing = 0.01f),
                color = if (n) c.muted else Color(0xFF17161C),
            )
        }
        Column(
            Modifier.padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.width(120.dp).height(4.dp).box(if (n) c.surface2 else Color(0x2917161C), 2.dp)) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(120.dp * line.value)
                        .box(if (n) c.lilac else Color(0xFF17161C), 2.dp),
                )
            }
            Txt(
                "NOT A MEDICAL DEVICE",
                style = type(14f, 700, letterSpacing = 0.06f),
                color = if (n) c.muted else Color(0xFF17161C),
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Welcome (README §4.2) — first launch only. Flex region: the spacer above the notice.
// ---------------------------------------------------------------------------------------------

@Composable
fun WelcomeScreenV2(onGetStarted: () -> Unit, figurePlaying: Boolean = true) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        Row(
            Modifier.heightIn(min = 48.dp).padding(horizontal = 8.dp).enter(0),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RailWordmark(width = (28f * 683f / 244f).dp, colour = c.ink)
                Txt(
                    "coach",
                    style = type(17f, 700, lineHeight = 1f),
                    color = c.muted,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
        Column(
            Modifier.enter(1).box(c.surface, 32.dp).padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FigureView(
                    step = 1, ear = 'R', night = n, playing = figurePlaying, size = 208,
                    description = "Figure: seated, head turned 45° toward the right ear, phone on the right cheek",
                )
            }
            Txt(
                "The Epley manoeuvre, one position at a time.",
                style = type(34f, 800, lineHeight = 1.02f, letterSpacing = -0.03f, wrap = Wrap.Balance),
                color = c.ink,
            )
        }
        Column(Modifier.enter(2).box(c.surface, 28.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
            WelcomeRow("health_and_safety", "A safety check before every run")
            WelcomeRow("hearing", "Six questions find the ear")
            WelcomeRow("screen_rotation_alt", "Your phone checks each angle")
        }
        Spacer(Modifier.flex())
        Txt(
            "Not a medical device. An unregulated prototype. It must never be used on a patient.",
            style = type(14f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty, align = TextAlign.Center),
            color = c.muted,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Box(
            Modifier.heightIn(min = 64.dp).pressable(onClick = onGetStarted).box(if (n) c.lilac else Color(0xFF17161C), 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Txt("Get started", style = type(20f, 700), color = if (n) Color(0xFF17161C) else Color.White)
        }
    }
}

@Composable
private fun WelcomeRow(icon: String, label: String) {
    val c = Ds
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(40.dp).box(if (c.night) c.surface2 else c.ground, 14.dp), contentAlignment = Alignment.Center) {
            Sym(icon, 22f, c.ink)
        }
        Txt(label, style = type(17f, 700, lineHeight = 1.3f), color = c.ink, modifier = Modifier.weight(1f))
    }
}
