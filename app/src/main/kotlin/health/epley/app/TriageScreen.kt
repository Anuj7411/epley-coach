package health.epley.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.HorizontalType
import health.epley.core.ProvokedBy
import health.epley.core.Side
import health.epley.core.Triage
import health.epley.core.TriageAnswers
import health.epley.core.TriageOutcome
import health.epley.core.TurningDuration

/**
 * The six-question canal triage from Kim et al. 2020, as used in the JAMA Neurology 2023 trial.
 *
 * One question per screen with two large answers, because the person answering is dizzy. The
 * wording keeps the published questions' meaning; the logic lives in [Triage] and is tested
 * against the published mapping.
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

    // Questions 1-3 are always all asked, so the result can say every reason it stopped.
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "SIX QUESTIONS · FOR PEOPLE ALREADY DIAGNOSED WITH BPPV",
            color = Color(0xFF888888),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )

        when (questionNumber) {
            1 -> YesNo(1, "Does it feel like the room — or you — is spinning or whirling?") { q1 = it }
            2 -> YesNo(2, "Do you get dizzy mainly when you move your head?") { q2 = it }
            3 -> YesNo(3, "Does each spell of dizziness last less than 3 minutes?") { q3 = it }
            4 -> Choice(
                4, "Which brings the dizziness on more?",
                "Lying down, or getting out of bed", "Turning my head while lying down",
            ) { first ->
                q4 = if (first) ProvokedBy.LYING_DOWN_OR_GETTING_UP else ProvokedBy.TURNING_WHILE_LYING
            }
            5 -> Choice(
                5, "Which way makes it worse?",
                "Turning my head to the right", "Turning my head to the left",
            ) { first -> q5 = if (first) Side.RIGHT else Side.LEFT }
            6 -> Choice(
                6, "When turning your head brings it on, how long does it last?",
                "Less than 1 minute", "More than 1 minute",
            ) { first ->
                q6 = if (first) TurningDuration.UNDER_ONE_MINUTE else TurningDuration.OVER_ONE_MINUTE
            }
            else -> outcome?.let { Result(it, onFinished) }
        }

        Spacer(Modifier.height(8.dp))
        // A mis-tap on "which way is worse" picks the wrong ear, and older users mis-tap more.
        // Undo the most recent answer rather than making them start the questionnaire again.
        if (q1 != null) {
            OutlinedButton(
                onClick = {
                    when {
                        q6 != null -> q6 = null
                        q5 != null -> q5 = null
                        q4 != null -> q4 = null
                        q3 != null -> q3 = null
                        q2 != null -> q2 = null
                        else -> q1 = null
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("CHANGE MY LAST ANSWER") }
        }
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("BACK") }
    }
}

@Composable
private fun YesNo(number: Int, question: String, onAnswer: (Boolean) -> Unit) =
    Choice(number, question, "Yes", "No", onAnswer)

@Composable
private fun Choice(
    number: Int,
    question: String,
    first: String,
    second: String,
    onAnswer: (firstChosen: Boolean) -> Unit,
) {
    Text("QUESTION $number OF 6", color = Color(0xFF7FB3FF), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    Text(question, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    for ((label, isFirst) in listOf(first to true, second to false)) {
        Button(
            onClick = { onAnswer(isFirst) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2A3F), contentColor = Color.White),
        ) { Text(label, fontSize = 18.sp) }
    }
}

@Composable
private fun Result(outcome: TriageOutcome, onFinished: (TriageOutcome) -> Unit) {
    val (title, body, colour) = when (outcome) {
        is TriageOutcome.PosteriorCanal -> Triple(
            "Posterior canal, ${sideWord(outcome.side)} ear",
            "This is the type the Epley manoeuvre treats.\n\n" +
                "Be aware: in published testing these questions matched a specialist's diagnosis " +
                "about 3 times in 4. If this attack feels different from the ones you've been " +
                "diagnosed with before, stop and see a doctor.",
            Color(0xFF6BCB77),
        )
        is TriageOutcome.HorizontalCanal -> Triple(
            "A different type — horizontal canal, ${sideWord(outcome.side)} side",
            "The Epley manoeuvre does not treat this type, so this app won't guide it.\n\n" +
                "In the trial these questions come from, it was treated with a " +
                (if (outcome.type == HorizontalType.CANALITHIASIS) "barbecue roll" else "Gufoni manoeuvre") +
                ". Ask your doctor to show you.",
            Color(0xFFFFD93D),
        )
        is TriageOutcome.NotConsistentWithBppv -> Triple(
            "This doesn't match the usual BPPV pattern",
            "Don't do the manoeuvre now — see a doctor.\n\n" +
                "Get emergency help straight away if you also have weakness or numbness, trouble " +
                "speaking or seeing, a sudden severe headache, or you can't walk steadily.",
            Color(0xFFFF6B6B),
        )
        is TriageOutcome.Incomplete -> return
    }
    Text(title, color = colour, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Text(body, color = Color(0xFFCCCCCC), fontSize = 16.sp)
    Spacer(Modifier.height(6.dp))
    Button(onClick = { onFinished(outcome) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
        Text(if (outcome is TriageOutcome.PosteriorCanal) "CONTINUE" else "DONE")
    }
}

private fun sideWord(side: Side) = if (side == Side.LEFT) "left" else "right"
