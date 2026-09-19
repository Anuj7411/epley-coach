package health.epley.app

import android.content.Context
import health.epley.core.Episode
import health.epley.core.EpisodeLog
import java.io.File

/**
 * The episode history, in a private file on the phone.
 *
 * Private app storage: no permission, no network, removed with the app. The format and its
 * tolerance of damaged lines live in [EpisodeLog], where they are tested.
 */
class EpisodeStore(context: Context) {

    private val file = File(context.filesDir, "episodes.csv")

    fun load(): List<Episode> =
        if (file.exists()) runCatching { EpisodeLog.decode(file.readText()) }.getOrDefault(emptyList()) else emptyList()

    /** Append one run and return the whole history as it now stands. */
    fun append(episode: Episode): List<Episode> {
        val all = load() + episode
        // Write to a temporary file and move it into place, so a crash mid-write cannot leave a
        // half-written history behind.
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(EpisodeLog.encode(all))
        if (!tmp.renameTo(file)) {
            file.writeText(EpisodeLog.encode(all))
            tmp.delete()
        }
        return all
    }
}
