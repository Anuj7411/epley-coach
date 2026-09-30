package health.epley.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Stress tests for the clinical logic: every reachable combination, checked against an independent
 * re-implementation of the handoff's rules (README §11 TriageState), and randomised round trips
 * for the stored run history.
 */
class StressLogicTest {

    // ---- Triage: the README's reference model, written from the spec, not from Triage.kt ----

    private sealed interface Ref {
        data class Next(val q: Int) : Ref
        data object NotBppv : Ref
        data object Horizontal : Ref
        data class Ear(val side: Side) : Ref
    }

    /** answers[i] = 0 for Yes / first option, 1 for No / second option; null if not yet answered. */
    private fun reference(a: List<Int?>): Ref {
        // Questions 1-3 are always asked, even after a No, so a stop can list every reason.
        for (q in 0..2) if (a[q] == null) return Ref.Next(q + 1)
        if ((0..2).any { a[it] == 1 }) return Ref.NotBppv                  // precedence 1
        if (a[3] == null) return Ref.Next(4)
        if (a[4] == null) return Ref.Next(5)
        if (a[3] == 1) {                                                   // turning -> horizontal
            if (a[5] == null) return Ref.Next(6)                           // Q6 only when turning
            return Ref.Horizontal                                          // precedence 2, no side
        }
        return Ref.Ear(if (a[4] == 1) Side.LEFT else Side.RIGHT)
    }

    private fun engine(a: List<Int?>): Ref {
        for (q in 0..2) if (a[q] == null) return Ref.Next(q + 1)
        val out = Triage.assess(
            TriageAnswers(
                spinning = a[0] == 0,
                worseWithHeadMovement = a[1] == 0,
                lastsUnderThreeMinutes = a[2] == 0,
                provokedBy = a[3]?.let { if (it == 0) ProvokedBy.LYING_DOWN_OR_GETTING_UP else ProvokedBy.TURNING_WHILE_LYING },
                worseSide = a[4]?.let { if (it == 0) Side.RIGHT else Side.LEFT },
                turningDizzinessLasts = a[5]?.let { if (it == 0) TurningDuration.UNDER_ONE_MINUTE else TurningDuration.OVER_ONE_MINUTE },
            ),
        )
        return when (out) {
            is TriageOutcome.Incomplete -> Ref.Next(out.nextQuestion)
            is TriageOutcome.NotConsistentWithBppv -> Ref.NotBppv
            is TriageOutcome.HorizontalCanal -> Ref.Horizontal
            is TriageOutcome.PosteriorCanal -> Ref.Ear(out.side)
        }
    }

    @Test
    fun `every combination of triage answers matches the handoff's rules`() {
        val choices = listOf(null, 0, 1)
        var checked = 0
        for (a in choices) for (b in choices) for (c in choices) for (d in choices) for (e in choices) for (f in choices) {
            val answers = listOf(a, b, c, d, e, f)
            assertEquals(reference(answers), engine(answers), "answers $answers")
            checked++
        }
        assertEquals(729, checked)
    }

    @Test
    fun `the ear only ever comes from question 5, and a stop never names one`() {
        for (side in listOf(0, 1)) {
            val out = Triage.assess(TriageAnswers(true, true, true, ProvokedBy.LYING_DOWN_OR_GETTING_UP, if (side == 0) Side.RIGHT else Side.LEFT, null))
            assertEquals(TriageOutcome.PosteriorCanal(if (side == 0) Side.RIGHT else Side.LEFT), out)
        }
        // Every No among 1-3 is listed, so the stop screen can say why.
        val out = Triage.assess(TriageAnswers(false, true, false, null, null, null))
        assertEquals(TriageOutcome.NotConsistentWithBppv(listOf(1, 3)), out)
    }

    // ---- Safety ----

    @Test
    fun `safety has no path to treatment except two Nos`() {
        for (e in listOf(null, true, false)) for (r in listOf(null, true, false)) {
            val out = Safety.assess(e, r)
            val clear = out == SafetyOutcome.Clear
            assertEquals(e == false && r == false, clear, "emergency=$e reasons=$r -> $out")
            if (e == true) assertEquals(SafetyOutcome.Emergency, out, "an emergency sign always stops first")
        }
    }

    @Test
    fun `the red flags are the design's six, verbatim`() {
        assertEquals(
            listOf("Weakness or numbness", "Trouble speaking", "Double vision", "Can’t walk", "Sudden severe headache", "Sudden hearing loss"),
            Safety.emergencySigns,
        )
    }

    // ---- The protocol the screens promise ----

    @Test
    fun `hold times are 3, 45, 45, 45, 5 as the design states`() {
        assertEquals(listOf(3, 45, 45, 45, 5), Epley.steps().map { it.holdSeconds })
    }

    // ---- Stored history: randomised round trips ----

    @Test
    fun `a thousand random runs survive being written and read back`() {
        val rnd = Random(7)
        val runs = List(1000) {
            Episode(
                epochMillis = rnd.nextLong(0, 4_000_000_000_000),
                side = if (rnd.nextBoolean()) Side.LEFT else Side.RIGHT,
                completed = rnd.nextBoolean(),
                feeling = listOf(null, Feeling.BETTER, Feeling.SAME, Feeling.WORSE)[rnd.nextInt(4)],
                practice = rnd.nextBoolean(),
                positionsCompleted = rnd.nextInt(0, 6),
                durationMillis = rnd.nextLong(0, 3_600_000),
                heldAngles = List(rnd.nextInt(0, 6)) {
                    // Stored to one decimal, so generate on that grid.
                    (rnd.nextInt(-1800, 1800) / 10.0) to (rnd.nextInt(-900, 900) / 10.0)
                },
            )
        }
        assertEquals(runs, EpisodeLog.decode(EpisodeLog.encode(runs)))
    }

    @Test
    fun `garbage lines never throw and never cost the good ones`() {
        val rnd = Random(11)
        val good = Episode(5L, Side.RIGHT, true, null, false, 5, 60_000, listOf(44.0 to 0.0))
        val alphabet = ",|:-.0123456789abcTRUEfalse"
        val junk = List(200) { String(CharArray(rnd.nextInt(0, 60)) { alphabet[rnd.nextInt(alphabet.length)] }) }
        val text = (junk + EpisodeLog.encode(listOf(good)) + junk).joinToString("\n")
        val decoded = EpisodeLog.decode(text)
        assertTrue(good in decoded, "the good line survives $decoded")
    }
}
