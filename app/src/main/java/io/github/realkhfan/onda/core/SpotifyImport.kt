package io.github.realkhfan.onda.core

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Reads Spotify's "Download your data" files and Exportify CSVs. Pure, for JVM tests. */
object SpotifyImport {

    /** [local]: a file Spotify played from the device, so only your own library can have it. */
    data class Song(val title: String, val artist: String, val album: String, val local: Boolean = false)

    /** Playlist1.json, Playlist2.json, …: name -> songs (podcast episodes are skipped). */
    fun playlists(json: String): List<Pair<String, List<Song>>> {
        val arr = JSONObject(json).optJSONArray("playlists") ?: return emptyList()
        return objects(arr).map { pl ->
            pl.optString("name").ifBlank { "Spotify playlist" } to objects(pl.optJSONArray("items") ?: JSONArray()).mapNotNull { item ->
                item.optJSONObject("track")?.let { t -> song(t.optString("trackName"), t.optString("artistName"), t.optString("albumName")) }
                    ?: item.optJSONObject("localTrack")?.let { localUri(it.optString("uri")) }
            }
        }
    }

    /** YourLibrary.json: your liked songs. */
    fun liked(json: String): List<Song> =
        objects(JSONObject(json).optJSONArray("tracks") ?: JSONArray()).mapNotNull { song(it.optString("track"), it.optString("artist"), it.optString("album")) }

    private val endTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /**
     * StreamingHistory*.json (endTime/msPlayed) or the extended Streaming_History_Audio_*.json (ts/ms_played).
     * Keeps songs heard 30 s or more since [sinceMs]; older plays would weigh almost nothing in the mixes anyway.
     */
    fun history(json: String, sinceMs: Long): List<Discovery.Play> = objects(JSONArray(json)).mapNotNull { o ->
        val extended = o.has("ts")
        val title = o.optString(if (extended) "master_metadata_track_name" else "trackName").takeIf { it.isNotBlank() && it != "null" }
        val artist = o.optString(if (extended) "master_metadata_album_artist_name" else "artistName")
        if (title == null || artist.isBlank() || o.optLong(if (extended) "ms_played" else "msPlayed") < 30_000) return@mapNotNull null
        val at = runCatching {
            if (extended) Instant.parse(o.getString("ts")).toEpochMilli()
            else LocalDateTime.parse(o.getString("endTime"), endTime).toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrNull() ?: return@mapNotNull null
        if (at < sinceMs) null else Discovery.Play("sp:" + o.optString("spotify_track_uri").substringAfterLast(':'), artist, title, at)
    }

    /** Exportify: one playlist per CSV, named after the file. Columns are found by their header. */
    fun csv(fileName: String, text: String): Pair<String, List<Song>> {
        val rows = parseCsv(text)
        val head = rows.firstOrNull().orEmpty().map { it.trim().lowercase() }
        fun col(name: String) = head.indexOfFirst { it.startsWith(name) }
        val t = col("track name")
        val a = col("artist name")
        val al = col("album name")
        val name = fileName.substringBeforeLast('.').replace('_', ' ').trim().ifBlank { "Spotify playlist" }
        if (t < 0 || a < 0) return name to emptyList()
        return name to rows.drop(1).mapNotNull { r -> song(r.getOrElse(t) { "" }, r.getOrElse(a) { "" }, if (al >= 0) r.getOrElse(al) { "" } else "") }
    }

    /** "spotify:local:Artist:Album:Title:215" (parts URL-encoded, "+" = space). */
    fun localUri(uri: String): Song? {
        val p = uri.split(':')
        if (p.size < 5 || p[1] != "local") return null
        fun d(s: String) = runCatching { URLDecoder.decode(s, "UTF-8") }.getOrDefault(s)
        return song(d(p[4]), d(p[2]), d(p[3]))?.copy(local = true)
    }

    /** RFC 4180: quoted fields may hold commas, quotes ("") and line breaks. */
    fun parseCsv(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == ',' -> { row += cell.toString(); cell.clear() }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row += cell.toString(); cell.clear()
                    if (row.any { it.isNotEmpty() }) rows += row
                    row = ArrayList()
                }
                else -> cell.append(c)
            }
            i++
        }
        row += cell.toString()
        if (row.any { it.isNotEmpty() }) rows += row
        return rows
    }

    /** Spotify writes versions as "Song - Remastered 2011", catalogs as "Song (Remastered 2011)". */
    fun matches(s: Song, title: String, artist: String): Boolean = MetaMatch.sameVersion(s.title, title) && (
        Match.same(s.title, s.artist, title, artist) ||
            (" - " in s.title && Match.same(s.title.substringBefore(" - "), s.artist, title, artist)))

    private fun song(title: String, artist: String, album: String): Song? =
        title.trim().takeIf { it.isNotEmpty() }?.let { Song(it, artist.trim(), album.trim()) }

    private fun objects(a: JSONArray): List<JSONObject> = (0 until a.length()).mapNotNull { a.optJSONObject(it) }
}
