package io.github.realkhfan.onda.player

import io.github.realkhfan.onda.core.RemoteArtist
import io.github.realkhfan.onda.data.ListenBrainz
import androidx.media3.common.MediaItem
import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.ArtistIds
import io.github.realkhfan.onda.data.Deezer
import io.github.realkhfan.onda.data.UserCollection

/** "Similar songs when the queue ends": popular songs of artists related to the last ones played. */
object Autoplay {

    /** Up to [n] songs, at most 2 per artist, never one already in the list; songs you own play in full. */
    suspend fun similar(lastArtists: List<String>, inList: Set<String>, library: List<Track>, n: Int = 10, listenBrainz: Boolean = false): List<MediaItem> {
        val byTitle = library.groupBy { Match.norm(it.title) }
        return similarTracks(lastArtists, inList, n, listenBrainz).mapNotNull { r ->
            val own = byTitle[Match.norm(r.title)]?.firstOrNull { Match.same(it.title, it.artist, r.title, r.artist) }
            if (own != null && own.id !in inList) own.toMediaItem() else r.toPreviewItem()
        }
    }

    /** The catalog songs themselves (song radio, top mixes, playlist recommendations). */
    suspend fun similarTracks(lastArtists: List<String>, inList: Set<String>, n: Int = 10, listenBrainz: Boolean = false): List<RemoteTrack> {
        val seeds = lastArtists.filter { it.isNotBlank() && !UserCollection.isBlocked(it) && !UserCollection.isAi(it) }
            .distinctBy(Match::norm).take(5)
            .mapNotNull { name -> (ArtistIds.cached(name) ?: runCatching { ArtistIds.resolve(name) }.getOrDefault(0)).takeIf { it > 0 } }
        if (seeds.isEmpty()) return emptyList()
        val lb = if (!listenBrainz) emptyList() else lastArtists.take(2).flatMap { ListenBrainz.similar(it, 4) }
            .mapNotNull { name -> (ArtistIds.cached(name) ?: runCatching { ArtistIds.resolve(name) }.getOrDefault(0)).takeIf { it > 0 }?.let { RemoteArtist(it, name, null) } }
        val related = (seeds.flatMap { runCatching { Deezer.related(it, 4) }.getOrDefault(emptyList()) } + lb)
            .distinctBy { it.id }.filterNot { UserCollection.isBlocked(it.name) || UserCollection.isAi(it.name) }.shuffled().take(8)
        val songs = (related.map { it.id } + seeds).distinct()
            .flatMap { id -> runCatching { Deezer.artistTop(id, 5) }.getOrDefault(emptyList()) }
            .filter { it.preview != null && it.key !in inList && !Discovery.isUnwanted(it.title, it.album) && !UserCollection.notWanted(it) }
            .distinctBy { Match.norm(it.artist) + "|" + Match.norm(it.title) }
            .shuffled()
        val perArtist = HashMap<String, Int>()
        val picked = ArrayList<RemoteTrack>()
        for (s in songs) {
            val a = Match.norm(s.artist)
            if ((perArtist[a] ?: 0) >= 2) continue
            perArtist[a] = (perArtist[a] ?: 0) + 1
            picked += s
            if (picked.size >= n) break
        }
        return picked
    }
}
