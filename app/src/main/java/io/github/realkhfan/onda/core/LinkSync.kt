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

    /** [cover]: the hash of your own cover photo (null: made from the songs' covers); [folder]: the folder's id, or null. */
    data class Playlist(val id: String, val name: String, val description: String, val songs: List<Song>, val cover: String? = null, val folder: String? = null)

    /** A folder of playlists. */
    data class Folder(val id: String, val name: String)

    /** Details you edited for one of your songs; found on the other side by the file's own artist and title. */
    data class Edit(val title: String, val artist: String, val album: String, val cover: String?, val deezerId: Long)

    /** Lyrics you chose for a song: the LRCLIB id (0 = the one found) and a timing shift in ms (+ = shown earlier). */
    data class Lyric(val id: Long, val shiftMs: Long)

    /** A Daily 10 / Weekly 30 ([Discovery.Kind] name) for its day or week, so both devices play the same one. */
    data class Mix(val kind: String, val period: String, val tracks: List<RemoteTrack>, val basedOn: List<String> = emptyList())

    /**
     * Settings both apps have, kept the same on both: the highlight colour, crossfade, even loudness (and by album), the
     * equalizer, lyrics size, how playlists are sorted, similar songs when the queue ends, release alerts, Apple's catalog in
     * search, ListenBrainz, streaming quality and the music server login. Folders, window, speakers, volume and the like stay
     * per device.
     */
    val SETTINGS = listOf(
        "accent", "crossfade", "normalize", "albumGain", "eq", "eqPreset", "lyricsScale", "playlistOrder",
        "autoplay", "releaseAlerts", "apple", "listenBrainz", "kbps", "server",
    )

    const val MAX_SEARCHES = 10

    /**
     * What played last on a device (its queue in play order around the song it was on) and when it last played. The newer
     * one wins a sync; a device that isn't playing takes it over, paused at the same moment, like Spotify's devices.
     */
    data class LastPlay(val queue: Queue, val at: Long)

    /** At most [max] songs of a queue in play order, around [start] (50 before it at most). */
    fun around(songs: List<Song>, start: Int, max: Int = 500): Pair<List<Song>, Int> {
        val from = (start - 50).coerceIn(0, (songs.size - max).coerceAtLeast(0).coerceAtMost(start.coerceAtLeast(0)))
        return songs.subList(from, (from + max).coerceAtMost(songs.size)) to (start - from)
    }

    data class Snapshot(
        val playlists: List<Playlist> = emptyList(),
        val folders: List<Folder> = emptyList(),
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
        /** Your profile picture's [hashOf]; "" for none; null from an Onda that doesn't sync it (then it stays as it is). */
        val picture: String? = null,
        // Each of these is null from an older Onda that doesn't sync it; then it stays as it is.
        /** Pinned playlists (ids, in pin order). */
        val pins: List<String>? = null,
        /** Your own songs hidden from the library. */
        val hidden: List<Song>? = null,
        /** Last searches, newest first. */
        val searches: List<String>? = null,
        /** [SETTINGS] as text. */
        val settings: Map<String, String>? = null,
        /** Lyrics you picked or re-timed, by [Song.id]. */
        val lyrics: Map<String, Lyric>? = null,
        /** When each of your songs first turned up, by [Song.id] (the earliest of both): "Recently added". */
        val added: Map<String, Long>? = null,
        val mixes: List<Mix>? = null,
        val last: LastPlay? = null,
    )

    fun idOf(artist: String, title: String) = "~" + Match.norm(artist) + "|" + Match.norm(title)

    /** Placeholders ([isMissing] ids) whose song is in [library] now, mapped to its id. */
    fun heal(ids: Collection<String>, library: List<Track>): Map<String, String> {
        if (ids.isEmpty()) return emptyMap()
        val byName = library.groupBy { idOf(it.artist, it.title) }
        return ids.mapNotNull { id -> missingTrack(id)?.let { m -> byName[idOf(m.artist, m.title)]?.first()?.let { id to it.id } } }.toMap()
    }

    /** A cover photo's id across devices: the start of its SHA-256. */
    fun hashOf(bytes: ByteArray): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(bytes).take(12).joinToString("") { "%02x".format(it) }

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
                if (x.cover != o?.cover) x.cover else y.cover,
                if (x.folder != o?.folder) x.folder else y.folder,
            )
        }
        val fBase = base?.folders?.associateBy { it.id }
        val fa = a.folders.associateBy { it.id }
        val fb = b.folders.associateBy { it.id }
        val folders = list(base?.folders, a.folders, b.folders) { it.id }.map { f ->
            val x = fa[f.id]; val y = fb[f.id]
            if (x != null && (y == null || x.name != fBase?.get(f.id)?.name)) x else y ?: f
        }
        val folderIds = folders.map { it.id }.toSet()
        val liked = list(base?.liked, a.liked, b.liked, newFirst = true) { it.id }
        val used = (playlists.flatMap { it.songs } + liked).mapNotNull { it.key }.toSet()
        val editKeys = list(base?.edits?.keys?.toList(), a.edits.keys.toList(), b.edits.keys.toList()) { it }
        return Snapshot(
            // A playlist in a folder that was deleted on the other side moves out of it.
            playlists = playlists.map { if (it.folder != null && it.folder !in folderIds) it.copy(folder = null) else it },
            folders = folders,
            liked = liked,
            want = list(base?.want, a.want, b.want, newFirst = true) { it.key },
            catalog = (a.catalog + b.catalog).filter { it.key in used }.distinctBy { it.key },
            followed = list(base?.followed, a.followed, b.followed, newFirst = true) { it.id.toString() },
            albums = list(base?.albums, a.albums, b.albums, newFirst = true) { it.source.prefix + it.ref.ifEmpty { it.id.toString() } },
            notInterested = list(base?.notInterested, a.notInterested, b.notInterested) { it },
            blocked = list(base?.blocked, a.blocked, b.blocked) { it },
            ai = list(base?.ai, a.ai, b.ai) { it },
            // The same play from both sides carries the same moment; loops of a short song a few seconds apart all stay.
            plays = (a.plays + b.plays).distinctBy { Triple(Match.norm(it.artist), Match.norm(it.title), it.atMs) }
                .sortedBy { it.atMs }.takeLast(MAX_PLAYS),
            edits = editKeys.associateWith { k -> a.edits[k]?.takeIf { it != base?.edits?.get(k) || b.edits[k] == null } ?: b.edits[k] ?: a.edits.getValue(k) },
            // The side that changed it since last time ([a] when both did).
            picture = when {
                b.picture == null -> a.picture
                a.picture == null -> b.picture
                a.picture != base?.picture -> a.picture
                else -> b.picture
            },
            pins = both(base?.pins, a.pins, b.pins) { o, x, y -> list(o, x, y) { it } },
            hidden = both(base?.hidden, a.hidden, b.hidden) { o, x, y -> list(o, x, y) { it.id } },
            searches = both(base?.searches, a.searches, b.searches) { o, x, y -> list(o, x, y, newFirst = true) { Match.norm(it) }.take(MAX_SEARCHES) },
            settings = both(base?.settings, a.settings, b.settings, ::keyed),
            lyrics = both(base?.lyrics, a.lyrics, b.lyrics, ::keyed),
            // Songs on either device, each with the earliest date either saw it.
            added = both(base?.added, a.added, b.added) { _, x, y -> x + y.filter { (k, t) -> x[k]?.let { it <= t } != true } },
            mixes = both(base?.mixes, a.mixes, b.mixes) { o, x, y -> mixes(o, x, y) },
            last = listOfNotNull(a.last, b.last).maxByOrNull { it.at },
        )
    }

    private fun <T> both(base: T?, a: T?, b: T?, merge: (T?, T, T) -> T): T? = if (a == null) b else if (b == null) a else merge(base, a, b)

    /**
     * Per key: the side that changed it since [base] ([a] when both did; gone when that side removed it). With no [base] (the
     * first time), [b]'s (the PC's) values win.
     */
    fun <V> keyed(base: Map<String, V>?, a: Map<String, V>, b: Map<String, V>): Map<String, V> =
        (a.keys + b.keys).mapNotNull { k ->
            when {
                base == null -> b[k] ?: a[k]
                a[k] != base[k] -> a[k]
                else -> b[k]
            }?.let { k to it }
        }.toMap()

    /**
     * Per kind, the newer day or week. For the same one: the side that built it anew since they last agreed (the phone's
     * when both did), so both play the same mix.
     */
    private fun mixes(base: List<Mix>?, a: List<Mix>, b: List<Mix>): List<Mix> = (a + b).map { it.kind }.distinct().mapNotNull { k ->
        val x = a.firstOrNull { it.kind == k }
        val y = b.firstOrNull { it.kind == k }
        when {
            x == null -> y
            y == null -> x
            x.period != y.period -> if (x.period > y.period) x else y
            x == base?.firstOrNull { it.kind == k } -> y
            else -> x
        }
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
    /**
     * Songs in play order, starting at [start] ([positionMs] into it). [shuffle] and [repeat] (0 off, 1 all, 2 one) go along,
     * so the other device carries on the same way; the order stays as sent.
     */
    data class Queue(
        val songs: List<Song>, val catalog: List<RemoteTrack>, val positionMs: Long,
        val start: Int = 0, val shuffle: Boolean = false, val repeat: Int = 0,
    )

    fun queueToJson(q: Queue): JSONObject = JSONObject().put("songs", JSONArray(q.songs.map(::song)))
        .put("catalog", JSONArray(q.catalog.map(::track))).put("pos", q.positionMs)
        .put("start", q.start).put("shuffle", q.shuffle).put("repeat", q.repeat)

    fun queueFromJson(o: JSONObject) = Queue(objects(o.optJSONArray("songs")).map(::song), objects(o.optJSONArray("catalog")).map(::track), o.optLong("pos"),
        o.optInt("start"), o.optBoolean("shuffle"), o.optInt("repeat").coerceIn(0, 2))

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
                .put("cover", p.cover).put("folder", p.folder)
        }))
        .put("folders", JSONArray(s.folders.map { JSONObject().put("id", it.id).put("name", it.name) }))
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
        .apply { s.picture?.let { put("picture", it) } }
        .apply { s.pins?.let { put("pins", JSONArray(it)) } }
        .apply { s.hidden?.let { put("hidden", JSONArray(it.map(::song))) } }
        .apply { s.searches?.let { put("searches", JSONArray(it)) } }
        .apply { s.settings?.let { put("settings", JSONObject(it)) } }
        .apply { s.lyrics?.let { m -> put("lyrics", JSONObject().apply { m.forEach { (k, l) -> put(k, JSONArray().put(l.id).put(l.shiftMs)) } }) } }
        .apply { s.added?.let { put("added", JSONObject(it)) } }
        .apply { s.last?.let { put("last", JSONObject().put("queue", queueToJson(it.queue)).put("at", it.at)) } }
        .apply {
            s.mixes?.let { m ->
                put("mixes", JSONArray(m.map { JSONObject().put("kind", it.kind).put("period", it.period).put("tracks", JSONArray(it.tracks.map(::track))).put("basedOn", JSONArray(it.basedOn)) }))
            }
        }

    fun fromJson(o: JSONObject): Snapshot = Snapshot(
        playlists = objects(o.optJSONArray("playlists")).map { p ->
            Playlist(p.getString("id"), p.optString("name"), p.optString("desc"), objects(p.optJSONArray("songs")).map(::song),
                p.optString("cover").ifBlank { null }, p.optString("folder").ifBlank { null })
        },
        folders = objects(o.optJSONArray("folders")).map { Folder(it.getString("id"), it.optString("name")) },
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
        picture = if (o.has("picture")) o.optString("picture") else null,
        pins = if (o.has("pins")) strings(o.optJSONArray("pins")) else null,
        hidden = if (o.has("hidden")) objects(o.optJSONArray("hidden")).map(::song) else null,
        searches = if (o.has("searches")) strings(o.optJSONArray("searches")) else null,
        settings = o.optJSONObject("settings")?.let { m -> m.keys().asSequence().associateWith { m.optString(it) } },
        lyrics = o.optJSONObject("lyrics")?.let { m ->
            m.keys().asSequence().mapNotNull { k -> m.optJSONArray(k)?.let { k to Lyric(it.optLong(0), it.optLong(1)) } }.toMap()
        },
        added = o.optJSONObject("added")?.let { m -> m.keys().asSequence().associateWith { m.optLong(it) } },
        last = o.optJSONObject("last")?.let { l -> l.optJSONObject("queue")?.let { LastPlay(queueFromJson(it), l.optLong("at")) } },
        mixes = if (!o.has("mixes")) null else objects(o.optJSONArray("mixes")).map {
            Mix(it.optString("kind"), it.optString("period"), objects(it.optJSONArray("tracks")).map(::track), strings(it.optJSONArray("basedOn")))
        },
    )

    /** "gif", "png", "webp" or "jpg" for a picture's bytes; null when it's none of them. */
    fun pictureType(bytes: ByteArray): String? {
        fun at(i: Int, vararg b: Int) = bytes.size >= i + b.size && b.indices.all { bytes[i + it] == b[it].toByte() }
        return when {
            at(0, 0x47, 0x49, 0x46, 0x38) -> "gif"
            at(0, 0x89, 0x50, 0x4E, 0x47) -> "png"
            at(0, 0x52, 0x49, 0x46, 0x46) && at(8, 0x57, 0x45, 0x42, 0x50) -> "webp"
            at(0, 0xFF, 0xD8, 0xFF) -> "jpg"
            else -> null
        }
    }

    fun songToJson(s: Song) = song(s)
    fun songFromJson(o: JSONObject) = song(o)

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

        /** [song]'s [Song.id] without keeping it for next time: for things kept by song (dates, lyrics), not lists. */
        fun songId(id: String): String? = if (Source.of(id) != null) id else byId[id]?.let { idOf(it.artist, it.title) }

        /** The id here of a [Song.id] (a catalog key, or one of your songs by artist and title); null when it isn't here. */
        fun local(songId: String): String? = if (Source.of(songId) != null) songId else byName[songId]?.first()?.id

        companion object { const val PLACEHOLDER = "~song:" }
    }
}
