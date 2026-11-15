package io.github.realkhfan.onda.desktop

import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.core.RemoteArtist
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Subsonic
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

/** Settings and caches in ~/.local/share/onda and ~/.cache/onda (XDG), readable only by you. */
object Store {
    private val home = System.getProperty("user.home")
    val data: File = xdg("XDG_DATA_HOME", "$home/.local/share")
    val cache: File = xdg("XDG_CACHE_HOME", "$home/.cache")

    private fun xdg(env: String, fallback: String) =
        File(System.getenv(env)?.takeIf { it.isNotBlank() } ?: fallback, "onda").apply { mkdirs(); private() }

    private fun File.private() = runCatching {
        Files.setPosixFilePermissions(toPath(), PosixFilePermissions.fromString(if (isDirectory) "rwx------" else "rw-------"))
    }

    /** [background]: closing the window keeps the music going (reopen from the app menu or the media widget). */
    data class Settings(
        val folders: List<String> = emptyList(), val server: Subsonic.Config? = null, val kbps: Int = 0, val volume: Double = 0.8,
        val autoplay: Boolean = true, val background: Boolean = false, val releaseAlerts: Boolean = true,
        /** [apple]: also search Apple's catalog; [listenBrainz]: its similar artists in mixes; [backupDir]: daily backup folder. */
        val apple: Boolean = false, val listenBrainz: Boolean = true, val backupDir: String? = null,
        /** Every song at about the same loudness (measured once per song). */
        val normalize: Boolean = true,
        /** Equalizer gains (10 bands, dB) and the preset they came from. */
        val eq: List<Double> = List(10) { 0.0 }, val eqPreset: String = "Flat",
        /** [software]: draw without the graphics card (when a game holds it); [songNotify]: a notification per song; [zoom]: UI size. */
        val software: Boolean = false, val songNotify: Boolean = false, val zoom: Float = 1f,
    )

    fun settings(): Settings = runCatching {
        val o = JSONObject(File(data, "settings.json").readText())
        Settings(
            folders = o.optJSONArray("folders").strings(),
            server = o.optJSONObject("server")?.let { Subsonic.Config(it.getString("url"), it.getString("user"), it.getString("salt"), it.getString("token")) },
            kbps = o.optInt("kbps"),
            volume = o.optDouble("volume", 0.8),
            autoplay = o.optBoolean("autoplay", true), background = o.optBoolean("background", false),
            releaseAlerts = o.optBoolean("releaseAlerts", true),
            apple = o.optBoolean("apple", false), listenBrainz = o.optBoolean("listenBrainz", true),
            backupDir = o.optString("backupDir").ifBlank { null }, normalize = o.optBoolean("normalize", true),
            eq = o.optJSONArray("eq")?.let { a -> List(10) { a.optDouble(it, 0.0) } } ?: List(10) { 0.0 }, eqPreset = o.optString("eqPreset", "Flat"),
            software = o.optBoolean("software"), songNotify = o.optBoolean("songNotify"),
            zoom = o.optDouble("zoom", 1.0).toFloat().coerceIn(0.7f, 1.6f),
        )
    }.getOrDefault(Settings())

    fun save(s: Settings) = write("settings.json", JSONObject()
        .put("folders", JSONArray(s.folders)).put("kbps", s.kbps).put("volume", s.volume)
        .put("autoplay", s.autoplay).put("background", s.background).put("releaseAlerts", s.releaseAlerts)
        .put("apple", s.apple).put("listenBrainz", s.listenBrainz).put("backupDir", s.backupDir).put("normalize", s.normalize)
        .put("eq", org.json.JSONArray(s.eq)).put("eqPreset", s.eqPreset).put("software", s.software).put("songNotify", s.songNotify).put("zoom", s.zoom.toDouble())
        .put("server", s.server?.let { JSONObject().put("url", it.url).put("user", it.user).put("salt", it.salt).put("token", it.token) })
        .toString())

    /** Local files by path, with their modification time, so a rescan only reads what changed. */
    fun localCache(): Map<String, Pair<Long, Track>> = runCatching {
        val arr = JSONArray(File(data, "library.json").readText())
        (0 until arr.length()).associate { i -> arr.getJSONObject(i).let { it.getString("path") to (it.getLong("mtime") to track(it)) } }
    }.getOrDefault(emptyMap())

    fun saveLocal(m: Map<String, Pair<Long, Track>>) =
        write("library.json", JSONArray(m.map { (path, v) -> json(v.second).put("path", path).put("mtime", v.first) }).toString())

    fun serverSongs(): List<Subsonic.Song> = runCatching {
        val arr = JSONArray(File(data, "server_songs.json").readText())
        (0 until arr.length()).map { i ->
            arr.getJSONObject(i).let {
                Subsonic.Song(it.getString("id"), it.optString("title"), it.optString("artist"), it.optString("album"), it.optString("albumArtist"),
                    it.optInt("track"), it.optInt("duration"), it.optString("cover").ifBlank { null })
            }
        }
    }.getOrDefault(emptyList())

    fun saveServerSongs(songs: List<Subsonic.Song>) = write("server_songs.json", JSONArray(songs.map {
        JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist).put("album", it.album).put("albumArtist", it.albumArtist)
            .put("track", it.track).put("duration", it.durationSec).put("cover", it.coverArt)
    }).toString())

    fun json(t: Track): JSONObject = JSONObject().put("id", t.id).put("uri", t.uri).put("title", t.title).put("artist", t.artist)
        .put("album", t.album).put("albumArtist", t.albumArtist).put("trackNo", t.trackNo).put("durationMs", t.durationMs)
        .put("folder", t.folderName).put("art", t.artUri).put("file", t.fileName)

    fun track(o: JSONObject) = Track(o.getString("id"), o.getString("uri"), o.optString("title"), o.optString("artist"), o.optString("album"),
        o.optString("albumArtist"), o.optInt("trackNo"), o.optLong("durationMs"), o.optString("folder"), o.optString("art").ifBlank { null }, o.optString("file"))

    data class Playlist(val id: String, val name: String, val trackIds: List<String>)

    /** Liked songs (ids), your playlists, and catalog songs saved to Want to listen. */
    /** [remote]: catalog songs that Liked Songs or a playlist hold, so they can be shown and played. */
    data class Collection(
        val liked: List<String> = emptyList(), val playlists: List<Playlist> = emptyList(), val want: List<RemoteTrack> = emptyList(),
        val remote: List<RemoteTrack> = emptyList(),
        val followed: List<RemoteArtist> = emptyList(), val albums: List<RemoteAlbum> = emptyList(),
        /** "Not interested": song keys and artists (normalised) left out of suggestions; [ai]: artists you marked as AI. */
        val hidden: List<String> = emptyList(), val blocked: List<String> = emptyList(), val ai: List<String> = emptyList(),
    )

    fun collection(): Collection = runCatching {
        val o = JSONObject(File(data, "collection.json").readText())
        Collection(
            liked = o.optJSONArray("liked").strings(),
            playlists = o.optJSONArray("playlists").objects().map { Playlist(it.getString("id"), it.getString("name"), it.optJSONArray("tracks").strings()) },
            want = o.optJSONArray("want").objects().map(::remote),
            remote = o.optJSONArray("remote").objects().map(::remote),
            followed = o.optJSONArray("followed").objects().map { RemoteArtist(it.getLong("id"), it.getString("name"), it.optString("pic").ifBlank { null }) },
            albums = o.optJSONArray("albums").objects().map(::album),
            hidden = o.optJSONArray("hidden").strings(), blocked = o.optJSONArray("blocked").strings(), ai = o.optJSONArray("ai").strings(),
        )
    }.getOrElse {
        File(data, "collection.json").takeIf { f -> f.exists() }?.let { f -> f.copyTo(File(data, "collection.json.bad"), true) } // never lose it silently
        Collection()
    }

    fun saveCollection(c: Collection) = write("collection.json", JSONObject()
        .put("liked", JSONArray(c.liked))
        .put("playlists", JSONArray(c.playlists.map { JSONObject().put("id", it.id).put("name", it.name).put("tracks", JSONArray(it.trackIds)) }))
        .put("want", JSONArray(c.want.map(::json)))
        .put("remote", JSONArray(c.remote.filter { r -> r.key in c.liked || c.playlists.any { r.key in it.trackIds } }.map(::json)))
        .put("followed", JSONArray(c.followed.map { JSONObject().put("id", it.id).put("name", it.name).put("pic", it.picture) }))
        .put("albums", JSONArray(c.albums.map(::json)))
        .put("hidden", JSONArray(c.hidden)).put("blocked", JSONArray(c.blocked)).put("ai", JSONArray(c.ai))
        .toString())

    fun json(a: RemoteAlbum): JSONObject = JSONObject().put("id", a.id).put("title", a.title).put("artist", a.artist).put("cover", a.cover)
        .put("date", a.date).put("type", a.type).put("artistId", a.artistId).put("src", a.source.name).put("ref", a.ref)

    fun album(o: JSONObject) = RemoteAlbum(o.optLong("id"), o.optString("title"), o.optString("artist"), o.optString("cover").ifBlank { null },
        o.optString("date").ifBlank { null }, o.optString("type").ifBlank { null }, o.optLong("artistId"),
        Source.entries.firstOrNull { it.name == o.optString("src") } ?: Source.DEEZER, o.optString("ref"))

    fun plays(): List<Discovery.Play> = runCatching {
        JSONArray(File(data, "plays.json").readText()).objects().map { Discovery.Play(it.getString("k"), it.optString("a"), it.optString("t"), it.getLong("at")) }
    }.getOrDefault(emptyList())
    fun savePlays(l: List<Discovery.Play>) = write("plays.json", JSONArray(l.map { JSONObject().put("k", it.key).put("a", it.artist).put("t", it.title).put("at", it.atMs) }).toString())

    fun mixes(): Map<Discovery.Kind, Discover.Mix> = runCatching {
        JSONArray(File(data, "mixes.json").readText()).objects().associate {
            val k = Discovery.Kind.valueOf(it.getString("kind"))
            k to Discover.Mix(k, it.getString("period"), it.optJSONArray("tracks").objects().map(::remote))
        }
    }.getOrDefault(emptyMap())
    fun saveMixes(m: Map<Discovery.Kind, Discover.Mix>) = write("mixes.json", JSONArray(m.values.map {
        JSONObject().put("kind", it.kind.name).put("period", it.period).put("tracks", JSONArray(it.tracks.map(::json)))
    }).toString())

    data class Releases(val items: List<RemoteAlbum> = emptyList(), val seen: Set<Long> = emptySet(), val lastCheck: Long = 0)
    fun releases(): Releases = runCatching {
        val o = JSONObject(File(data, "releases.json").readText())
        Releases(o.optJSONArray("items").objects().map(::album), o.optJSONArray("seen")?.let { a -> (0 until a.length()).map { a.getLong(it) }.toSet() } ?: emptySet(), o.optLong("last"))
    }.getOrDefault(Releases())
    fun saveReleases(r: Releases) = write("releases.json", JSONObject().put("items", JSONArray(r.items.map(::json))).put("seen", JSONArray(r.seen)).put("last", r.lastCheck).toString())

    fun itemJson(i: Player.Item): JSONObject = JSONObject().put("id", i.id).put("title", i.title).put("artist", i.artist).put("album", i.album)
        .put("art", i.art).put("uri", i.uri).put("preview", i.preview).put("dz", i.deezerId).put("dur", i.durationMs)

    fun itemOf(o: JSONObject) = Player.Item(o.getString("id"), o.optString("title"), o.optString("artist"), o.optString("album"),
        o.optString("art").ifBlank { null }, o.optString("uri"), o.optBoolean("preview"), o.optLong("dz"), o.optLong("dur"))

    /** Recently played songs for Home, newest first. */
    fun recent(): List<Player.Item> = runCatching { JSONArray(File(data, "recent.json").readText()).objects().map(::itemOf) }.getOrDefault(emptyList())
    fun saveRecent(l: List<Player.Item>) = write("recent.json", JSONArray(l.map(::itemJson)).toString())

    fun json(r: RemoteTrack): JSONObject = JSONObject().put("id", r.id).put("title", r.title).put("artist", r.artist).put("album", r.album)
        .put("cover", r.cover).put("duration", r.durationSec).put("src", r.source.name).put("ref", r.ref).put("dl", r.canDownload)

    fun remote(o: JSONObject) = RemoteTrack(o.optLong("id"), o.optString("title"), o.optString("artist"), o.optString("album"),
        o.optString("cover").ifBlank { null }, null, o.optInt("duration"),
        Source.entries.firstOrNull { it.name == o.optString("src") } ?: Source.DEEZER, ref = o.optString("ref"), canDownload = o.optBoolean("dl"))

    data class Session(val items: List<Player.Item>, val index: Int, val positionMs: Long, val shuffle: Boolean, val repeat: Int)

    fun session(): Session? = runCatching {
        val o = JSONObject(File(data, "session.json").readText())
        Session(o.getJSONArray("items").objects().map {
            Player.Item(it.getString("id"), it.optString("title"), it.optString("artist"), it.optString("album"), it.optString("art").ifBlank { null },
                it.optString("uri"), it.optBoolean("preview"), it.optLong("dz"), it.optLong("dur"))
        }, o.getInt("index"), o.optLong("pos"), o.optBoolean("shuffle"), o.optInt("repeat"))
    }.getOrNull()

    fun saveSession(s: Session) = write("session.json", JSONObject()
        .put("items", JSONArray(s.items.take(2000).map {
            JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist).put("album", it.album).put("art", it.art)
                .put("uri", it.uri).put("preview", it.preview).put("dz", it.deezerId).put("dur", it.durationMs)
        }))
        .put("index", s.index).put("pos", s.positionMs).put("shuffle", s.shuffle).put("repeat", s.repeat)
        .toString())

    fun serverPlaylists(): List<Subsonic.Playlist> = runCatching {
        JSONArray(File(data, "server_playlists.json").readText()).objects().map { Subsonic.Playlist(it.getString("id"), it.getString("name"), it.optJSONArray("songs").strings()) }
    }.getOrDefault(emptyList())

    fun saveServerPlaylists(l: List<Subsonic.Playlist>) =
        write("server_playlists.json", JSONArray(l.map { JSONObject().put("id", it.id).put("name", it.name).put("songs", JSONArray(it.songIds)) }).toString())

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    /** Atomic: a crash mid-write never leaves a half file. */
    private fun write(name: String, text: String) {
        val f = File(data, name)
        val tmp = File(data, "$name.tmp")
        tmp.writeText(text)
        tmp.private()
        tmp.renameTo(f)
    }

    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else (0 until length()).map { getString(it) }
}
