package health.epley.app

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.ProvokedBy
import health.epley.core.Side
import health.epley.core.Triage
import health.epley.core.TriageAnswers
import health.epley.core.TriageOutcome
import health.epley.core.TurningDuration
import health.epley.core.word

/**
 * The six-question triage, in the v2 system (handoff §4.6, §4a, §7, §7b, §7c).
 *
 * The questions, their order, the branching and the outcome precedence are unchanged — they come
 * from Kim HJ et al., Neurology 2020, and the app is the source of truth for them, not the
 * mockups. Only the presentation is new.
 *
 * The ear comes from question 5 and never from the user picking a side. Asking directly is what
 * the JAMA 2023 trial's control arm did, and it resolved 42.9% against 72.4%.
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
    val c = Ds
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

    val note = "These questions agree with a specialist about 71% of the time. They are a guide, " +
        "not a diagnosis."

    if (questionNumber != null) {
        fun answer(firstChosen: Boolean) {
            when (questionNumber) {
                1 -> q1 = firstChosen
                2 -> q2 = firstChosen
                3 -> q3 = firstChosen
                4 -> q4 = if (firstChosen) ProvokedBy.LYING_DOWN_OR_GETTING_UP else ProvokedBy.TURNING_WHILE_LYING
                5 -> q5 = if (firstChosen) Side.RIGHT else Side.LEFT
                else -> q6 = if (firstChosen) TurningDuration.UNDER_ONE_MINUTE else TurningDuration.OVER_ONE_MINUTE
            }
        }
        val yesNo = questionNumber <= 3
        val (question, first, second) = when (questionNumber) {
            1 -> Triple("Does it feel like the room, or you, is spinning or whirling?", "Yes", "No")
            2 -> Triple("Do you get dizzy mainly when you move your head?", "Yes", "No")
            3 -> Triple("Does each spell of dizziness last less than 3 minutes?", "Yes", "No")
            4 -> Triple(
                "Which brings the dizziness on more?",
                "Lying down, or getting out of bed",
                "Turning my head or body while lying down",
            )
            5 -> Triple("Which way makes it worse?", "Turning my head to the right", "Turning my head to the left")
            else -> Triple(
                "When turning your head brings it on, how long does it last?",
                "Less than 1 minute",
                "More than 1 minute",
            )
        }
        val viewport = LocalViewportHeight.current

        DsScreen(
            top = {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = Space.s),
                    verticalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    DsProgressSegments(total = 6, current = questionNumber)
                    Text("Step 2 of 6 · question $questionNumber of 6", style = DsType.label, color = c.muted)
                }
            },
            bottom = {
                if (undo != null) {
                    DsButton(
                        label = "Change my last answer",
                        onClick = undo,
                        fill = c.surface,
                        contentColor = c.ink,
                        leading = Icons.AutoMirrored.Rounded.ArrowBack,
                    )
                }
                DsNote(note)
            },
        ) {
            Text(
                question,
                style = DsType.title,
                color = c.ink,
                modifier = Modifier.padding(start = Space.s).enter(0),
            )
            if (questionNumber == 1) {
                Text(
                    "For people a doctor has already diagnosed with BPPV. BPPV often comes back " +
                        "in a different ear, so this is asked every time.",
                    style = DsType.body,
                    color = c.muted,
                    modifier = Modifier.padding(start = Space.s, bottom = Space.s).enter(1),
                )
            }
            Spacer(Modifier.height(Space.s))
            if (yesNo) {
                val tile = (viewport * 0.26f).coerceIn(150.dp, 230.dp)
                Row(
                    Modifier.fillMaxWidth().enter(2),
                    horizontalArrangement = Arrangement.spacedBy(Space.s),
                ) {
                    YesNoTile(Modifier.weight(1f), first, Icons.Rounded.Check, tile) { answer(true) }
                    YesNoTile(Modifier.weight(1f), second, Icons.Rounded.Clear, tile) { answer(false) }
                }
            } else {
                val tile = (viewport * 0.17f).coerceIn(104.dp, 150.dp)
                OptionTile(
                    first,
                    if (questionNumber == 5) Icons.AutoMirrored.Rounded.ArrowForward else null,
                    tile,
                    Modifier.enter(2),
                ) { answer(true) }
                OptionTile(
                    second,
                    if (questionNumber == 5) Icons.AutoMirrored.Rounded.ArrowBack else null,
                    tile,
                    Modifier.enter(3),
                ) { answer(false) }
            }
        }
        return
    }

    when (val result = outcome!!) {
        is TriageOutcome.PosteriorCanal -> EarResult(result, note, undo, onFinished)
        is TriageOutcome.HorizontalCanal -> HorizontalResult(note, undo, result, onFinished)
        is TriageOutcome.NotConsistentWithBppv -> NotBppvResult(
            undo = undo,
            onCall = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
            onHome = { onFinished(result) },
        )
        is TriageOutcome.Incomplete -> Unit
    }
}

/** Yes / No: the word set large with its icon, the pair filling the width. */
@Composable
private fun YesNoTile(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    minHeight: Dp,
    onClick: () -> Unit,
) {
    val c = Ds
    Column(
        modifier
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.card))
            .background(c.surface)
            .heightIn(min = minHeight)
            .padding(Space.l),
        verticalArrangement = Arrangement.spacedBy(Space.s),
    ) {
        Icon(icon, contentDescription = null, tint = c.muted, modifier = Modifier.size(28.dp))
        Spacer(Modifier.weight(1f))
        Text(label, style = DsType.count(56.sp), color = c.ink)
    }
}

/** Questions 4-6: full width, a radio outline, and a label that may run to two lines. */
@Composable
private fun OptionTile(
    label: String,
    trailing: ImageVector?,
    minHeight: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = Ds
    Row(
        modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(Radius.card))
            .background(c.surface)
            .heightIn(min = minHeight)
            .padding(Space.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.m),
    ) {
        Box(Modifier.size(28.dp).clip(RoundedCornerShape(999.dp)).border(2.dp, c.line, RoundedCornerShape(999.dp)))
        Text(label, style = DsType.cardTitle, color = c.ink, modifier = Modifier.weight(1f))
        // A non-verbal cue for which way the head turns, for anyone reading little English.
        if (trailing != null) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(999.dp)).background(c.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(trailing, contentDescription = null, tint = c.ink, modifier = Modifier.size(24.dp))
            }
        }
    }
}

/** Result a: the ear. The headline is the whole answer, so it is set as large as it will go. */
@Composable
private fun EarResult(
    result: TriageOutcome.PosteriorCanal,
    note: String,
    undo: (() -> Unit)?,
    onFinished: (TriageOutcome) -> Unit,
) {
    val c = Ds
    val side = result.side.word
    val headline = (LocalViewportHeight.current.value * 0.115f).coerceIn(56f, 96f).sp
    DsScreen(
        bottom = {
            DsButton("Start treatment", { onFinished(result) }, trailing = Icons.AutoMirrored.Rounded.ArrowForward)
            Text(
                "Questions from Kim HJ et al., Neurology 2020.",
                style = DsType.label,
                color = c.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        DsCard(
            modifier = Modifier.enter(0),
            fill = c.lilac,
            tint = c.lilacTint,
            outline = c.lilac,
            radius = Radius.hero,
        ) {
            DsChip("Your result", Icons.Rounded.Check, if (c.night) ChipStyle.Outlined else ChipStyle.OnPastel, c.lilac)
            Spacer(Modifier.height(Space.m))
            // The two words are one headline, so they sit on consecutive lines with no gap
            // between them rather than as two separately spaced children of the card.
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    side.replaceFirstChar { it.uppercase() },
                    style = DsType.count(headline),
                    color = if (c.night) c.ink else Color(0xFF17161C),
                )
                Text(
                    "ear",
                    style = DsType.count(headline),
                    color = if (c.night) c.ink else Color(0xFF17161C),
                )
            }
            Spacer(Modifier.height(Space.m))
            Text(
                "Posterior canal, the type the Epley manoeuvre treats. We will set up for your $side side.",
                style = DsType.body,
                color = if (c.night) c.soft else Color(0xCC17161C),
            )
        }
        Box(Modifier.enter(1)) { DsNote(note) }
        if (undo != null) {
            Box(Modifier.enter(2)) {
                DsButton("Change my last answer", undo, fill = c.surface, contentColor = c.ink, leading = Icons.AutoMirrored.Rounded.ArrowBack)
            }
        }
    }
}

/** Result b: horizontal canal. No side is shown — the app does not treat this type. */
@Composable
private fun HorizontalResult(
    note: String,
    undo: (() -> Unit)?,
    result: TriageOutcome.HorizontalCanal,
    onFinished: (TriageOutcome) -> Unit,
) {
    val c = Ds
    DsScreen(
        bottom = { DsButton("Back to home", { onFinished(result) }) },
    ) {
        DsCard(
            modifier = Modifier.enter(0),
            fill = c.surface,
            tint = c.surface,
            outline = c.ink,
            radius = Radius.hero,
        ) {
            DsChip("Not treated here", Icons.Rounded.Close, ChipStyle.Outlined, c.ink)
            Spacer(Modifier.height(Space.l))
            Text("This app can't treat this type", style = DsType.display, color = c.ink)
            Spacer(Modifier.height(Space.s))
            Text(
                "Your answers point to the horizontal canal. The Epley manoeuvre does not treat " +
                    "it, so this app will not guide one. Ask your doctor to show you the right " +
                    "manoeuvre.",
                style = DsType.body,
                color = c.muted,
            )
        }
        if (undo != null) {
            Box(Modifier.enter(1)) {
                DsButton("Change my last answer", undo, fill = c.surface, contentColor = c.ink, leading = Icons.AutoMirrored.Rounded.ArrowBack)
            }
        }
        Box(Modifier.enter(2)) { DsNote(note) }
    }
}

/** Result c: the hard-stop family, the same treatment as the emergency screen. */
@Composable
private fun NotBppvResult(
    undo: (() -> Unit)?,
    onCall: () -> Unit,
    onHome: () -> Unit,
) {
    val c = Ds
    val body = "Don't do the manoeuvre now. See a doctor. Get emergency help straight away if you " +
        "also have weakness or numbness, trouble speaking or seeing, a sudden severe headache, " +
        "or you can't walk steadily."
    DsScreen(
        background = if (c.night) c.ground else c.coral,
        bottom = {
            if (undo != null) {
                DsButton(
                    "Change my last answer",
                    undo,
                    fill = if (c.night) c.surface else Color(0x33FFFFFF),
                    contentColor = c.ink,
                    leading = Icons.AutoMirrored.Rounded.ArrowBack,
                )
            }
            DsButton(
                "Call emergency",
                onCall,
                fill = Color.Transparent,
                contentColor = c.ink,
                outline = c.ink,
                leading = Icons.Rounded.Call,
            )
            DsButton(
                "Back to home",
                onHome,
                fill = if (c.night) c.surface else Color(0xFF17161C),
                contentColor = if (c.night) c.ink else Color.White,
            )
        },
    ) {
        val inner: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.l)) {
                Box(Modifier.enter(0)) { DsChip("Stop", Icons.Rounded.Warning, ChipStyle.Filled) }
                Text("This doesn't sound like BPPV", style = DsType.result, color = c.ink, modifier = Modifier.enter(1))
                Text(body, style = DsType.body, color = c.ink, modifier = Modifier.enter(2))
            }
        }
        Spacer(Modifier.height(Space.l))
        if (c.night) {
            DsCard(fill = c.coralTint, tint = c.coralTint, outline = c.coral, radius = Radius.hero) { inner() }
        } else {
            inner()
        }
    }
}
