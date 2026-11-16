package io.github.realkhfan.onda.core

import org.json.JSONArray
import org.json.JSONObject

/**
 * Library sync between your phone and your PC, over your Wi-Fi only. Each side describes its collection with
 * songs both can find, and the phone merges the two against what they agreed on last time (a three-way merge):
 * an add or a removal on either side wins, as if both used one library.
 */
object LinkSync {
    /** A song both sides can find: a catalog key ("dz:123"), or a song of your library by its artist and title. */
    data class Song(val key: String?, val title: String, val artist: String, val album: String = "", val ms: Long = 0) {
        val id: String get() = key ?: idOf(artist, title)
    }

    data class Playlist(val id: String, val name: String, val description: String, val songs: List<Song>)

    /** Details you edited for one of your songs; found on the other side by the file's own artist and title. */
    data class Edit(val title: String, val artist: String, val album: String, val cover: String?, val deezerId: Long)

    data class Snapshot(
        val playlists: List<Playlist> = emptyList(),
        val liked: List<Song> = emptyList(),
        val want: List<RemoteTrack> = emptyList(),
        /** Catalog songs that playlists and Liked Songs hold, so the other side can show and play them. */
        val catalog: List<RemoteTrack> = emptyList(),
        val followed: List<RemoteArtist> = emptyList(),
        val albums: List<RemoteAlbum> = emptyList(),
        /** "Not interested" catalog songs (keys); artists left out of suggestions; artists marked as AI (normalised names). */
        val notInterested: List<String> = emptyList(),
        val blocked: List<String> = emptyList(),
        val ai: List<String> = emptyList(),
        /** Play history; [Discovery.Play.key] is a catalog key or "" for your own songs. */
        val plays: List<Discovery.Play> = emptyList(),
        val edits: Map<String, Edit> = emptyMap(),
    )

    fun idOf(artist: String, title: String) = "~" + Match.norm(artist) + "|" + Match.norm(title)

    /** A song that's in a list but not on this device (it came from the other one): shown greyed out, kept, not played. */
    fun isMissing(id: String) = id.startsWith(Library.PLACEHOLDER)

    /** The greyed-out row for such a song (title and artist are all that's known here). */
    fun missingTrack(id: String): Track? = if (!isMissing(id)) null else id.removePrefix(Library.PLACEHOLDER).split('\u001F').takeIf { it.size == 2 }
        ?.let { (artist, title) -> Track(id, "", title, artist, "", artist, 0, 0, "", null, "") }

    /** History kept on both sides (the newest). */
    const val MAX_PLAYS = 5000

    /** [a] wins where both changed the same thing (the phone, which merges). [base] null: the first sync, so nothing is removed. */
    fun merge(base: Snapshot?, a: Snapshot, b: Snapshot): Snapshot {
        val pBase = base?.playlists?.associateBy { it.id }
        val pa = a.playlists.associateBy { it.id }
        val pb = b.playlists.associateBy { it.id }
        val playlists = list(base?.playlists, a.playlists, b.playlists) { it.id }.map { p ->
            val x = pa[p.id]; val y = pb[p.id]; val o = pBase?.get(p.id)
            if (x == null || y == null) p
            else Playlist(
                p.id,
                if (x.name != o?.name) x.name else y.name,
                if (x.description != o?.description) x.description else y.description,
                list(o?.songs, x.songs, y.songs) { it.id },
            )
        }
        val liked = list(base?.liked, a.liked, b.liked, newFirst = true) { it.id }
        val used = (playlists.flatMap { it.songs } + liked).mapNotNull { it.key }.toSet()
        val editKeys = list(base?.edits?.keys?.toList(), a.edits.keys.toList(), b.edits.keys.toList()) { it }
        return Snapshot(
            playlists = playlists,
            liked = liked,
            want = list(base?.want, a.want, b.want, newFirst = true) { it.key },
            catalog = (a.catalog + b.catalog).filter { it.key in used }.distinctBy { it.key },
            followed = list(base?.followed, a.followed, b.followed, newFirst = true) { it.id.toString() },
            albums = list(base?.albums, a.albums, b.albums, newFirst = true) { it.source.prefix + it.ref.ifEmpty { it.id.toString() } },
            notInterested = list(base?.notInterested, a.notInterested, b.notInterested) { it },
            blocked = list(base?.blocked, a.blocked, b.blocked) { it },
            ai = list(base?.ai, a.ai, b.ai) { it },
            plays = (a.plays + b.plays).distinctBy { Triple(Match.norm(it.artist), Match.norm(it.title), it.atMs / 60_000) }
                .sortedBy { it.atMs }.takeLast(MAX_PLAYS),
            edits = editKeys.associateWith { k -> a.edits[k]?.takeIf { it != base?.edits?.get(k) || b.edits[k] == null } ?: b.edits[k] ?: a.edits.getValue(k) },
        )
    }

    /**
     * One list from two: kept when both have it or one side added it since [base]; gone when one side removed it.
     * The order is the side that changed it ([a]'s when both did, with [b]'s additions first if [newFirst], else last).
     */
    fun <T> list(base: List<T>?, a: List<T>, b: List<T>, newFirst: Boolean = false, key: (T) -> String): List<T> {
        val was = base?.map(key)?.toSet()
        val inA = a.map(key).toSet()
        val inB = b.map(key).toSet()
        val ordered = when {
            base != null && a.map(key) == base.map(key) -> b + a
            newFirst -> b.filter { key(it) !in inA } + a
            else -> a + b.filter { key(it) !in inA }
        }
        return ordered.distinctBy(key).filter { val k = key(it); was == null || (k in inA && k in inB) || k !in was }
    }

    /** What's playing, handed to the other device ("Play on…"): the songs from the current one on, and where in it. */
    data class Queue(val songs: List<Song>, val catalog: List<RemoteTrack>, val positionMs: Long)

    fun queueToJson(q: Queue): JSONObject = JSONObject().put("songs", JSONArray(q.songs.map(::song)))
        .put("catalog", JSONArray(q.catalog.map(::track))).put("pos", q.positionMs)

    fun queueFromJson(o: JSONObject) = Queue(objects(o.optJSONArray("songs")).map(::song), objects(o.optJSONArray("catalog")).map(::track), o.optLong("pos"))

    /** A catalog song from a player item's key and tags (null for your own songs). */
    fun remoteOf(key: String, title: String, artist: String, album: String, art: String?, durationMs: Long = 0): RemoteTrack? {
        val source = Source.of(key) ?: return null
        val rest = key.removePrefix(source.prefix)
        return if (source.full) RemoteTrack(0, title, artist, album, art, null, (durationMs / 1000).toInt(), source, ref = rest)
        else RemoteTrack(rest.toLongOrNull() ?: return null, title, artist, album, art, null, (durationMs / 1000).toInt(), source)
    }

    /* ---------- JSON (both apps read and write the same) ---------- */

    fun toJson(s: Snapshot): JSONObject = JSONObject()
        .put("v", 1)
        .put("playlists", JSONArray(s.playlists.map { p ->
            JSONObject().put("id", p.id).put("name", p.name).put("desc", p.description).put("songs", JSONArray(p.songs.map(::song)))
        }))
        .put("liked", JSONArray(s.liked.map(::song)))
        .put("want", JSONArray(s.want.map(::track)))
        .put("catalog", JSONArray(s.catalog.map(::track)))
        .put("followed", JSONArray(s.followed.map { JSONObject().put("id", it.id).put("name", it.name).put("pic", it.picture) }))
        .put("albums", JSONArray(s.albums.map(::album)))
        .put("notInterested", JSONArray(s.notInterested)).put("blocked", JSONArray(s.blocked)).put("ai", JSONArray(s.ai))
        .put("plays", JSONArray(s.plays.map { JSONObject().put("k", it.key).put("a", it.artist).put("t", it.title).put("at", it.atMs) }))
        .put("edits", JSONObject().apply {
            s.edits.forEach { (k, e) -> put(k, JSONObject().put("t", e.title).put("a", e.artist).put("al", e.album).put("c", e.cover).put("dz", e.deezerId)) }
        })

    fun fromJson(o: JSONObject): Snapshot = Snapshot(
        playlists = objects(o.optJSONArray("playlists")).map { p ->
            Playlist(p.getString("id"), p.optString("name"), p.optString("desc"), objects(p.optJSONArray("songs")).map(::song))
        },
        liked = objects(o.optJSONArray("liked")).map(::song),
        want = objects(o.optJSONArray("want")).map(::track),
        catalog = objects(o.optJSONArray("catalog")).map(::track),
        followed = objects(o.optJSONArray("followed")).map { RemoteArtist(it.getLong("id"), it.optString("name"), it.optString("pic").ifBlank { null }) },
        albums = objects(o.optJSONArray("albums")).map(::album),
        notInterested = strings(o.optJSONArray("notInterested")), blocked = strings(o.optJSONArray("blocked")), ai = strings(o.optJSONArray("ai")),
        plays = objects(o.optJSONArray("plays")).map { Discovery.Play(it.optString("k"), it.optString("a"), it.optString("t"), it.optLong("at")) },
        edits = o.optJSONObject("edits")?.let { e ->
            e.keys().asSequence().associateWith { k ->
                e.getJSONObject(k).let { Edit(it.optString("t"), it.optString("a"), it.optString("al"), it.optString("c").ifBlank { null }, it.optLong("dz")) }
            }
        }.orEmpty(),
    )

    fun songsToJson(m: Map<String, Song>) = JSONObject().apply { m.forEach { (k, v) -> put(k, song(v)) } }
    fun songsFromJson(o: JSONObject?): Map<String, Song> = o?.let { j -> j.keys().asSequence().associateWith { song(j.getJSONObject(it)) } }.orEmpty()

    private fun song(s: Song) = JSONObject().put("k", s.key).put("t", s.title).put("a", s.artist).put("al", s.album).put("ms", s.ms)
    private fun song(o: JSONObject) = Song(o.optString("k").ifBlank { null }, o.optString("t"), o.optString("a"), o.optString("al"), o.optLong("ms"))

    private fun track(r: RemoteTrack) = JSONObject().put("id", r.id).put("title", r.title).put("artist", r.artist).put("album", r.album)
        .put("cover", r.cover).put("duration", r.durationSec).put("src", r.source.name).put("ref", r.ref).put("dl", r.canDownload)
    private fun track(o: JSONObject) = RemoteTrack(o.optLong("id"), o.optString("title"), o.optString("artist"), o.optString("album"),
        o.optString("cover").ifBlank { null }, null, o.optInt("duration"),
        Source.entries.firstOrNull { it.name == o.optString("src") } ?: Source.DEEZER, ref = o.optString("ref"), canDownload = o.optBoolean("dl"))

    private fun album(a: RemoteAlbum) = JSONObject().put("id", a.id).put("title", a.title).put("artist", a.artist).put("cover", a.cover)
        .put("date", a.date).put("type", a.type).put("artistId", a.artistId).put("src", a.source.name).put("ref", a.ref)
    private fun album(o: JSONObject) = RemoteAlbum(o.optLong("id"), o.optString("title"), o.optString("artist"), o.optString("cover").ifBlank { null },
        o.optString("date").ifBlank { null }, o.optString("type").ifBlank { null }, o.optLong("artistId"),
        Source.entries.firstOrNull { it.name == o.optString("src") } ?: Source.DEEZER, o.optString("ref"))

    private fun objects(a: JSONArray?): List<JSONObject> = if (a == null) emptyList() else (0 until a.length()).mapNotNull { a.optJSONObject(it) }
    private fun strings(a: JSONArray?): List<String> = if (a == null) emptyList() else (0 until a.length()).map { a.getString(it) }

    /* ---------- your library <-> songs both sides can find ---------- */

    /**
     * Finds songs in one device's library. [library] is what's shown (edits applied); [raw] the files' own tags.
     * A song that isn't here keeps a placeholder id, so it isn't lost and syncs back as it came.
     */
    class Library(library: List<Track>, raw: List<Track>, private val catalog: Map<String, RemoteTrack>, private val known: Map<String, Song> = emptyMap()) {
        /**
         * Every song id seen here with its song, to keep for next time: a song that's missing for a moment (a scan not
         * done, the server offline) is then still sent as itself instead of looking like you removed it.
         */
        val learned = HashMap<String, Song>()
        private val byId = (raw + library).associateBy { it.id }
        // A file you have beats the same song on your server.
        private val byName = (library + raw).sortedBy { if (it.uri.startsWith("http")) 1 else 0 }.groupBy { idOf(it.artist, it.title) }

        fun song(id: String): Song? {
            if (id.startsWith(PLACEHOLDER)) return id.removePrefix(PLACEHOLDER).split('\u001F').takeIf { it.size == 2 }?.let { Song(null, it[1], it[0]) }
            if (Source.of(id) != null) return catalog[id]?.let { Song(id, it.title, it.artist, it.album, it.durationSec * 1000L) } ?: Song(id, "", "")
            return (byId[id]?.let { Song(null, it.title, it.artist, it.album, it.durationMs) } ?: known[id])?.also { learned[id] = it }
        }

        fun id(s: Song): String = s.key ?: byName[s.id]?.first()?.id?.also { learned[it] = s } ?: (PLACEHOLDER + s.artist + '\u001F' + s.title)

        companion object { const val PLACEHOLDER = "~song:" }
    }
}
