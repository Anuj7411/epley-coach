package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Words for a correction. Expected phrases are written out by hand from what a person lying on a
 * bed needs to hear, not derived from the implementation.
 */
class PhrasingTest {

    private val tolerance = 7.0
    private val positive = RotationPolarity(towardAffectedSideIsPositive = true)
    private val negative = RotationPolarity(towardAffectedSideIsPositive = false)

    @Test
    fun `lying back, too little hang asks for the head to hang lower`() {
        assertEquals(
            listOf(CorrectionPhrase("Let your head hang lower", 12)),
            Phrasing.corrections(Correction(12.0, 0.0), positive, Side.RIGHT, tolerance),
        )
    }

    @Test
    fun `lying back, too much hang asks for the head to come up`() {
        assertEquals(
            listOf(CorrectionPhrase("Raise your head a little", 9)),
            Phrasing.corrections(Correction(-9.0, 2.0), positive, Side.RIGHT, tolerance),
        )
    }

    @Test
    fun `a turn is named by the side it goes toward, whichever sign the sensor uses`() {
        // Correction +20 means "increase the sensor reading". With positive polarity that is
        // toward the affected (right) ear; with negative polarity it is toward the left.
        assertEquals(
            listOf(CorrectionPhrase("Turn toward your right", 20)),
            Phrasing.corrections(Correction(0.0, 20.0), positive, Side.RIGHT, tolerance),
        )
        assertEquals(
            listOf(CorrectionPhrase("Turn toward your left", 20)),
            Phrasing.corrections(Correction(0.0, 20.0), negative, Side.RIGHT, tolerance),
        )
        assertEquals(
            listOf(CorrectionPhrase("Turn toward your right", 15)),
            Phrasing.corrections(Correction(0.0, -15.0), positive, Side.LEFT, tolerance),
        )
    }

    @Test
    fun `the axis furthest out comes first`() {
        assertEquals(
            listOf(
                CorrectionPhrase("Turn toward your left", 30),
                CorrectionPhrase("Let your head hang lower", 10),
            ),
            Phrasing.corrections(Correction(10.0, -30.0), positive, Side.RIGHT, tolerance),
        )
    }

    @Test
    fun `sitting, any pitch error is a lean to straighten`() {
        // "Hang lower" means nothing upright. Either direction of lean gets the same instruction.
        for (pitch in listOf(12.0, -12.0)) {
            assertEquals(
                listOf(CorrectionPhrase("Sit up straight", 12)),
                Phrasing.corrections(Correction(pitch, 0.0), positive, Side.RIGHT, tolerance, seated = true),
            )
        }
    }

    @Test
    fun `nothing to say when both axes are inside the band`() {
        assertEquals(
            emptyList(),
            Phrasing.corrections(Correction(6.9, -6.9), positive, Side.RIGHT, tolerance),
        )
    }
}
