package health.epley.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import health.epley.core.Cue
import health.epley.core.Haptic
import java.util.Locale

/**
 * Speaks and vibrates the cues the planner decides on.
 *
 * This is the only channel that reaches the user at position four, face toward the floor with the
 * screen out of sight. Deliberately thin: every decision about *when* lives in [health.epley.core.CuePlanner],
 * where it is tested; this class only knows *how*.
 */
class GuidanceOutput(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private var ready = false

    /**
     * Speech requested before the engine finished starting. The first instruction of a run is
     * usually spoken within a second of opening the app, and the engine can take longer than that
     * to bind; dropping it would lose the most important sentence.
     */
    private val pending = mutableListOf<Cue.Speak>()

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val preferred = tts.setLanguage(Locale.getDefault())
            if (preferred == TextToSpeech.LANG_MISSING_DATA || preferred == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.setLanguage(Locale.US)
            }
            // Well below default: the listener is dizzy, lying down, possibly nauseous, and a
            // practice run on hardware found 0.9 too fast to follow.
            tts.setSpeechRate(0.8f)
            ready = true
            pending.forEach(::speak)
            pending.clear()
        }
    }

    fun play(cue: Cue) {
        when (cue) {
            is Cue.Speak -> if (ready) speak(cue) else pending += cue
            is Cue.Buzz -> buzz(cue.pattern)
        }
    }

    private fun speak(cue: Cue.Speak) {
        val mode = if (cue.interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts.speak(cue.text, mode, null, cue.text.hashCode().toString())
    }

    private fun buzz(pattern: Haptic) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        // Timings alternate off/on in milliseconds. Chosen to be told apart by feel alone.
        val timings = when (pattern) {
            Haptic.IN_POSITION -> longArrayOf(0, 120)
            Haptic.STEP_DONE -> longArrayOf(0, 150, 120, 150)
            Haptic.FINISHED -> longArrayOf(0, 600)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(timings, -1)
        }
    }

    /** Stop talking now — used when a run is stopped mid-sentence. */
    fun silence() {
        pending.clear()
        if (ready) tts.stop()
    }

    fun release() {
        tts.stop()
        tts.shutdown()
    }
}
