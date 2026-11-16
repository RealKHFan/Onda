package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.player.Autoplay

/** Top mixes, release radar and playlist recommendations, kept for this run of the app (like on the desktop). */
object Suggest {
    private val mixes = HashMap<String, List<RemoteTrack>>()
    private val recs = HashMap<String, List<RemoteTrack>>()
    private var radar: List<RemoteTrack>? = null

    /** Similar artists' songs for "<artist> Mix"; your own songs are added by the screen. */
    suspend fun artistMix(name: String, ownKeys: Set<String>, listenBrainz: Boolean): List<RemoteTrack> =
        mixes[Match.norm(name)] ?: Autoplay.similarTracks(listOf(name), emptySet(), n = 20, listenBrainz = listenBrainz)
            .filterNot { Match.norm(it.artist) + "|" + Match.norm(it.title) in ownKeys }
            .also { if (it.isNotEmpty()) mixes[Match.norm(name)] = it }

    /** Up to 3 songs from each of your newest 12 releases. */
    suspend fun releaseRadar(releases: List<RemoteAlbum>): List<RemoteTrack> = radar ?: releases.take(12)
        .flatMap { a -> runCatching { Deezer.album(a.id).second }.getOrDefault(emptyList()).take(3) }
        .filterNot(UserCollection::notWanted)
        .also { if (it.isNotEmpty()) radar = it }

    /** Songs like a playlist's (its most frequent artists), not already in it. [refresh] finds others. */
    suspend fun recommendFor(playlistId: String, songs: List<Pair<String, String>>, inIt: Set<String>, listenBrainz: Boolean, refresh: Boolean): List<RemoteTrack> {
        if (!refresh) recs[playlistId]?.let { return it }
        val artists = songs.groupingBy { it.first }.eachCount().entries.sortedByDescending { it.value }.map { it.key }.take(8).shuffled().take(5)
        if (artists.isEmpty()) return emptyList()
        val names = songs.map { Match.norm(it.first) + "|" + Match.norm(it.second) }.toSet()
        return Autoplay.similarTracks(artists, inIt, n = 10, listenBrainz = listenBrainz)
            .filterNot { Match.norm(it.artist) + "|" + Match.norm(it.title) in names }
            .also { recs[playlistId] = it }
    }
}
