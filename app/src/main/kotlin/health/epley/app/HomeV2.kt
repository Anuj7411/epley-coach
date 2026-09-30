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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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

/** Day and night headlines (README §4.3). Night is for someone woken by an attack. */
private fun greeting(night: Boolean) = if (night) "Take it slow.\nSit up first." else "Ready when\nyou are."

/** Today's date as the header shows it: "Tuesday 30 September". */
fun todayLabel(): String = SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(Date())

/** Home (README §4.3). No logo: the space goes to the one action. Flex region: the Start card. */
@Composable
fun HomeScreenV2(
    onStart: () -> Unit,
    onPractice: () -> Unit,
    onInstrument: () -> Unit,
    onRuns: () -> Unit,
    date: String = todayLabel(),
) {
    val c = Ds
    val n = c.night
    val ink = Color(0xFF17161C)
    DScreen(bg = c.ground, bottom = if (n) 16.dp else 0.dp) {
        // Header: date and the settings circle (which leads to Your runs and the export).
        Row(
            Modifier.padding(horizontal = 8.dp).enter(0),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Txt(date, style = type(16f, 600), color = c.muted, maxLines = 1)
            Box(
                Modifier.size(48.dp).pressable(onClick = onRuns).box(if (n) c.surface else Color.White, 24.dp),
                contentAlignment = Alignment.Center,
            ) { Sym("settings", 24f, c.ink) }
        }
        Box(Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 24.dp).enter(1)) {
            Txt(greeting(n), style = type(44f, 800, lineHeight = 1f, letterSpacing = -0.035f), color = c.ink)
        }

        // The Start card takes whatever height is left (min 232).
        Column(
            Modifier
                .flex()
                .heightIn(min = 232.dp)
                .enter(2)
                .pressable(onClick = onStart)
                // No ring at night: the design file types its ring inside the onClick attribute,
                // so the rendered design has none. Matching what the design renders.
                .box(if (n) c.lilacTint else c.lilac, 32.dp)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                Modifier
                    .box(if (n) Color.Transparent else Color.White.copy(alpha = 0.55f), 999.dp, ring = if (n) c.lilac else null)
                    .padding(start = if (n) 8.dp else 12.dp, end = 12.dp, top = if (n) 6.dp else 8.dp, bottom = if (n) 6.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("health_and_safety", 20f, if (n) c.lilac else ink, weight = if (n) 700 else 600)
                Txt(
                    "Safety check first",
                    style = type(16f, if (n) 700 else 600),
                    color = if (n) c.lilac else ink,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Txt(
                        "Start\ntreatment",
                        style = type(34f, 800, lineHeight = 1f, letterSpacing = -0.03f),
                        color = if (n) c.ink else ink,
                    )
                    Txt(
                        "5 positions · about 6 min",
                        style = type(16f, 600),
                        color = if (n) c.soft else ink,
                        maxLines = 1,
                    )
                }
                Box(
                    Modifier.size(64.dp).box(if (n) c.lilac else ink, 32.dp),
                    contentAlignment = Alignment.Center,
                ) { Sym("arrow_forward", 32f, if (n) ink else Color.White) }
            }
        }

        Row(Modifier.enter(3), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeTile(
                "Practise\nin hand", "back_hand",
                fill = if (n) c.surface else c.butter,
                iconColour = if (n) c.butter else ink,
                onClick = onPractice,
                modifier = Modifier.weight(1f),
            )
            HomeTile(
                "Check\nsensors", "sensors",
                fill = if (n) c.surface else Color.White,
                iconColour = if (n) c.muted else ink,
                onClick = onInstrument,
                modifier = Modifier.weight(1f),
            )
        }

        // Required in-app (§1.1).
        Row(
            Modifier
                .enter(4)
                .box(if (n) c.coralTint else c.coral, 28.dp, ring = if (n) c.coral else null)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(48.dp).box(if (n) c.coral else Color.White.copy(alpha = 0.6f), 16.dp),
                contentAlignment = Alignment.Center,
            ) { Sym("warning", 24f, ink) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt(
                    "NOT A MEDICAL DEVICE",
                    style = type(14f, 800, letterSpacing = 0.06f),
                    color = if (n) c.coral else ink,
                )
                Txt(
                    "An unregulated prototype. It must never be used on a patient.",
                    style = type(16f, 600, lineHeight = 1.35f, wrap = Wrap.Pretty),
                    color = if (n) c.ink else ink,
                )
            }
        }
    }
}

@Composable
private fun HomeTile(
    label: String,
    icon: String,
    fill: Color,
    iconColour: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .heightIn(min = 136.dp)
            .pressable(onClick = onClick)
            .box(fill, 28.dp)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Sym(icon, 28f, iconColour)
        Spacer(Modifier.height(24.dp))
        Txt(label, style = type(20f, 700, lineHeight = 1.1f, letterSpacing = -0.01f), color = Ds.ink)
    }
}

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
                DsIconCircle(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
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
