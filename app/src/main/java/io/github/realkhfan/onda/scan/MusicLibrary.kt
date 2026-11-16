package io.github.realkhfan.onda.scan

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.realkhfan.onda.core.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Holds the scanned local tracks and the set of folders the user picked (persisted). */
object MusicLibrary {

    private const val PREFS = "onda"
    private const val KEY_FOLDERS = "folders"

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _folders = MutableStateFlow<List<String>>(emptyList())
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()

    /** When each song came (its file's date the first time Onda saw it, so a backup rewriting files doesn't change it). */
    private val _addedAt = MutableStateFlow<Map<String, Long>>(emptyMap())
    val addedAt: StateFlow<Map<String, Long>> = _addedAt.asStateFlow()

    fun load(context: Context) {
        _folders.value = stored(context).toList()
        _addedAt.value = runCatching {
            org.json.JSONObject(java.io.File(context.filesDir, "added.json").readText()).let { o -> o.keys().asSequence().associateWith { o.getLong(it) } }
        }.getOrDefault(emptyMap())
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

    suspend fun rescan(context: Context) {
        _scanning.value = true
        try {
            val trees = stored(context).map { Uri.parse(it) }
            val modified = HashMap<String, Long>()
            _tracks.value = LocalScanner.scan(context, trees, modified)
            val now = System.currentTimeMillis()
            val kept = _addedAt.value
            _addedAt.value = _tracks.value.associate { t -> t.id to (kept[t.id] ?: modified[t.id]?.takeIf { it > 0 } ?: now) }
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    java.io.File(context.filesDir, "added.json.tmp").apply { writeText(org.json.JSONObject(_addedAt.value).toString()) }
                        .renameTo(java.io.File(context.filesDir, "added.json"))
                }
            }
            // Edits on songs whose file moved (another folder picked, files renamed) follow them by file name.
            io.github.realkhfan.onda.data.Overrides.relink(_tracks.value)
        } finally {
            _scanning.value = false
        }
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
