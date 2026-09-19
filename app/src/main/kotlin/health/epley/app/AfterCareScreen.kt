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
import health.epley.core.AfterCare
import health.epley.core.AfterCareAdvice
import health.epley.core.Feeling

/**
 * After the manoeuvre: how do you feel, and what to do next.
 *
 * The advice comes from [AfterCare], where the rules and their sources are tested. In practice mode
 * there is nothing to feel better from, so the screen just closes the rehearsal.
 *
 * @param runsBefore treatment runs already recorded in this episode, not counting this one
 */
@Composable
fun AfterCareScreen(
    practice: Boolean,
    runsBefore: Int,
    onDone: (Feeling?) -> Unit,
) {
    var feeling by remember { mutableStateOf<Feeling?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("MANOEUVRE COMPLETE", color = Color(0xFF6BCB77), fontSize = 14.sp, fontFamily = FontFamily.Monospace)

        if (practice) {
            Text("Practice finished", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "You've been through all five positions. When you do it for real, the phone goes " +
                    "against your cheek and the voice guides you the same way.",
                color = Color(0xFFCCCCCC),
                fontSize = 17.sp,
            )
            Big("DONE", Color(0xFF2E6B3E)) { onDone(null) }
            return@Column
        }

        val chosen = feeling
        if (chosen == null) {
            Text("Stay sitting for a minute. How do you feel now?", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Big("Better", Color(0xFF2E6B3E)) { feeling = Feeling.BETTER }
            Big("About the same", Color(0xFF1E2A3F)) { feeling = Feeling.SAME }
            Big("Worse", Color(0xFF4A1E1E)) { feeling = Feeling.WORSE }
            return@Column
        }

        val (title, body, colour) = when (AfterCare.advise(chosen, runsThisEpisode = runsBefore + 1)) {
            AfterCareAdvice.Done -> Triple(
                "Good. You're done.",
                "If the spinning comes back another day, answer the six questions again — it may be " +
                    "a different ear or canal next time.",
                Color(0xFF6BCB77),
            )
            AfterCareAdvice.RepeatInAnHour -> Triple(
                "Try once more in an hour",
                "It often takes more than one go. The trial this app follows had people repeat the " +
                    "manoeuvre one hour later. Come back then, and do the safety check again first.",
                Color(0xFF7FB3FF),
            )
            AfterCareAdvice.SeeDoctor -> Triple(
                "See a doctor",
                if (chosen == Feeling.WORSE) {
                    "Don't repeat the manoeuvre. If you have any of the emergency signs from the " +
                        "safety check, get help now."
                } else {
                    "You've tried twice today and it hasn't helped. It may not be the type of BPPV " +
                        "this manoeuvre treats. A doctor can check."
                },
                Color(0xFFFFD93D),
            )
        }
        Text(title, color = colour, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(body, color = Color(0xFFDDDDDD), fontSize = 17.sp)
        Text(AfterCare.noRestrictionsNote, color = Color(0xFFAAAAAA), fontSize = 15.sp)
        Spacer(Modifier.height(4.dp))
        Big("DONE", Color(0xFF2E6B3E)) { onDone(chosen) }
    }
}

@Composable
private fun Big(label: String, colour: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = colour, contentColor = Color.White),
    ) { Text(label, fontSize = 19.sp) }
}
