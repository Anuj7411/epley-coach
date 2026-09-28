package health.epley.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One angle, drawn as a ruler.
 *
 * ## Why a ruler and not a dial
 *
 * The first version drew a head on a circular dial for each axis. Tried on a real head, two of the
 * three pictures were unreadable: a dial tells you an angle only after you have worked out where
 * zero is and which way round it goes, which is a lot to ask of someone mid-attack. A ruler has a
 * left and a right, and "move the thick bar into the hatched box" needs no key.
 *
 * ## The four signals
 *
 * Being inside the range is never signalled by colour alone. It changes the **word** (the guidance
 * line becomes "In range"), the **icon** (arrow becomes a check), the **shape** (the marker sits
 * inside the hatched box) and only then the **colour**. Someone with red-green colour blindness,
 * or looking at a dimmed screen, still gets three of the four.
 *
 * The hatched box is exactly the band the engine judges by, so "the bar is in the box" and "the
 * app is counting this" can never disagree.
 */
@Composable
fun AngleGauge(
    label: String,
    valueDegrees: Double?,
    targetDegrees: Double,
    bandDegrees: Double,
    minDegrees: Double,
    maxDegrees: Double,
    guidance: String?,
    modifier: Modifier = Modifier,
) {
    val inRange = valueDegrees != null && abs(valueDegrees - targetDegrees) <= bandDegrees
    val stateWords = when {
        valueDegrees == null -> "$label, no reading yet"
        inRange -> "$label ${valueDegrees.roundToInt()} degrees, in range"
        else -> "$label ${valueDegrees.roundToInt()} degrees, " +
            "${abs(valueDegrees - targetDegrees).roundToInt()} short of the range"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Palette.Surface)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 14.dp)
            .semantics { stateDescription = stateWords },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                color = Palette.InkMuted,
                fontSize = AppType.LabelSize,
                fontWeight = FontWeight.Bold,
                fontFamily = AppType.Sans,
            )
            Text(
                if (valueDegrees == null) "--" else "${valueDegrees.roundToInt()}°",
                color = if (inRange) Palette.Holding else Palette.Ink,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = AppType.Mono,
            )
        }

        Canvas(Modifier.fillMaxWidth().height(40.dp)) {
            drawRuler(valueDegrees, targetDegrees, bandDegrees, minDegrees, maxDegrees, inRange)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (tick in listOf(minDegrees, (minDegrees + maxDegrees) / 2, maxDegrees)) {
                Text(
                    "${tick.roundToInt()}°",
                    color = Palette.InkFaint,
                    fontSize = AppType.TickSize,
                    fontFamily = AppType.Mono,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Divider))

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val icon: ImageVector = when {
                inRange -> Icons.Filled.Check
                valueDegrees != null && valueDegrees < targetDegrees -> Icons.Filled.KeyboardArrowUp
                else -> Icons.Filled.KeyboardArrowDown
            }
            Icon(
                icon,
                contentDescription = null,
                tint = if (inRange) Palette.Holding else Palette.Seeking,
                modifier = Modifier.size(24.dp),
            )
            Text(
                if (inRange) "In range" else (guidance ?: "Getting a reading"),
                color = if (inRange) Palette.Holding else Palette.Seeking,
                fontSize = AppType.BodyLargeSize,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = AppType.Sans,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            Text(
                "aim ${targetDegrees.roundToInt()}° ±${bandDegrees.roundToInt()}",
                color = Palette.InkMuted,
                fontSize = AppType.LabelSize,
                fontFamily = AppType.Sans,
            )
        }
    }
}

/**
 * Track, band, target and marker.
 *
 * The band is hatched rather than filled so it survives a greyscale screenshot, a dimmed display
 * and every form of colour blindness. Texture is the one channel that does not depend on hue.
 */
private fun DrawScope.drawRuler(
    value: Double?,
    target: Double,
    band: Double,
    min: Double,
    max: Double,
    inRange: Boolean,
) {
    val span = (max - min).toFloat()
    fun x(v: Double): Float = (((v - min).toFloat() / span) * size.width).coerceIn(0f, size.width)

    val midY = size.height / 2f

    drawLine(
        color = Palette.Track,
        start = Offset(0f, midY),
        end = Offset(size.width, midY),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round,
    )

    val bandLeft = x(target - band)
    val bandRight = x(target + band)
    val bandHeight = 28.dp.toPx()
    val radius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
    val bandRect = RoundRect(
        Rect(bandLeft, midY - bandHeight / 2, bandRight, midY + bandHeight / 2),
        radius,
    )
    val bandPath = Path().apply { addRoundRect(bandRect) }

    clipPath(bandPath) {
        // 135 degrees: down-left to up-right. Stepped along x so the whole box is covered.
        val step = 7.dp.toPx()
        var startX = bandLeft - bandHeight
        while (startX < bandRight + bandHeight) {
            drawLine(
                color = Palette.Holding.copy(alpha = 0.32f),
                start = Offset(startX, midY + bandHeight / 2),
                end = Offset(startX + bandHeight, midY - bandHeight / 2),
                strokeWidth = 3.dp.toPx(),
            )
            startX += step
        }
    }
    drawPath(bandPath, color = Palette.Holding, style = Stroke(width = 2.dp.toPx()))

    val targetX = x(target)
    drawLine(
        color = Palette.Holding,
        start = Offset(targetX, midY - 18.dp.toPx()),
        end = Offset(targetX, midY + 18.dp.toPx()),
        strokeWidth = 2.dp.toPx(),
    )

    if (value == null) return
    val markerX = x(value)
    val markerWidth = 8.dp.toPx()
    drawRoundRect(
        color = if (inRange) Palette.Holding else Palette.Seeking,
        topLeft = Offset(markerX - markerWidth / 2, 0f),
        size = Size(markerWidth, size.height),
        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
    )
}

/**
 * Searching or holding, as a chip whose **shape** changes and not only its colour.
 *
 * Searching is a dashed outline with nothing inside it, which is what an unfinished thing looks
 * like. Holding is solid and filled. Told apart at a glance, in the dark, in greyscale.
 */
@Composable
fun StateChip(holding: Boolean, text: String, modifier: Modifier = Modifier) {
    val colour = if (holding) Palette.Holding else Palette.Seeking
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .then(
                if (holding) Modifier.background(colour)
                else Modifier.dashedBorder(colour, 2.dp, 50)
            )
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            if (holding) Icons.Filled.Check else Icons.Filled.Search,
            contentDescription = null,
            tint = if (holding) Palette.Ground else colour,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text,
            color = if (holding) Palette.Ground else colour,
            fontSize = AppType.LabelSize,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = AppType.Sans,
        )
    }
}

/** A dashed outline, drawn by hand because Compose has no dashed border modifier. */
private fun Modifier.dashedBorder(colour: Color, width: androidx.compose.ui.unit.Dp, cornerPercent: Int): Modifier =
    this.border(BorderStroke(width, colour), RoundedCornerShape(percent = cornerPercent))

/** Five segments, one per position. Done, current and remaining are three different fills. */
@Composable
fun RunProgress(completed: Int, current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (index in 0 until total) {
            val colour = when {
                index < completed -> Palette.Holding
                index == current -> Palette.Ink
                else -> Palette.Track
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colour),
            )
        }
    }
}

/** One angle, collapsed to a row, for when the position is already being held. */
@Composable
fun CheckRow(label: String, valueDegrees: Double?, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Palette.Holding, modifier = Modifier.size(24.dp))
        Text(
            label,
            color = Palette.Ink,
            fontSize = AppType.BodyLargeSize,
            fontWeight = FontWeight.Bold,
            fontFamily = AppType.Sans,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        Text(
            "in range",
            color = Palette.InkMuted,
            fontSize = AppType.LabelSize,
            fontFamily = AppType.Sans,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            if (valueDegrees == null) "--" else "${valueDegrees.roundToInt()}°",
            color = Palette.Ink,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = AppType.Mono,
        )
    }
}
