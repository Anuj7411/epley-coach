package health.epley.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * After-care rules. Expected outcomes come from the sources in docs/DESIGN.md: AAO-HNS 2017 (no
 * post-procedure restrictions) and the JAMA Neurology 2023 protocol (repeat once, an hour later).
 */
class AfterCareTest {

    @Test
    fun `better means done, and no restrictions`() {
        val advice = AfterCare.advise(Feeling.BETTER, runsThisEpisode = 1)
        assertEquals(AfterCareAdvice.Done, advice)
        // The guideline's strong recommendation, which many videos still contradict.
        assertTrue("sleep upright" in AfterCare.noRestrictionsNote.lowercase())
    }

    @Test
    fun `still dizzy after the first run means repeat once in an hour`() {
        assertEquals(AfterCareAdvice.RepeatInAnHour, AfterCare.advise(Feeling.SAME, runsThisEpisode = 1))
    }

    @Test
    fun `still dizzy after the repeat means see a doctor`() {
        assertEquals(AfterCareAdvice.SeeDoctor, AfterCare.advise(Feeling.SAME, runsThisEpisode = 2))
    }

    @Test
    fun `worse is never answered with another go`() {
        for (runs in 1..3) {
            assertEquals(AfterCareAdvice.SeeDoctor, AfterCare.advise(Feeling.WORSE, runsThisEpisode = runs))
        }
    }
}

class EpisodeLogTest {

    private val hour = 3_600_000L
    private val day = 24 * hour

    private fun episode(at: Long, feeling: Feeling? = Feeling.BETTER, side: Side = Side.RIGHT, practice: Boolean = false) =
        Episode(epochMillis = at, side = side, completed = true, feeling = feeling, practice = practice)

    @Test
    fun `how far the run got is recorded, and old files without it still load`() {
        // FR-10: a run that stopped at position three is different evidence from one that
        // finished, and the difference has to survive being written to the file.
        val partial = Episode(9L, Side.LEFT, completed = false, feeling = null, practice = false, positionsCompleted = 3)
        assertEquals(listOf(partial), EpisodeLog.decode(EpisodeLog.encode(listOf(partial))))

        // Files written before this field existed have five fields; they must still load.
        assertEquals(
            listOf(Episode(9L, Side.LEFT, completed = true, feeling = Feeling.BETTER, practice = false, positionsCompleted = 0)),
            EpisodeLog.decode("9,LEFT,true,BETTER,false"),
        )
    }

    @Test
    fun `duration and held angles are recorded, and six-field files still load`() {
        // The doctor's PDF promises angles and hold times; they have to survive the file.
        val run = Episode(
            9L, Side.RIGHT, completed = true, feeling = null, practice = false, positionsCompleted = 5,
            durationMillis = 372_000L,
            heldAngles = listOf(44.0 to 0.0, 45.0 to -26.0, -44.5 to -25.0),
        )
        assertEquals(listOf(run), EpisodeLog.decode(EpisodeLog.encode(listOf(run))))
        assertEquals(
            listOf(Episode(9L, Side.LEFT, completed = false, feeling = null, practice = false, positionsCompleted = 2)),
            EpisodeLog.decode("9,LEFT,false,,false,2"),
        )
    }

    @Test
    fun `an episode survives being written and read back`() {
        val original = listOf(
            episode(1_000L, Feeling.SAME, Side.LEFT),
            Episode(epochMillis = 2_000L, side = Side.RIGHT, completed = false, feeling = null, practice = true),
        )
        assertEquals(original, EpisodeLog.decode(EpisodeLog.encode(original)))
    }

    @Test
    fun `a damaged line is skipped, not fatal`() {
        // The log is a file on the phone. One bad line must not cost someone their whole history.
        val text = EpisodeLog.encode(listOf(episode(5_000L))) + "\ngarbage,,line\n"
        assertEquals(listOf(episode(5_000L)), EpisodeLog.decode(text))
    }

    @Test
    fun `runs within a few hours count as the same episode`() {
        val now = 10 * day
        val log = listOf(episode(now - 1 * hour), episode(now - 30 * day))
        assertEquals(1, EpisodeLog.runsThisEpisode(log, now))
    }

    @Test
    fun `practice runs are not treatment and are not counted`() {
        val now = 10 * day
        val log = listOf(episode(now - hour, practice = true), episode(now - 2 * hour, practice = true))
        assertEquals(0, EpisodeLog.runsThisEpisode(log, now))
        assertEquals(0, EpisodeLog.treatments(log).size)
    }

    @Test
    fun `newest first for display`() {
        val log = listOf(episode(1L), episode(3L), episode(2L))
        assertEquals(listOf(3L, 2L, 1L), EpisodeLog.treatments(log).map { it.epochMillis })
    }
}

class EpisodeReportTest {

    private fun at(millis: Long) = "T$millis"

    private val log = listOf(
        Episode(3_000L, Side.RIGHT, completed = true, feeling = Feeling.BETTER, practice = false),
        Episode(2_000L, Side.LEFT, completed = false, feeling = null, practice = false),
        Episode(1_000L, Side.RIGHT, completed = true, feeling = null, practice = true),
    )

    @Test
    fun `the report lists real runs newest first, in plain words`() {
        val lines = EpisodeLog.report(log, ::at).lines()
        assertTrue(lines[0].startsWith("Epley Coach"), lines[0])
        val runs = lines.filter { it.startsWith("T") }
        assertEquals(2, runs.size, "practice runs must not appear: $runs")
        assertTrue(runs[0].startsWith("T3000"), runs[0])
        assertTrue("right ear" in runs[0] && "felt better" in runs[0], runs[0])
        assertTrue("stopped early" in runs[1], runs[1])
    }

    @Test
    fun `the report says what the app is, so a clinician reading it is not guessing`() {
        val text = EpisodeLog.report(log, ::at)
        assertTrue("not a medical device" in text.lowercase(), text)
        assertTrue("Epley" in text)
    }

    @Test
    fun `an empty history still produces a valid report`() {
        val text = EpisodeLog.report(emptyList(), ::at)
        assertTrue("No runs" in text, text)
    }
}
