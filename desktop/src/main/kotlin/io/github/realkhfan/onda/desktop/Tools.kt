package io.github.realkhfan.onda.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.core.FileNames
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.SpotifyImport
import io.github.realkhfan.onda.core.SpotifyImport.Song
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Deezer
import io.github.realkhfan.onda.data.FreeCatalogs
import io.github.realkhfan.onda.data.Subsonic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.util.zip.ZipInputStream

/** Renames music files to "Artist - Title" from their details. Only names change, never the audio or its tags. */
object Renames {
    class Result(val moved: Map<String, String>, val failed: List<String>, val undo: List<FileNames.Rename>)

    fun plan(tracks: List<Track>): List<FileNames.Rename> = FileNames.plan(tracks.filter { it.id.startsWith("file:") })

    /** Never overwrites: a name that's already taken, a missing file or a read-only folder is left as it was. */
    fun apply(plan: List<FileNames.Rename>): Result {
        val moved = LinkedHashMap<String, String>()
        val failed = ArrayList<String>()
        val undo = ArrayList<FileNames.Rename>()
        for (r in plan) {
            val ok = runCatching {
                val from = File(URI(r.id))
                val to = File(from.parentFile, r.to)
                if (!from.isFile || to.exists()) return@runCatching false
                Files.move(from.toPath(), to.toPath())
                moved[r.id] = to.toURI().toString()
                undo += FileNames.Rename(to.toURI().toString(), r.to, r.from)
                true
            }.getOrDefault(false)
            if (!ok) failed += r.from
        }
        return Result(moved, failed, undo)
    }
}

/** Imports a Spotify data export (ZIP or its JSON files) or Exportify CSVs into playlists, likes, Want to listen and history. */
object SpotifyImports {
    class State(val running: Boolean = false, val done: Int = 0, val total: Int = 0, val result: String? = null, val error: String? = null)

    var state by mutableStateOf(State()); private set
    private const val MAX_FILE = 64 shl 20 // a big extended-history file is ~15 MB; anything far bigger isn't Spotify's
    private const val HISTORY_DAYS = 90L
    private val cacheFile = File(Store.data, "spotify_matches.json")

    private class Found(
        val playlists: MutableList<Pair<String, List<Song>>> = ArrayList(),
        val liked: MutableList<Song> = ArrayList(),
        val plays: MutableList<Discovery.Play> = ArrayList(),
    )

    suspend fun run(file: File, app: AppState) {
        if (state.running) return
        state = State(running = true)
        try {
            val found = withContext(Dispatchers.IO) { read(file) }
            if (found.playlists.isEmpty() && found.liked.isEmpty() && found.plays.isEmpty()) {
                state = State(error = "No Spotify playlists, liked songs or listening history in that file.")
                return
            }
            importAll(found, app)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            state = State(error = "Couldn't read that file (${e.javaClass.simpleName}).")
        }
    }

    private fun read(file: File): Found {
        val found = Found()
        val since = System.currentTimeMillis() - HISTORY_DAYS * 86_400_000
        fun take(name: String, text: () -> String?) {
            val n = name.substringAfterLast('/')
            when {
                Regex("""Playlist\d*\.json""", RegexOption.IGNORE_CASE).matches(n) -> text()?.let { found.playlists += SpotifyImport.playlists(it) }
                n.equals("YourLibrary.json", ignoreCase = true) -> text()?.let { found.liked += SpotifyImport.liked(it) }
                n.startsWith("StreamingHistory", true) || n.startsWith("Streaming_History_Audio", true) ->
                    text()?.let { found.plays += SpotifyImport.history(it, since) }
                n.endsWith(".csv", true) -> text()?.let { found.playlists += SpotifyImport.csv(n, it) }
            }
        }
        file.inputStream().use { input ->
            if (file.name.endsWith(".zip", true)) {
                ZipInputStream(input).use { zip ->
                    while (true) {
                        val e = zip.nextEntry ?: break
                        if (!e.isDirectory) take(e.name) { readText(zip) }
                    }
                }
            } else {
                take(file.name) { readText(input) }
            }
        }
        return found
    }

    private suspend fun importAll(found: Found, app: AppState) {
        val byTitle = app.library.groupBy { Match.norm(it.title) }
        fun owned(s: Song): Track? = (byTitle[Match.norm(s.title)].orEmpty() + byTitle[Match.norm(s.title.substringBefore(" - "))].orEmpty())
            .firstOrNull { SpotifyImport.matches(s, it.title, it.artist) }

        val songs = (found.playlists.flatMap { it.second } + found.liked).distinctBy(::keyOf)
        val cache = runCatching { JSONObject(cacheFile.readText()) }.getOrElse { JSONObject() }
        val todo = songs.filter { owned(it) == null && !it.local && !cache.has(keyOf(it)) }
        state = State(running = true, total = todo.size)

        var problem: String? = null
        var failuresInARow = 0
        for ((i, s) in todo.withIndex()) {
            state = State(running = true, done = i, total = todo.size)
            val hit = try {
                Deezer.search("${s.artist} ${s.title.substringBefore(" - ")}".trim(), 10)
                    .firstOrNull { SpotifyImport.matches(s, it.title, it.artist) }
                    .also { failuresInARow = 0 }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (++failuresInARow >= 5) {
                    problem = "Couldn't reach Deezer, so some songs are missing. Import the same file again to finish; " +
                        "songs already found aren't looked up twice."
                    break
                }
                delay(1000)
                continue
            }
            cache.put(keyOf(s), hit?.let(Store::json) ?: JSONObject.NULL)
            if (i % 25 == 24) save(cache)
            delay(120) // Deezer allows about 50 requests in 5 seconds
        }
        save(cache)

        fun remote(s: Song): RemoteTrack? = cache.optJSONObject(keyOf(s))?.let(Store::remote)
        var inLibrary = 0
        var previews = 0
        var missing = 0
        val lists = ArrayList<Pair<String, List<String>>>()
        val catalog = ArrayList<RemoteTrack>()
        for ((name, list) in found.playlists) {
            val ids = ArrayList<String>()
            for (s in list) {
                val own = owned(s)
                val r = if (own == null) remote(s) else null
                when {
                    own != null -> { ids += own.id; inLibrary++ }
                    r != null -> { ids += r.key; catalog += r; previews++ }
                    else -> missing++
                }
            }
            if (ids.isNotEmpty()) lists += name to ids
        }
        val likedOwned = found.liked.mapNotNull { owned(it)?.id }
        val likedWant = found.liked.filter { owned(it) == null }.mapNotNull(::remote)
        app.importSpotify(lists, catalog, likedOwned, likedWant, found.plays)

        val parts = listOfNotNull(
            found.playlists.size.takeIf { it > 0 }?.let { "$it playlists: $inLibrary songs in your library, $previews as previews, $missing not found" },
            found.liked.size.takeIf { it > 0 }?.let { "${likedOwned.size} liked songs added to Liked Songs, ${likedWant.size} to Want to listen" },
            found.plays.size.takeIf { it > 0 }?.let { "${found.plays.size} plays from the last $HISTORY_DAYS days for your mixes" },
        )
        state = State(result = parts.joinToString(".\n") + ".", error = problem)
    }

    private fun keyOf(s: Song) = Match.norm(s.artist) + "|" + Match.norm(s.title)

    private fun save(cache: JSONObject) {
        runCatching { File(cacheFile.path + ".tmp").apply { writeText(cache.toString()) }.renameTo(cacheFile) }
    }

    /** Null when bigger than [MAX_FILE], so a bad or hostile file can't fill the memory. */
    private fun readText(input: InputStream): String? {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_FILE) return null
            out.write(buf, 0, n)
        }
        return out.toString("UTF-8")
    }
}

/**
 * Saves songs as music files in "Onda downloads" inside your first music folder, so they join the library and play offline.
 * Only from sources that allow it: your own server, and free catalogs where [RemoteTrack.canDownload].
 */
object Downloads {
    /** [replaces]: the server song's id, so playlists and likes move to the file once it's saved. */
    data class Item(val title: String, val artist: String, val url: String, val ext: String?, val replaces: String? = null)

    /** [paused]: stopped without a connection; the rest continues the next time Onda opens. */
    data class State(val running: Boolean = false, val done: Int = 0, val total: Int = 0, val failed: Int = 0,
                     val current: String? = null, val paused: Boolean = false)

    /** A failure that retrying can't fix (not audio, no usable name). */
    private class Permanent(message: String) : IOException(message)

    const val FOLDER = "Onda downloads"
    var state by mutableStateOf(State()); private set
    private val queue = ArrayDeque<Item>() // touched on the UI thread only
    private val pending = File(Store.data, "downloads.json")

    fun forTrack(t: Track, cfg: Subsonic.Config?): Item? =
        if (cfg != null && t.id.startsWith(Navidrome.PREFIX)) {
            Item(t.title, t.artist, Subsonic.downloadUrl(cfg, t.id.removePrefix(Navidrome.PREFIX)), null, replaces = t.id)
        } else null

    fun forRemote(r: RemoteTrack): Item? = if (!r.canDownload) null else FreeCatalogs.stream(r)?.let { url ->
        Item(r.title, r.artist, url, if (r.ref.contains('/')) r.ref.substringAfterLast('.', "mp3") else "mp3")
    }

    fun target(app: AppState): File? = app.settings.folders.map(::File).firstOrNull { it.isDirectory && it.canWrite() }

    /** Starts (or extends) the downloads; returns why not when it can't. */
    fun start(app: AppState, items: List<Item>): String? {
        if (items.isEmpty()) return "Nothing here can be downloaded."
        if (target(app) == null) return "Add a music folder first (Settings → Music folders), so downloads have a place to go."
        val new = items.filter { i -> queue.none { it.url == i.url } }.distinctBy { it.url }
        if (new.isEmpty()) return null
        val fresh = !state.running
        queue.addAll(new)
        persist()
        state = if (fresh) State(running = true, total = queue.size) else state.copy(total = state.total + new.size)
        if (fresh) app.launch { run(app) }
        return null
    }

    /** Continues downloads that didn't finish last time (closed app, lost connection). */
    fun resume(app: AppState) {
        val saved = runCatching {
            val arr = JSONArray(pending.readText())
            (0 until arr.length()).map { i ->
                arr.getJSONObject(i).let { Item(it.getString("t"), it.getString("a"), it.getString("u"), it.optString("e").ifBlank { null }, it.optString("r").ifBlank { null }) }
            }
        }.getOrDefault(emptyList())
        if (saved.isNotEmpty()) start(app, saved)
    }

    private fun persist() {
        runCatching {
            val arr = JSONArray(queue.map { JSONObject().put("t", it.title).put("a", it.artist).put("u", it.url).put("e", it.ext).put("r", it.replaces) })
            File(pending.path + ".tmp").apply { writeText(arr.toString()) }.renameTo(pending)
        }
    }

    private suspend fun run(app: AppState) {
        val moved = HashMap<String, String>()
        try {
            val dir = File(target(app) ?: throw IOException("No music folder"), FOLDER).apply { mkdirs() }
            outer@ while (queue.isNotEmpty()) {
                val item = queue.first()
                state = state.copy(current = item.title)
                var attempt = 0
                while (true) {
                    try {
                        val saved = withContext(Dispatchers.IO) { save(dir, item) }
                        if (saved != null && item.replaces != null) moved[item.replaces] = saved
                        break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (offline(e)) { state = state.copy(paused = true); break@outer } // keep it and everything after it
                        if (e is Permanent || ++attempt >= 3) { state = state.copy(failed = state.failed + 1); break }
                        delay(attempt * 5_000L) // a short hiccup: try again
                    }
                }
                queue.removeFirst()
                persist()
                state = state.copy(done = state.done + 1)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            state = state.copy(failed = state.failed + state.total - state.done, done = state.total) // no folder: none could go
            queue.clear()
            persist()
        } finally {
            queue.clear() // what's left (paused) stays in the file for next time
            state = state.copy(running = false, current = null)
        }
        app.rescanNow()
        // After the scan has found the files: playlists, likes and history that held the server copy move to them.
        app.migrateIds(moved)
        val s = state
        val ok = s.done - s.failed
        if (s.paused) Discover.notify("Downloads paused: no connection", "${s.total - s.done} left. They continue the next time you open Onda.")
        else Discover.notify(if (s.failed == 0) "Downloaded $ok songs" else "Downloaded $ok songs, ${s.failed} failed", "They're in your library, in “$FOLDER”.")
    }

    private fun offline(e: Exception) =
        e is java.net.UnknownHostException || e is java.net.ConnectException || e is java.net.NoRouteToHostException

    /** The new file's id (as the library scan gives it), or null if a file of that name was already there. */
    internal fun save(dir: File, item: Item): String? {
        val conn = URL(item.url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("User-Agent", "Onda")
        try {
            val code = conn.responseCode
            if (code in 400..499) throw Permanent("HTTP $code") // gone or not allowed: retrying won't help
            if (code !in 200..299) throw IOException("HTTP $code")
            val type = conn.contentType.orEmpty()
            // A music server reports a refused download as JSON/XML; never save that as a song.
            if ("json" in type || "xml" in type || type.startsWith("text/")) throw Permanent("Not audio: $type")
            val ext = item.ext ?: FileNames.extFrom(conn.getHeaderField("Content-Disposition"), type)
            val name = FileNames.forSong(item.title, item.artist, ext) ?: throw Permanent("No usable name")
            val file = File(dir, name)
            if (file.exists()) return null
            val part = File(dir, ".$name.part") // hidden from the scan until complete: no half-written songs
            try {
                part.outputStream().use { out -> conn.inputStream.use { it.copyTo(out, 64 * 1024) } }
                Files.move(part.toPath(), file.toPath())
            } finally {
                part.delete()
            }
            return file.toURI().toString()
        } finally {
            conn.disconnect()
        }
    }
}

/** Playlist files (.m3u/.m3u8) that other players read too. Only files on this PC are written: server links hold your login. */
object M3u {
    /** Returns how many songs were written as files and how many are listed only as a comment (not a file here). */
    fun write(name: String, tracks: List<Track>, to: File): Pair<Int, Int> {
        var files = 0
        val text = buildString {
            append("#EXTM3U\n#PLAYLIST:").append(name.replace('\n', ' ')).append('\n')
            for (t in tracks) {
                val path = t.id.takeIf { it.startsWith("file:") }?.let { runCatching { File(URI(it)).path }.getOrNull() }
                append("#EXTINF:").append(t.durationMs / 1000).append(',').append(t.artist).append(" - ").append(t.title).append('\n')
                if (path != null) { append(path).append('\n'); files++ } else append("# not a file on this PC\n")
            }
        }
        to.writeText(text)
        return files to tracks.size - files
    }

    /** Matches entries by file (absolute, relative to the playlist, or file:// URI), else by "Artist - Title". */
    fun read(file: File, library: List<Track>): Pair<String, List<String>> {
        val byId = library.associateBy { it.id }
        val byName = library.groupBy { Match.norm(it.artist) + "|" + Match.norm(it.title) }
        var name = file.nameWithoutExtension
        var info: String? = null
        val ids = ArrayList<String>()
        for (raw in file.readLines().take(20_000)) {
            val line = raw.trim().removePrefix("\uFEFF")
            when {
                line.startsWith("#PLAYLIST:") -> name = line.substringAfter(':').trim().ifEmpty { name }
                line.startsWith("#EXTINF:") -> info = line.substringAfter(',', "").trim()
                line == "# not a file on this PC" -> { // written by Onda for a server song or preview: found by name
                    info?.takeIf { " - " in it }?.let { byName[Match.norm(it.substringBefore(" - ")) + "|" + Match.norm(it.substringAfter(" - "))]?.firstOrNull() }?.let { ids += it.id }
                    info = null
                }
                line.isEmpty() || line.startsWith("#") -> {}
                else -> {
                    val f = runCatching { if (line.startsWith("file:")) File(URI(line)) else File(line).let { if (it.isAbsolute) it else File(file.parentFile, line) } }.getOrNull()
                    val hit = f?.let { byId[it.absoluteFile.toURI().toString()] }
                        ?: info?.takeIf { " - " in it }?.let { byName[Match.norm(it.substringBefore(" - ")) + "|" + Match.norm(it.substringAfter(" - "))]?.firstOrNull() }
                    hit?.let { ids += it.id }
                    info = null
                }
            }
        }
        return name to ids.distinct()
    }
}
