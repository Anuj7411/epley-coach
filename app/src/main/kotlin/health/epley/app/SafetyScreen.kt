package health.epley.app

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import health.epley.core.Safety
import health.epley.core.SafetyOutcome

/**
 * The safety check before every run: two questions, two taps, never paywalled.
 *
 * The logic and the item lists live in [Safety], where they are tested. This screen only asks.
 */
@Composable
fun SafetyScreen(
    onFinished: (SafetyOutcome) -> Unit,
    onCancel: () -> Unit,
) {
    var emergency by remember { mutableStateOf<Boolean?>(null) }
    var notToTreat by remember { mutableStateOf<Boolean?>(null) }
    val outcome = Safety.assess(emergency, notToTreat)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "SAFETY CHECK · BEFORE EVERY RUN",
            color = Color(0xFF888888),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
        )

        when (outcome) {
            is SafetyOutcome.Incomplete -> if (outcome.nextQuestion == 1) {
                AnyOf(
                    question = "Do you have ANY of these right now?",
                    items = Safety.emergencySigns,
                    onAnswer = { emergency = it },
                )
            } else {
                AnyOf(
                    question = "Do ANY of these apply to you?",
                    items = Safety.reasonsNotToTreat,
                    onAnswer = { notToTreat = it },
                )
            }
            SafetyOutcome.Emergency -> EmergencyResult(onDone = { onFinished(outcome) })
            SafetyOutcome.SeeDoctor -> Result(
                title = "Don't self-treat today",
                body = "One of those means this may not be the BPPV you were diagnosed with, or " +
                    "the manoeuvre may not be safe for you. See a doctor before doing it.",
                colour = Color(0xFFFFD93D),
                button = "DONE",
                onDone = { onFinished(outcome) },
            )
            SafetyOutcome.Clear -> Result(
                title = "Safety check passed",
                body = "If any of those signs appear during the manoeuvre, stop and get help.",
                colour = Color(0xFF6BCB77),
                button = "CONTINUE",
                onDone = { onFinished(outcome) },
            )
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("BACK") }
    }
}

@Composable
private fun AnyOf(question: String, items: List<String>, onAnswer: (Boolean) -> Unit) {
    Text(question, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    for (item in items) {
        Text("•  $item", color = Color(0xFFDDDDDD), fontSize = 17.sp)
    }
    Spacer(Modifier.height(6.dp))
    Button(
        onClick = { onAnswer(true) },
        modifier = Modifier.fillMaxWidth().height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1E1E), contentColor = Color.White),
    ) { Text("Yes, one or more", fontSize = 18.sp) }
    Button(
        onClick = { onAnswer(false) },
        modifier = Modifier.fillMaxWidth().height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2A3F), contentColor = Color.White),
    ) { Text("No, none of these", fontSize = 18.sp) }
}

@Composable
private fun EmergencyResult(onDone: () -> Unit) {
    val context = LocalContext.current
    Text("Get emergency help now", color = Color(0xFFFF6B6B), fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Text(
        "These can be signs of a stroke, which can look like vertigo. Don't do the manoeuvre. " +
            "Call your local emergency number — 112 in India and Europe, 911 in the US.",
        color = Color(0xFFDDDDDD),
        fontSize = 17.sp,
    )
    Button(
        // Opens the dialer with the number filled in; the person still presses call. No
        // permission is needed and nothing is dialled without them.
        onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) },
        modifier = Modifier.fillMaxWidth().height(60.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E), contentColor = Color.White),
    ) { Text("OPEN DIALER · 112", fontSize = 18.sp) }
    OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("DONE") }
}

@Composable
private fun Result(title: String, body: String, colour: Color, button: String, onDone: () -> Unit) {
    Text(title, color = colour, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Text(body, color = Color(0xFFCCCCCC), fontSize = 16.sp)
    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(button) }
}
