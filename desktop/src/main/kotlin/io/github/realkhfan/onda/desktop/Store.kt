package io.github.realkhfan.onda.desktop

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

    data class Settings(val folders: List<String> = emptyList(), val server: Subsonic.Config? = null, val kbps: Int = 0, val volume: Double = 0.8)

    fun settings(): Settings = runCatching {
        val o = JSONObject(File(data, "settings.json").readText())
        Settings(
            folders = o.optJSONArray("folders").strings(),
            server = o.optJSONObject("server")?.let { Subsonic.Config(it.getString("url"), it.getString("user"), it.getString("salt"), it.getString("token")) },
            kbps = o.optInt("kbps"),
            volume = o.optDouble("volume", 0.8),
        )
    }.getOrDefault(Settings())

    fun save(s: Settings) = write("settings.json", JSONObject()
        .put("folders", JSONArray(s.folders)).put("kbps", s.kbps).put("volume", s.volume)
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
