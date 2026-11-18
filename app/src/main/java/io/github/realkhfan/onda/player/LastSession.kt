package io.github.realkhfan.onda.player

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * What played when Onda closed: the queue, the song and where in it, shuffle (with the list's own order) and repeat, and
 * what the queue came from. The playback service keeps it and puts it back, paused, when Onda starts again.
 */
object LastSession {
    /** "Playing from <kind>" / <label>, and the playlist id when it's one. */
    data class Source(val kind: String, val label: String, val id: String?)

    @Volatile var source = Source("Library", "Your library", null)

    data class Saved(
        val items: List<MediaItem>, val index: Int, val positionMs: Long, val repeat: Int,
        /** Shuffle on, and the list in its own order (to go back to when it's turned off). */
        val shuffle: Boolean, val context: List<MediaItem>, val source: Source,
    )

    private fun item(m: MediaItem): JSONObject = m.mediaMetadata.let { md ->
        JSONObject().put("id", m.mediaId).put("uri", m.localConfiguration?.uri?.toString()).put("title", md.title?.toString())
            .put("artist", md.artist?.toString()).put("album", md.albumTitle?.toString()).put("art", md.artworkUri?.toString())
            .put("queued", m.isQueued)
    }

    private fun item(o: JSONObject): MediaItem? {
        val uri = o.optString("uri").ifBlank { return null }
        fun text(k: String) = o.optString(k).takeIf { o.has(k) && !o.isNull(k) }
        val m = MediaItem.Builder().setMediaId(o.optString("id").ifBlank { uri }).setUri(uri).setMediaMetadata(
            MediaMetadata.Builder().setTitle(text("title")).setArtist(text("artist")).setAlbumTitle(text("album"))
                .setArtworkUri(text("art")?.let(Uri::parse)).build(),
        ).build()
        return if (o.optBoolean("queued")) m.markQueued() else m
    }

    fun toJson(s: Saved): JSONObject = JSONObject().put("items", JSONArray(s.items.map(::item))).put("index", s.index)
        .put("pos", s.positionMs).put("repeat", s.repeat).put("shuffle", s.shuffle).put("context", JSONArray(s.context.map(::item)))
        .put("kind", s.source.kind).put("label", s.source.label).put("source", s.source.id)

    /** Null when there's nothing to put back. */
    fun fromJson(o: JSONObject): Saved? {
        fun list(k: String) = o.optJSONArray(k)?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(::item) } }.orEmpty()
        val items = list("items").ifEmpty { return null }
        return Saved(
            items, o.optInt("index").coerceIn(0, items.lastIndex), o.optLong("pos").coerceAtLeast(0), o.optInt("repeat").coerceIn(0, 2),
            o.optBoolean("shuffle"), list("context"),
            Source(o.optString("kind", "Library"), o.optString("label", "Your library"), o.optString("source").ifBlank { null }),
        )
    }

    fun save(file: File, s: Saved) {
        runCatching { File(file.path + ".tmp").apply { writeText(toJson(s).toString()) }.renameTo(file) }
    }

    fun load(file: File): Saved? = runCatching { fromJson(JSONObject(file.readText())) }.getOrNull()
}
