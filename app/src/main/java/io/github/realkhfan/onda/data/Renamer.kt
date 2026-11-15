package io.github.realkhfan.onda.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import io.github.realkhfan.onda.core.FileNames
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Renames music files through the folder grant (Storage Access Framework). Only names change, never the audio
 * or its tags. A rename gives the file a new id, so playlists, likes, matches and history are moved along.
 */
object Renamer {

    class Result(val renamed: Int, val failed: List<String>, val undo: List<FileNames.Rename>)

    /** Folders granted read-only (added before 0.6.0) can't be renamed in until they're re-added once. */
    fun canWrite(context: Context, treeUri: String): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri.toString() == treeUri && it.isWritePermission }

    suspend fun apply(context: Context, plan: List<FileNames.Rename>): Result {
        val moved = LinkedHashMap<String, String>()
        val undo = ArrayList<FileNames.Rename>()
        val failed = ArrayList<String>()
        withContext(Dispatchers.IO) {
            for (r in plan) {
                val newUri = try {
                    DocumentsContract.renameDocument(context.contentResolver, Uri.parse(r.id), r.to)
                } catch (_: Exception) {
                    null // missing file, no write access, or a name the folder won't take: left as it was
                }
                if (newUri == null) {
                    failed += r.from
                } else {
                    moved[r.id] = newUri.toString()
                    undo += FileNames.Rename(newUri.toString(), r.to, r.from)
                }
            }
        }
        PlaylistStore.migrateIds(moved)
        Overrides.migrateIds(moved)
        PlayLog.migrateIds(moved)
        return Result(moved.size, failed, undo)
    }
}
