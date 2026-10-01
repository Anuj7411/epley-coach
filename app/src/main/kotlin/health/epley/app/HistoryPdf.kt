package health.epley.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.PathParser
import health.epley.core.Epley
import health.epley.core.Episode
import health.epley.core.EpisodeLog
import health.epley.core.Side
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The history as a PDF for a doctor (v2.1 §16 H, "Doctor PDF.dc.html"): a header with the period,
 * the run count and the ears treated; one block per treatment run with each position's hold time
 * and the angles the phone measured; the method and the not-a-medical-device box; a running footer.
 *
 * A4 in points, the design's 16 mm margins, its millimetre column grid, and its CSS sizes converted
 * at 1 px = 0.75 pt. A run never splits across pages, and neither does the closing section.
 * Practice runs are left out — they measured a phone, not a head.
 */
object HistoryPdf {

    fun write(context: Context, episodes: List<Episode>, now: Long = System.currentTimeMillis()): File {
        val doc = PdfDocument()
        val font = runCatching { ResourcesCompat.getFont(context, R.font.bricolage_grotesque) }.getOrNull()
        render(font, episodes, now) { pageNo ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), pageNo).create())
            page.canvas to { doc.finishPage(page) }
        }
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(dir, "epley-coach-history.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    const val PAGE_W = 595f
    const val PAGE_H = 842f
    private const val MM = 72f / 25.4f
    private const val PX = 0.75f
    private val M = 16 * MM

    private val INK = Color.rgb(0x17, 0x16, 0x1C)
    private val MUTED = Color.rgb(0x55, 0x53, 0x5E)
    private val RULE = Color.rgb(0xD4, 0xD2, 0xDC)
    private val HAIR = Color.rgb(0xEF, 0xED, 0xE8)
    private val MINT = Color.rgb(0xA8, 0xEB, 0xD6)
    private val CORAL = Color.rgb(0xFF, 0xC4, 0xB8)

    /**
     * Lays the document out page by page. [newPage] returns a canvas and the call that finishes it,
     * so the same code draws into a PdfDocument for sharing and into a bitmap for the parity test.
     */
    fun render(font: Typeface?, episodes: List<Episode>, now: Long, newPage: (Int) -> Pair<Canvas, () -> Unit>) {
        val runs = EpisodeLog.treatments(episodes)
        val l = Layout(font)
        var pageNo = 0
        lateinit var canvas: Canvas
        var finish: () -> Unit = {}
        var y = 0f
        val bottom = PAGE_H - M
        val dateOnly = dates("d MMM yyyy")

        fun page() {
            if (pageNo > 0) finish()
            pageNo++
            val (c, f) = newPage(pageNo)
            canvas = c
            finish = f
            y = M
            l.footer(canvas, "Epley Coach · run history · not a medical device", "Generated on this phone, ${dateOnly.format(Date(now))}")
        }

        page()
        y = l.header(canvas, y, period(runs), runCount(runs), earsTreated(runs))
        y += 8 * MM
        if (runs.isEmpty()) {
            y = l.empty(canvas, y)
        } else {
            y = l.columnHeads(canvas, y)
            runs.forEachIndexed { i, e ->
                val block = runBlock(e)
                val h = l.articleHeight(block, withHead = i == 0)
                // break-inside: avoid — a run is never split across pages.
                if (y + h > bottom && y > M + 1) page()
                y = l.article(canvas, y, block, withHead = i == 0)
            }
        }
        val closing = l.closingHeight()
        if (y + 10 * MM + closing > bottom) page() else y += 10 * MM
        l.closing(canvas, y)
        finish()
    }

    // ---------------------------------------------------------------------------------------
    // Content, from the recorded runs
    // ---------------------------------------------------------------------------------------

    internal class Row(val pos: Int, val name: String, val held: String, val turn: String, val tip: String, val stopped: Boolean)
    internal class Block(val date: String, val ear: String, val outcome: String, val completed: Boolean, val time: String, val rows: List<Row>)

    private val NAMES = listOf(
        "Head turned right, seated",
        "Lying back, head hanging",
        "Head turned left, hanging",
        "Rolled onto left side",
        "Sitting up, facing forward",
    )

    private fun start(e: Episode) = e.epochMillis - e.durationMillis

    /** English dates with the design's three-letter months ("Sep"; newer JDKs write "Sept"). */
    private fun dates(pattern: String) = SimpleDateFormat(pattern, Locale.UK).apply {
        dateFormatSymbols = java.text.DateFormatSymbols(Locale.UK).apply {
            shortMonths = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        }
    }

    internal fun runBlock(e: Episode): Block {
        val ear = if (e.side == Side.LEFT) 'L' else 'R'
        val holds = Epley.steps().map { it.holdSeconds }
        val done = e.positionsCompleted.coerceIn(0, 5)
        val rows = buildList {
            for (i in 0 until done) {
                val a = e.heldAngles.getOrNull(i)
                add(Row(i + 1, mirrorFor(ear, NAMES[i]), "${holds[i]} s", a?.first?.let(::deg) ?: "—", a?.second?.let(::deg) ?: "—", false))
            }
            val stop = e.stop
            if (!e.completed && done < 5 && stop != null) {
                add(
                    Row(
                        done + 1, mirrorFor(ear, NAMES[done]), "stopped at ${stop.heldSeconds} s",
                        stop.turn?.let(::deg) ?: "—", stop.tip?.let(::deg) ?: "—", true,
                    ),
                )
            }
        }
        return Block(
            date = dates("d MMM yyyy, HH:mm").format(Date(start(e))),
            ear = (if (ear == 'L') "Left" else "Right") + " · posterior",
            outcome = if (e.completed) "All 5 positions held" else "Stopped at position ${done + 1}",
            completed = e.completed,
            time = if (e.durationMillis > 0) "${(e.durationMillis / 60_000.0).roundToInt().coerceAtLeast(1)} min" else "—",
            rows = rows,
        )
    }

    internal fun period(runs: List<Episode>): String {
        if (runs.isEmpty()) return "—"
        val first = Date(runs.minOf(::start))
        val last = Date(runs.maxOf(::start))
        val full = dates("d MMM yyyy")
        val cal = { d: Date -> Calendar.getInstance().apply { time = d } }
        val sameYear = cal(first).get(Calendar.YEAR) == cal(last).get(Calendar.YEAR)
        if (full.format(first) == full.format(last)) return full.format(last)
        val from = dates(if (sameYear) "d MMM" else "d MMM yyyy").format(first)
        return "$from – ${full.format(last)}"
    }

    internal fun runCount(runs: List<Episode>): String {
        val done = runs.count { it.completed }
        val stopped = runs.size - done
        val parts = listOfNotNull(
            if (done > 0) "$done completed" else null,
            if (stopped > 0) "$stopped stopped" else null,
        )
        return if (parts.isEmpty()) "0" else "${runs.size} (${parts.joinToString(", ")})"
    }

    internal fun earsTreated(runs: List<Episode>): String {
        if (runs.isEmpty()) return "—"
        return runs.groupingBy { it.side }.eachCount().entries
            .sortedByDescending { it.value }
            .joinToString(", ") { (side, n) -> (if (side == Side.LEFT) "Left" else "Right") + " ×$n" }
    }

    // ---------------------------------------------------------------------------------------
    // Drawing
    // ---------------------------------------------------------------------------------------

    private class Layout(private val font: Typeface?) {
        val x0 = M
        val w = PAGE_W - 2 * M

        fun paint(sizePt: Float, weight: Int, color: Int, tnum: Boolean = false, tracking: Float = 0f) =
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
                typeface = font ?: Typeface.create(Typeface.DEFAULT, if (weight >= 700) Typeface.BOLD else Typeface.NORMAL)
                textSize = sizePt
                this.color = color
                letterSpacing = tracking
                if (Build.VERSION.SDK_INT >= 26 && font != null) {
                    // Optical size follows the CSS pixel size, as the browser does.
                    fontVariationSettings = "'wght' $weight, 'opsz' ${(sizePt / PX).coerceIn(12f, 96f)}"
                }
                if (tnum) fontFeatureSettings = "'tnum'"
            }

        /** CSS line-height: normal. */
        fun normal(p: Paint) = p.fontMetrics.let { it.descent - it.ascent + it.leading }

        /** Draws one line in a line box [top, top + lh), on the browser's baseline. */
        fun text(c: Canvas, s: String, x: Float, top: Float, lh: Float, p: Paint, alignRight: Boolean = false) {
            val fm = p.fontMetrics
            val base = top + (lh - (fm.descent - fm.ascent)) / 2 - fm.ascent
            c.drawText(s, if (alignRight) x - p.measureText(s) else x, base, p)
        }

        fun rule(c: Canvas, y: Float, h: Float, color: Int, x: Float = x0, width: Float = w) {
            c.drawRect(x, y, x + width, y + h, Paint().apply { this.color = color })
        }

        // Header -------------------------------------------------------------------------

        fun header(c: Canvas, top: Float, period: String, runs: String, ears: String): Float {
            val markH = 22 * PX
            val coach = paint(13f, 700, MUTED)
            val h1 = paint(26f, 800, INK, tracking = -0.03f)
            val leftH = markH + 10 * PX + 26f
            val label = paint(10f, 600, MUTED)
            val value = paint(10f, 700, INK)
            val rowH = 10f * 1.35f
            val rowGap = 2 * PX
            val rightH = 3 * rowH + 2 * rowGap
            val hH = max(leftH, rightH)

            // Left: wordmark + "coach", then the title. Items sit on the row's end edge.
            val lTop = top + hH - leftH
            val markW = markH * MarkWidth / MarkHeight
            wordmark(c, x0, lTop, markH)
            text(c, "coach", x0 + markW + 6 * PX, lTop + markH - 1 * PX - 13f, 13f, coach)
            text(c, "Run history", x0, lTop + markH + 10 * PX, 26f, h1)

            // Right: a two-column grid, right-aligned.
            val pairs = listOf("Period" to period, "Runs" to runs, "Ears treated" to ears)
            val lw = pairs.maxOf { label.measureText(it.first) }
            val vw = pairs.maxOf { value.measureText(it.second) }
            val gx = x0 + w - (lw + 14 * PX + vw)
            var ry = top + hH - rightH
            for ((k, v) in pairs) {
                text(c, k, gx, ry, rowH, label)
                text(c, v, gx + lw + 14 * PX, ry, rowH, value)
                ry += rowH + rowGap
            }
            val ruleY = top + hH + 16 * PX
            rule(c, ruleY, 2 * PX, INK)
            return ruleY + 2 * PX
        }

        fun wordmark(c: Canvas, x: Float, top: Float, h: Float) {
            val s = h / MarkHeight
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = INK; style = Paint.Style.STROKE; strokeWidth = MarkStroke
                strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            }
            c.save()
            c.translate(x, top)
            c.scale(s, s)
            c.translate(-MarkLeft, -MarkTop)
            WordmarkStrokes.forEach { d -> PathParser.createPathFromPathData(d)?.let { c.drawPath(it, p) } }
            c.drawCircle(256f, 120f, 52f, p)
            c.restore()
        }

        // Runs ---------------------------------------------------------------------------

        val c1 = 44 * MM
        val c2 = 34 * MM
        val c4 = 16 * MM
        val cg = 6 * MM
        val c3 = w - c1 - c2 - c4 - 3 * cg

        fun columnHeads(c: Canvas, top: Float): Float {
            val p = paint(9f, 700, MUTED, tracking = 0.04f)
            val lh = normal(p)
            text(c, "DATE", x0, top, lh, p)
            text(c, "EAR", x0 + c1 + cg, top, lh, p)
            text(c, "OUTCOME", x0 + c1 + c2 + 2 * cg, top, lh, p)
            text(c, "TIME", x0 + w, top, lh, p, alignRight = true)
            val y = top + lh + 6 * PX
            rule(c, y, 1 * PX, RULE)
            return y + 1 * PX
        }

        fun empty(c: Canvas, top: Float): Float {
            val p = paint(11f, 700, MUTED)
            val lh = normal(p)
            text(c, "No runs yet.", x0, top + 12 * PX, lh, p)
            val y = top + 12 * PX + lh + 14 * PX
            rule(c, y, 1 * PX, RULE)
            return y + 1 * PX
        }

        private val summary = { w: Int -> paint(11f, w, INK) }
        private val cell = paint(9.5f, 600, INK, tnum = true)
        private val cellMuted = paint(9.5f, 600, MUTED, tnum = true)
        private val head = paint(9.5f, 700, MUTED, tnum = true)

        fun tableHeights(rows: Int, withHead: Boolean): Float {
            val lh = normal(cell)
            // A collapsed row border adds its own width to the row (each body row but an unheaded first).
            val bordered = if (withHead) rows else (rows - 1).coerceAtLeast(0)
            return (if (withHead) lh + 6 * PX else 0f) + rows * (lh + 8 * PX) + bordered * PX
        }

        fun articleHeight(b: Block, withHead: Boolean): Float =
            12 * PX + normal(summary(800)) + 10 * PX + tableHeights(b.rows.size, withHead) + 14 * PX + 1 * PX

        fun article(c: Canvas, top: Float, b: Block, withHead: Boolean): Float {
            var y = top + 12 * PX
            val s800 = summary(800)
            val s700 = summary(700)
            val lh = normal(s800)
            text(c, b.date, x0, y, lh, s800)
            text(c, b.ear, x0 + c1 + cg, y, lh, s700)
            val ox = x0 + c1 + c2 + 2 * cg
            val dot = 9 * PX
            val dy = y + (lh - dot) / 2
            val r = RectF(ox, dy, ox + dot, dy + dot)
            val corner = if (b.completed) 5 * PX else 2 * PX
            c.drawRoundRect(r, corner, corner, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (b.completed) MINT else CORAL })
            val ring = 1.5f * PX
            c.drawRoundRect(
                RectF(r.left + ring / 2, r.top + ring / 2, r.right - ring / 2, r.bottom - ring / 2), corner - ring / 2, corner - ring / 2,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; style = Paint.Style.STROKE; strokeWidth = ring },
            )
            text(c, b.outcome, ox + dot + 6 * PX, y, lh, s700)
            text(c, b.time, x0 + w, y, lh, summary(700).apply { fontFeatureSettings = "'tnum'" }, alignRight = true)
            y += lh + 10 * PX
            y = table(c, y, b.rows, withHead)
            y += 14 * PX
            rule(c, y, 1 * PX, RULE)
            return y + 1 * PX
        }

        /** Pos 12 %, Position 40 %, and the three numeric columns share the rest by content. */
        private fun table(c: Canvas, top: Float, rows: List<Row>, withHead: Boolean): Float {
            val lh = normal(cell)
            val heads = listOf("Held", "Turn", "Tip")
            val values = rows.map { listOf(it.held, it.turn, it.tip) }
            val widths = (0..2).map { k ->
                max(if (withHead) head.measureText(heads[k]) else 0f, values.maxOfOrNull { cell.measureText(it[k]) } ?: 0f)
            }
            val rest = w * 0.48f
            val sum = widths.sum().coerceAtLeast(1f)
            val cols = widths.map { it / sum * rest }
            val xPos = x0
            val xName = x0 + w * 0.12f
            val rights = listOf(x0 + w * 0.52f + cols[0], x0 + w * 0.52f + cols[0] + cols[1], x0 + w)
            var y = top
            if (withHead) {
                val hh = lh + 3 * PX * 2
                text(c, "Pos", xPos, y, hh, head)
                text(c, "Position", xName, y, hh, head)
                heads.forEachIndexed { k, s -> text(c, s, rights[k], y, hh, head, alignRight = true) }
                y += hh
            }
            rows.forEachIndexed { i, r ->
                val border = withHead || i > 0
                if (border) {
                    rule(c, y, 1 * PX, HAIR)
                    y += 1 * PX
                }
                val rh = lh + 4 * PX * 2
                val p = if (r.stopped) cellMuted else cell
                text(c, r.pos.toString(), xPos, y, rh, p)
                text(c, r.name, xName, y, rh, p)
                listOf(r.held, r.turn, r.tip).forEachIndexed { k, s -> text(c, s, rights[k], y, rh, p, alignRight = true) }
                y += rh
            }
            return y
        }

        // Closing section and footer -----------------------------------------------------

        private val colW = (w - 8 * MM) / 2
        private val body = paint(9.5f, 400, INK)
        private val bold = paint(9.5f, 700, INK)
        private val bodyLh = 9.5f * 1.5f
        private val h2 = paint(11f, 800, INK)
        private val h2Caps = paint(11f, 800, INK, tracking = 0.04f)
        private val h2Lh = 11f * 1.5f

        private val method = listOf(
            "The affected ear comes from a six-question questionnaire (Kim HJ et al., Neurology 2020). Angles are read from the phone’s orientation sensor with the phone against the cheek or in a headband, after a seated calibration and a direction check. " to false,
            "Turn" to true,
            " is head rotation from straight ahead; " to false,
            "Tip" to true,
            " is head extension below horizontal (negative = back). Hold times are counted only while both angles are in range. After-care advice follows the AAO-HNS Clinical Practice Guideline: BPPV, 2017." to false,
        )
        private val device = listOf(
            "Epley Coach is an unregulated prototype. It must never be used on a patient. These values are self-recorded by the user’s own phone and have not been checked by a clinician." to false,
        )

        /** Greedy wrap, as CSS does by default, across bold and regular runs. */
        private fun wrap(runs: List<Pair<String, Boolean>>, width: Float): List<List<Pair<String, Boolean>>> {
            val words = runs.flatMap { (s, b) -> Regex("\\S+\\s*|\\s+").findAll(s).map { it.value to b }.toList() }
            val lines = mutableListOf<MutableList<Pair<String, Boolean>>>(mutableListOf())
            var x = 0f
            for ((word, b) in words) {
                val p = if (b) bold else body
                val ww = p.measureText(word.trimEnd())
                if (x > 0 && x + ww > width && word.isNotBlank()) {
                    lines += mutableListOf<Pair<String, Boolean>>()
                    x = 0f
                }
                if (x == 0f && word.isBlank()) continue
                lines.last() += word to b
                x += p.measureText(word)
            }
            return lines
        }

        private fun paragraph(c: Canvas?, runs: List<Pair<String, Boolean>>, x: Float, top: Float, width: Float): Float {
            val lines = wrap(runs, width)
            if (c != null) lines.forEachIndexed { i, line ->
                var lx = x
                line.forEach { (s, b) ->
                    val p = if (b) bold else body
                    text(c, s, lx, top + i * bodyLh, bodyLh, p)
                    lx += p.measureText(s)
                }
            }
            return lines.size * bodyLh
        }

        private val boxPadV = 10 * PX
        private val boxPadH = 12 * PX

        fun closingHeight(): Float {
            val left = h2Lh + 6 * PX + paragraph(null, method, 0f, 0f, colW)
            val right = boxPadV * 2 + h2Lh + 6 * PX + paragraph(null, device, 0f, 0f, colW - 2 * boxPadH)
            return max(left, right)
        }

        fun closing(c: Canvas, top: Float) {
            text(c, "Method", x0, top, h2Lh, h2)
            paragraph(c, method, x0, top + h2Lh + 6 * PX, colW)
            val bx = x0 + colW + 8 * MM
            val h = closingHeight()
            val ring = 1.5f * PX
            c.drawRoundRect(
                RectF(bx + ring / 2, top + ring / 2, bx + colW - ring / 2, top + h - ring / 2), 8 * PX - ring / 2, 8 * PX - ring / 2,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK; style = Paint.Style.STROKE; strokeWidth = ring },
            )
            text(c, "NOT A MEDICAL DEVICE", bx + boxPadH, top + boxPadV, h2Lh, h2Caps)
            paragraph(c, device, bx + boxPadH, top + boxPadV + h2Lh + 6 * PX, colW - 2 * boxPadH)
        }

        fun footer(c: Canvas, left: String, right: String) {
            val p = paint(9f, 600, MUTED)
            val lh = normal(p)
            val textTop = PAGE_H - M * 0.45f - lh
            // The running footer spans the sheet edge to edge; only its text keeps the margins.
            rule(c, textTop - 8 * PX - 1 * PX, 1 * PX, RULE, x = 0f, width = PAGE_W)
            text(c, left, x0, textTop, lh, p)
            text(c, right, x0 + w, textTop, lh, p, alignRight = true)
        }
    }
}
