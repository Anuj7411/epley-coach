package health.epley.app

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import health.epley.core.Epley
import health.epley.core.Episode
import health.epley.core.EpisodeLog
import health.epley.core.Side
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * The history as a PDF for a doctor: every treatment run, how long it took, and for each position
 * the hold time and the head angles the phone measured when it was held.
 *
 * A doctor seeing this weeks after the attack has a few minutes; one A4 page per few runs, plain
 * sentences, no charts. Practice runs are left out — they measured a phone, not a head.
 */
object HistoryPdf {

    private const val W = 595 // A4 in points
    private const val H = 842
    private const val MARGIN = 56f

    fun write(context: Context, episodes: List<Episode>): File {
        val runs = EpisodeLog.treatments(episodes)
        val doc = PdfDocument()
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x17, 0x16, 0x1C)
            textSize = 22f
            typeface = runCatching { ResourcesCompat.getFont(context, R.font.bricolage_grotesque) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        }
        val strong = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0x17, 0x16, 0x1C); textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0x17, 0x16, 0x1C); textSize = 11f }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0x55, 0x53, 0x5E); textSize = 10f }

        var pageNo = 0
        var page: PdfDocument.Page? = null
        var y = 0f
        fun newPage() {
            page?.let { doc.finishPage(it) }
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            y = MARGIN
        }
        fun line(text: String, paint: Paint, gapAfter: Float = 4f) {
            // Wrap to the text width.
            var rest = text
            while (rest.isNotEmpty()) {
                if (y + paint.textSize > H - MARGIN) newPage()
                val n = paint.breakText(rest, true, W - 2 * MARGIN, null).coerceAtLeast(1)
                val cut = if (n < rest.length) rest.lastIndexOf(' ', n).takeIf { it > 0 } ?: n else n
                y += paint.textSize
                page!!.canvas.drawText(rest.substring(0, cut).trimEnd(), MARGIN, y, paint)
                y += 3f
                rest = rest.substring(cut).trimStart()
            }
            y += gapAfter
        }

        newPage()
        line("Epley Coach — self-treatment history", heading, 6f)
        line("Generated ${SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date())}", muted, 18f)

        val when_ = SimpleDateFormat("EEE d MMM yyyy, h:mm a", Locale.getDefault())
        val names = listOf("Turn head right", "Lie back", "Turn head left", "Roll onto left side", "Sit up")
        val holds = Epley.steps().map { it.holdSeconds }
        if (runs.isEmpty()) line("No runs recorded.", body)
        for (e in runs) {
            val ear = if (e.side == Side.LEFT) 'L' else 'R'
            val minutes = if (e.durationMillis > 0) " · ${(e.durationMillis / 60_000.0).roundToInt().coerceAtLeast(1)} min" else ""
            val outcome = if (e.completed) "all 5 positions held" else "stopped at position ${e.positionsCompleted + 1} of 5"
            line("${when_.format(Date(e.epochMillis))} — ${if (ear == 'L') "Left" else "Right"} ear — $outcome$minutes", strong, 2f)
            for (i in 0 until minOf(5, e.positionsCompleted)) {
                val angles = e.heldAngles.getOrNull(i)?.let { (turn, tip) -> " — measured turn ${deg(turn)}, tip ${deg(tip)}" } ?: ""
                line("   ${i + 1}. ${mirrorFor(ear, names[i])}: held ${holds[i]} s$angles", body, 0f)
            }
            y += 12f
        }
        y += 8f
        line(
            "Each run is a guided Epley manoeuvre for posterior canal BPPV. The ear was identified by " +
                "a six-question symptom triage (Kim HJ et al., Neurology 2020; about 71% agreement with " +
                "a specialist). Head angles were measured with the phone's orientation sensor, relative " +
                "to a seated calibration. Turn is toward the treated ear; tip is below horizontal when " +
                "negative. This app is not a medical device and does not diagnose.",
            muted,
        )
        page?.let { doc.finishPage(it) }

        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "epley-coach-history.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
