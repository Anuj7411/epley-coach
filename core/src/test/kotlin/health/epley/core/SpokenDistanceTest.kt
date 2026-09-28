package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The spoken correction has to say *how far*, not just which way.
 *
 * Found by using the app on a real head: "turn toward your left" sounds the same whether you are
 * four degrees out or forty. Someone nearly in position over-corrects, someone far out
 * under-corrects, and both conclude the guidance is wrong. The distance was computed all along
 * and shown only on a screen that, at that moment, is pressed against the user's cheek.
 */
class SpokenDistanceTest {

    @Test
    fun `small errors are described, not numbered`() {
        // Under ten degrees is inside or near every published tolerance band. Naming a number
        // there sends someone hunting for a precision the manoeuvre does not ask for.
        assertEquals("a little", Phrasing.distance(3))
        assertEquals("a little", Phrasing.distance(9))
    }

    @Test
    fun `middling errors get a number, rounded to five`() {
        assertEquals("about 15 degrees", Phrasing.distance(14))
        assertEquals("about 20 degrees", Phrasing.distance(18))
        assertEquals("about 30 degrees", Phrasing.distance(30))
    }

    @Test
    fun `large errors say so before the number`() {
        // "A long way" is the part that survives being half-heard by someone mid-movement.
        assertEquals("a long way, about 45 degrees", Phrasing.distance(44))
        assertTrue(Phrasing.distance(60).startsWith("a long way"))
    }

    @Test
    fun `no number is ever more precise than a person can act on`() {
        // Nobody can execute "seventeen degrees". Every spoken figure is a multiple of five.
        for (degrees in 10..90) {
            val words = Phrasing.distance(degrees)
            val number = words.filter { it.isDigit() }.toInt()
            assertEquals(0, number % 5, "$degrees -> $words")
        }
    }

    @Test
    fun `the sentence keeps the direction first`() {
        // Direction is the part that must survive if the listener stops paying attention halfway.
        val phrase = CorrectionPhrase("Turn toward your left", 22)
        assertEquals("Turn toward your left, about 20 degrees", Phrasing.spoken(phrase))
    }

    @Test
    fun `the spoken distance matches the one on screen`() {
        // The screen shows "22° more" from the same phrase the voice reads. If these two ever
        // disagree, the app is arguing with itself in front of someone who is dizzy.
        val correction = Correction(pitchDegrees = 0.0, headRotationDegrees = -22.4)
        val phrase = Phrasing.corrections(
            correction,
            RotationPolarity(towardAffectedSideIsPositive = true),
            Side.LEFT,
            toleranceDegrees = 7.0,
        ).single()
        assertEquals(22, phrase.degrees)
        assertTrue(Phrasing.spoken(phrase).endsWith("about 20 degrees"))
    }
}

/**
 * The screen and the voice say the same thing in different lengths.
 *
 * Found on the device: the spoken sentence set at 18sp ran to three lines and collided with the
 * gauge's own aim label, printing "about 60aim 45 ±15". Speech has to spell a quantity out; a
 * screen can show the figure.
 */
class OnScreenPhrasingTest {

    @Test
    fun `the screen gets the figure, the voice gets the sentence`() {
        val phrase = CorrectionPhrase("Turn toward your left", 62)
        assertEquals("Turn toward your left, 62°", Phrasing.onScreen(phrase))
        assertEquals("Turn toward your left, a long way, about 60 degrees", Phrasing.spoken(phrase))
    }

    @Test
    fun `both name the same direction`() {
        // If these ever diverge the app is telling the eye and the ear different things while
        // someone is dizzy, which is worse than saying nothing.
        val phrase = CorrectionPhrase("Let your head hang lower", 18)
        assertTrue(Phrasing.onScreen(phrase).startsWith(phrase.text))
        assertTrue(Phrasing.spoken(phrase).startsWith(phrase.text))
    }
}
