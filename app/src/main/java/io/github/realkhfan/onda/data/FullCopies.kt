package io.github.realkhfan.onda.data

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import io.github.realkhfan.onda.core.MetaMatch
import io.github.realkhfan.onda.core.RemoteTrack

/** Finds a free whole-song copy (Jamendo, Openverse, Audius) of a Deezer song, so it plays in full instead of a preview. */
object FullCopies {
    /** Null when there's none; throws when a catalog can't be reached (so "none" isn't remembered by mistake). */
    suspend fun find(deezerId: Long, audius: Boolean, jamendoId: String): RemoteTrack? {
        val t = Deezer.track(deezerId) ?: return null
        val q = "${t.artist} ${t.title}"
        // [audius] is the "Free full songs" switch; it covers Openverse too. All asked at once, so a preview isn't held up.
        val candidates = coroutineScope {
            val ja = async { if (jamendoId.isNotBlank()) FreeCatalogs.jamendo(q, jamendoId) else emptyList() }
            val ov = async { if (audius) FreeCatalogs.openverse(q) else emptyList() }
            val au = async { if (audius) FreeCatalogs.audius(q) else emptyList() }
            ja.await() + ov.await() + au.await()
        }
        return candidates.firstOrNull { MetaMatch.isFullCopy(t, it) }
    }
}
