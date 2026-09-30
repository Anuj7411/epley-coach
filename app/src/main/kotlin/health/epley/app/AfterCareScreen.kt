package health.epley.app

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import health.epley.core.Epley
import health.epley.core.Feeling

/**
 * Done and after-care (handoff §4.10). Flex region: the spacer above Finish.
 *
 * The after-care is AAO-HNS 2017 and verbatim: there are **no** postural restrictions. Any mockup
 * that says "stay upright" or "sleep propped up" is wrong, and this is the one screen someone
 * might copy that from. The three lines cover every outcome — fine, still dizzy, worse — so the
 * screen needs no question first.
 *
 * Practice shows the same summary without the after-care: nothing was treated, so advice about
 * what to do after treatment would be wrong.
 */
@Composable
fun AfterCareScreen(
    practice: Boolean,
    runsBefore: Int,
    driftDegrees: Double? = null,
    onDone: (Feeling?) -> Unit,
    ear: Char = 'R',
) {
    DoneView(ear = ear, showAfterCare = !practice, onFinish = { onDone(null) })
}

@Composable
fun DoneView(ear: Char, showAfterCare: Boolean, onFinish: () -> Unit) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        Column(
            Modifier
                .enter(0)
                .heightIn(min = 128.dp)
                .box(if (n) c.lilacTint else c.lilac, 32.dp, ring = if (n) c.lilac else null)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(if (n) 24.dp else 16.dp),
        ) {
            Box(Modifier.size(64.dp).box(if (n) c.lilac else Ink, 20.dp), contentAlignment = Alignment.Center) {
                Sym("check", 40f, if (n) Ink else Color.White)
            }
            // The design's empty flex spacer: zero height, but it still takes its gap either side.
            Spacer(Modifier.height(0.dp))
            Txt("All five\npositions held.", type(44f, 800, lineHeight = 1f, letterSpacing = -0.035f), if (n) c.ink else Ink)
        }
        Column(Modifier.enter(1).box(c.surface, 28.dp).padding(horizontal = 16.dp, vertical = if (n) 4.dp else 8.dp)) {
            val names = listOf("Turn head right", "Lie back", "Turn head left", "Roll onto left side", "Sit up")
            Epley.steps().forEachIndexed { i, step ->
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(if (n) c.surface2 else c.ground))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 40.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Txt("${i + 1}", type(16f, 700, tnum = true), c.muted, modifier = Modifier.width(24.dp))
                    Txt(mirrorFor(ear, names[i]), type(17f, 600), c.ink, modifier = Modifier.weight(1f))
                    // Read from the engine, so the list can never disagree with what was held.
                    Txt("${step.holdSeconds} s", type(16f, 600, tnum = true), c.muted)
                    Sym("check_circle", 22f, if (n) c.mint else Ink, fill = true)
                }
            }
        }
        if (showAfterCare) {
            // AAO-HNS 2017, verbatim. No postural restrictions, ever.
            Column(
                Modifier
                    .enter(2)
                    .box(if (n) c.mintTint else c.mint, 28.dp, ring = if (n) c.mint else null)
                    .padding(start = 16.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AfterCareLine("bed", "No restrictions afterwards. You can lie flat and sleep normally.")
                AfterCareLine("hourglass_top", "Still dizzy? Repeat once, an hour later.")
                AfterCareLine("stethoscope", "Worse, or still dizzy after the repeat? See a doctor.")
            }
        }
        Spacer(Modifier.flex())
        PillButton("Finish", onFinish, fill = if (n) c.lilac else Ink, content = if (n) Ink else Color.White)
    }
}

@Composable
private fun AfterCareLine(icon: String, text: String) {
    val c = Ds
    val n = c.night
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Sym(icon, 24f, if (n) c.mint else Ink)
        Txt(text, type(17f, 600, lineHeight = 1.3f, wrap = Wrap.Pretty), if (n) c.ink else Ink, modifier = Modifier.weight(1f))
    }
}
