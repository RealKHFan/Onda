package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.MetaMatch
import io.github.realkhfan.onda.core.RemoteTrack

/** Finds a free whole-song copy (Jamendo, Openverse, Audius) of a Deezer song, so it plays in full instead of a preview. */
object FullCopies {
    /** Null when there's none; throws when a catalog can't be reached (so "none" isn't remembered by mistake). */
    suspend fun find(deezerId: Long, audius: Boolean, jamendoId: String): RemoteTrack? {
        val t = Deezer.track(deezerId) ?: return null
        val q = "${t.artist} ${t.title}"
        // [audius] is the "Free full songs" switch; it covers Openverse too.
        val candidates = (if (jamendoId.isNotBlank()) FreeCatalogs.jamendo(q, jamendoId) else emptyList()) +
            (if (audius) FreeCatalogs.openverse(q) + FreeCatalogs.audius(q) else emptyList())
        return candidates.firstOrNull { MetaMatch.isFullCopy(t, it) }
    }
}
