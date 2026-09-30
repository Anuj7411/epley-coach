package health.epley.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import health.epley.core.Episode
import health.epley.core.EpisodeLog
import health.epley.core.Feeling
import health.epley.core.word
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Home and Your runs, design_handoff_epley_coach_v2 §4.3 and §4.11.
 *
 * The handoff's Home carries no history list — the space goes to the one action. The run record
 * moves behind the header button, which is also where "Share with your doctor" lives, so the
 * export (and the paywall in front of it) stays reachable in one tap from Home.
 */

/** Day: "Ready when you are." Night: the handoff's quieter line, for someone woken by an attack. */
private fun greeting(night: Boolean) = if (night) "Take it slow.\nSit up first." else "Ready when you are."

@Composable
fun HomeScreenV2(
    onStart: () -> Unit,
    onPractice: () -> Unit,
    onInstrument: () -> Unit,
    onRuns: () -> Unit,
) {
    val c = Ds
    val today = remember_today()
    // Proportional, not fixed: the hero takes a share of whatever height this handset has, so it
    // fills a tall screen without overflowing a short one. Bounded both ways so the proportion
    // can never produce something unusable.
    val viewport = LocalViewportHeight.current
    val heroHeight = (viewport * 0.34f).coerceIn(200.dp, 340.dp)
    val tileHeight = (viewport * 0.20f).coerceIn(124.dp, 200.dp)
    DsScreen(
        top = {
            Row(
                Modifier.fillMaxWidth().padding(vertical = Space.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(today, style = DsType.label, color = c.muted, modifier = Modifier.weight(1f))
                DsIconCircle(Icons.Rounded.Settings, "Your runs and settings", onRuns)
            }
        },
    ) {
        Text(
            greeting(c.night),
            style = DsType.title,
            color = c.ink,
            modifier = Modifier.padding(start = Space.s, bottom = Space.s).enter(0),
        )

        // The one thing this screen is for, given the space that implies.
        Box(Modifier.enter(1)) {
            DsCard(
                fill = c.lilac,
                tint = c.lilacTint,
                outline = c.lilac,
                radius = Radius.hero,
                minHeight = heroHeight,
                onClick = onStart,
            ) {
                DsChip(
                    "Safety check first",
                    Icons.Rounded.CheckCircle,
                    if (c.night) ChipStyle.Outlined else ChipStyle.OnPastel,
                    c.lilac,
                )
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Start\ntreatment",
                            style = DsType.cardTitle,
                            color = if (c.night) c.ink else Color(0xFF17161C),
                        )
                        Text(
                            "5 positions · about 6 min",
                            style = DsType.label,
                            color = if (c.night) c.muted else Color(0xCC17161C),
                        )
                    }
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (c.night) c.lilac else Color(0xFF17161C)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = if (c.night) Color(0xFF17161C) else Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().enter(2),
            horizontalArrangement = Arrangement.spacedBy(Space.s),
        ) {
            SideTile(
                modifier = Modifier.weight(1f),
                minHeight = tileHeight,
                line1 = "Practise",
                line2 = "in hand",
                onClick = onPractice,
            ) { HandGlyph(if (c.night) c.butter else Color(0xFFE0A800)) }
            SideTile(
                modifier = Modifier.weight(1f),
                minHeight = tileHeight,
                line1 = "Check",
                line2 = "sensors",
                onClick = onInstrument,
            ) { SignalGlyphV2(c.muted) }
        }

        // Required in-app (§1.1). It keeps its own card and sits above the fold.
        Box(Modifier.enter(3)) {
            DsWarning(
                caption = "NOT A MEDICAL DEVICE",
                body = "An unregulated prototype. It must never be used on a patient.",
            )
        }
    }
}

@Composable
private fun remember_today(): String {
    val format = SimpleDateFormat("EEEE d MMMM", Locale.getDefault())
    return format.format(Date())
}

@Composable
private fun SideTile(
    modifier: Modifier,
    minHeight: Dp,
    line1: String,
    line2: String,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit,
) {
    val c = Ds
    Column(
        modifier
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.card))
            .background(c.surface)
            .heightIn(min = minHeight)
            .padding(Space.l),
        verticalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        glyph()
        Spacer(Modifier.weight(1f))
        Column {
            Text(line1, style = DsType.cardTitle, color = c.ink)
            Text(line2, style = DsType.cardTitle, color = c.ink)
        }
    }
}

/** A raised hand. Drawn, because the extended icon set is not a dependency. */
@Composable
private fun HandGlyph(colour: Color) {
    Canvas(Modifier.size(28.dp)) {
        val w = size.width
        val h = size.height
        val finger = w * 0.12f
        listOf(0.30f, 0.19f, 0.21f, 0.31f).forEachIndexed { i, top ->
            val x = w * (0.31f + i * 0.145f)
            drawLine(colour, Offset(x, h * top), Offset(x, h * 0.62f), finger, StrokeCap.Round)
        }
        drawLine(colour, Offset(w * 0.20f, h * 0.50f), Offset(w * 0.31f, h * 0.66f), finger, StrokeCap.Round)
        drawRoundRect(
            color = colour,
            topLeft = Offset(w * 0.25f, h * 0.55f),
            size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.33f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(finger, finger),
        )
    }
}

/** A phone held upright. Drawn, because the extended icon set is not a dependency. */
@Composable
private fun PhoneGlyphV2(colour: Color) {
    Canvas(Modifier.size(26.dp)) {
        val weight = 2.4.dp.toPx()
        val inset = size.width * 0.24f
        drawRoundRect(
            color = colour,
            topLeft = Offset(inset, size.height * 0.05f),
            size = androidx.compose.ui.geometry.Size(size.width - inset * 2f, size.height * 0.90f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(weight * 1.8f, weight * 1.8f),
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

/** Concentric arcs around a dot: something read off the air. */
@Composable
private fun SignalGlyphV2(colour: Color) {
    Canvas(Modifier.size(26.dp)) {
        val weight = 2.4.dp.toPx()
        val middle = Offset(size.width / 2f, size.height / 2f)
        drawCircle(colour, radius = size.minDimension * 0.11f, center = middle)
        listOf(0.28f, 0.44f).forEach { fraction ->
            val radius = size.minDimension * fraction
            val box = Offset(middle.x - radius, middle.y - radius)
            val extent = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
            drawArc(colour, -58f, 116f, false, box, extent, style = Stroke(weight, cap = StrokeCap.Round))
            drawArc(colour, 122f, 116f, false, box, extent, style = Stroke(weight, cap = StrokeCap.Round))
        }
    }
}

/**
 * Your runs (§4.11). Grouped by month, a mint check or coral cross per row, and the lilac
 * "Share with your doctor" card — which is the export, and therefore the paywall's front door.
 */
@Composable
fun RunsScreenV2(
    episodes: List<Episode>,
    onExport: (() -> Unit)?,
    onBack: () -> Unit,
) {
    val c = Ds
    val rows = EpisodeLog.treatments(episodes)
    val month = SimpleDateFormat("MMMM", Locale.getDefault())
    val day = SimpleDateFormat("EEE d MMM", Locale.getDefault())
    DsScreen(
        top = {
            Row(Modifier.fillMaxWidth().padding(vertical = Space.s), verticalAlignment = Alignment.CenterVertically) {
                DsIconCircle(Icons.Rounded.ArrowForward, "Back", onBack)
            }
        },
        bottom = {
            if (onExport != null) {
                DsCard(fill = c.lilac, tint = c.lilacTint, outline = c.lilac, onClick = onExport) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Share with your doctor",
                                style = DsType.cardTitle,
                                color = if (c.night) c.ink else Color(0xFF17161C),
                            )
                            Text(
                                "Every run, as plain text",
                                style = DsType.label,
                                color = if (c.night) c.muted else Color(0xCC17161C),
                            )
                        }
                        Box(
                            Modifier.size(48.dp).clip(RoundedCornerShape(999.dp))
                                .background(if (c.night) c.lilac else Color(0xFF17161C)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Share,
                                contentDescription = null,
                                tint = if (c.night) Color(0xFF17161C) else Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }
        },
    ) {
        Text("Your runs", style = DsType.title, color = c.ink, modifier = Modifier.padding(start = Space.s).enter(0))
        if (rows.isEmpty()) {
            DsCard(modifier = Modifier.enter(1)) {
                Text("No runs yet.", style = DsType.body, color = c.muted)
            }
            return@DsScreen
        }
        var lastMonth = ""
        rows.forEachIndexed { index, episode ->
            val thisMonth = month.format(Date(episode.epochMillis))
            if (thisMonth != lastMonth) {
                lastMonth = thisMonth
                Text(
                    thisMonth,
                    style = DsType.label,
                    color = c.muted,
                    modifier = Modifier.padding(start = Space.s, top = Space.m).enter(minOf(index, 4)),
                )
            }
            val completed = episode.completed
            DsCard(modifier = Modifier.enter(minOf(index, 4)), padding = Space.l) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DsIconTile(
                        if (completed) Icons.Rounded.CheckCircle else Icons.Rounded.Close,
                        fill = if (c.night) (if (completed) c.mintTint else c.coralTint) else (if (completed) c.mint else c.coral),
                        tint = if (c.night) (if (completed) c.mint else c.coral) else Color(0xFF17161C),
                    )
                    Spacer(Modifier.width(Space.m))
                    Column(Modifier.weight(1f)) {
                        Text(day.format(Date(episode.epochMillis)), style = DsType.rowTitle, color = c.ink)
                        Text(
                            buildString {
                                append(episode.side.word.replaceFirstChar { it.uppercase() })
                                append(" ear · ")
                                append(
                                    when {
                                        !completed -> "stopped at ${episode.positionsCompleted}"
                                        episode.feeling == Feeling.BETTER -> "all 5 held, felt better"
                                        episode.feeling == Feeling.WORSE -> "all 5 held, felt worse"
                                        else -> "all 5 held"
                                    },
                                )
                            },
                            style = DsType.label,
                            color = c.muted,
                        )
                    }
                }
            }
        }
    }
}
