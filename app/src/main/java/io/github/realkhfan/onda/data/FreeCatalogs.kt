package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.core.MetaMatch
import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** Catalogs that stream whole songs for free and legally: Audius, Jamendo (needs a free client ID), Internet Archive. */
object FreeCatalogs {

    private suspend fun fetch(url: String): String = withContext(Dispatchers.IO) { Http.get(url) }

    /** The whole-song URL. Built from the id alone, so saved songs still play later. */
    fun stream(source: Source, ref: String): String? = when (source) {
        Source.AUDIUS -> "https://api.audius.co/v1/tracks/${Http.enc(ref)}/stream?app_name=Onda"
        Source.JAMENDO -> "https://prod-1.storage.jamendo.com/?trackid=${Http.enc(ref)}&format=mp32"
        Source.ARCHIVE -> "https://archive.org/download/" + ref.split('/').joinToString("/") { Http.enc(it).replace("+", "%20") }
        else -> null
    }

    fun stream(t: RemoteTrack): String? = if (t.ref.isEmpty()) null else stream(t.source, t.ref)

    /* ---------- Audius ---------- */

    suspend fun audius(query: String): List<RemoteTrack> =
        parseAudius(fetch("https://api.audius.co/v1/tracks/search?query=${Http.enc(query)}&app_name=Onda&limit=10"))

    fun parseAudius(json: String): List<RemoteTrack> {
        val data = JSONObject(json).optJSONArray("data") ?: return emptyList()
        return objects(data).mapNotNull { o ->
            val id = o.str("id") ?: return@mapNotNull null
            val title = o.str("title") ?: return@mapNotNull null
            val artist = o.optJSONObject("user")?.str("name") ?: return@mapNotNull null
            // Paid, gated, deleted or hidden songs don't stream in full.
            val blocked = !o.optBoolean("is_streamable", true) || o.optBoolean("is_stream_gated") ||
                o.optBoolean("is_delete") || o.optBoolean("is_unlisted") || !o.optBoolean("is_available", true)
            // Anyone can upload to Audius: skip re-uploads and remixes/bootlegs of other people's songs.
            if (blocked || MetaMatch.namesOtherArtist(title, artist) || Discovery.isUnwanted(title, "")) return@mapNotNull null
            RemoteTrack(0, title, artist, "", o.optJSONObject("artwork")?.str("480x480"),
                stream(Source.AUDIUS, id), o.optInt("duration"), Source.AUDIUS, ref = id,
                canDownload = o.optBoolean("is_downloadable") && !o.optBoolean("is_download_gated"))
        }
    }

    /* ---------- Jamendo ---------- */

    suspend fun jamendo(query: String, clientId: String): List<RemoteTrack> = parseJamendo(
        fetch("https://api.jamendo.com/v3.0/tracks/?client_id=${Http.enc(clientId)}&format=json&limit=10&imagesize=300&search=${Http.enc(query)}"),
    )

    fun parseJamendo(json: String): List<RemoteTrack> {
        val root = JSONObject(json)
        val head = root.optJSONObject("headers")
        if (head?.optString("status") == "failed") throw IOException("Jamendo: ${head.optString("error_message")}")
        return objects(root.optJSONArray("results") ?: JSONArray()).mapNotNull { o ->
            val id = o.str("id") ?: return@mapNotNull null
            RemoteTrack(0, o.str("name") ?: return@mapNotNull null, o.str("artist_name") ?: "Unknown artist",
                o.optString("album_name"), o.str("image") ?: o.str("album_image"),
                stream(Source.JAMENDO, id), o.optInt("duration"), Source.JAMENDO, ref = id,
                canDownload = o.optBoolean("audiodownload_allowed"))
        }
    }

    /* ---------- Internet Archive ---------- */

    // Netlabels (Creative Commons releases) and the Live Music Archive (bands that allow taping) only; the open
    // "community audio" uploads are left out because anyone can post anything there.
    suspend fun archive(query: String): List<RemoteAlbum> {
        val words = query.replace(Regex("""[^\p{L}\p{N}]+"""), " ").trim()
        if (words.isEmpty()) return emptyList()
        val q = "($words) AND mediatype:audio AND collection:(netlabels OR etree)"
        return parseArchiveSearch(fetch("https://archive.org/advancedsearch.php?q=${Http.enc(q)}" +
            "&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=creator&fl%5B%5D=date&rows=10&output=json"))
    }

    fun parseArchiveSearch(json: String): List<RemoteAlbum> {
        val docs = JSONObject(json).optJSONObject("response")?.optJSONArray("docs") ?: return emptyList()
        return objects(docs).mapNotNull { o ->
            val id = o.str("identifier") ?: return@mapNotNull null
            RemoteAlbum(0, o.first("title") ?: id, o.first("creator") ?: "Unknown artist", cover(id),
                o.first("date")?.take(10), "album", source = Source.ARCHIVE, ref = id)
        }
    }

    suspend fun archiveItem(id: String): Pair<RemoteAlbum, List<RemoteTrack>> =
        parseArchiveItem(id, fetch("https://archive.org/metadata/${Http.enc(id)}"))

    private val audio = Regex("""\.(mp3|ogg|m4a|opus|flac)$""", RegexOption.IGNORE_CASE)
    private val preferred = listOf("mp3", "ogg", "m4a", "opus", "flac")

    fun parseArchiveItem(id: String, json: String): Pair<RemoteAlbum, List<RemoteTrack>> {
        val root = JSONObject(json)
        val m = root.optJSONObject("metadata") ?: throw IOException("Internet Archive: no such item")
        val album = RemoteAlbum(0, m.first("title") ?: id, m.first("creator") ?: "Unknown artist", cover(id),
            m.first("date")?.take(10), "album", source = Source.ARCHIVE, ref = id)
        val files = objects(root.optJSONArray("files") ?: JSONArray())
        val byName = files.associateBy { it.optString("name") }
        // Each recording is an original plus copies made from it (mp3 -> ogg, flac -> mp3); play one copy of each.
        fun rootOf(f: JSONObject): JSONObject {
            var cur = f
            repeat(5) { if (cur.optString("source") != "derivative") return cur; cur = byName[cur.optString("original")] ?: return cur }
            return cur
        }
        val tracks = files.filter { audio.containsMatchIn(it.optString("name")) }
            .groupBy { rootOf(it).optString("name") }
            .map { (_, copies) ->
                val pick = copies.minWith(compareBy({ preferred.indexOf(it.optString("name").substringAfterLast('.').lowercase()) },
                    { "_64kb" in it.optString("name") }))
                val orig = rootOf(pick)
                val name = pick.optString("name")
                val no = orig.str("track")?.substringBefore('/')?.trim()?.toIntOrNull() ?: Int.MAX_VALUE
                no to RemoteTrack(0, orig.str("title") ?: name.substringAfterLast('/').substringBeforeLast('.'),
                    orig.str("creator") ?: album.artist, album.title, album.cover, stream(Source.ARCHIVE, "$id/$name"),
                    seconds(orig.str("length") ?: pick.str("length")), Source.ARCHIVE, ref = "$id/$name", canDownload = true)
            }
            .sortedWith(compareBy({ it.first }, { it.second.ref }))
            .map { it.second }
        return album to tracks
    }

    /** "158.7" or "02:38" or "1:02:38" -> seconds. */
    fun seconds(s: String?): Int {
        if (s == null) return 0
        if (':' !in s) return s.toDoubleOrNull()?.toInt() ?: 0
        return s.split(':').fold(0) { acc, part -> acc * 60 + (part.trim().toDoubleOrNull()?.toInt() ?: 0) }
    }

    private fun cover(id: String) = "https://archive.org/services/img/${Http.enc(id)}"

    private fun objects(a: JSONArray): List<JSONObject> = (0 until a.length()).mapNotNull { a.optJSONObject(it) }

    /** The Archive gives some fields as a string, some as a list; take the first either way. */
    private fun JSONObject.first(key: String): String? = optJSONArray(key)?.optString(0)?.takeIf { it.isNotBlank() } ?: str(key)
}
