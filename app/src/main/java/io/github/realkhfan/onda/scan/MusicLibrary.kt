package io.github.realkhfan.onda.scan

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.realkhfan.onda.core.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    fun load(context: Context) {
        _folders.value = stored(context).toList()
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
            _tracks.value = LocalScanner.scan(context, trees)
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
