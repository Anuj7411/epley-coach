package health.epley.app

import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import health.epley.core.ProvokedBy
import health.epley.core.Side
import health.epley.core.Triage
import health.epley.core.TriageAnswers
import health.epley.core.TriageOutcome
import health.epley.core.TurningDuration

/**
 * The six questions and their three results (handoff §4.6, §4.7, §4a).
 *
 * The questions, their order and branching are the validated questionnaire from Kim HJ et al.,
 * Neurology 2020; the copy is the design's, verbatim. The ear comes from question 5 and never from
 * the user picking a side: asking directly is what the JAMA 2023 trial's control arm did, and it
 * resolved 42.9% against 72.4%.
 */
@Composable
fun TriageScreen(
    onFinished: (TriageOutcome) -> Unit,
    onCancel: () -> Unit,
) {
    var q1 by remember { mutableStateOf<Boolean?>(null) }
    var q2 by remember { mutableStateOf<Boolean?>(null) }
    var q3 by remember { mutableStateOf<Boolean?>(null) }
    var q4 by remember { mutableStateOf<ProvokedBy?>(null) }
    var q5 by remember { mutableStateOf<Side?>(null) }
    var q6 by remember { mutableStateOf<TurningDuration?>(null) }
    val context = LocalContext.current

    // Questions 1-3 are always all asked, so a stop result can list every reason it stopped.
    val outcome: TriageOutcome? = if (q1 == null || q2 == null || q3 == null) {
        null
    } else {
        Triage.assess(TriageAnswers(q1!!, q2!!, q3!!, q4, q5, q6))
    }
    val questionNumber = when {
        q1 == null -> 1
        q2 == null -> 2
        q3 == null -> 3
        outcome is TriageOutcome.Incomplete -> outcome.nextQuestion
        else -> null
    }

    // A mis-tap on "which way is worse" picks the wrong ear, and older users mis-tap more.
    val undo: (() -> Unit)? = if (q1 == null) null else {
        {
            when {
                q6 != null -> q6 = null
                q5 != null -> q5 = null
                q4 != null -> q4 = null
                q3 != null -> q3 = null
                q2 != null -> q2 = null
                else -> q1 = null
            }
        }
    }

    if (questionNumber != null) {
        TriageQuestion(questionNumber, onUndo = undo) { firstChosen ->
            when (questionNumber) {
                1 -> q1 = firstChosen
                2 -> q2 = firstChosen
                3 -> q3 = firstChosen
                4 -> q4 = if (firstChosen) ProvokedBy.LYING_DOWN_OR_GETTING_UP else ProvokedBy.TURNING_WHILE_LYING
                5 -> q5 = if (firstChosen) Side.RIGHT else Side.LEFT
                else -> q6 = if (firstChosen) TurningDuration.UNDER_ONE_MINUTE else TurningDuration.OVER_ONE_MINUTE
            }
        }
        return
    }

    when (val result = outcome!!) {
        is TriageOutcome.PosteriorCanal -> EarResult(result.side, onStart = { onFinished(result) })
        is TriageOutcome.HorizontalCanal -> HorizontalResult(onUndo = undo, onHome = { onFinished(result) })
        is TriageOutcome.NotConsistentWithBppv -> NotBppvResult(
            onUndo = undo,
            onCall = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
            onHome = { onFinished(result) },
        )
        is TriageOutcome.Incomplete -> Unit
    }
}

// ---------------------------------------------------------------------------------------------
// Questions (§4a — copy is final)
// ---------------------------------------------------------------------------------------------

private class Question(val text: String, val sub: String? = null, val options: List<Pair<String, String?>>? = null)

private val Questions = mapOf(
    1 to Question(
        "Does it feel like the room, or you, is spinning or whirling?",
        sub = "For people a doctor has already diagnosed with BPPV. BPPV often comes back in a " +
            "different ear, so this is asked every time.",
    ),
    2 to Question("Do you get dizzy mainly when you move your head?"),
    3 to Question("Does each spell of dizziness last less than 3 minutes?"),
    4 to Question(
        "Which brings the dizziness on more?",
        options = listOf("Lying down, or getting out of bed" to null, "Turning my head or body while lying down" to null),
    ),
    // The arrows are a non-verbal cue for which way, for anyone reading slowly in a second language.
    5 to Question(
        "Which way makes it worse?",
        options = listOf("Turning my head to the right" to "arrow_forward", "Turning my head to the left" to "arrow_back"),
    ),
    6 to Question(
        "When turning your head brings it on, how long does it last?",
        options = listOf("Less than 1 minute" to null, "More than 1 minute" to null),
    ),
)

/**
 * One question (§4.6). The label stays "question N of 6" even when question 6 is skipped. Flex
 * region: the spacer above "Change my last answer".
 */
@Composable
fun TriageQuestion(n: Int, onUndo: (() -> Unit)?, onAnswer: (firstChosen: Boolean) -> Unit) {
    val c = Ds
    val q = Questions.getValue(n)
    DScreen(bg = c.ground) {
        Column(
            Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp).enter(0),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ProgressBars(total = 6, current = n)
            Txt("Step 2 of 6 · question $n of 6", type(16f, 600), c.muted)
        }
        Column(
            Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 16.dp).enter(1),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Txt(q.text, type(34f, 800, lineHeight = 1.05f, letterSpacing = -0.03f, wrap = Wrap.Balance), c.ink)
            if (q.sub != null) Txt(q.sub, type(17f, 500, lineHeight = 1.45f, wrap = Wrap.Pretty), c.muted)
        }
        if (q.options == null) {
            Row(Modifier.enter(2), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                YesNoTile("Yes", "check", Modifier.weight(1f)) { onAnswer(true) }
                YesNoTile("No", "close", Modifier.weight(1f)) { onAnswer(false) }
            }
        } else {
            Column(Modifier.enter(2), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                q.options.forEachIndexed { i, (label, icon) ->
                    OptionTile(label, icon) { onAnswer(i == 0) }
                }
            }
        }
        Spacer(Modifier.flex())
        if (onUndo != null) UndoRow(onUndo)
        AccuracyNote()
    }
}

/** Segments: done = ink fill, current = 2 dp ring, to do = line (§4.6). */
@Composable
fun ProgressBars(total: Int, current: Int, empty: Color = Ds.line) {
    val c = Ds
    val fg = c.ink
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..total) {
            val (bgTarget, ringTarget) = when {
                i < current -> fg to fg
                i == current -> Color.Transparent to fg
                else -> empty to empty
            }
            // Segments step by cross-fading their colours (M3, M5); they never slide.
            val spec = androidx.compose.animation.core.tween<Color>(Motion.TOGGLE)
            val bg by androidx.compose.animation.animateColorAsState(bgTarget, spec, label = "seg")
            val ring by androidx.compose.animation.animateColorAsState(ringTarget, spec, label = "ring")
            Box(Modifier.weight(1f).height(8.dp).box(bg, 4.dp, ring = ring))
        }
    }
}

@Composable
private fun YesNoTile(word: String, icon: String, modifier: Modifier, onClick: () -> Unit) {
    val c = Ds
    Column(
        modifier
            .heightIn(min = 168.dp)
            .pressable(onClick = onClick)
            .box(c.surface, 32.dp)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Sym(icon, 28f, c.muted)
        Spacer(Modifier.height(24.dp))
        Txt(word, type(56f, 800, lineHeight = 0.9f, letterSpacing = -0.045f), c.ink)
    }
}

@Composable
private fun OptionTile(label: String, icon: String?, onClick: () -> Unit) {
    val c = Ds
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .pressable(onClick = onClick)
            .box(c.surface, 28.dp)
            .padding(start = 20.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Sym("radio_button_unchecked", 28f, c.muted)
        Txt(
            label,
            type(22f, 800, lineHeight = 1.15f, letterSpacing = -0.01f, wrap = Wrap.Balance),
            c.ink,
            modifier = Modifier.weight(1f),
        )
        if (icon != null) {
            Box(
                Modifier.size(48.dp).box(if (c.night) c.surface2 else c.ground, 24.dp),
                contentAlignment = Alignment.Center,
            ) { Sym(icon, 28f, c.ink) }
        }
    }
}

/** "Change my last answer": from question 2 on, and on the stop results. */
@Composable
fun UndoRow(onClick: () -> Unit, onPastel: Boolean = false) {
    val c = Ds
    Row(
        Modifier
            .heightIn(min = 64.dp)
            .pressable(onClick = onClick)
            .box(if (onPastel) Color.White.copy(alpha = 0.6f) else c.surface, 28.dp)
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            Modifier.size(48.dp).box(
                when {
                    onPastel -> Color.White.copy(alpha = 0.7f)
                    c.night -> c.surface2
                    else -> c.ground
                },
                16.dp,
            ),
            contentAlignment = Alignment.Center,
        ) { Sym("arrow_back", 24f, if (onPastel) Ink else c.ink) }
        Txt("Change my last answer", type(17f, 700), if (onPastel) Ink else c.ink, modifier = Modifier.weight(1f))
    }
}

/** The persistent accuracy note (§1.3): on every question and on the results. */
@Composable
fun AccuracyNote() {
    val c = Ds
    Row(
        Modifier.box(Color.Transparent, 24.dp, ring = c.line, ringWidth = 1.5.dp).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Sym("info", 24f, c.muted)
        Txt(
            "These questions agree with a specialist about 71% of the time. They are a guide, not a diagnosis.",
            type(16f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty),
            c.ink,
            modifier = Modifier.weight(1f),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Results (§4.7). Flex region: the main card.
// ---------------------------------------------------------------------------------------------

@Composable
fun EarResult(side: Side, onStart: () -> Unit, figurePlaying: Boolean = false) {
    val c = Ds
    val n = c.night
    val word = if (side == Side.LEFT) "left" else "right"
    DScreen(bg = c.ground) {
        Column(
            Modifier
                .flex()
                .enter(0)
                .box(if (n) c.lilacTint else c.lilac, 32.dp, ring = if (n) c.lilac else null)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier
                    .box(if (n) Color.Transparent else Color.White.copy(alpha = 0.55f), 999.dp, ring = if (n) c.lilac else null)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("hearing", 20f, if (n) c.lilac else Ink, weight = 600)
                Txt("Your result", type(16f, 600), if (n) c.lilac else Ink, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Txt(
                (if (side == Side.LEFT) "Left" else "Right") + "\near",
                type(96f, 800, lineHeight = 0.86f, letterSpacing = -0.055f),
                if (n) c.ink else Ink,
            )
            Txt(
                "Posterior canal, the type the Epley manoeuvre treats. We will set up for your $word side.",
                type(17f, 600, lineHeight = 1.4f, wrap = Wrap.Pretty),
                if (n) c.soft else Ink,
            )
        }
        Row(
            Modifier.enter(1).box(c.surface, 28.dp).padding(start = 8.dp, end = 24.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FigureView(
                step = 1, ear = if (side == Side.LEFT) 'L' else 'R', night = n, playing = figurePlaying, size = 136,
                description = "Figure: seated, head turned 45° toward the $word ear",
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Txt("First move", type(16f, 600), c.muted)
                Txt(
                    "Turn your head 45° toward your $word ear",
                    type(20f, 800, lineHeight = 1.15f, letterSpacing = -0.01f, wrap = Wrap.Balance),
                    c.ink,
                )
            }
        }
        AccuracyNote()
        PillButton(
            "Start treatment", onStart,
            fill = if (n) c.lilac else Ink, content = if (n) Ink else Color.White, icon = "arrow_forward",
        )
        Txt(
            "Questions from Kim HJ et al., Neurology 2020.",
            type(14f, 600, align = TextAlign.Center),
            c.muted,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Horizontal canal: not treated here, and no side shown (§4.7b). */
@Composable
fun HorizontalResult(onUndo: (() -> Unit)?, onHome: () -> Unit) {
    val c = Ds
    val n = c.night
    DScreen(bg = c.ground) {
        Column(
            Modifier
                .flex()
                .enter(0)
                .box(c.surface, 32.dp, ring = c.ink)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                Modifier
                    .box(Color.Transparent, 999.dp, ring = c.ink)
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("block", 20f, c.ink, weight = 700)
                Txt("Not treated here", type(16f, 700), c.ink, maxLines = 1)
            }
            Spacer(Modifier.weight(1f))
            Txt(
                "This app can’t treat this type",
                type(44f, 800, lineHeight = 1f, letterSpacing = -0.035f, wrap = Wrap.Balance),
                c.ink,
            )
            Txt(
                "Your answers point to the horizontal canal. The Epley manoeuvre does not treat it, " +
                    "so this app will not guide one. Ask your doctor to show you the right manoeuvre.",
                type(17f, 500, lineHeight = 1.45f, wrap = Wrap.Pretty),
                if (n) c.soft else Ink,
            )
        }
        if (onUndo != null) UndoRow(onUndo)
        AccuracyNote()
        PillButton("Back to home", onHome, fill = if (n) c.lilac else Ink, content = if (n) Ink else Color.White)
    }
}

/** Not consistent with BPPV: the hard-stop family (§4.7c). */
@Composable
fun NotBppvResult(onUndo: (() -> Unit)?, onCall: () -> Unit, onHome: () -> Unit) {
    val c = Ds
    val n = c.night
    DScreen(bg = if (n) c.ground else c.coral) {
        Column(
            Modifier
                .flex()
                .then(
                    if (n) Modifier.box(c.coralTint, 32.dp, ring = c.coral).padding(24.dp)
                    else Modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp),
                )
                .enter(0),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                Modifier
                    .box(if (n) c.coral else Ink, 999.dp)
                    .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Sym("warning", 20f, if (n) Ink else c.coral, fill = true, weight = 700)
                Txt("Stop", type(16f, 700), if (n) Ink else c.coral, maxLines = 1)
            }
            Txt(
                "This doesn’t sound like BPPV",
                type(if (n) 48f else 56f, 800, lineHeight = 0.95f, letterSpacing = -0.045f, wrap = Wrap.Balance),
                c.ink,
            )
            Txt(
                "Don’t do the manoeuvre now. See a doctor. Get emergency help straight away if you " +
                    "also have weakness or numbness, trouble speaking or seeing, a sudden severe " +
                    "headache, or you can’t walk steadily.",
                type(if (n) 18f else 19f, 600, lineHeight = 1.45f, wrap = Wrap.Pretty),
                if (n) c.soft else Ink,
            )
        }
        if (onUndo != null) UndoRow(onUndo, onPastel = !n)
        PillButton(
            "Call emergency", onCall,
            fill = Color.Transparent, content = if (n) c.coral else Ink, ring = if (n) c.coral else Ink, icon = "call",
        )
        PillButton("Back to home", onHome, fill = if (n) c.coral else Ink, content = if (n) Ink else Color.White)
    }
}
