package io.github.realkhfan.onda.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import io.github.realkhfan.onda.core.FileNames
import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.scan.MusicLibrary
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

    /**
     * [at]: when each rename counts as made (now for yours; the PC's date when matching its rename). Recorded, so the PC
     * names its copy the same; the copy in "Copy new songs to a folder" follows too.
     */
    suspend fun apply(context: Context, plan: List<FileNames.Rename>, at: (FileNames.Rename) -> Long = { System.currentTimeMillis() }): Result {
        val raw = MusicLibrary.tracks.value.associateBy { it.id } // own tags, before a rescan gives the files their new ids
        val moved = LinkedHashMap<String, String>()
        val undo = ArrayList<FileNames.Rename>()
        val failed = ArrayList<String>()
        withContext(Dispatchers.IO) {
            for (r in plan) {
                if (taken(context, Uri.parse(r.id), r.to)) { failed += r.from; continue }
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
        Overrides.knowNames(plan.filter { it.id in moved }.mapNotNull { r -> raw[r.id]?.let { LinkSync.nameOf(it, r.to, at(r)) } }.toMap())
        PlaylistStore.migrateIds(moved)
        Overrides.migrateIds(moved)
        PlayLog.migrateIds(moved)
        SongCopy.renamed(context, moved)
        return Result(moved.size, failed, undo)
    }

    /**
     * Android names a file renamed onto a taken name "name (1)", a second copy of the song. Taken means the song is there under
     * that name already (your backup brought it), so the old file is left for the backup to remove.
     */
    private fun taken(context: Context, doc: Uri, name: String): Boolean = runCatching {
        val id = sibling(DocumentsContract.getDocumentId(doc), name) ?: return false
        context.contentResolver.query(DocumentsContract.buildDocumentUriUsingTree(doc, id), arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)
            ?.use { it.moveToFirst() } == true
    }.getOrDefault(false)

    /** The file called [name] next to document [id], where ids are paths ("primary:Music/a.mp3", the phone's storage); else null. */
    internal fun sibling(id: String, name: String): String? =
        maxOf(id.lastIndexOf('/'), id.lastIndexOf(':')).takeIf { it >= 0 }?.let { id.substring(0, it + 1) + name }
}
