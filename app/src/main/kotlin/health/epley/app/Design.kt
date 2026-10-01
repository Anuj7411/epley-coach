package health.epley.app

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import java.util.Calendar

/*
 * Design system v2 — design_handoff_epley_coach_v2, README §3, §6, §8, §12.
 *
 * Lives beside the older `Palette` while screens are moved across one at a time, so a screen that
 * has not been converted yet keeps its old dark look rather than becoming unreadable against a
 * day background halfway through the migration.
 */

/** One theme's colours. Night follows the rule "no white or pastel floods" (README §3.2). */
data class EpleyColors(
    val night: Boolean,
    val ground: Color,
    val surface: Color,
    val surface2: Color,
    val ink: Color,
    val muted: Color,
    val soft: Color,
    val line: Color,
    val divider: Color,
    val lilac: Color,
    val lilacTint: Color,
    val butter: Color,
    val butterTint: Color,
    val mint: Color,
    val mintTint: Color,
    val mintDim: Color,
    val zone: Color,
    val coral: Color,
    val coralTint: Color,
    /** Primary button: ink on day, lilac on night. */
    val primaryFill: Color,
    val onPrimary: Color,
    /** Icon tiles and chips sitting on pastel fills. */
    val onPastel: Color,
)

val DayColors = EpleyColors(
    night = false,
    ground = Color(0xFFEFEDE8), surface = Color(0xFFFFFFFF), surface2 = Color(0xFFEFEDE8),
    ink = Color(0xFF17161C), muted = Color(0xFF55535E), soft = Color(0xFF17161C),
    line = Color(0xFFD4D2DC), divider = Color(0xFFEFEDE8),
    lilac = Color(0xFFC9BCFF), lilacTint = Color(0xFFC9BCFF),
    butter = Color(0xFFFFE38A), butterTint = Color(0xFFFFE38A),
    mint = Color(0xFFA8EBD6), mintTint = Color(0xFFA8EBD6), mintDim = Color(0x3317161C), zone = Color(0xFFA8EBD6),
    coral = Color(0xFFFFC4B8), coralTint = Color(0xFFFFC4B8),
    primaryFill = Color(0xFF17161C), onPrimary = Color(0xFFFFFFFF),
    onPastel = Color(0x99FFFFFF),
)

val NightColors = EpleyColors(
    night = true,
    ground = Color(0xFF111015), surface = Color(0xFF1D1C23), surface2 = Color(0xFF2A2931),
    ink = Color(0xFFF1F0F5), muted = Color(0xFFA9A7B3), soft = Color(0xFFD8D6E0),
    line = Color(0xFF34333C), divider = Color(0xFF2A2931),
    lilac = Color(0xFFC9BCFF), lilacTint = Color(0xFF221E30),
    butter = Color(0xFFFFE38A), butterTint = Color(0xFF2B2718),
    mint = Color(0xFF8FDCC4), mintTint = Color(0xFF1B2B26), mintDim = Color(0xFF3F6B5E), zone = Color(0xFF2E5C4F),
    coral = Color(0xFFF5AE9F), coralTint = Color(0xFF33211D),
    primaryFill = Color(0xFFC9BCFF), onPrimary = Color(0xFF17161C),
    onPastel = Color(0xFF2A2931),
)

val LocalEpley = staticCompositionLocalOf { DayColors }
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Height available to a screen's content, between the top and bottom slots.
 *
 * Screens size their hero elements as a fraction of this rather than in fixed dp, so a card
 * tuned on one handset does not leave a third of a taller screen empty, or overflow a shorter
 * one. Always used with coerceIn bounds so the proportion cannot produce something unusable.
 */
val LocalViewportHeight = staticCompositionLocalOf { 640.dp }

/** The current theme's colours. */
val Ds: EpleyColors @Composable get() = LocalEpley.current

/**
 * Night when the system is dark, or between 21:00 and 06:00 whatever the system says (README §11).
 * Evaluated when a screen is entered, never mid-run, so the colours cannot change under someone
 * lying with their head back.
 */
fun isNightNow(systemDark: Boolean): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return systemDark || hour >= 21 || hour < 6
}

/** README §6.3: animator duration scale of zero, or "Remove animations", means reduced motion. */
fun isReducedMotion(context: android.content.Context): Boolean = try {
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
} catch (_: Exception) {
    false
}

@Composable
fun EpleyDesign(night: Boolean, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val reduced = remember(context) { isReducedMotion(context) }
    CompositionLocalProvider(
        LocalEpley provides if (night) NightColors else DayColors,
        LocalReducedMotion provides reduced,
        content = content,
    )
}

// ---------------------------------------------------------------------------------------------
// Type — Bricolage Grotesque (README §3.3)
// ---------------------------------------------------------------------------------------------

/**
 * One variable font file, registered once per weight the design uses.
 *
 * `variationSettings` only takes effect from API 26; below that the file renders at its default
 * instance, which is wght 800. That is the heaviest weight here, so on Android 7 the lighter text
 * comes out bold rather than wrong — the layout is unchanged and nothing becomes unreadable.
 *
 * opsz is pinned at 40 rather than varied per size. The real design moves it with the type size,
 * but a Compose FontFamily resolves on weight alone, and 40 sits between the 14sp labels and the
 * 56sp results without either looking mis-cut.
 */
private fun bricolage(weight: Int) = Font(
    resId = R.font.bricolage_grotesque,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.Setting("opsz", 40f),
    ),
)

val Bricolage = FontFamily(
    bricolage(400), bricolage(500), bricolage(600), bricolage(700), bricolage(800),
)

object DsType {
    var family: FontFamily = Bricolage
    private val tnum = "tnum"

    fun count(size: TextUnit) = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = size,
        lineHeight = size * 0.72f, letterSpacing = (-0.075).em, fontFeatureSettings = tnum)
    val display get() = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp,
        lineHeight = 44.sp, letterSpacing = (-0.035).em)
    val result get() = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 56.sp,
        lineHeight = 53.sp, letterSpacing = (-0.045).em, fontFeatureSettings = tnum)
    val title get() = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp,
        lineHeight = 35.sp, letterSpacing = (-0.03).em)
    val cardTitle get() = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp,
        lineHeight = 25.sp, letterSpacing = (-0.015).em)
    val action get() = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 20.sp,
        lineHeight = 24.sp)
    val body get() = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
        lineHeight = 24.sp)
    val label get() = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
        lineHeight = 21.sp)
    val rowTitle get() = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 17.sp,
        lineHeight = 22.sp)
    val caps get() = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp,
        lineHeight = 18.sp, letterSpacing = 0.06.em)
    val chip get() = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold, fontSize = 16.sp,
        lineHeight = 20.sp)
    val live get() = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 44.sp,
        lineHeight = 44.sp, letterSpacing = (-0.04).em, fontFeatureSettings = tnum)
}

// ---------------------------------------------------------------------------------------------
// Motion (README §6.1, §12)
// ---------------------------------------------------------------------------------------------

val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val StandardEase = CubicBezierEasing(0.2f, 0f, 0f, 1f)

object Motion {
    // v2.1 polish, on request: a longer, more visible entrance than the handoff's 280 / 24 / 8 dp.
    const val ENTER = 400
    const val STAGGER = 40
    const val RISE = 16
    /** The press goes down fast and comes back slower, so even a quick tap is seen. */
    const val PRESS_DOWN = 90
    const val PRESS_SCALE = 0.95f
    /** The outgoing screen fades out under the incoming one instead of cutting. */
    const val EXIT = 140
    const val BG = 360
    const val PRESS = 160
    const val TOGGLE = 200
    const val MARKER = 240
    const val REDUCED = 150
}

object Space { val xs = 4.dp; val s = 8.dp; val m = 12.dp; val l = 16.dp; val xl = 24.dp; val xxl = 32.dp; val xxxl = 48.dp }
object Radius { val tile = 16.dp; val note = 24.dp; val card = 28.dp; val hero = 32.dp; val button = 32.dp }

/**
 * M1: each child fades 0→1 and rises 8dp→0, staggered 24ms, capped at 5 steps. Reduced motion
 * turns it into a 150ms linear fade with no rise. Opacity and a small vertical rise only — never a
 * horizontal slide, never a zoom.
 */
@Composable
fun Modifier.enter(index: Int): Modifier {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            if (reduced) tween(Motion.REDUCED, easing = LinearEasing)
            else tween(Motion.ENTER, delayMillis = minOf(index, 5) * Motion.STAGGER, easing = Emphasized),
        )
    }
    val rise = with(LocalDensity.current) { Motion.RISE.dp.toPx() }
    return graphicsLayer {
        alpha = progress.value
        translationY = if (reduced) 0f else (1f - progress.value) * rise
    }
}

/** False while a screen is fading out (PageFade): it must no longer claim the ground colour. */
val LocalPageActive = androidx.compose.runtime.compositionLocalOf { true }

/**
 * Screen change: the outgoing screen fades out over 140 ms under the incoming one, whose children
 * then rise in (M1). Opacity only — no slide, no zoom. Reduced motion: a 150 ms linear cross-fade.
 */
@Composable
fun <K> PageFade(key: K, content: @Composable (K) -> Unit) {
    val reduced = LocalReducedMotion.current
    androidx.compose.animation.AnimatedContent(
        targetState = key,
        transitionSpec = {
            val t = if (reduced) Motion.REDUCED else Motion.EXIT
            (
                androidx.compose.animation.fadeIn(tween(t, easing = LinearEasing)) togetherWith
                    androidx.compose.animation.fadeOut(tween(t, easing = LinearEasing))
                ).apply { targetContentZIndex = 1f }
                .using(androidx.compose.animation.SizeTransform(clip = false))
        },
        label = "page",
    ) { k ->
        androidx.compose.runtime.CompositionLocalProvider(LocalPageActive provides (transition.targetState == k)) {
            content(k)
        }
    }
}

/**
 * M4: press down to 0.95 with a slight dim, release on a critically damped spring (no bounce).
 * A tap shorter than the press-down still plays the whole press, so every touch is answered.
 * No ripple flood. Off under reduced motion.
 */
@Composable
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val reduced = LocalReducedMotion.current
    val press = remember { Animatable(0f) }
    LaunchedEffect(source, reduced) {
        if (reduced) return@LaunchedEffect
        var down: kotlinx.coroutines.Job? = null
        source.interactions.collect { i ->
            when (i) {
                is androidx.compose.foundation.interaction.PressInteraction.Press ->
                    down = launch { press.animateTo(1f, tween(Motion.PRESS_DOWN, easing = StandardEase)) }
                is androidx.compose.foundation.interaction.PressInteraction.Release,
                is androidx.compose.foundation.interaction.PressInteraction.Cancel -> launch {
                    down?.join()
                    press.animateTo(0f, androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = 500f))
                }
            }
        }
    }

    // A light tick with every tap, the way the platform's own controls answer a touch. It goes
    // through the system, so it follows the phone's haptic setting and is silent when that is off.
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    return graphicsLayer {
        // Read inside the layer, so the press animates without recomposing the button.
        val scale = 1f - (1f - Motion.PRESS_SCALE) * press.value
        scaleX = scale; scaleY = scale
        alpha = 1f - 0.12f * press.value
    }
        .clickable(interactionSource = source, indication = null, enabled = enabled) {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.ContextClick)
            onClick()
        }
}

// ---------------------------------------------------------------------------------------------
// Screen frame
// ---------------------------------------------------------------------------------------------

/**
 * A screen: background cross-fades (M2), content scrolls, the bottom slot holds the actions in the
 * thumb zone. [background] lets hard-stop and hold screens flood coral or mint on day.
 */
@Composable
fun DsScreen(
    background: Color = Ds.ground,
    top: (@Composable ColumnScope.() -> Unit)? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduced = LocalReducedMotion.current
    val bg by animateColorAsState(
        background,
        if (reduced) tween(Motion.REDUCED, easing = LinearEasing) else tween(Motion.BG, easing = StandardEase),
        label = "bg",
    )
    Column(
        Modifier
            .fillMaxSize()
            .background(bg)
            .safeDrawingPadding()
            .padding(horizontal = Space.l, vertical = Space.s),
    ) {
        if (top != null) Column(Modifier.fillMaxWidth(), content = top)
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(LocalViewportHeight provides maxHeight) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier),
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                    content = content,
                )
            }
        }
        if (bottom != null) {
            Column(
                Modifier.fillMaxWidth().padding(top = Space.s),
                verticalArrangement = Arrangement.spacedBy(Space.s),
                content = bottom,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Components (README §8)
// ---------------------------------------------------------------------------------------------

/**
 * Card. On day a flat fill; on night a dark tint with a 2dp inset outline, because night must never
 * flood a pastel ([fill] is the day colour, [tint]/[outline] the night pair).
 */
@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    fill: Color = Ds.surface,
    tint: Color? = null,
    outline: Color? = null,
    radius: Dp = Radius.card,
    padding: Dp = Space.xl,
    minHeight: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ds
    val shape = RoundedCornerShape(radius)
    val bg = if (c.night && tint != null) tint else fill
    Column(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .clip(shape)
            .background(bg)
            .then(if (c.night && outline != null) Modifier.border(2.dp, outline, shape) else Modifier)
            .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(Space.s),
        content = content,
    )
}

enum class ChipStyle { Outlined, Filled, OnPastel }

@Composable
fun DsChip(text: String, icon: ImageVector?, style: ChipStyle, accent: Color = Ds.ink) {
    val c = Ds
    val shape = RoundedCornerShape(999.dp)
    val (bg, fg, border) = when (style) {
        ChipStyle.Filled -> Triple(c.ink, if (c.night) c.ground else Color.White, null)
        ChipStyle.OnPastel -> Triple(if (c.night) c.surface2 else Color(0x99FFFFFF), c.ink, null)
        ChipStyle.Outlined -> Triple(Color.Transparent, accent, BorderStroke(2.dp, accent))
    }
    Row(
        Modifier
            .clip(shape)
            .background(bg)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        Text(text, style = DsType.chip, color = fg)
    }
}

/** 64dp, radius 32, 20/700, optional 28dp leading or trailing icon. */
@Composable
fun DsButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = Ds.primaryFill,
    contentColor: Color = Ds.onPrimary,
    outline: Color? = null,
    leading: ImageVector? = null,
    trailing: ImageVector? = null,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(Radius.button)
    val content = if (enabled) contentColor else Ds.muted
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .pressable(enabled = enabled, onClick = onClick)
            .clip(shape)
            // Disabled goes flat and muted rather than a faded pastel: a washed-out fill under
            // dark text reads as a rendering fault, not as "not yet".
            .background(if (enabled) fill else Ds.surface2)
            .then(if (outline != null) Modifier.border(2.dp, outline, shape) else Modifier)
            .padding(horizontal = Space.xl, vertical = Space.m),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (trailing != null) Arrangement.SpaceBetween else Arrangement.Center,
    ) {
        if (leading != null) {
            Icon(leading, contentDescription = null, tint = content, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(Space.s))
        }
        Text(label, style = DsType.action, color = content, textAlign = TextAlign.Center)
        if (trailing != null) Icon(trailing, contentDescription = null, tint = content, modifier = Modifier.size(28.dp))
    }
}

/** Stop: coral fill, ink text, cross. On every run screen, full width, 64dp, bottom. */
@Composable
fun DsStopButton(onClick: () -> Unit) {
    val c = Ds
    DsButton(
        label = "Stop",
        onClick = onClick,
        fill = c.coral,
        contentColor = Color(0xFF17161C),
        leading = Icons.Rounded.Close,
    )
}

/** 48dp circle, 24dp icon. Back and settings. */
@Composable
fun DsIconCircle(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(999.dp))
            .background(Ds.surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = Ds.ink, modifier = Modifier.size(24.dp))
    }
}

/** A square icon tile, radius 16. */
@Composable
fun DsIconTile(icon: ImageVector, size: Dp = 48.dp, iconSize: Dp = 24.dp, fill: Color = Ds.surface2, tint: Color = Ds.ink) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(Radius.tile)).background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** 8dp segments, gap 4: done = ink fill, current = 2dp ink outline, todo = line. */
@Composable
fun DsProgressSegments(total: Int, current: Int) {
    val c = Ds
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..total) {
            val shape = RoundedCornerShape(4.dp)
            val m = Modifier.weight(1f).height(8.dp).clip(shape)
            when {
                i < current -> Box(m.background(c.ink))
                i == current -> Box(m.border(2.dp, c.ink, shape))
                else -> Box(m.background(c.line))
            }
        }
    }
}

/**
 * Hold blocks: 5s blocks for 45s holds (9 blocks), 1s blocks for the 3s and 5s holds. Each fills
 * left to right continuously (M9); the fill is linear because it is a timer.
 */
@Composable
fun DsHoldBlocks(totalSeconds: Int, elapsedSeconds: Float, fill: Color, empty: Color) {
    val per = if (totalSeconds > 5) 5 else 1
    val blocks = (totalSeconds + per - 1) / per
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until blocks) {
            val frac = ((elapsedSeconds - i * per) / per).coerceIn(0f, 1f)
            val shape = RoundedCornerShape(4.dp)
            Box(
                Modifier.weight(1f).height(16.dp).clip(shape).border(2.dp, fill, shape).background(empty),
            ) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(fill))
            }
        }
    }
}

/**
 * Range meter: 12dp track, the in-range zone filled, a 4×24dp marker. [value], [zoneStart],
 * [zoneEnd] are fractions of the scale. The marker glides to each new reading (M7).
 */
@Composable
fun DsRangeMeter(value: Float, zoneStart: Float, zoneEnd: Float) {
    val c = Ds
    val reduced = LocalReducedMotion.current
    val v by animateFloatAsState(
        value.coerceIn(0f, 1f),
        if (reduced) tween(0) else tween(Motion.MARKER, easing = StandardEase),
        label = "marker",
    )
    Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(c.surface2)) {
            Row(Modifier.fillMaxSize()) {
                if (zoneStart > 0f) Spacer(Modifier.weight(zoneStart))
                Box(Modifier.weight((zoneEnd - zoneStart).coerceAtLeast(0.001f)).fillMaxHeight().background(c.zone))
                if (zoneEnd < 1f) Spacer(Modifier.weight(1f - zoneEnd))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            if (v > 0f) Spacer(Modifier.weight(v))
            Box(Modifier.width(4.dp).height(24.dp).clip(RoundedCornerShape(2.dp)).background(c.ink))
            if (v < 1f) Spacer(Modifier.weight(1f - v))
        }
    }
}

/** Note card: 1.5dp line outline, radius 24, info icon muted, 16/600. */
@Composable
fun DsNote(text: String) {
    val c = Ds
    val shape = RoundedCornerShape(Radius.note)
    Row(
        Modifier.fillMaxWidth().clip(shape).border(1.5.dp, c.line, shape).padding(Space.l),
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = c.muted, modifier = Modifier.size(24.dp))
        Text(text, style = DsType.label, color = c.ink)
    }
}

/** Warning card: coral (day) / coral tint + outline (night), warning tile, caps caption + body. */
@Composable
fun DsWarning(caption: String, body: String) {
    DsCard(fill = Ds.coral, tint = Ds.coralTint, outline = Ds.coral, padding = Space.l) {
        Row(horizontalArrangement = Arrangement.spacedBy(Space.m), verticalAlignment = Alignment.CenterVertically) {
            DsIconTile(Icons.Rounded.Warning, fill = if (Ds.night) Ds.coral else Color(0x99FFFFFF), tint = Color(0xFF17161C))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(caption, style = DsType.caps, color = if (Ds.night) Ds.coral else Ds.ink)
                Text(body, style = DsType.label, color = Ds.ink)
            }
        }
    }
}

/** Row: 48dp icon tile, title 17/700, subtitle 16/600 muted, optional trailing content. */
@Composable
fun DsRow(
    icon: ImageVector?,
    title: String,
    subtitle: String? = null,
    iconFill: Color = Ds.surface2,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.pressable(onClick = onClick) else Modifier)
            .padding(vertical = Space.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        if (icon != null) DsIconTile(icon, fill = iconFill)
        Column(Modifier.weight(1f)) {
            Text(title, style = DsType.rowTitle, color = Ds.ink)
            if (subtitle != null) Text(subtitle, style = DsType.label, color = Ds.muted)
        }
        if (trailing != null) trailing()
    }
}

/** 1dp divider inside white cards. */
@Composable
fun DsDivider(start: Dp = 0.dp) {
    Box(Modifier.fillMaxWidth().padding(start = start).height(1.dp).background(Ds.divider))
}
