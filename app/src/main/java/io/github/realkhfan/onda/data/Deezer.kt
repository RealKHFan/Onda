package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.core.RemoteArtist
import io.github.realkhfan.onda.core.RemoteTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

/** Anonymous lookups against Deezer's public API: no key, no account, no cookies. */
object Deezer {
    private const val API = "https://api.deezer.com"

    private suspend fun get(path: String): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(Http.get("$API$path")).also { root ->
            root.optJSONObject("error")?.let { throw IOException("Deezer: ${it.optString("message")}") }
        }
    }

    suspend fun search(query: String, limit: Int = 40): List<RemoteTrack> =
        parseTracks(get("/search?q=${Http.enc(query)}&limit=$limit"))
    suspend fun searchArtists(query: String): List<RemoteArtist> = parseArtists(get("/search/artist?q=${Http.enc(query)}&limit=10"))
    suspend fun searchAlbums(query: String): List<RemoteAlbum> = parseAlbums(get("/search/album?q=${Http.enc(query)}&limit=15"))

    suspend fun artist(id: Long): RemoteArtist? = parseArtist(get("/artist/$id"))
    suspend fun artistTop(id: Long, limit: Int = 10): List<RemoteTrack> = parseTracks(get("/artist/$id/top?limit=$limit"))

    /** Artists Deezer considers similar (used to build the Daily and Weekly mixes). */
    suspend fun related(id: Long, limit: Int): List<RemoteArtist> = parseArtists(get("/artist/$id/related?limit=$limit"))

    /**
     * A fresh preview URL, fetched on the calling (background) thread. Preview links expire after
     * ~12 minutes, so the player asks for one just before each preview starts.
     */
    fun previewNow(id: Long): String? = parseTrack(JSONObject(Http.get("$API/track/$id")))?.preview

    /** Albums, singles and EPs, newest first. */
    suspend fun artistAlbums(id: Long, artistName: String, limit: Int = 100): List<RemoteAlbum> =
        parseAlbums(get("/artist/$id/albums?limit=$limit"), artistName).sortedByDescending { it.date.orEmpty() }

    suspend fun album(id: Long): Pair<RemoteAlbum, List<RemoteTrack>> = parseAlbum(get("/album/$id"))

    /** Fresh copy of one track. Preview URLs expire, so saved songs re-fetch theirs before playing. */
    suspend fun track(id: Long): RemoteTrack? = parseTrack(get("/track/$id"))

    // Kept for tests and older callers: parse a raw /search response.
    fun parseSearch(json: String): List<RemoteTrack> {
        val root = JSONObject(json)
        root.optJSONObject("error")?.let { throw IOException("Deezer: ${it.optString("message")}") }
        return parseTracks(root)
    }

    fun parseTracks(root: JSONObject, album: String? = null, cover: String? = null): List<RemoteTrack> {
        val data = root.optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).mapNotNull { parseTrack(data.getJSONObject(it), album, cover) }.distinctBy { it.id }
    }

    /** Tracks Deezer won't stream in your country stay listed (so albums are complete) but get no preview. */
    fun parseTrack(o: JSONObject, albumFallback: String? = null, coverFallback: String? = null): RemoteTrack? {
        if (o.has("error")) return null
        val id = o.optLong("id", -1L)
        if (id <= 0L) return null
        val album = o.optJSONObject("album")
        val artist = o.optJSONObject("artist")
        return RemoteTrack(
            id = id,
            title = o.str("title") ?: return null,
            artist = artist?.str("name") ?: "",
            album = album?.str("title") ?: albumFallback ?: "",
            cover = album?.str("cover_big") ?: album?.str("cover_medium") ?: coverFallback,
            preview = if (o.optBoolean("readable", true)) o.str("preview") else null,
            durationSec = o.optInt("duration"),
            artistPicture = artist?.let { picture(it) },
        )
    }

    // Artists without a photo get a generic grey placeholder whose path has an empty image hash.
    private fun picture(o: JSONObject): String? =
        (o.str("picture_big") ?: o.str("picture_medium"))?.takeUnless { it.contains("/artist//") }

    fun parseArtists(root: JSONObject): List<RemoteArtist> {
        val data = root.optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).mapNotNull { parseArtist(data.getJSONObject(it)) }.distinctBy { it.id }
    }

    fun parseArtist(o: JSONObject): RemoteArtist? {
        val id = o.optLong("id", -1L)
        if (id <= 0L) return null
        return RemoteArtist(id, o.str("name") ?: return null, picture(o))
    }

    fun parseAlbums(root: JSONObject, artistName: String? = null): List<RemoteAlbum> {
        val data = root.optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).mapNotNull { parseAlbumInfo(data.getJSONObject(it), artistName) }.distinctBy { it.id }
    }

    private fun parseAlbumInfo(o: JSONObject, artistName: String?): RemoteAlbum? {
        val id = o.optLong("id", -1L)
        if (id <= 0L) return null
        val artist = o.optJSONObject("artist")
        return RemoteAlbum(
            id = id,
            title = o.str("title") ?: return null,
            artist = artist?.str("name") ?: artistName ?: "",
            cover = o.str("cover_big") ?: o.str("cover_medium"),
            date = o.str("release_date"),
            type = o.str("record_type"),
            artistId = artist?.optLong("id", 0L) ?: 0L,
        )
    }

    fun parseAlbum(root: JSONObject): Pair<RemoteAlbum, List<RemoteTrack>> {
        val info = parseAlbumInfo(root, null) ?: throw IOException("Deezer: album not found")
        val tracks = root.optJSONObject("tracks")?.let { parseTracks(it, info.title, info.cover) } ?: emptyList()
        return info to tracks
    }
}
