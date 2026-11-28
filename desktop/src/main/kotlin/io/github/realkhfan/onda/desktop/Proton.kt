package io.github.realkhfan.onda.desktop

import io.github.realkhfan.onda.core.Track
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Songs in Proton Drive folders you choose, through Proton's official command-line tool (proton-drive). You sign in with it
 * in your browser; Onda never sees your Proton password. Songs are listed by their file and folder names without
 * downloading anything, and a song is downloaded (the tool decrypts it) when it plays, then kept in [SongCache].
 */
object Proton {
    const val PREFIX = "proton:"
    const val FOLDER = "Proton Drive"
    const val ROOT = "/my-files"
    const val DOWNLOAD_PAGE = "https://proton.me/download/drive/cli/index.html"
    private val AUDIO = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "wma", "aif", "aiff", "alac", "ape", "wv", "mka", "webm")
    private val home = System.getProperty("user.home")

    /** A song in Drive: [path] to download it, [rel] (below the chosen folder) for its name. */
    data class Song(val uid: String, val path: String, val rel: String, val size: Long)

    /** An entry of a Drive folder. */
    data class Node(val uid: String, val name: String, val folder: Boolean, val size: Long)

    /** What Onda keeps: the tool's place if you chose it, your Drive folders, and the songs found in them. */
    data class Saved(val cli: String? = null, val folders: List<String> = emptyList(), val songs: List<Song> = emptyList(), val scanned: Long = 0)

    /** The tool failed; [signedOut] when it asks to sign in first. */
    class Failed(message: String, val signedOut: Boolean = false) : IOException(message)

    private val file get() = File(Store.data, "proton.json")

    fun load(): Saved = runCatching {
        val o = JSONObject(file.readText())
        val songs = o.optJSONArray("songs")?.let { a ->
            (0 until a.length()).map { a.getJSONObject(it) }.map { Song(it.getString("uid"), it.getString("path"), it.getString("rel"), it.optLong("size")) }
        }.orEmpty()
        val folders = o.optJSONArray("folders")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
        Saved(o.optString("cli").ifEmpty { null }, folders, songs, o.optLong("scanned"))
    }.getOrDefault(Saved())

    fun save(s: Saved) {
        val o = JSONObject().put("cli", s.cli ?: "").put("folders", JSONArray(s.folders)).put("scanned", s.scanned)
            .put("songs", JSONArray(s.songs.map { JSONObject().put("uid", it.uid).put("path", it.path).put("rel", it.rel).put("size", it.size) }))
        Store.data.mkdirs()
        val tmp = File(file.path + ".tmp")
        tmp.writeText(o.toString())
        java.nio.file.Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
    }

    /** The tool: the one you chose, else on your PATH or where downloads usually land. */
    fun cli(chosen: String?): File? {
        val places = listOfNotNull(chosen) + System.getenv("PATH").orEmpty().split(':').filter { it.isNotEmpty() }.map { "$it/proton-drive" } +
            listOf("$home/.local/bin/proton-drive", "$home/bin/proton-drive", "$home/Applications/proton-drive", "$home/Downloads/proton-drive")
        return places.map(::File).firstOrNull { it.isFile && it.canExecute() }
    }

    /** Runs the tool with [args] and JSON output; its answer, or [Failed] with what it said went wrong. */
    fun run(cli: File, seconds: Long, vararg args: String): String {
        val p = ProcessBuilder(listOf(cli.path) + args + "--json").redirectInput(ProcessBuilder.Redirect.from(File("/dev/null"))).start()
        val out = CompletableFuture.supplyAsync { p.inputStream.readBytes() }
        val err = CompletableFuture.supplyAsync { p.errorStream.readBytes() }
        if (!p.waitFor(seconds, TimeUnit.SECONDS)) {
            p.destroyForcibly()
            throw Failed("Proton Drive didn't answer in time.")
        }
        val text = String(out.get())
        if (p.exitValue() != 0) {
            val said = String(err.get()).lines().map { it.trim() }.lastOrNull { it.isNotEmpty() } ?: "The Proton Drive tool stopped (${p.exitValue()})."
            throw Failed(said, signedOut = "login first" in said.lowercase())
        }
        return text
    }

    /** A folder's entries, from the tool's JSON (a list of nodes; a name the tool couldn't read is left out). */
    fun parseList(json: String): List<Node> {
        val start = json.indexOf('[')
        val end = json.lastIndexOf(']')
        if (start < 0 || end < start) return emptyList()
        val a = JSONArray(json.substring(start, end + 1))
        return (0 until a.length()).mapNotNull { i ->
            val o = a.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optJSONObject("name")?.takeIf { it.optBoolean("ok") }?.optString("value")?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            if ('/' in name) return@mapNotNull null // can't be addressed by path
            Node(o.optString("uid").ifEmpty { return@mapNotNull null }, name, o.optString("type") == "folder", o.optJSONObject("activeRevision")?.optLong("claimedSize") ?: 0)
        }
    }

    fun list(cli: File, path: String): List<Node> = parseList(run(cli, 120, "filesystem", "list", path))

    fun isAudio(name: String) = name.substringAfterLast('.', "").lowercase() in AUDIO

    /** Every song below [folders] (folders inside folders too), by name; a folder that can't be read is skipped. */
    fun scan(cli: File, folders: List<String>, limit: Int = 50_000): List<Song> {
        val out = ArrayList<Song>()
        fun walk(root: String, path: String, depth: Int) {
            if (depth > 12 || out.size >= limit) return
            val nodes = runCatching { list(cli, path) }.getOrElse { if (it is Failed && it.signedOut || path == root) throw it else return }
            for (n in nodes.sortedBy { it.name.lowercase() }) {
                val p = "$path/${n.name}"
                if (n.folder) walk(root, p, depth + 1)
                else if (isAudio(n.name)) out += Song(n.uid, p, p.removePrefix("$root/"), n.size)
            }
        }
        for (f in folders) walk(f, f, 0)
        return out.distinctBy { it.uid }
    }

    /**
     * A song's details from its file and folder names: "Artist - Title.mp3" or "01 Title.mp3" in "Artist/Album/", a leading
     * track number dropped. No download needed; there's no length until it plays.
     */
    fun track(s: Song): Track {
        val parts = s.rel.split('/').filter { it.isNotEmpty() }
        val file = parts.lastOrNull() ?: s.rel
        val dirs = parts.dropLast(1)
        var base = file.substringBeforeLast('.').trim()
        val number = Regex("^(\\d{1,3})(?:\\s*[-._]\\s*|\\s+)(?=\\S)").find(base)
        val trackNo = number?.groupValues?.get(1)?.toIntOrNull() ?: 0
        if (number != null) base = base.substring(number.range.last + 1)
        val dash = base.indexOf(" - ")
        val artist = (if (dash > 0) base.substring(0, dash).trim() else null)?.ifEmpty { null }
            ?: dirs.getOrNull(dirs.size - 2) ?: dirs.lastOrNull() ?: "Unknown artist"
        val title = (if (dash > 0) base.substring(dash + 3) else base).trim().ifEmpty { file }
        val album = if (dirs.size >= 2 || (dirs.size == 1 && dash > 0)) dirs.last() else ""
        return Track(PREFIX + s.uid, PREFIX + s.uid, title, artist, album, artist, trackNo, 0L, FOLDER, null, file)
    }

    private val fetching = ConcurrentHashMap<String, Any>()

    /** The song's file here: kept from before, or downloaded now (one download per song at a time) and kept. */
    fun fetch(cli: File, s: Song): File {
        val id = PREFIX + s.uid
        SongCache.get(id)?.let { return it }
        synchronized(fetching.computeIfAbsent(s.uid) { Any() }) {
            SongCache.get(id)?.let { return it }
            SongCache.dir.mkdirs()
            // What a download cut short by closing Onda left behind.
            SongCache.dir.listFiles { f -> f.isDirectory && f.name.startsWith(".proton") && f.lastModified() < System.currentTimeMillis() - 86_400_000L }
                ?.forEach { it.deleteRecursively() }
            val tmp = java.nio.file.Files.createTempDirectory(SongCache.dir.toPath(), ".proton").toFile()
            try {
                run(cli, 600, "filesystem", "download", s.path, tmp.path, "--file-conflict-strategy", "remove")
                val got = tmp.walkTopDown().firstOrNull { it.isFile && it.length() > 0 } ?: throw Failed("Proton Drive sent nothing for ${s.rel}.")
                return SongCache.put(id, got)
            } finally {
                tmp.deleteRecursively()
            }
        }
    }
}
