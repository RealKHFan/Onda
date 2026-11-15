package io.github.realkhfan.onda.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.SpotifyImport
import io.github.realkhfan.onda.core.SpotifyImport.Song
import io.github.realkhfan.onda.core.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Imports a Spotify data export (ZIP or its JSON files) or Exportify CSVs into playlists, likes and history. */
object SpotifyImporter {

    data class State(
        val running: Boolean = false,
        val done: Int = 0,
        val total: Int = 0,
        val result: String? = null,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private const val MAX_FILE = 64 shl 20 // a big extended-history file is ~15 MB; anything far bigger isn't Spotify's
    private const val HISTORY_DAYS = 90L

    private class Found(
        val playlists: MutableList<Pair<String, List<Song>>> = ArrayList(),
        val liked: MutableList<Song> = ArrayList(),
        val plays: MutableList<Discovery.Play> = ArrayList(),
    )

    /** [library]: your files and server songs (owned songs play in full). */
    fun start(context: Context, uris: List<Uri>, library: List<Track>) {
        if (_state.value.running || uris.isEmpty()) return
        val app = context.applicationContext
        _state.value = State(running = true)
        scope.launch {
            try {
                val found = withContext(Dispatchers.IO) { read(app, uris) }
                if (found.playlists.isEmpty() && found.liked.isEmpty() && found.plays.isEmpty()) {
                    _state.value = State(error = "No Spotify playlists, liked songs or listening history in that file.")
                    return@launch
                }
                importAll(app, found, library)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = State(error = "Couldn't read that file (${e.javaClass.simpleName}).")
            }
        }
    }

    private fun read(context: Context, uris: List<Uri>): Found {
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
        for (uri in uris) {
            val name = displayName(context, uri) ?: uri.lastPathSegment.orEmpty()
            context.contentResolver.openInputStream(uri)?.use { input ->
                if (name.endsWith(".zip", true)) {
                    ZipInputStream(input).use { zip ->
                        while (true) {
                            val e = zip.nextEntry ?: break
                            if (!e.isDirectory) take(e.name) { readText(zip) }
                        }
                    }
                } else {
                    take(name) { readText(input) }
                }
            }
        }
        return found
    }

    private suspend fun importAll(context: Context, found: Found, library: List<Track>) {
        val byTitle = library.groupBy { Match.norm(it.title) }
        fun owned(s: Song): Track? = (byTitle[Match.norm(s.title)].orEmpty() + byTitle[Match.norm(s.title.substringBefore(" - "))].orEmpty())
            .firstOrNull { SpotifyImport.matches(s, it.title, it.artist) }

        val songs = (found.playlists.flatMap { it.second } + found.liked).distinctBy { keyOf(it) }
        val cache = loadCache(context)
        val todo = songs.filter { owned(it) == null && !it.local && !cache.has(keyOf(it)) }
        _state.update { it.copy(total = todo.size) }

        var problem: String? = null
        var failuresInARow = 0
        for ((i, s) in todo.withIndex()) {
            _state.update { it.copy(done = i) }
            val hit = try {
                Deezer.search("${s.artist} ${s.title.substringBefore(" - ")}".trim(), limit = 10)
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
            cache.put(keyOf(s), hit?.toJson() ?: JSONObject.NULL)
            if (i % 25 == 24) saveCache(context, cache)
            delay(120) // Deezer allows about 50 requests in 5 seconds
        }
        saveCache(context, cache)

        fun remote(s: Song): RemoteTrack? = cache.optJSONObject(keyOf(s))?.let { remoteFromJson(it) }
        var inLibrary = 0
        var previews = 0
        var missing = 0
        for ((name, list) in found.playlists) {
            val ids = ArrayList<String>()
            val catalog = ArrayList<RemoteTrack>()
            for (s in list) {
                val own = owned(s)
                val r = if (own == null) remote(s) else null
                when {
                    own != null -> { ids += own.id; inLibrary++ }
                    r != null -> { ids += r.key; catalog += r; previews++ }
                    else -> missing++
                }
            }
            if (ids.isNotEmpty()) PlaylistStore.importInto(name, ids, catalog)
        }
        val likedOwned = found.liked.mapNotNull { owned(it)?.id }
        val likedWant = found.liked.filter { owned(it) == null }.mapNotNull { remote(it) }
        PlaylistStore.addLikes(likedOwned)
        PlaylistStore.addWant(likedWant)
        PlayLog.import(found.plays)

        val parts = listOfNotNull(
            found.playlists.size.takeIf { it > 0 }?.let { "$it playlists: $inLibrary songs in your library, $previews as previews, $missing not found" },
            found.liked.size.takeIf { it > 0 }?.let { "${likedOwned.size} liked songs added to Liked Songs, ${likedWant.size} to Want to listen" },
            found.plays.size.takeIf { it > 0 }?.let { "${found.plays.size} plays from the last $HISTORY_DAYS days for your mixes" },
        )
        _state.value = State(result = parts.joinToString(".\n") + ".", error = problem)
    }

    private fun keyOf(s: Song) = Match.norm(s.artist) + "|" + Match.norm(s.title)

    private fun displayName(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

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

    /* Found matches, kept between runs (artist|title -> Deezer song, or null when Deezer has none). */

    private fun cacheFile(context: Context) = File(context.filesDir, "spotify_matches.json")

    private fun loadCache(context: Context): JSONObject =
        runCatching { JSONObject(cacheFile(context).readText()) }.getOrElse { JSONObject() }

    private fun saveCache(context: Context, cache: JSONObject) {
        runCatching {
            val f = cacheFile(context)
            val tmp = File(f.path + ".tmp")
            tmp.writeText(cache.toString())
            tmp.renameTo(f)
        }
    }
}
