package io.github.realkhfan.onda.scan

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.realkhfan.onda.core.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/** Holds the scanned local tracks and the set of folders the user picked (persisted). */
object MusicLibrary {

    private const val PREFS = "onda"
    private const val KEY_FOLDERS = "folders"

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    /** False from start until the saved song list shows (true when nothing was loaded, e.g. in tests). */
    private val _loaded = MutableStateFlow(true)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    /** When each song came (its file's date the first time Onda saw it, so a backup rewriting files doesn't change it). */
    private val _addedAt = MutableStateFlow<Map<String, Long>>(emptyMap())
    val addedAt: StateFlow<Map<String, Long>> = _addedAt.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val lock = Mutex() // one scan at a time
    /** The last scan (uri → file date, song): shown at start, and a rescan only reads files that changed. */
    private var known: Map<String, Pair<Long, Track>> = emptyMap()

    fun load(context: Context) {
        _folders.value = stored(context).toList()
        _addedAt.value = runCatching {
            org.json.JSONObject(File(context.filesDir, "added.json").readText()).let { o -> o.keys().asSequence().associateWith { o.getLong(it) } }
        }.getOrDefault(emptyMap())
        val c = context.applicationContext
        _loaded.value = false
        scope.launch { lock.withLock { try { showSaved(c) } finally { _loaded.value = true } } } // first in line, so a scan started now waits for it
    }

    private suspend fun showSaved(context: Context) {
        known = withContext(Dispatchers.IO) {
            runCatching {
                val arr = JSONArray(File(context.filesDir, "library.json").readText())
                (0 until arr.length()).associate { i -> arr.getJSONObject(i).let { it.getString("uri") to (it.getLong("mtime") to Track.fromJson(it)) } }
            }.getOrDefault(emptyMap())
        }
        val folders = _folders.value
        if (_tracks.value.isEmpty()) _tracks.value = known.values.map { it.second }.filter { t -> folders.any { t.uri.startsWith(prefixOf(it)) } }
    }

    /** First-seen dates from your PC (by song id here): the earlier date wins, so both show the same "Recently added". */
    fun adoptAdded(context: Context, dates: Map<String, Long>) {
        val now = _addedAt.value
        val earlier = dates.filter { (id, t) -> t > 0 && now[id]?.let { t < it } == true }
        if (earlier.isEmpty()) return
        _addedAt.value = now + earlier
        val text = org.json.JSONObject(_addedAt.value).toString()
        scope.launch(Dispatchers.IO) { save(context.applicationContext, "added.json", text) }
    }

    /** A scan that leaving the app doesn't cancel. */
    fun rescanSoon(context: Context) {
        val c = context.applicationContext
        scope.launch { rescan(c) }
    }

    fun addFolder(context: Context, uri: Uri) {
        // Read + write (the folder picker grants both); write is needed only for "Rename files".
        // Re-adding a folder that was granted read-only upgrades it.
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (_: Throwable) {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Throwable) {
            }
        }
        val set = stored(context).toMutableSet()
        set.add(uri.toString())
        save(context, set)
        _folders.value = set.toList()
    }

    fun removeFolder(context: Context, uriString: String) {
        val set = stored(context).toMutableSet()
        set.remove(uriString)
        save(context, set)
        _folders.value = set.toList()
        for (flags in listOf(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )) {
            try {
                context.contentResolver.releasePersistableUriPermission(Uri.parse(uriString), flags)
                break
            } catch (_: Throwable) {
            }
        }
        _tracks.value = _tracks.value.filterNot { it.uri.startsWith(prefixOf(uriString)) }
    }

    suspend fun rescan(context: Context): Unit = lock.withLock {
        _scanning.value = true
        try {
            val trees = stored(context).map { Uri.parse(it) }
            val modified = HashMap<String, Long>()
            _tracks.value = LocalScanner.scan(context, trees, modified, known)
            known = _tracks.value.mapNotNull { t -> modified[t.uri]?.let { t.uri to (it to t) } }.toMap()
            val now = System.currentTimeMillis()
            val kept = _addedAt.value
            _addedAt.value = _tracks.value.associate { t -> t.id to (kept[t.id] ?: modified[t.id]?.takeIf { it > 0 } ?: now) }
            val saved = known
            withContext(Dispatchers.IO) {
                save(context, "added.json", org.json.JSONObject(_addedAt.value).toString())
                save(context, "library.json", JSONArray(saved.values.map { (date, t) -> t.toJson().put("mtime", date) }).toString())
            }
            // Edits on songs whose file moved (another folder picked, files renamed) follow them by file name.
            io.github.realkhfan.onda.data.Overrides.relink(_tracks.value)
        } finally {
            _scanning.value = false
        }
    }

    private fun save(context: Context, name: String, text: String) = runCatching {
        File(context.filesDir, "$name.tmp").apply { writeText(text) }.renameTo(File(context.filesDir, name))
    }

    private fun prefixOf(treeUriString: String): String = treeUriString

    private fun stored(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_FOLDERS, emptySet()) ?: emptySet()

    private fun save(context: Context, set: Set<String>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_FOLDERS, set)
            .apply()
    }
}
