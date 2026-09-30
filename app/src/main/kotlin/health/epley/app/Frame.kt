package health.epley.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// README ★P1 — one responsive rule for every screen.
//
// The design is authored on a 390 dp frame. Everything is scaled by s = clamp(W / 390, 0.85,
// 1.25) by scaling the density once, here, so every screen is written with the literal numbers
// from the design. Extra height goes only to the regions each screen marks with Modifier.flex().
// ---------------------------------------------------------------------------------------------

val LocalDesignScale = staticCompositionLocalOf { 1f }

@Composable
fun DesignFrame(content: @Composable () -> Unit) {
    val cfg = LocalConfiguration.current
    val d = LocalDensity.current
    val s = (cfg.screenWidthDp / 390f).coerceIn(0.85f, 1.25f)
    CompositionLocalProvider(
        LocalDensity provides Density(d.density * s, d.fontScale),
        LocalDesignScale provides s,
    ) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}

// ---------------------------------------------------------------------------------------------
// Type (README §3.3, ★P4). Bricolage Grotesque variable, opsz following the font size as Chrome's
// font-optical-sizing: auto does. Measured against the references: Chrome takes opsz from the CSS
// size before the frame's zoom (opsz = design size, not design size × s) — the other reading made
// every heading's strokes visibly thinner.
// ---------------------------------------------------------------------------------------------

private val bricolageCache = HashMap<Int, FontFamily>()

private fun bricolageAt(opsz: Float, weight: Int): FontFamily {
    val o = ((opsz.coerceIn(12f, 96f)) * 4f).roundToInt()   // quarter-point steps
    return bricolageCache.getOrPut(o * 1000 + weight) {
        FontFamily(
            Font(
                resId = R.font.bricolage_grotesque,
                weight = FontWeight(weight),
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(weight),
                    FontVariation.Setting("opsz", o / 4f),
                ),
            ),
        )
    }
}

enum class Wrap { Normal, Balance, Pretty }

/**
 * A text style from the design's CSS: size in px, weight, line-height as a multiplier (CSS
 * `normal` is 1.2 for this font — its line gap is 0), letter-spacing in em.
 */
@Composable
fun type(
    size: Float,
    weight: Int,
    lineHeight: Float = 1.2f,
    letterSpacing: Float = 0f,
    tnum: Boolean = false,
    wrap: Wrap = Wrap.Normal,
    align: TextAlign = TextAlign.Unspecified,
): TextStyle {
    return TextStyle(
        fontFamily = bricolageAt(size, weight),
        fontWeight = FontWeight(weight),
        fontSize = size.sp,
        lineHeight = (size * lineHeight).sp,
        letterSpacing = if (letterSpacing == 0f) TextUnit.Unspecified else letterSpacing.em,
        fontFeatureSettings = if (tnum) "tnum" else null,
        textAlign = align,
        lineBreak = when (wrap) {
            Wrap.Normal -> LineBreak.Simple
            Wrap.Balance -> LineBreak.Heading
            Wrap.Pretty -> LineBreak.Paragraph
        },
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
    )
}

// ---------------------------------------------------------------------------------------------
// Icons (★P4): Material Symbols Rounded, pinned to the design's opsz 24 / wght 500 / GRAD 0,
// FILL kept as the one live axis, subset to the 30 icons the design uses.
// ---------------------------------------------------------------------------------------------

// Declared as weight 500 because that is the only weight the design loads. An icon inside a
// font-weight 600+ element is fake-bolded by Chrome; asking Compose for the same weight makes
// Android fake-bold it the same way (both are Skia's emboldening), so pass the inherited weight.
private val symbolsOutline = FontFamily(
    Font(R.font.material_symbols_rounded, weight = FontWeight(500), variationSettings = FontVariation.Settings(FontVariation.Setting("FILL", 0f))),
)
private val symbolsFilled = FontFamily(
    Font(R.font.material_symbols_rounded, weight = FontWeight(500), variationSettings = FontVariation.Settings(FontVariation.Setting("FILL", 1f))),
)

private val Codepoints = mapOf(
    "arrow_back" to "\uE5C4",
    "arrow_forward" to "\uE5C8",
    "back_hand" to "\uE764",
    "battery_3_bar" to "\uF09E",
    "battery_5_bar" to "\uF0A0",
    "bed" to "\uEFDF",
    "block" to "\uF08C",
    "call" to "\uF0D4",
    "check" to "\uE668",
    "check_circle" to "\uF0BE",
    "chevron_right" to "\uE5CC",
    "close" to "\uE5CD",
    "explore" to "\uE87A",
    "first_page" to "\uE5DC",
    "health_and_safety" to "\uE1D5",
    "hearing" to "\uE023",
    "help" to "\uE8FD",
    "hourglass_top" to "\uEA5B",
    "info" to "\uE88E",
    "ios_share" to "\uE6B8",
    "pause" to "\uE034",
    "play_arrow" to "\uE037",
    "radio_button_checked" to "\uE837",
    "radio_button_unchecked" to "\uE836",
    "screen_rotation_alt" to "\uEBEE",
    "sensors" to "\uE51E",
    "settings" to "\uE8B8",
    "signal_cellular_alt" to "\uE202",
    "stethoscope" to "\uF805",
    "warning" to "\uF083",
)

/**
 * One icon as the design sets it: `font-size: N; line-height: 1` — an N × N box, the glyph's
 * baseline on the box's bottom edge. The font is 1.2 em tall, so 0.1 em comes off the top and the
 * bottom exactly as CSS trims it; without that the glyph sat 0.1 em low and lost its bottom edge.
 *
 * @param weight the font-weight the icon inherits from its element in the design (spec `font`).
 */
@Composable
fun Sym(
    name: String,
    size: Float,
    color: Color,
    modifier: Modifier = Modifier,
    fill: Boolean = false,
    weight: Int = 400,
) {
    val glyph = Codepoints[name] ?: error("Icon '$name' is not in the subset font")
    // Icons are px in the design: they do not follow the system font scale.
    val sp = with(LocalDensity.current) { size.dp.toSp() }
    Text(
        glyph,
        color = color,
        style = TextStyle(
            fontFamily = if (fill) symbolsFilled else symbolsOutline,
            fontWeight = FontWeight(weight),
            fontSize = sp,
            lineHeight = sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        ),
        modifier = modifier.layout { measurable, _ ->
            val box = size.dp.roundToPx()
            val p = measurable.measure(Constraints())
            layout(box, box) { p.place((box - p.width) / 2, (box - p.height) / 2) }
        },
    )
}

// ---------------------------------------------------------------------------------------------
// Boxes: background, radius and the design's inset rings (`box-shadow: 0 0 0 Npx C inset`).
// ---------------------------------------------------------------------------------------------

fun Modifier.box(bg: Color = Color.Transparent, radius: Dp = 0.dp, ring: Color? = null, ringWidth: Dp = 2.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(bg, shape)
        .then(if (ring != null) Modifier.border(ringWidth, ring, shape) else Modifier)
}

// ---------------------------------------------------------------------------------------------
// FlexColumn: CSS `display:flex; flex-direction:column; gap`. Children stretch to the full width
// unless aligned; children marked flex() share whatever height is left — and only them. When the
// content is taller than the space (large font scale), nothing shrinks and the screen scrolls.
// ---------------------------------------------------------------------------------------------

enum class SelfAlign { Stretch, Start, Center, End }

private class FlexData(var weight: Float = 0f, var align: SelfAlign = SelfAlign.Stretch)

private class FlexParentData(val weight: Float?, val align: SelfAlign?) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any {
        val d = parentData as? FlexData ?: FlexData()
        if (weight != null) d.weight = weight
        if (align != null) d.align = align
        return d
    }
}

/** `flex: 1` — this child takes the leftover height. */
fun Modifier.flex(weight: Float = 1f): Modifier = this.then(FlexParentData(weight, null))

/** `align-self` — by default children stretch across. */
fun Modifier.alignSelf(align: SelfAlign): Modifier = this.then(FlexParentData(null, align))

@Composable
fun FlexColumn(modifier: Modifier = Modifier, gap: Dp = 0.dp, content: @Composable () -> Unit) {
    Layout(content, modifier) { measurables, c ->
        val w = c.maxWidth
        val gapPx = gap.roundToPx()
        val data = measurables.map { it.parentData as? FlexData ?: FlexData() }
        fun widthConstraints(a: SelfAlign) =
            if (a == SelfAlign.Stretch && w != Constraints.Infinity) Constraints(minWidth = w, maxWidth = w) else Constraints(maxWidth = w)

        val placeables = arrayOfNulls<Placeable>(measurables.size)
        var used = gapPx * (measurables.size - 1).coerceAtLeast(0)
        measurables.forEachIndexed { i, m ->
            if (data[i].weight == 0f) {
                val p = m.measure(widthConstraints(data[i].align))
                placeables[i] = p
                used += p.height
            }
        }
        val totalWeight = data.sumOf { it.weight.toDouble() }.toFloat()
        val remaining = (c.minHeight - used).coerceAtLeast(0)
        measurables.forEachIndexed { i, m ->
            if (data[i].weight > 0f) {
                val share = (remaining * data[i].weight / totalWeight).roundToInt()
                val intrinsic = m.minIntrinsicHeight(w)
                val h = max(share, intrinsic)
                val wc = widthConstraints(data[i].align)
                placeables[i] = m.measure(Constraints(wc.minWidth, wc.maxWidth, h, h))
            }
        }
        val height = max(c.minHeight, placeables.sumOf { it!!.height } + gapPx * (measurables.size - 1).coerceAtLeast(0))
        layout(if (w == Constraints.Infinity) placeables.maxOf { it!!.width } else w, height) {
            var y = 0
            placeables.forEachIndexed { i, p ->
                val x = when (data[i].align) {
                    SelfAlign.Center -> (w - p!!.width) / 2
                    SelfAlign.End -> w - p!!.width
                    else -> 0
                }
                p!!.place(x, y)
                y += p.height + gapPx
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// The screen: 48 dp status zone, content, 32 dp gesture zone (★P1), drawn edge to edge. The zones
// never shrink below the real system bars, so three-button navigation cannot cover a button.
// ---------------------------------------------------------------------------------------------

@Composable
fun DScreen(
    bg: Color,
    top: Dp = 8.dp,
    horizontal: Dp = 16.dp,
    bottom: Dp = 16.dp,
    gap: Dp = 8.dp,
    content: @Composable () -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val animated by animateColorAsState(
        bg,
        if (reduced) tween(Motion.REDUCED, easing = LinearEasing) else tween(Motion.BG, easing = StandardEase),
        label = "ground",
    )
    val bars = WindowInsets.systemBars.asPaddingValues()
    val topZone = max(48.dp.value, bars.calculateTopPadding().value).dp
    val bottomZone = max(32.dp.value, bars.calculateBottomPadding().value).dp
    BoxWithConstraints(Modifier.fillMaxSize().background(animated)) {
        val viewport = maxHeight - topZone - bottomZone - top - bottom
        Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            CompositionLocalProvider(LocalViewportHeight provides viewport) {
                FlexColumn(
                    Modifier
                        .padding(start = horizontal, end = horizontal, top = topZone + top, bottom = bottomZone + bottom)
                        .heightIn(min = viewport),
                    gap = gap,
                    content = content,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Text with CSS line boxes. When line-height is smaller than the font's own height (1.2 em for
// Bricolage), CSS lets the line box be shorter than the glyphs: half the difference comes off the
// top of the first line and the bottom of the last. Compose keeps the full font height there, which
// pushed every tight heading — and everything under it — down by that amount. Measured on Home:
// 4.4 dp at the top of a 44 px line-height-1 headline, 8.8 dp on the card below it.
// ---------------------------------------------------------------------------------------------

@Composable
fun Txt(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
) {
    val size = style.fontSize
    val line = style.lineHeight
    val trim = if (size.isSp && line.isSp) ((size.value * 1.2f - line.value) / 2f).coerceAtLeast(0f) else 0f
    Text(
        text,
        style = style,
        color = color,
        maxLines = maxLines,
        modifier = if (trim == 0f) modifier else modifier.layout { measurable, constraints ->
            val cut = trim.sp.roundToPx()
            val p = measurable.measure(constraints)
            layout(p.width, (p.height - 2 * cut).coerceAtLeast(0)) { p.place(0, -cut) }
        },
    )
}
