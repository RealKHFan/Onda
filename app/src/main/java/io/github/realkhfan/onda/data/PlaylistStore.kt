package io.github.realkhfan.onda.data

import android.content.Context
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Local JSON store for user playlists and Liked Songs. Tracks are referenced by their stable id. */
object PlaylistStore {

    /** [cover]: an image of yours, copied into Onda's files; null = made from the album covers. */
    data class Playlist(val id: String, val name: String, val trackIds: List<String>, val description: String = "", val cover: String? = null)

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    /** Liked track ids, newest first. */
    private val _liked = MutableStateFlow<List<String>>(emptyList())
    val liked: StateFlow<List<String>> = _liked.asStateFlow()

    /** Catalog songs saved from Search, newest first. Deezer previews are fetched fresh at play time (links expire). */
    private val _want = MutableStateFlow<List<RemoteTrack>>(emptyList())
    val want: StateFlow<List<RemoteTrack>> = _want.asStateFlow()

    /** Catalog songs that playlists refer to by key (from a Spotify import), so they can be shown and previewed. */
    private val _remote = MutableStateFlow<Map<String, RemoteTrack>>(emptyMap())
    val remote: StateFlow<Map<String, RemoteTrack>> = _remote.asStateFlow()

    private var file: File? = null

    fun toggleWant(t: RemoteTrack) {
        val cur = _want.value
        _want.value = if (cur.any { it.key == t.key }) cur.filterNot { it.key == t.key } else listOf(t) + cur
        write()
    }

    /* Spotify import: add without toggling, never remove. */

    fun addWant(list: List<RemoteTrack>) {
        val have = _want.value.map { it.key }.toSet()
        _want.value = list.filter { it.key !in have }.distinctBy { it.key } + _want.value
        write()
    }

    fun addLikes(trackIds: List<String>) {
        _liked.value = trackIds.distinct().filter { it !in _liked.value } + _liked.value
        write()
    }

    /** The playlist with this name, or a new one: importing again fills the same playlist instead of a copy. */
    fun importInto(name: String, ids: List<String>, songs: List<RemoteTrack>) {
        val id = _playlists.value.firstOrNull { it.name == name }?.id ?: createPlaylist(name)
        addToPlaylist(id, ids, songs)
    }

    fun load(context: Context) {
        file = File(context.filesDir, "playlists.json")
        read()
    }

    fun createPlaylist(name: String): String {
        val id = UUID.randomUUID().toString()
        _playlists.value = _playlists.value + Playlist(id, name.ifBlank { "New playlist" }, emptyList())
        write()
        return id
    }

    /** Name, description and cover (like the desktop's Edit playlist). */
    fun setDetails(id: String, name: String, description: String, cover: String?) {
        _playlists.value = _playlists.value.map { if (it.id == id) it.copy(name = name.ifBlank { it.name }, description = description.trim(), cover = cover) else it }
        write()
    }

    /** "… (copy)" with the same songs; returns its id. */
    fun duplicate(id: String): String? {
        val pl = _playlists.value.firstOrNull { it.id == id } ?: return null
        val newId = UUID.randomUUID().toString()
        val cover = pl.cover?.let { c -> runCatching { File(c).let { f -> File(f.parentFile, "$newId.${f.extension}").also { f.copyTo(it) }.path } }.getOrNull() }
        _playlists.value = _playlists.value + pl.copy(id = newId, name = "${pl.name} (copy)", cover = cover)
        write()
        return newId
    }

    /** Undo for a deleted playlist, back in its place (its catalog songs are still known until the app restarts). */
    fun restore(pl: Playlist, at: Int) {
        _playlists.value = _playlists.value.toMutableList().apply { add(at.coerceIn(0, size), pl) }
        write()
    }

    /** Undo: Liked Songs as they were. */
    fun setLiked(ids: List<String>) {
        _liked.value = ids
        write()
    }

    fun deletePlaylist(id: String) {
        _playlists.value = _playlists.value.filterNot { it.id == id }
        write()
    }

    /** [songs]: catalog songs among [trackIds], kept so the playlist can show and preview them. */
    fun addToPlaylist(id: String, trackIds: List<String>, songs: List<RemoteTrack> = emptyList()) {
        if (songs.isNotEmpty()) _remote.value = _remote.value + songs.associateBy { it.key }
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id == id) pl.copy(trackIds = pl.trackIds + trackIds.filterNot { pl.trackIds.contains(it) })
            else pl
        }
        write()
    }

    fun removeFromPlaylist(id: String, trackId: String) {
        _playlists.value = _playlists.value.map {
            if (it.id == id) it.copy(trackIds = it.trackIds - trackId) else it
        }
        write()
    }

    /** Undo: a playlist's songs as they were. */
    fun setTracks(id: String, trackIds: List<String>) {
        _playlists.value = _playlists.value.map { if (it.id == id) it.copy(trackIds = trackIds) else it }
        write()
    }

    fun moveInPlaylist(id: String, from: Int, to: Int) {
        _playlists.value = _playlists.value.map { pl ->
            if (pl.id != id) return@map pl
            val list = pl.trackIds.toMutableList()
            if (from in list.indices && to in list.indices) list.add(to, list.removeAt(from))
            pl.copy(trackIds = list)
        }
        write()
    }

    /** After files are renamed (their ids change), playlists and likes follow them. */
    fun migrateIds(map: Map<String, String>) {
        if (map.isEmpty()) return
        _playlists.value = _playlists.value.map { pl -> pl.copy(trackIds = pl.trackIds.map { map[it] ?: it }) }
        _liked.value = _liked.value.map { map[it] ?: it }
        write()
    }

    fun toggleLike(trackId: String) {
        val cur = _liked.value
        _liked.value = if (cur.contains(trackId)) cur - trackId else listOf(trackId) + cur
        write()
    }

    private fun read() {
        val f = file ?: return
        if (!f.exists()) return
        try {
            val root = JSONObject(f.readText())
            val pls = root.optJSONArray("playlists") ?: JSONArray()
            _playlists.value = (0 until pls.length()).map { i ->
                val o = pls.getJSONObject(i)
                val tracks = o.optJSONArray("tracks") ?: JSONArray()
                Playlist(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    trackIds = (0 until tracks.length()).map { tracks.getString(it) },
                    description = o.optString("desc"),
                    cover = o.optString("cover").ifBlank { null },
                )
            }
            val liked = root.optJSONArray("liked") ?: JSONArray()
            _liked.value = (0 until liked.length()).map { liked.getString(it) }
            val want = root.optJSONArray("want") ?: JSONArray()
            _want.value = (0 until want.length()).map { remoteFromJson(want.getJSONObject(it)) }
            val remote = root.optJSONArray("remote") ?: JSONArray()
            _remote.value = (0 until remote.length()).map { remoteFromJson(remote.getJSONObject(it)) }.associateBy { it.key }
        } catch (_: Throwable) {
            // Keep the unreadable file aside so the next save can't silently wipe the user's playlists.
            runCatching { f.copyTo(File(f.path + ".bad"), overwrite = true) }
        }
    }

    private fun write() {
        val f = file ?: return
        try {
            val root = JSONObject()
            val pls = JSONArray()
            _playlists.value.forEach { pl ->
                pls.put(
                    JSONObject()
                        .put("id", pl.id)
                        .put("name", pl.name)
                        .put("tracks", JSONArray(pl.trackIds))
                        .put("desc", pl.description.ifBlank { null })
                        .put("cover", pl.cover),
                )
            }
            root.put("playlists", pls)
            root.put("liked", JSONArray(_liked.value))
            root.put("want", JSONArray(_want.value.map { it.toJson() }))
            // Only songs a playlist still holds (deleted playlists and removed songs drop out).
            val used = _playlists.value.flatMap { it.trackIds }.toSet()
            root.put("remote", JSONArray(_remote.value.values.filter { it.key in used }.map { it.toJson() }))
            // Write to a temp file and rename, so a crash mid-write never leaves a half-written file.
            val tmp = File(f.path + ".tmp")
            tmp.writeText(root.toString())
            tmp.renameTo(f)
        } catch (_: Throwable) {
        }
    }
}

internal fun RemoteTrack.toJson(): JSONObject = JSONObject()
    .put("id", id).put("title", title).put("artist", artist).put("album", album).put("cover", cover)
    .put("preview", preview).put("duration", durationSec).put("src", source.name).put("ref", ref).put("dl", canDownload)

internal fun remoteFromJson(o: JSONObject) = RemoteTrack(
    id = o.optLong("id"),
    title = o.optString("title"),
    artist = o.optString("artist"),
    album = o.optString("album"),
    cover = o.optString("cover").ifBlank { null },
    preview = o.optString("preview").ifBlank { null }, // saved before 0.8.0: none, so it's fetched on play
    durationSec = o.optInt("duration"),
    // Entries saved before 0.2.4 have no "src"; they were all Deezer.
    source = Source.entries.firstOrNull { it.name == o.optString("src") } ?: Source.DEEZER,
    ref = o.optString("ref"),
    canDownload = o.optBoolean("dl"),
)
