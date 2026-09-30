package health.epley.app

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.Episode
import health.epley.core.EpisodeLog
import health.epley.core.Feeling
import health.epley.core.MountMode
import health.epley.core.Side
import health.epley.core.word
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Every flow screen shares one frame: a step label and progress bar at the top, content in the
 * middle, and the single main action anchored at the bottom within thumb reach (docs/DESIGN.md).
 * No transitions between screens — motion is what the people using this are sensitive to.
 */
@Composable
fun FlowFrame(
    stepLabel: String?,
    progress: Float?,
    onBack: (() -> Unit)?,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.Background)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        if (stepLabel != null || onBack != null) {
            Row(modifier = Modifier.fillMaxWidth()) {
                if (stepLabel != null) {
                    Text(
                        stepLabel,
                        color = Palette.InkMuted,
                        fontSize = AppType.LabelSize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppType.Sans,
                        modifier = Modifier.weight(1f).padding(top = 12.dp),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                if (onBack != null) {
                    TextButton(onClick = onBack) {
                        Text(
                            "Back",
                            color = Palette.Ink,
                            fontSize = AppType.ReadingFloor,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppType.Sans,
                        )
                    }
                }
            }
        }
        if (progress != null) {
            Box(
                Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp)).background(Palette.Raised),
            ) {
                Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(4.dp).background(Palette.Action))
            }
        }
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = bottom,
        )
    }
}

/**
 * The one thing a screen asks. Set heavy and tight: weight carries the hierarchy so that size does
 * not have to, which keeps the whole screen inside a 160% system font scale.
 */
@Composable
fun Title(text: String, color: androidx.compose.ui.graphics.Color = Palette.Ink) =
    Text(
        text,
        color = color,
        fontSize = AppType.TitleSize,
        lineHeight = AppType.TitleLine,
        fontWeight = FontWeight.ExtraBold,
        fontFamily = AppType.Sans,
    )

/**
 * Body copy, at 17sp with 24sp leading.
 *
 * Secondary text is InkMuted rather than a lower alpha: a faded white on a warm dark ground loses
 * contrast faster than a chosen grey, and everything here has to clear 7:1.
 */
@Composable
fun Body(text: String, secondary: Boolean = false) =
    Text(
        text,
        color = if (secondary) Palette.InkMuted else Palette.InkBody,
        fontSize = AppType.BodySize,
        lineHeight = AppType.BodyLine,
        fontFamily = AppType.Sans,
    )

/** A card for grouped information. Depth by a lighter surface, never a shadow. */
@Composable
fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Palette.Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * The first screen, as a set of cards rather than a stack of paragraphs.
 *
 * Each card carries one job — what this is, the thing you came to do, the two side doors, the
 * warning, the record — so the screen can be read by its shapes before any of it is read as
 * words. That matters more than usual here: the person looking at it is dizzy.
 */
@Composable
fun HomeScreen(
    episodes: List<Episode>,
    onStart: () -> Unit,
    onPractice: () -> Unit,
    onExport: (() -> Unit)?,
    onInstrument: () -> Unit,
) {
    FlowFrame(stepLabel = null, progress = null, onBack = null, bottom = {}) {
        Spacer(Modifier.height(4.dp))
        Card {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark()
                Spacer(Modifier.width(14.dp))
                Text(
                    "Epley Coach",
                    color = Palette.Ink,
                    fontSize = AppType.TitleSize,
                    lineHeight = AppType.TitleLine,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = AppType.Sans,
                )
            }
            Text(
                "Five guided head positions for BPPV vertigo, checked by your phone.",
                color = Palette.InkBody,
                fontSize = AppType.ReadingFloor,
                lineHeight = AppType.LabelLine,
                fontFamily = AppType.Sans,
            )
        }

        StartCard(onStart)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SideDoor(
                modifier = Modifier.weight(1f),
                label = "Practise in hand",
                onClick = onPractice,
            ) { PhoneGlyph(Palette.Seeking) }
            SideDoor(
                modifier = Modifier.weight(1f),
                label = "Sensor readout",
                onClick = onInstrument,
            ) { SignalGlyph(Palette.Holding) }
        }

        // The warning keeps its own card and stays above the record, so it is read before any of
        // this starts to look like a thing to simply get on with.
        Card {
            Text(
                "NOT A MEDICAL DEVICE",
                color = Palette.Urgent,
                fontSize = AppType.LabelSize,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = AppType.Sans,
            )
            Text(
                "An unregulated prototype. It must never be used on a patient.",
                color = Palette.InkMuted,
                fontSize = AppType.ReadingFloor,
                lineHeight = AppType.LabelLine,
                fontFamily = AppType.Sans,
            )
        }

        EpisodeHistory(episodes, onExport)
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * The one thing this screen is for, given the space that implies.
 *
 * Big enough to hit without aiming, which is the point: the hand reaching for it belongs to
 * someone whose horizon is moving.
 */
@Composable
private fun StartCard(onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Palette.Selected)
            .clickable(onClick = onStart)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PlayGlyph(Palette.Ground)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Start treatment",
                color = Palette.Ground,
                fontSize = AppType.TitleSize,
                lineHeight = AppType.TitleLine,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = AppType.Sans,
                modifier = Modifier.weight(1f),
            )
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(26.dp)).background(Palette.Ground),
                contentAlignment = Alignment.Center,
            ) { ArrowGlyph(Palette.Selected) }
        }
    }
}

/** One of the two smaller ways in. Same shape, different errand. */
@Composable
private fun SideDoor(
    modifier: Modifier,
    label: String,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Palette.Surface)
            .clickable(onClick = onClick)
            .heightIn(min = 112.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        glyph()
        Text(
            label,
            color = Palette.Ink,
            fontSize = AppType.ReadingFloor,
            lineHeight = AppType.LabelLine,
            fontWeight = FontWeight.Bold,
            fontFamily = AppType.Sans,
        )
    }
}

/** A filled triangle in a ring. Drawn, not imported: the extended icon set is not a dependency. */
@Composable
private fun PlayGlyph(colour: Color) {
    Canvas(Modifier.size(30.dp)) {
        val ring = 2.5.dp.toPx()
        val radius = size.minDimension / 2f
        val middle = Offset(size.width / 2f, size.height / 2f)
        drawCircle(color = colour, radius = radius - ring / 2f, style = Stroke(width = ring))
        val head = Path().apply {
            moveTo(middle.x - radius * 0.24f, middle.y - radius * 0.38f)
            lineTo(middle.x + radius * 0.44f, middle.y)
            lineTo(middle.x - radius * 0.24f, middle.y + radius * 0.38f)
            close()
        }
        drawPath(head, colour)
    }
}

/** A right-pointing arrow: shaft and two barbs. */
@Composable
private fun ArrowGlyph(colour: Color) {
    Canvas(Modifier.size(22.dp)) {
        val weight = 2.6.dp.toPx()
        val midY = size.height / 2f
        val tip = Offset(size.width * 0.84f, midY)
        drawLine(colour, Offset(size.width * 0.16f, midY), tip, weight, StrokeCap.Round)
        drawLine(colour, Offset(size.width * 0.54f, midY - size.height * 0.26f), tip, weight, StrokeCap.Round)
        drawLine(colour, Offset(size.width * 0.54f, midY + size.height * 0.26f), tip, weight, StrokeCap.Round)
    }
}

/** A phone held upright: practice mode measures this, not a head. */
@Composable
private fun PhoneGlyph(colour: Color) {
    Canvas(Modifier.size(26.dp)) {
        val weight = 2.4.dp.toPx()
        val inset = size.width * 0.24f
        drawRoundRect(
            color = colour,
            topLeft = Offset(inset, size.height * 0.05f),
            size = Size(size.width - inset * 2f, size.height * 0.90f),
            cornerRadius = CornerRadius(weight * 1.8f, weight * 1.8f),
            style = Stroke(width = weight),
        )
        drawLine(
            colour,
            Offset(size.width * 0.41f, size.height * 0.80f),
            Offset(size.width * 0.59f, size.height * 0.80f),
            weight,
            StrokeCap.Round,
        )
    }
}

/** Concentric arcs around a dot: something being read off the air. */
@Composable
private fun SignalGlyph(colour: Color) {
    Canvas(Modifier.size(26.dp)) {
        val weight = 2.4.dp.toPx()
        val middle = Offset(size.width / 2f, size.height / 2f)
        drawCircle(colour, radius = size.minDimension * 0.11f, center = middle)
        listOf(0.28f, 0.44f).forEach { fraction ->
            val radius = size.minDimension * fraction
            val box = Offset(middle.x - radius, middle.y - radius)
            val extent = Size(radius * 2f, radius * 2f)
            drawArc(colour, -58f, 116f, false, box, extent, style = Stroke(weight, cap = StrokeCap.Round))
            drawArc(colour, 122f, 116f, false, box, extent, style = Stroke(weight, cap = StrokeCap.Round))
        }
    }
}

/**
 * The brand mark, the same "e" the launcher icon and the splash use, drawn from the one vector
 * in res/drawable so the three cannot drift apart.
 *
 * Lilac tile, ink mark — the launcher icon exactly. Opening the app should show you the thing
 * you just tapped.
 */
@Composable
private fun BrandMark() {
    Box(
        Modifier.size(56.dp).clip(RoundedCornerShape(19.dp)).background(Palette.Selected),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_brand_mark),
            contentDescription = null,
            tint = Palette.Ground,
            modifier = Modifier.size(38.dp),
        )
    }
}

/**
 * The last few real treatment runs. Practice runs are left out: they measured a phone.
 *
 * One card, hairlines between rows, a filled chip carrying the outcome as a colour *and* a shape.
 * The date and the outcome sit on separate lines by construction, which is also why this no
 * longer needs the font-scale rule the old side-by-side row did — there is nothing left to
 * squeeze. BPPV coming back is the premise of the app, so the person it keeps happening to
 * should be able to see the pattern.
 */
@Composable
fun EpisodeHistory(episodes: List<Episode>, onExport: (() -> Unit)?) {
    val rows = EpisodeLog.treatments(episodes).take(4)
    if (rows.isEmpty()) return
    val format = SimpleDateFormat("EEE d MMM", Locale.getDefault())
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Palette.Surface),
    ) {
        rows.forEachIndexed { index, episode ->
            val completed = episode.completed
            val outcome = when {
                !completed -> "Stopped at position ${episode.positionsCompleted}"
                episode.feeling == Feeling.BETTER -> "Completed, felt better"
                episode.feeling == Feeling.WORSE -> "Completed, felt worse"
                else -> "Completed"
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (completed) Palette.Holding else Palette.Urgent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (completed) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = Palette.Ground,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${format.format(Date(episode.epochMillis))} · " +
                            episode.side.word.replaceFirstChar { it.uppercase() },
                        color = Palette.Ink,
                        fontSize = AppType.ReadingFloor,
                        lineHeight = AppType.LabelLine,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppType.Sans,
                    )
                    Text(
                        outcome,
                        color = Palette.InkMuted,
                        fontSize = AppType.ReadingFloor,
                        lineHeight = AppType.LabelLine,
                        fontFamily = AppType.Sans,
                    )
                }
            }
            if (index != rows.lastIndex) {
                Box(Modifier.fillMaxWidth().padding(start = 70.dp).height(1.dp).background(Palette.Divider))
            }
        }
        if (onExport != null) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Divider))
            TextButton(onClick = onExport, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text(
                    "Export for a clinician",
                    color = Palette.Selected,
                    fontSize = AppType.ReadingFloor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = AppType.Sans,
                )
            }
        }
    }
}
