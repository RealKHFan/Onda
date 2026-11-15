package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.LyricLine
import io.github.realkhfan.onda.core.Lrc
import io.github.realkhfan.onda.core.Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import kotlin.math.abs

/** Lyrics from LRCLIB (free, no account), line-synced where available. Cached on the phone after the first fetch. */
object Lrclib {
    private const val API = "https://lrclib.net/api"

    /** [verified]: LRCLIB's match for this exact song and length; false: the same title and artist at another length (maybe another version). */
    data class Lyrics(val synced: List<LyricLine>, val plain: String?, val instrumental: Boolean, val verified: Boolean = true)

    private const val VERIFIED = "ondaVerified"

    private var dir: File? = null

    /** [cacheDir]: the app's cache folder (phone or desktop). */
    fun init(cacheDir: File) {
        dir = File(cacheDir, "lyrics").apply { mkdirs() }
    }

    /** Network errors throw (and aren't cached); "no lyrics" is a normal, cached answer. */
    suspend fun get(title: String, artist: String, album: String, durationSec: Int): Lyrics = withContext(Dispatchers.IO) {
        val cache = dir?.let { File(it, sha1("v2|${Match.norm(artist)}|${Match.norm(title)}|$durationSec") + ".json") }
        val json = cache?.takeIf { it.exists() }?.readText()
            ?: (fetch(title, artist, album, durationSec)?.toString() ?: "{}").also { cache?.writeText(it) }
        parse(JSONObject(json))
    }

    private fun fetch(title: String, artist: String, album: String, durationSec: Int): JSONObject? {
        // The exact lookup needs album and length; it fails (404) on any mismatch, so fall back to search.
        if (album.isNotBlank() && album != "Unknown album" && durationSec > 0) {
            Http.getOrNull("$API/get?artist_name=${Http.enc(artist)}&track_name=${Http.enc(title)}" +
                "&album_name=${Http.enc(album)}&duration=$durationSec")?.let { return JSONObject(it).put(VERIFIED, true) }
        }
        val arr = JSONArray(Http.getOrNull("$API/search?track_name=${Http.enc(title)}&artist_name=${Http.enc(artist)}") ?: "[]")
        return pick(arr, durationSec)?.put(VERIFIED, durationSec > 0) ?: closest(arr, title, artist)?.put(VERIFIED, false)
    }

    /** Same title and artist at any length: shown with a note, since it may be another edit or version. */
    fun closest(arr: JSONArray, title: String, artist: String): JSONObject? {
        val t = Match.norm(title)
        val a = Match.norm(artist)
        if (t.isEmpty() || a.isEmpty()) return null
        return (0 until arr.length()).map { arr.getJSONObject(it) }
            .filter { Match.norm(it.optString("trackName")) == t && Match.norm(it.optString("artistName")).let { x -> x.isNotEmpty() && (x.contains(a) || a.contains(x)) } }
            .minByOrNull { it.str("syncedLyrics") == null }
    }

    /** Closest length within 10 s (any, if our length is unknown); synced lyrics preferred. */
    fun pick(arr: JSONArray, durationSec: Int): JSONObject? =
        (0 until arr.length()).map { arr.getJSONObject(it) }
            .filter { durationSec <= 0 || abs(it.optDouble("duration", 0.0) - durationSec) <= 10 }
            .minWithOrNull(compareBy({ it.str("syncedLyrics") == null }, { abs(it.optDouble("duration", 0.0) - durationSec) }))

    fun parse(o: JSONObject): Lyrics =
        Lyrics(o.str("syncedLyrics")?.let(Lrc::parse).orEmpty(), o.str("plainLyrics"), o.optBoolean("instrumental"), o.optBoolean(VERIFIED, true))

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
