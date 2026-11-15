package io.github.realkhfan.onda.desktop

import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Http
import io.github.realkhfan.onda.data.Subsonic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.SecureRandom

/** Your Navidrome / OpenSubsonic server: the same protocol code as the phone app. */
object Navidrome {
    const val PREFIX = "srv:"

    /** Checks the login and returns the config to keep (a salted token, never the password). */
    suspend fun connect(url: String, user: String, password: String): Subsonic.Config = withContext(Dispatchers.IO) {
        val salt = ByteArray(8).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        val cfg = Subsonic.Config(Subsonic.normalizeUrl(url), user.trim(), salt, Subsonic.token(password, salt))
        Subsonic.unwrap(Http.get(Subsonic.url(cfg, "ping")))
        cfg
    }

    suspend fun songs(cfg: Subsonic.Config): List<Subsonic.Song> = withContext(Dispatchers.IO) {
        val all = LinkedHashMap<String, Subsonic.Song>()
        var offset = 0
        while (true) {
            val q = "query=%22%22&songCount=500&songOffset=$offset&artistCount=0&albumCount=0"
            val page = Subsonic.parseSongs(Subsonic.unwrap(Http.get(Subsonic.url(cfg, "search3", q), readTimeoutMs = 30_000)))
            if (page.isEmpty() || page.all { it.id in all }) break // a server ignoring the offset would loop forever
            page.forEach { all[it.id] = it }
            if (page.size < 500) break
            offset += 500
        }
        all.values.toList()
    }

    fun tracks(songs: List<Subsonic.Song>, cfg: Subsonic.Config?, kbps: Int): List<Track> = if (cfg == null) emptyList() else songs.map { s ->
        Track(PREFIX + s.id, Subsonic.streamUrl(cfg, s.id, kbps), s.title, s.artist.ifBlank { "Unknown artist" }, s.album.ifBlank { "Unknown album" },
            s.albumArtist.ifBlank { s.artist.ifBlank { "Unknown artist" } }, s.track, s.durationSec * 1000L, "Music server",
            s.coverArt?.let { Subsonic.coverUrl(cfg, it) })
    }
}
