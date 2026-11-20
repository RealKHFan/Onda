package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
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

    /** [at]: when it was made (0: before Onda kept dates); the newer edit wins a sync. */
    data class Meta(val title: String, val artist: String, val album: String, val cover: String?, val deezerId: Long, val at: Long = 0)

    private val _meta = MutableStateFlow<Map<String, Meta>>(emptyMap())
    val meta: StateFlow<Map<String, Meta>> = _meta.asStateFlow()

    /** Track ids already looked at, so "Match new songs" skips them. */
    private val _reviewed = MutableStateFlow<Set<String>>(emptySet())
    val reviewed: StateFlow<Set<String>> = _reviewed.asStateFlow()

    /** artistKey -> Deezer picture URL; "" means looked up, none found. */
    private val _artistPics = MutableStateFlow<Map<String, String>>(emptyMap())
    val artistPics: StateFlow<Map<String, String>> = _artistPics.asStateFlow()

    /** Songs whose original tags you restored since the last sync (and when): only these lose their edit on the other device too. */
    private val _restored = MutableStateFlow<Map<String, Long>>(emptyMap())

    /** The newest rename this device knows of per song ([LinkSync.nameKey]): its own and those taken over from the other device. */
    private val _names = MutableStateFlow<Map<String, LinkSync.Name>>(emptyMap())
    val names: StateFlow<Map<String, LinkSync.Name>> = _names.asStateFlow()

    private var file: File? = null

    /** [dir]: the app's own storage (the phone's files folder, the desktop's data folder). */
    fun load(dir: File) {
        file = File(dir, "overrides.json")
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
        // Only a Deezer id is kept: it makes the song's link; an Apple id there would point to another song.
        _meta.value = _meta.value + (local.id to Meta(r.title, r.artist, r.album.ifBlank { local.album }, r.cover, if (r.source == Source.DEEZER) r.id else 0, now()))
        _reviewed.value = _reviewed.value + local.id
        _restored.value = _restored.value - local.id
        val key = r.artist.lowercase()
        if (r.artistPicture != null && _artistPics.value[key].isNullOrBlank()) {
            _artistPics.value = _artistPics.value + (key to r.artistPicture)
        }
        write()
    }

    /** Details typed in by hand; a cover ([cover], or one from an earlier match) and Deezer link stay. Blank fields keep what was shown. */
    fun edit(t: Track, title: String, artist: String, album: String, cover: String? = null) {
        val old = _meta.value[t.id]
        _meta.value = _meta.value + (t.id to Meta(title.trim().ifEmpty { t.title }, artist.trim().ifEmpty { t.artist },
            album.trim().ifEmpty { t.album }, cover ?: old?.cover, old?.deezerId ?: 0, now()))
        _reviewed.value = _reviewed.value + t.id
        _restored.value = _restored.value - t.id
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
        _restored.value = _restored.value.mapKeys { (k, _) -> map[k] ?: k }
        write()
    }

    /**
     * Edits whose song file moved (a new path is a new id) follow it by file name: only when exactly one song here has
     * that name and no edit of its own. Call after a full scan. Returns how many moved.
     */
    fun relink(raw: List<Track>): Int {
        val ids = raw.mapTo(HashSet()) { it.id }
        val lost = _meta.value.keys.filter { it !in ids }.mapNotNull { id -> fileNameOf(id)?.let { id to it.lowercase() } }
        if (lost.isEmpty()) return 0
        val free = raw.filter { it.id !in _meta.value && it.fileName.isNotBlank() }.groupBy { it.fileName.lowercase() }
        val moves = lost.mapNotNull { (id, name) -> free[name]?.singleOrNull()?.let { id to it.id } }
            .groupBy({ it.second }, { it.first }).filter { it.value.size == 1 }.map { (to, from) -> from.single() to to }.toMap()
        migrateIds(moves)
        return moves.size
    }

    /** The file name in a file or document id ("file:/…/Tides.mp3", "content://…/document/primary%3AMusic%2FTides.mp3"). */
    private fun fileNameOf(id: String): String? =
        if (!id.startsWith("file:") && !id.startsWith("content:")) null
        else runCatching { java.net.URI(id).path?.substringAfterLast('/')?.takeIf { '.' in it } }.getOrNull()

    fun restore(trackId: String) {
        _meta.value = _meta.value - trackId
        _restored.value = _restored.value + (trackId to now())
        write()
    }

    private fun now() = System.currentTimeMillis()

    /**
     * The edits to sync: those agreed last time ([agreed]) and yours here, minus the songs you restored. A song merely
     * missing an edit here (not loaded yet, renamed, scanned again) keeps it; only "Restore original tags" removes it.
     */
    fun forSync(raw: List<Track>, agreed: Map<String, LinkSync.Edit>): Map<String, LinkSync.Edit> {
        val restored = restoresForSync(raw).keys
        // An edit here older than the agreed one is one this device missed (its songs weren't loaded yet), not news.
        // Agreed edits under the old loose keys ("~") are left behind: each device sends its own under the exact key.
        return agreed.filterKeys { it !in restored && !it.startsWith("~") } + forSync(raw).filter { (k, e) -> agreed[k]?.let { it.at > e.at } != true }
    }

    /** Your restores of original tags, by song key, with when: a restore newer than an edit removes it on both devices. */
    fun restoresForSync(raw: List<Track>): Map<String, Long> =
        raw.mapNotNull { t -> _restored.value[t.id]?.let { LinkSync.nameKey(t) to it } }.groupBy({ it.first }, { it.second }).mapValues { (_, at) -> at.max() }

    /** After a sync took in your restores (those made up to [upTo]), they're news no more. */
    fun synced(upTo: Long = Long.MAX_VALUE) {
        val left = _restored.value.filterValues { it > upTo }
        if (left == _restored.value) return
        _restored.value = left
        write()
    }

    /** For library sync: your edits by each song's own tags (artist and title), so the other device finds the same song. */
    fun forSync(raw: List<Track>): Map<String, io.github.realkhfan.onda.core.LinkSync.Edit> {
        val byId = raw.associateBy { it.id }
        return _meta.value.mapNotNull { (id, m) ->
            byId[id]?.let { t -> LinkSync.fileKey(t) to LinkSync.Edit(m.title, m.artist, m.album, m.cover, m.deezerId, m.at) }
        }
            // The same file twice (two folders): the newest edit of them, not whichever happens to come last.
            .groupBy({ it.first }, { it.second }).mapValues { (_, edits) -> edits.maxBy { it.at } }
    }

    /** The synced edits on the songs found here (an edit undone on the other side is undone here); returns those for songs not here. */
    fun fromSync(raw: List<Track>, edits: Map<String, io.github.realkhfan.onda.core.LinkSync.Edit>): Map<String, io.github.realkhfan.onda.core.LinkSync.Edit> {
        val exact = edits.filterKeys { !it.startsWith("~") } // old loose keys fit every version of a song: not used
        // Named differently here (renamed, not synced yet): the one edit for exactly this artist and title, if only one.
        val bySong = exact.entries.groupBy { LinkSync.songPart(it.key) }
        val known = raw.map { it.id }.toSet()
        val used = HashSet<String>()
        val next = _meta.value.filterKeys { it !in known }.toMutableMap() // songs this device can't see right now keep theirs
        for (t in raw) {
            val k = LinkSync.fileKey(t)
            val (key, e) = exact[k]?.let { k to it } ?: bySong[LinkSync.songPart(k)]?.singleOrNull()?.toPair() ?: continue
            used += key
            next[t.id] = Meta(e.title, e.artist, e.album, e.cover, e.deezerId, e.at)
        }
        if (next != _meta.value) { _meta.value = next; _reviewed.value = _reviewed.value + next.keys; write() }
        return exact.filterKeys { it !in used }
    }

    /** Files renamed here ([LinkSync.nameOf]): by you, or to match the other device (with its date), so each is done once. */
    fun knowNames(names: Map<String, LinkSync.Name>) {
        val newer = names.filter { (k, n) -> n.at > (_names.value[k]?.at ?: 0L) }
        if (newer.isEmpty()) return
        _names.value = _names.value + newer
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
                    o.optString("cover").ifBlank { null }, o.optLong("dz"), o.optLong("at"))
            }
            val rv = root.optJSONArray("reviewed") ?: JSONArray()
            _reviewed.value = (0 until rv.length()).map { rv.getString(it) }.toSet()
            // Before 0.29.0 a list of ids without dates.
            _restored.value = root.optJSONObject("restored")?.let { o -> o.keys().asSequence().associateWith { o.optLong(it) } }
                ?: root.optJSONArray("restored")?.let { a -> (0 until a.length()).associate { a.getString(it) to 0L } }.orEmpty()
            _names.value = root.optJSONObject("names")?.let { o ->
                o.keys().asSequence().associateWith { k -> o.getJSONObject(k).let { LinkSync.Name(it.optString("n"), it.optString("al"), it.optLong("at")) } }
            }.orEmpty()
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
                    .put("cover", v.cover).put("dz", v.deezerId).put("at", v.at))
            }
            val ap = JSONObject()
            _artistPics.value.forEach { (k, v) -> ap.put(k, v) }
            val root = JSONObject()
                .put("meta", m)
                .put("reviewed", JSONArray(_reviewed.value.toList()))
                .put("restored", JSONObject(_restored.value as Map<*, *>))
                .put("names", JSONObject().apply { _names.value.forEach { (k, n) -> put(k, JSONObject().put("n", n.stem).put("al", n.album).put("at", n.at)) } })
                .put("artistPics", ap)
            // Temp file + rename, so a crash mid-write never leaves a half-written file.
            val tmp = File(f.path + ".tmp")
            tmp.writeText(root.toString())
            tmp.renameTo(f)
        } catch (_: Throwable) {
        }
    }
}
