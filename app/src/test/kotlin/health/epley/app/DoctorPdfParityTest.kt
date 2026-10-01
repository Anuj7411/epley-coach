package health.epley.app

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.res.ResourcesCompat
import health.epley.core.Episode
import health.epley.core.Side
import health.epley.core.StopPoint
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar

/**
 * Renders the doctor's PDF from the design's sample runs (v2.1 §16 H) at 144 dpi, page by page, for
 * comparison with "Doctor PDF.dc.html" printed by Chrome (tools/render-pdf.mjs). The runs are the
 * design's samples; the app builds the same document from the recorded history.
 * Opt-in, like the screen parity: -Dparity.pdf=true.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DoctorPdfParityTest {

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, min) }.timeInMillis

    private fun run(start: Long, side: Side, minutes: Int, angles: List<Pair<Double, Double>>, stop: StopPoint? = null) = Episode(
        epochMillis = start + minutes * 60_000L, side = side, completed = stop == null, feeling = null, practice = false,
        positionsCompleted = angles.size, durationMillis = minutes * 60_000L, heldAngles = angles, stop = stop,
    )

    @Test
    fun render() {
        assumeTrue(System.getProperty("parity.pdf") == "true")
        val episodes = listOf(
            run(at(2026, 10, 1, 3, 19), Side.RIGHT, 6, listOf(44.0 to 0.0, 45.0 to -26.0, 92.0 to -25.0, 131.0 to -27.0, 0.0 to -2.0)),
            run(at(2026, 9, 22, 21, 40), Side.RIGHT, 6, listOf(46.0 to 1.0, 44.0 to -24.0, 88.0 to -23.0, 127.0 to -29.0, 1.0 to -1.0)),
            run(at(2026, 9, 21, 22, 5), Side.RIGHT, 1, listOf(43.0 to 0.0), StopPoint(12, 45.0, -22.0)),
            run(at(2026, 7, 4, 8, 12), Side.LEFT, 7, listOf(45.0 to 0.0, 46.0 to -27.0, 90.0 to -24.0, 134.0 to -26.0, -1.0 to -3.0)),
        )
        val font = ResourcesCompat.getFont(RuntimeEnvironment.getApplication(), R.font.bricolage_grotesque)
        val out = File("build/outputs/pdf").apply { mkdirs() }
        val scale = 2f
        HistoryPdf.render(font, episodes, at(2026, 10, 1, 5, 0)) { n ->
            val bmp = Bitmap.createBitmap((HistoryPdf.PAGE_W * scale).toInt(), (HistoryPdf.PAGE_H * scale).toInt(), Bitmap.Config.ARGB_8888)
            bmp.eraseColor(android.graphics.Color.WHITE)
            val canvas = Canvas(bmp).apply { scale(scale, scale) }
            canvas to { File(out, "doctor-pdf-$n.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        }
    }
}
