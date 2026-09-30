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
import androidx.compose.foundation.layout.fillMaxSize
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

/** Wordmark + "coach", the lockup used on Welcome. */
@Composable
private fun WordmarkLockup(colour: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RailWordmark(width = 78.dp, colour = colour)
        Spacer(Modifier.width(Space.m))
        Text(
            "coach",
            style = DsType.cardTitle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),
            color = Ds.muted,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Splash (README §4.1, motion M6)
// ---------------------------------------------------------------------------------------------

/**
 * The in-app splash the system splash hands over to.
 *
 * It exists for one reason: the app has work to do before Home is honest — reading the run
 * history off disk, asking the sensor whether it is there at all. A blank frame would be worse
 * than a held one. It is capped at 1.4 s and calls [onDone] whether or not anything is still
 * loading, so it can never become a wall.
 *
 * The reveal runs left→right along the rail, which is the one direction that cannot read as the
 * world rotating. Under reduced motion it shows the finished frame and gets out of the way.
 */
@Composable
fun SplashScreenV2(onDone: () -> Unit) {
    val c = Ds
    val reduced = LocalReducedMotion.current
    val ground = if (c.night) c.ground else c.lilac
    val mark = if (c.night) c.ink else Color(0xFF17161C)

    val reveal = remember { Animatable(if (reduced) 1f else 0f) }
    val line = remember { Animatable(if (reduced) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!reduced) {
            line.animateTo(1f, tween(Motion.BG * 3, easing = StandardEase))
        }
    }
    LaunchedEffect(Unit) {
        if (reduced) {
            delay(400)
        } else {
            reveal.animateTo(1f, tween(700, easing = Emphasized))
            delay(700)
        }
        onDone()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(ground)
            .safeDrawingPadding()
            .padding(Space.l),
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RailWordmark(width = 208.dp, colour = mark, reveal = reveal.value)
            Spacer(Modifier.height(Space.l))
            Text(
                "coach",
                style = DsType.action.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                color = mark,
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Space.xxxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.width(120.dp), contentAlignment = Alignment.CenterStart) {
                Box(
                    Modifier
                        .width(120.dp * line.value)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(mark),
                )
            }
            Spacer(Modifier.height(Space.l))
            Text("NOT A MEDICAL DEVICE", style = DsType.caps, color = mark)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Welcome (README §4.2) — first launch only
// ---------------------------------------------------------------------------------------------

/**
 * Shown once, on the first launch, and never again.
 *
 * It says three things before anyone starts: there is a safety check, the app picks the ear, and
 * the phone measures the angles. Those are the three facts that stop someone treating the wrong
 * side or skipping the red-flag questions, and they are cheaper to read here than mid-manoeuvre.
 */
@Composable
fun WelcomeScreenV2(onGetStarted: () -> Unit) {
    val c = Ds
    val viewport = LocalViewportHeight.current
    val figure = (viewport * 0.30f).coerceIn(132.dp, 208.dp)

    DsScreen(
        top = {
            Box(Modifier.padding(vertical = Space.m).enter(0)) {
                WordmarkLockup(c.ink)
            }
        },
        bottom = {
            Text(
                "Not a medical device. An unregulated prototype.\n" +
                    "It must never be used on a patient.",
                style = DsType.label.copy(fontSize = 15.sp, lineHeight = 20.sp),
                color = c.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = Space.xs),
            )
            DsButton("Get started", onGetStarted)
        },
    ) {
        DsCard(modifier = Modifier.enter(1), radius = Radius.hero) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                HeadAngleFigure(figure)
            }
            Spacer(Modifier.height(Space.m))
            Text(
                "The Epley manoeuvre, one position at a time.",
                style = DsType.display,
                color = c.ink,
            )
        }
        DsCard(modifier = Modifier.enter(2), padding = Space.l) {
            WelcomeRow("A safety check before every run") { ShieldGlyph(it) }
            WelcomeRow("Six questions find the ear") { EarGlyph(it) }
            WelcomeRow("Your phone checks each angle") { AngleGlyph(it) }
        }
    }
}

@Composable
private fun WelcomeRow(label: String, glyph: @Composable (Color) -> Unit) {
    val c = Ds
    Row(
        Modifier.fillMaxWidth().padding(vertical = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.l),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(Radius.tile))
                .background(c.surface2),
            contentAlignment = Alignment.Center,
        ) {
            glyph(c.ink)
        }
        Text(
            label,
            style = DsType.cardTitle.copy(fontSize = 18.sp, lineHeight = 22.sp),
            color = c.ink,
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Glyphs. material-icons-core has no shield, ear or head-angle, and pulling the extended set in
// for three drawings would cost more than it is worth. Drawn, like the Home tiles.
// ---------------------------------------------------------------------------------------------

@Composable
private fun ShieldGlyph(colour: Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width
        val path = Path().apply {
            moveTo(w * 0.5f, w * 0.06f)
            lineTo(w * 0.90f, w * 0.22f)
            lineTo(w * 0.90f, w * 0.52f)
            cubicTo(w * 0.90f, w * 0.78f, w * 0.72f, w * 0.90f, w * 0.5f, w * 0.96f)
            cubicTo(w * 0.28f, w * 0.90f, w * 0.10f, w * 0.78f, w * 0.10f, w * 0.52f)
            lineTo(w * 0.10f, w * 0.22f)
            close()
        }
        drawPath(path, colour, style = Stroke(width = w * 0.09f))
        val arm = w * 0.15f
        drawLine(colour, Offset(w * 0.5f, w * 0.5f - arm), Offset(w * 0.5f, w * 0.5f + arm),
            strokeWidth = w * 0.09f, cap = StrokeCap.Round)
        drawLine(colour, Offset(w * 0.5f - arm, w * 0.5f), Offset(w * 0.5f + arm, w * 0.5f),
            strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
private fun EarGlyph(colour: Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        // Outer helix.
        val outer = Path().apply {
            moveTo(w * 0.24f, w * 0.44f)
            cubicTo(w * 0.24f, w * 0.12f, w * 0.80f, w * 0.10f, w * 0.80f, w * 0.44f)
            cubicTo(w * 0.80f, w * 0.68f, w * 0.56f, w * 0.68f, w * 0.54f, w * 0.86f)
            cubicTo(w * 0.53f, w * 0.96f, w * 0.40f, w * 0.98f, w * 0.34f, w * 0.90f)
        }
        drawPath(outer, colour, style = stroke)
        // Inner turn.
        val inner = Path().apply {
            moveTo(w * 0.42f, w * 0.46f)
            cubicTo(w * 0.42f, w * 0.32f, w * 0.62f, w * 0.32f, w * 0.62f, w * 0.46f)
            cubicTo(w * 0.62f, w * 0.56f, w * 0.52f, w * 0.56f, w * 0.50f, w * 0.64f)
        }
        drawPath(inner, colour, style = stroke)
    }
}

@Composable
private fun AngleGlyph(colour: Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = size.width
        val stroke = w * 0.09f
        // A phone tilted, with an arrow at each end: the angle it reads, not a rotating circle.
        rotate(-28f) {
            drawRoundRect(
                color = colour,
                topLeft = Offset(w * 0.34f, w * 0.18f),
                size = Size(w * 0.32f, w * 0.64f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.09f),
                style = Stroke(width = stroke),
            )
        }
        val head = w * 0.13f
        // Upper-right arrowhead.
        drawLine(colour, Offset(w * 0.80f, w * 0.20f), Offset(w * 0.80f - head, w * 0.20f),
            strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(colour, Offset(w * 0.80f, w * 0.20f), Offset(w * 0.80f, w * 0.20f + head),
            strokeWidth = stroke, cap = StrokeCap.Round)
        // Lower-left arrowhead.
        drawLine(colour, Offset(w * 0.20f, w * 0.80f), Offset(w * 0.20f + head, w * 0.80f),
            strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(colour, Offset(w * 0.20f, w * 0.80f), Offset(w * 0.20f, w * 0.80f - head),
            strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

// ---------------------------------------------------------------------------------------------
// The Welcome illustration
// ---------------------------------------------------------------------------------------------

/**
 * Position 1 seen from above: a head turned 45° with the phone on the cheekbone.
 *
 * The handoff renders this from a 3D model. This is the same drawing held still — which is what
 * the handoff itself specifies for every held frame (M12), and what reduced motion shows in all
 * cases. It is a diagram, not decoration: the dashed line is the target, the plain line is
 * straight ahead, and the angle between them is the one the app measures.
 */
@Composable
private fun HeadAngleFigure(side: Dp) {
    val c = Ds
    val guide = if (c.night) c.butter else Color(0xFF17161C)
    val skin = Color(0xFFC9BCFF)
    val shirt = Color(0xFF9A89E0)
    val contour = if (c.night) Color(0xFF6F6790) else Color(0xFF55535E)

    Box(Modifier.size(side), contentAlignment = Alignment.TopStart) {
        Canvas(
            Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription =
                        "A head seen from above, turned 45 degrees, with a phone held against the cheek."
                },
        ) {
            val s = size.width
            val head = Offset(s * 0.50f, s * 0.58f)

            // Shoulders, faded out by a radial gradient rather than a blur (which needs API 31)
            // so there is no crop line against the card, as in the handoff's shirt shader.
            drawOval(
                Brush.radialGradient(
                    0f to shirt.copy(alpha = 0.62f),
                    0.55f to shirt.copy(alpha = 0.42f),
                    1f to shirt.copy(alpha = 0f),
                    center = Offset(s * 0.50f, s * 0.66f),
                    radius = s * 0.52f,
                ),
                topLeft = Offset(s * 0.00f, s * 0.40f),
                size = Size(s * 1.00f, s * 0.52f),
            )

            // The head, tipped slightly so it reads as turned rather than face-on.
            rotate(-20f, pivot = head) {
                drawOval(
                    skin,
                    topLeft = Offset(head.x - s * 0.20f, head.y - s * 0.24f),
                    size = Size(s * 0.40f, s * 0.48f),
                )
                drawOval(
                    contour,
                    topLeft = Offset(head.x - s * 0.20f, head.y - s * 0.24f),
                    size = Size(s * 0.40f, s * 0.48f),
                    style = Stroke(width = s * 0.010f),
                )
                // The ear the phone sits over.
                drawOval(
                    skin,
                    topLeft = Offset(head.x - s * 0.235f, head.y - s * 0.10f),
                    size = Size(s * 0.07f, s * 0.11f),
                )
                drawOval(
                    contour,
                    topLeft = Offset(head.x - s * 0.235f, head.y - s * 0.10f),
                    size = Size(s * 0.07f, s * 0.11f),
                    style = Stroke(width = s * 0.010f),
                )
            }

            // Straight ahead.
            drawLine(
                contour,
                Offset(head.x, head.y),
                Offset(head.x, s * 0.16f),
                strokeWidth = s * 0.012f,
            )

            // The target, 45° from straight ahead, toward the treated ear.
            val reach = s * 0.40f
            val rad = Math.toRadians(45.0)
            val tip = Offset(
                head.x + (reach * Math.sin(rad)).toFloat(),
                head.y - (reach * Math.cos(rad)).toFloat(),
            )
            drawLine(
                guide,
                head,
                tip,
                strokeWidth = s * 0.016f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(s * 0.035f, s * 0.030f)),
            )

            // The angle between them.
            val r = s * 0.26f
            drawArc(
                color = guide,
                startAngle = -90f,
                sweepAngle = 45f,
                useCenter = false,
                topLeft = Offset(head.x - r, head.y - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = s * 0.014f, cap = StrokeCap.Round),
            )

            // The phone, flat against the cheek, along the target line.
            drawPhone(head, rad, s, c.night)
        }

        // The reading, as a pill — drawn in Compose so it uses the app's own type.
        Box(
            Modifier
                .offset(x = side * 0.545f, y = side * 0.105f)
                .clip(RoundedCornerShape(50))
                .background(if (c.night) c.butter else Color(0xFF17161C))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                "45°",
                style = DsType.caps,
                color = if (c.night) Color(0xFF17161C) else Color.White,
            )
        }
    }
}

/** The phone: ink body, mint screen, lying along the target line at the cheekbone. */
private fun DrawScope.drawPhone(head: Offset, targetRadians: Double, s: Float, night: Boolean) {
    val body = if (night) Color(0xFFF1F0F5) else Color(0xFF17161C)
    val screen = Color(0xFFA8EBD6)
    val distance = s * 0.190f
    val centre = Offset(
        head.x + (distance * Math.sin(targetRadians)).toFloat(),
        head.y - (distance * Math.cos(targetRadians)).toFloat(),
    )
    val w = s * 0.072f
    val h = s * 0.200f
    // The phone lies flat on the cheek, so its long edge runs across the target line.
    rotate(45f, pivot = centre) {
        drawRoundRect(
            color = body,
            topLeft = Offset(centre.x - w / 2f, centre.y - h / 2f),
            size = Size(w, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.32f),
        )
        drawRoundRect(
            color = screen,
            topLeft = Offset(centre.x - w * 0.30f, centre.y - h * 0.40f),
            size = Size(w * 0.60f, h * 0.80f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.18f),
        )
    }
}
