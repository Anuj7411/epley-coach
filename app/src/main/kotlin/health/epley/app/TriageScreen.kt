package health.epley.app

import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.sp
import health.epley.core.HorizontalType
import health.epley.core.ProvokedBy
import health.epley.core.Side
import health.epley.core.Triage
import health.epley.core.TriageAnswers
import health.epley.core.TriageOutcome
import health.epley.core.TurningDuration
import health.epley.core.word

/**
 * The six-question canal triage from Kim et al. 2020, as used in the JAMA Neurology 2023 trial.
 *
 * One question per screen with two large answers, because the person answering is dizzy (the Ada
 * Health pattern: one question at a time). The wording keeps the published questions' meaning;
 * the logic lives in [Triage] and is tested against the published mapping.
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

    // A mis-tap on "which way is worse" picks the wrong ear, and older users mis-tap more.
    // Undo the most recent answer rather than making them start again.
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

    // Progress runs 1/6 to 2/6 across the flow's step two, one sixth of it per question.
    val progress = (1f + (questionNumber ?: 6) / 6f) / 6f

    if (questionNumber != null) {
        val (question, first, second) = when (questionNumber) {
            1 -> Triple("Does it feel like the room, or you, is spinning or whirling?", "Yes", "No")
            2 -> Triple("Do you get dizzy mainly when you move your head?", "Yes", "No")
            3 -> Triple("Does each spell of dizziness last less than 3 minutes?", "Yes", "No")
            4 -> Triple("Which brings the dizziness on more?", "Lying down, or getting out of bed", "Turning my head while lying down")
            5 -> Triple("Which way makes it worse?", "Turning my head to the right", "Turning my head to the left")
            else -> Triple("When turning your head brings it on, how long does it last?", "Less than 1 minute", "More than 1 minute")
        }
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
        FlowFrame(
            stepLabel = "Step 2 of 6 · question $questionNumber of 6",
            progress = progress,
            onBack = onCancel,
            bottom = {
                ChoiceButton(first, { answer(true) })
                ChoiceButton(second, { answer(false) })
                if (undo != null) SecondaryButton("Change my last answer", undo)
            },
        ) {
            Title(question)
            if (questionNumber == 1) {
                Body("For people a doctor has already diagnosed with BPPV. BPPV often comes back in a different ear, so this is asked every time.", secondary = true)
            }
            Spacer(Modifier.weight(1f))
            // On every question, not just the first. A person answering six questions builds
            // confidence with each one, and the accuracy of the set is the thing that should be
            // in front of them at the moment they answer the last.
            InfoNote(
                "These questions agree with a specialist about 71% of the time. They are a guide, " +
                    "not a diagnosis.",
            )
        }
        return
    }

    val result = outcome ?: return
    // The headline is the answer itself, set large. A person who has just answered six questions
    // wants the conclusion, not a restatement of the question, and "Left ear" is the whole of it.
    val (headline, body) = when (result) {
        is TriageOutcome.PosteriorCanal -> Pair(
            "${result.side.word.replaceFirstChar { it.uppercase() }} ear",
            "Posterior canal, the type the Epley manoeuvre treats. We will set up for your " +
                "${result.side.word} side.",
        )
        is TriageOutcome.HorizontalCanal -> Pair(
            "This app can't treat this type",
            "Your answers point to the horizontal canal, ${result.side.word} side. The Epley " +
                "manoeuvre does not treat it, so this app will not guide one. In the trial these " +
                "questions come from it was treated with a " +
                (if (result.type == HorizontalType.CANALITHIASIS) "barbecue roll" else "Gufoni manoeuvre") +
                ". Ask your doctor to show you.",
        )
        is TriageOutcome.NotConsistentWithBppv -> Pair(
            "This doesn't sound like BPPV",
            "Don't do the manoeuvre now. See a doctor. Get emergency help straight away if you " +
                "also have weakness or numbness, trouble speaking or seeing, a sudden severe " +
                "headache, or you can't walk steadily.",
        )
        is TriageOutcome.Incomplete -> return
    }
    val posterior = result is TriageOutcome.PosteriorCanal
    FlowFrame(
        stepLabel = "Result",
        progress = 2 / 6f,
        onBack = onCancel,
        bottom = {
            PrimaryButton(if (posterior) "Continue" else "Back to home", { onFinished(result) })
            if (undo != null) SecondaryButton("Change my last answer", undo)
        },
    ) {
        Text(
            if (posterior) "Your answers suggest" else "Your answers say",
            color = Palette.InkMuted,
            fontSize = AppType.LabelSize,
            fontWeight = FontWeight.Bold,
            fontFamily = AppType.Sans,
        )
        Text(
            headline,
            color = if (posterior) Palette.Ink else Palette.Urgent,
            fontSize = if (posterior) AppType.HeroSize else AppType.DisplaySize,
            lineHeight = if (posterior) 52.sp else AppType.DisplayLine,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = AppType.Sans,
        )
        Text(
            body,
            color = Palette.InkBody,
            fontSize = AppType.BodyLargeSize,
            lineHeight = AppType.BodyLargeLine,
            fontFamily = AppType.Sans,
        )
        if (posterior) {
            Spacer(Modifier.weight(1f))
            InfoNote(
                "About 71% accurate compared with a specialist. If your doctor told you a " +
                    "different ear, use theirs.",
            )
        }
    }
}
