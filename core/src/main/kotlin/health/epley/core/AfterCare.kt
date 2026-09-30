package health.epley.core

/** How the user says they feel once the manoeuvre is finished. */
enum class Feeling { BETTER, SAME, WORSE }

sealed interface AfterCareAdvice {
    /** Better. Nothing more to do, and no restrictions. */
    data object Done : AfterCareAdvice

    /** Still dizzy after the first go: one repeat, an hour from now. */
    data object RepeatInAnHour : AfterCareAdvice

    /** Worse, or still dizzy after the repeat: stop self-treating and see a doctor. */
    data object SeeDoctor : AfterCareAdvice
}

/**
 * What to say once the manoeuvre is over.
 *
 * The 2026 IMU study found 23.5% of initially resolved patients had recurred within 24 hours and
 * named post-manoeuvre guidance as the missing piece. These rules are that guidance, from two
 * sources (docs/DESIGN.md):
 *  - JAMA Neurology 2023 protocol: patients still dizzy repeated the manoeuvre one hour later.
 *  - AAO-HNS 2017: a strong recommendation *against* post-procedure postural restrictions.
 *
 * "Worse" never earns another attempt. Repeating a manoeuvre that made things worse is how a
 * wrong diagnosis gets reinforced instead of caught.
 */
object AfterCare {

    /** @param runsThisEpisode completed runs in this episode, including the one just finished */
    fun advise(feeling: Feeling, runsThisEpisode: Int): AfterCareAdvice = when (feeling) {
        Feeling.BETTER -> AfterCareAdvice.Done
        Feeling.WORSE -> AfterCareAdvice.SeeDoctor
        Feeling.SAME -> if (runsThisEpisode < MAX_RUNS_PER_EPISODE) {
            AfterCareAdvice.RepeatInAnHour
        } else {
            AfterCareAdvice.SeeDoctor
        }
    }

    /** One first attempt and one repeat, as in the trial protocol. */
    const val MAX_RUNS_PER_EPISODE = 2

    /**
     * Said whatever the outcome. Many videos still tell people to sleep propped up for two
     * nights; the guideline says not to bother, and saying so is a small act of accuracy.
     */
    const val noRestrictionsNote =
        "You don't need to sleep upright or avoid lying flat afterwards. The US ENT guideline " +
            "recommends against those restrictions."
}

/**
 * One completed or abandoned run, as stored on the phone.
 *
 * Kept deliberately small: date, ear, whether it finished, how it went. Nothing leaves the device.
 */
data class Episode(
    val epochMillis: Long,
    val side: Side,
    val completed: Boolean,
    val feeling: Feeling?,
    /** Practice runs measure a phone, not a head. They are kept apart from treatment. */
    val practice: Boolean,
    /**
     * How many of the five positions were completed. A run abandoned at position three is
     * different evidence from one that finished, and a clinician reading the history can see it.
     */
    val positionsCompleted: Int = 0,
    /** How long the run took, start to finish or stop. 0 in histories written before it existed. */
    val durationMillis: Long = 0,
    /**
     * The measured head angles at the end of each held position, in order: turn then tip, in the
     * engine's frame. What "angles and hold times" in the doctor's PDF refers to.
     */
    val heldAngles: List<Pair<Double, Double>> = emptyList(),
)

/** Reading, writing and summarising the on-device episode history. */
object EpisodeLog {

    /**
     * Runs this close together belong to the same episode. The trial's repeat comes an hour after
     * the first attempt; six hours leaves room for a slow repeat without merging separate attacks.
     */
    const val EPISODE_WINDOW_MILLIS = 6 * 60 * 60 * 1000L

    fun encode(episodes: List<Episode>): String = episodes.joinToString("\n") { e ->
        listOf(
            e.epochMillis, e.side.name, e.completed, e.feeling?.name ?: "", e.practice, e.positionsCompleted,
            e.durationMillis,
            // turn:tip pairs joined by |, so the record stays one comma-separated line.
            e.heldAngles.joinToString("|") { (turn, tip) -> "%.1f:%.1f".format(java.util.Locale.ROOT, turn, tip) },
        ).joinToString(",")
    }

    /** Lines that do not parse are skipped: one damaged line must not cost the whole history. */
    fun decode(text: String): List<Episode> = text.lineSequence().mapNotNull { line ->
        val parts = line.split(",")
        // Five fields is the format before positionsCompleted existed, six before duration and
        // angles; those files still load.
        if (parts.size !in 5..8) return@mapNotNull null
        runCatching {
            Episode(
                epochMillis = parts[0].toLong(),
                side = Side.valueOf(parts[1]),
                completed = parts[2].toBooleanStrict(),
                feeling = parts[3].takeIf { it.isNotEmpty() }?.let { Feeling.valueOf(it) },
                practice = parts[4].toBooleanStrict(),
                positionsCompleted = parts.getOrNull(5)?.toInt() ?: 0,
                durationMillis = parts.getOrNull(6)?.toLong() ?: 0,
                heldAngles = parts.getOrNull(7)?.takeIf { it.isNotEmpty() }?.split("|")?.map {
                    val (turn, tip) = it.split(":")
                    turn.toDouble() to tip.toDouble()
                } ?: emptyList(),
            )
        }.getOrNull()
    }.toList()

    /** Real treatment runs, newest first. */
    fun treatments(log: List<Episode>): List<Episode> =
        log.filterNot { it.practice }.sortedByDescending { it.epochMillis }

    /**
     * The history as text to hand to a clinician.
     *
     * Plain sentences rather than a CSV: the reader is a doctor with four minutes, not a
     * spreadsheet. Practice runs are left out — they measured a phone. [formatTime] is supplied by
     * the caller so this stays free of Android and testable.
     */
    fun report(log: List<Episode>, formatTime: (Long) -> String): String {
        val runs = treatments(log)
        val body = if (runs.isEmpty()) {
            "No runs recorded."
        } else {
            runs.joinToString(System.lineSeparator()) { e ->
                val outcome = when {
                    !e.completed -> "stopped early at position ${e.positionsCompleted + 1} of 5"
                    e.feeling == Feeling.BETTER -> "felt better afterwards"
                    e.feeling == Feeling.SAME -> "no change afterwards"
                    e.feeling == Feeling.WORSE -> "felt worse afterwards"
                    else -> "completed"
                }
                "${formatTime(e.epochMillis)} — ${e.side.word} ear — $outcome"
            }
        }
        return buildString {
            appendLine("Epley Coach — self-treatment history")
            appendLine()
            appendLine(body)
            appendLine()
            appendLine(
                "Each run is a guided Epley manoeuvre for posterior canal BPPV. The ear was " +
                    "identified by a six-question symptom triage (about 71% accurate against a " +
                    "specialist), and head angles were measured with the phone's orientation " +
                    "sensor. This app is not a medical device and does not diagnose.",
            )
        }
    }

    /** Treatment runs in the current episode, i.e. within [EPISODE_WINDOW_MILLIS] of [nowMillis]. */
    fun runsThisEpisode(log: List<Episode>, nowMillis: Long): Int =
        treatments(log).count { nowMillis - it.epochMillis in 0..EPISODE_WINDOW_MILLIS }
}
