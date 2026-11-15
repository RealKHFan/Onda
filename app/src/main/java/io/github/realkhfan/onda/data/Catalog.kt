package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.RemoteResults
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Online search across the catalogs. Each source may fail alone; only all failing is an error. */
object Catalog {

    /** [free]: also Audius and the Internet Archive; [jamendoId]: also Jamendo (blank = off). */
    suspend fun search(query: String, apple: Boolean, free: Boolean, jamendoId: String): RemoteResults = coroutineScope {
        val none = Result.success(emptyList<RemoteTrack>())
        val audius = async { if (free) runCatching { FreeCatalogs.audius(query) } else none }
        val jamendo = async { if (jamendoId.isNotBlank()) runCatching { FreeCatalogs.jamendo(query, jamendoId) } else none }
        val archive = async { if (free) runCatching { FreeCatalogs.archive(query) } else Result.success(emptyList()) }
        val tracks = async { runCatching { Deezer.search(query) } }
        val artists = async { runCatching { Deezer.searchArtists(query) } }
        val albums = async { runCatching { Deezer.searchAlbums(query) } }
        val itunes = async { if (apple) runCatching { Itunes.searchSongs(query) } else Result.success(emptyList()) }
        val t = tracks.await()
        val ar = artists.await()
        val al = albums.await()
        val it = itunes.await()
        val au = audius.await()
        val ja = jamendo.await()
        val ia = archive.await()
        val used = listOf(t, ar, al) + listOfNotNull(it.takeIf { apple }, au.takeIf { free }, ia.takeIf { free }, ja.takeIf { jamendoId.isNotBlank() })
        if (used.all { r -> r.isFailure }) throw t.exceptionOrNull()!!
        // AI-generated "artists" (community list + your own marks) never show up.
        val ai = UserCollection::isAi
        RemoteResults(
            tracks = Match.mergeTracks(t.getOrDefault(emptyList()), it.getOrDefault(emptyList())).filterNot { x -> ai(x.artist) },
            artists = ar.getOrDefault(emptyList()).filterNot { x -> ai(x.name) },
            albums = al.getOrDefault(emptyList()).filterNot { x -> ai(x.artist) },
            free = Match.mergeTracks(ja.getOrDefault(emptyList()), au.getOrDefault(emptyList())).filterNot { x -> ai(x.artist) },
            freeAlbums = ia.getOrDefault(emptyList()).filterNot { x -> ai(x.artist) },
        )
    }

    /** A fresh audio URL for a saved song (stored previews would have expired). */
    suspend fun freshPreview(t: RemoteTrack): String? = runCatching {
        when (t.source) {
            Source.DEEZER -> Deezer.track(t.id)?.preview
            Source.ITUNES -> Itunes.track(t.id)?.preview
            else -> FreeCatalogs.stream(t)
        }
    }.getOrNull()
}
