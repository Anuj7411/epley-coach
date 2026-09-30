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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
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

/**
 * The ground under every screen, owned by the frame rather than each screen, so a screen change
 * cross-fades it (README M2: bone to mint on Hold, to coral on Emergency) instead of snapping.
 */
class Ground { var target by mutableStateOf(Color.Unspecified) }

val LocalGround = staticCompositionLocalOf<Ground?> { null }

@Composable
fun DesignFrame(content: @Composable () -> Unit) {
    val cfg = LocalConfiguration.current
    val d = LocalDensity.current
    val s = (cfg.screenWidthDp / 390f).coerceIn(0.85f, 1.25f)
    val ground = remember { Ground() }
    val reduced = LocalReducedMotion.current
    val shown by animateColorAsState(
        if (ground.target == Color.Unspecified) Color.Transparent else ground.target,
        if (reduced) tween(Motion.REDUCED, easing = LinearEasing) else tween(Motion.BG, easing = StandardEase),
        label = "ground",
    )
    CompositionLocalProvider(
        LocalDensity provides Density(d.density * s, d.fontScale),
        LocalDesignScale provides s,
        LocalGround provides ground,
    ) {
        Box(Modifier.fillMaxSize().background(shown)) { content() }
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
    // The frame paints the ground and cross-fades it between screens (M2); a screen only says
    // which colour it wants. Outside a frame (previews), it paints its own.
    val ground = LocalGround.current
    SideEffect { ground?.target = bg }
    val bars = WindowInsets.systemBars.asPaddingValues()
    val topZone = max(48.dp.value, bars.calculateTopPadding().value).dp
    val bottomZone = max(32.dp.value, bars.calculateBottomPadding().value).dp
    BoxWithConstraints(Modifier.fillMaxSize().then(if (ground == null) Modifier.background(bg) else Modifier)) {
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
    val mode = when (style.lineBreak) {
        LineBreak.Heading -> WrapMode.Balance
        LineBreak.Paragraph -> WrapMode.Pretty
        else -> WrapMode.Greedy
    }
    CssLines(text, style, color, mode, modifier, maxLines)
}

/**
 * Text laid out as CSS lays it out, line by line.
 *
 * Breaks: greedy, or Chrome's balance / pretty (cssWrap below), computed from real measurements at
 * the current width and font scale — larger text still wraps correctly.
 *
 * Lines: each drawn at Chrome's exact baseline, `top + (line-height − 1.2 em) / 2 + 0.93 em` for
 * this font (ascent 0.93, descent 0.27, no line gap). Android snaps every line box to whole pixels
 * (a 27.55 dp line is 76 px, not 76.3), so paragraphs drifted about a pixel a line from the design;
 * placing each line independently removes the accumulation. The block is lines × line-height tall,
 * so tight headings no longer need their half-leading trimmed separately.
 *
 * A plain layout rather than BoxWithConstraints, so a flex parent can still ask its intrinsic height.
 */
@Composable
private fun CssLines(
    text: String,
    style: TextStyle,
    color: Color,
    mode: WrapMode,
    modifier: Modifier,
    maxLines: Int,
) {
    val measurer = rememberTextMeasurer()
    val plain = remember(style) { style.copy(lineBreak = LineBreak.Simple, hyphens = Hyphens.None, textAlign = TextAlign.Unspecified) }
    val holder = remember(text, plain, mode, maxLines) { LinesHolder() }
    val center = style.textAlign == TextAlign.Center
    Layout(
        content = {},
        modifier = modifier
            .semantics { this.text = AnnotatedString(text) }
            .drawBehind {
                val lines = holder.lines ?: return@drawBehind
                for (i in lines.indices) {
                    val r = lines[i]
                    val x = (if (center) (size.width - r.size.width) / 2f else 0f)
                    drawText(r, color = color, topLeft = Offset(x, holder.baselines[i] - r.firstBaseline))
                }
            },
    ) { _, c ->
        val w = c.maxWidth
        val broken = if (maxLines == 1 || w == Constraints.Infinity) {
            text.lines()
        } else {
            holder.byWidth.getOrPut(w) { cssWrap(text, w, mode, measurer, plain).lines() }
        }.take(maxLines)
        val layouts = broken.map { measurer.measure(it, plain, softWrap = false, maxLines = 1) }
        val em = plain.fontSize.toPx()
        val lh = if (plain.lineHeight.isSp) plain.lineHeight.toPx() else em * 1.2f
        holder.lines = layouts
        holder.baselines = FloatArray(layouts.size) { i -> i * lh + (lh - 1.2f * em) / 2f + 0.93f * em }
        val widest = layouts.maxOfOrNull { it.size.width } ?: 0
        val width = if (center && w != Constraints.Infinity) w else widest
        layout(c.constrainWidth(width), c.constrainHeight(kotlin.math.round(layouts.size * lh).toInt())) {}
    }
}

private class LinesHolder {
    val byWidth = HashMap<Int, List<String>>()
    var lines: List<TextLayoutResult>? = null
    var baselines = FloatArray(0)
}

private enum class WrapMode { Greedy, Balance, Pretty }

/** Breaks each paragraph (split on explicit newlines) as Chrome would, as explicit newlines. */
private fun cssWrap(text: String, width: Int, mode: WrapMode, m: TextMeasurer, style: TextStyle): String =
    text.split('\n').joinToString("\n") { wrapParagraph(it, width, mode, m, style) }

private fun wrapParagraph(p: String, w: Int, mode: WrapMode, m: TextMeasurer, style: TextStyle): String {
    val words = p.split(' ').filter { it.isNotEmpty() }
    if (words.size < 2) return p
    val cache = HashMap<Long, Int>()
    // Width of words [i, j) set on one line, measured exactly as the Text will lay it out.
    fun width(i: Int, j: Int): Int = cache.getOrPut((i.toLong() shl 32) or j.toLong()) {
        m.measure(words.subList(i, j).joinToString(" "), style, softWrap = false, maxLines = 1).size.width
    }
    fun greedy(limit: Int): List<Int> {
        val starts = mutableListOf(0)
        var i = 0
        while (i < words.size) {
            var j = i + 1
            while (j < words.size && width(i, j + 1) <= limit) j++
            i = j
            if (i < words.size) starts += i
        }
        return starts
    }
    var starts = greedy(w)
    if (starts.size < 2) return p
    when (mode) {
        WrapMode.Greedy -> Unit
        // Chrome: the same number of lines, broken to minimise the squared slack of every line,
        // the last included — the most even set, not the narrowest width. Measured against the
        // references: "This doesn't / sound / like BPPV", where bisecting the width gave
        // "sound like / BPPV". Up to six lines.
        WrapMode.Balance -> if (starts.size <= 6) {
            evenBreaks(starts.size, w, words.size, ::width)?.let { starts = it }
        }
        // Chrome: when the last line would be a single orphaned word, re-break the last four
        // lines, minimising the squared slack of the non-last lines, with at least two words on
        // the last. Measured against the references: "...show you the / right manoeuvre." is
        // re-broken; "...so this is asked / every time." (two words, just as short) is not.
        WrapMode.Pretty -> if (starts.last() == words.size - 1) {
            prettyTail(starts, w, words.size, ::width)?.let { starts = it }
        }
    }
    val ends = starts.drop(1) + words.size
    return starts.indices.joinToString("\n") { k -> words.subList(starts[k], ends[k]).joinToString(" ") }
}

/** Exactly [lines] lines over all [n] words, minimising the sum of squared slack of every line. */
private fun evenBreaks(lines: Int, w: Int, n: Int, width: (Int, Int) -> Int): List<Int>? {
    val inf = Long.MAX_VALUE / 4
    val best = Array(lines + 1) { LongArray(n + 1) { inf } }
    val prev = Array(lines + 1) { IntArray(n + 1) { -1 } }
    best[0][0] = 0
    for (l in 1..lines) {
        for (i in 0 until n) {
            if (best[l - 1][i] >= inf) continue
            for (j in i + 1..n) {
                val lw = width(i, j)
                if (lw > w) break
                val slack = (w - lw).toLong()
                val score = best[l - 1][i] + slack * slack
                if (score < best[l][j]) { best[l][j] = score; prev[l][j] = i }
            }
        }
    }
    if (best[lines][n] >= inf) return null
    val starts = ArrayList<Int>()
    var at = n
    for (l in lines downTo 1) { at = prev[l][at]; starts += at }
    return starts.reversed()
}

private fun prettyTail(starts: List<Int>, w: Int, n: Int, width: (Int, Int) -> Int): List<Int>? {
    val k = minOf(4, starts.size)
    val from = starts[starts.size - k]
    val inf = Long.MAX_VALUE / 4
    // best[l][i]: least score for words [from, i) in l full lines; prev for the path back.
    val best = Array(k) { LongArray(n + 1) { inf } }
    val prev = Array(k) { IntArray(n + 1) { -1 } }
    best[0][from] = 0
    for (l in 1 until k) {
        for (i in from until n) {
            if (best[l - 1][i] >= inf) continue
            for (j in i + 1..n) {
                val lw = width(i, j)
                if (lw > w) break
                val slack = (w - lw).toLong()
                val score = best[l - 1][i] + slack * slack
                if (score < best[l][j]) { best[l][j] = score; prev[l][j] = i }
            }
        }
    }
    var bestEnd = -1
    var bestScore = inf
    for (i in from until n) {
        val lastWidth = width(i, n)
        if (best[k - 1][i] < bestScore && lastWidth <= w && n - i >= 2) {
            bestScore = best[k - 1][i]; bestEnd = i
        }
    }
    if (bestEnd < 0) return null
    val tail = ArrayList<Int>()
    var at = bestEnd
    for (l in k - 1 downTo 1) { tail += at; at = prev[l][at] }
    return starts.subList(0, starts.size - k) + from + tail.reversed()
}

/** The design's ink, used as a literal wherever a pastel ground keeps its dark text at night too. */
val Ink = Color(0xFF17161C)

/**
 * The design's 64 dp pill button: `display:flex; justify-content:center; gap:8px;
 * min-height:64px; border-radius:32px; font: 20px/700`, optional leading icon (28 px, inheriting
 * the button's weight 700).
 */
@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    fill: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: String? = null,
    ring: Color? = null,
    enabled: Boolean = true,
) {
    androidx.compose.foundation.layout.Row(
        modifier
            .heightIn(min = 64.dp)
            .pressable(enabled = enabled, onClick = onClick)
            .box(fill, 32.dp, ring = ring),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Sym(icon, 28f, content, weight = 700)
        Txt(label, type(20f, 700), content)
    }
}
