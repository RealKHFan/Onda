package io.github.realkhfan.onda.data

import android.content.Context
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Metadata accepted from Deezer, shown instead of a file's own tags. Lives only in Onda's storage:
 * the audio files are never modified, and "Restore original tags" removes an override.
 */
object Overrides {

    data class Meta(val title: String, val artist: String, val album: String, val cover: String?, val deezerId: Long)

    private val _meta = MutableStateFlow<Map<String, Meta>>(emptyMap())
    val meta: StateFlow<Map<String, Meta>> = _meta.asStateFlow()

    /** Track ids already looked at, so "Match new songs" skips them. */
    private val _reviewed = MutableStateFlow<Set<String>>(emptySet())
    val reviewed: StateFlow<Set<String>> = _reviewed.asStateFlow()

    /** artistKey -> Deezer picture URL; "" means looked up, none found. */
    private val _artistPics = MutableStateFlow<Map<String, String>>(emptyMap())
    val artistPics: StateFlow<Map<String, String>> = _artistPics.asStateFlow()

    private var file: File? = null

    fun load(context: Context) {
        file = File(context.filesDir, "overrides.json")
        read()
    }

    fun apply(raw: List<Track>, meta: Map<String, Meta>): List<Track> =
        if (meta.isEmpty()) raw
        else raw.map { t ->
            meta[t.id]?.let { m ->
                t.copy(title = m.title, artist = m.artist, album = m.album, albumArtist = m.artist, artUri = m.cover ?: t.artUri)
            } ?: t
        }

    fun replace(local: Track, r: RemoteTrack) {
        _meta.value = _meta.value + (local.id to Meta(r.title, r.artist, r.album.ifBlank { local.album }, r.cover, r.id))
        _reviewed.value = _reviewed.value + local.id
        val key = r.artist.lowercase()
        if (r.artistPicture != null && _artistPics.value[key].isNullOrBlank()) {
            _artistPics.value = _artistPics.value + (key to r.artistPicture)
        }
        write()
    }

    fun keep(trackIds: Collection<String>) {
        if (trackIds.isEmpty()) return
        _reviewed.value = _reviewed.value + trackIds
        write()
    }

    /** After files are renamed (their ids change), accepted matches follow them. */
    fun migrateIds(map: Map<String, String>) {
        if (map.isEmpty()) return
        _meta.value = _meta.value.mapKeys { (k, _) -> map[k] ?: k }
        _reviewed.value = _reviewed.value.map { map[it] ?: it }.toSet()
        write()
    }

    fun restore(trackId: String) {
        _meta.value = _meta.value - trackId
        write()
    }

    fun setArtistPics(pics: Map<String, String>) {
        if (pics.isEmpty()) return
        _artistPics.value = _artistPics.value + pics
        write()
    }

    private fun read() {
        val f = file ?: return
        if (!f.exists()) return
        try {
            val root = JSONObject(f.readText())
            val m = root.optJSONObject("meta") ?: JSONObject()
            _meta.value = m.keys().asSequence().associateWith { id ->
                val o = m.getJSONObject(id)
                Meta(o.getString("title"), o.getString("artist"), o.optString("album"),
                    o.optString("cover").ifBlank { null }, o.optLong("dz"))
            }
            val rv = root.optJSONArray("reviewed") ?: JSONArray()
            _reviewed.value = (0 until rv.length()).map { rv.getString(it) }.toSet()
            val ap = root.optJSONObject("artistPics") ?: JSONObject()
            _artistPics.value = ap.keys().asSequence().associateWith { ap.getString(it) }
        } catch (_: Throwable) {
            // Keep the unreadable file aside so the next save can't silently wipe the user's matches.
            runCatching { f.copyTo(File(f.path + ".bad"), overwrite = true) }
        }
    }

    private fun write() {
        val f = file ?: return
        try {
            val m = JSONObject()
            _meta.value.forEach { (id, v) ->
                m.put(id, JSONObject().put("title", v.title).put("artist", v.artist).put("album", v.album)
                    .put("cover", v.cover).put("dz", v.deezerId))
            }
            val ap = JSONObject()
            _artistPics.value.forEach { (k, v) -> ap.put(k, v) }
            val root = JSONObject()
                .put("meta", m)
                .put("reviewed", JSONArray(_reviewed.value.toList()))
                .put("artistPics", ap)
            // Temp file + rename, so a crash mid-write never leaves a half-written file.
            val tmp = File(f.path + ".tmp")
            tmp.writeText(root.toString())
            tmp.renameTo(f)
        } catch (_: Throwable) {
        }
    }
}
