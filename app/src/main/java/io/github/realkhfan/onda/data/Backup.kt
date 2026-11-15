package io.github.realkhfan.onda.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** One ZIP with your playlists, likes, history, matches, collection and settings; restorable on any phone. */
object Backup {
    // Not the server login (a credential) and not music folders (their access belongs to this phone).
    private val FILES = listOf("playlists.json", "overrides.json", "collection.json", "plays.json", "releases.json", "artist_ids.json", "mixes.json")
    private const val SETTINGS = "settings.json"
    private const val MAX_FILE = 32 shl 20
    private const val KEEP = 5

    fun name(): String = "onda-backup-${LocalDate.now()}.zip"

    fun write(ctx: Context, out: OutputStream) {
        ZipOutputStream(out).use { z ->
            FILES.map { File(ctx.filesDir, it) }.filter { it.isFile }.forEach { f ->
                z.putNextEntry(ZipEntry(f.name)); f.inputStream().use { it.copyTo(z) }; z.closeEntry()
            }
            z.putNextEntry(ZipEntry(SETTINGS)); z.write(Prefs.export(ctx).toString().toByteArray()); z.closeEntry()
        }
    }

    /** Replaces Onda's data with the backup's. Keeps a copy of the current data first. Returns how many parts came back. */
    fun restore(ctx: Context, input: InputStream): Int {
        val found = LinkedHashMap<String, String>()
        ZipInputStream(input).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                val n = e.name
                if (e.isDirectory || (n !in FILES && n != SETTINGS)) continue // anything else in the ZIP is ignored
                val text = readText(z) ?: continue
                if (runCatching { JSONTokener(text).nextValue() is JSONObject || JSONTokener(text).nextValue() is JSONArray }.getOrDefault(false)) {
                    found[n] = text
                }
            }
        }
        if (found.isEmpty()) throw IllegalArgumentException("Not an Onda backup")
        File(ctx.filesDir, "before-restore.zip").outputStream().use { write(ctx, it) } // undo, should the backup be the wrong one
        found.forEach { (n, text) ->
            if (n == SETTINGS) Prefs.import(ctx, JSONObject(text))
            else File(ctx.filesDir, "$n.tmp").apply { writeText(text) }.renameTo(File(ctx.filesDir, n))
        }
        PlaylistStore.load(ctx); Overrides.load(ctx); UserCollection.load(ctx); PlayLog.load(ctx)
        Releases.load(ctx); ArtistIds.load(ctx); Mixes.load(ctx); Server.load(ctx)
        return found.size
    }

    /** Daily, into the folder chosen in Settings; keeps the newest [KEEP]. */
    fun autoIfDue(ctx: Context) {
        val tree = Prefs.backupTree(ctx)?.let(Uri::parse) ?: return
        if (System.currentTimeMillis() - Prefs.lastBackup(ctx) < 20 * 3_600_000L) return
        runCatching {
            val cr = ctx.contentResolver
            val root = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val kids = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val old = ArrayList<Pair<String, String>>() // name, id
            cr.query(kids, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { c ->
                while (c.moveToNext()) if (c.getString(0).startsWith("onda-backup-")) old += c.getString(0) to c.getString(1)
            }
            val today = name()
            val doc = old.firstOrNull { it.first == today }?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it.second) }
                ?: DocumentsContract.createDocument(cr, root, "application/zip", today) ?: return
            cr.openOutputStream(doc, "wt")?.use { write(ctx, it) } ?: return
            Prefs.setLastBackup(ctx, System.currentTimeMillis())
            old.filter { it.first != today }.sortedByDescending { it.first }.drop(KEEP - 1).forEach { (_, id) ->
                runCatching { DocumentsContract.deleteDocument(cr, DocumentsContract.buildDocumentUriUsingTree(tree, id)) }
            }
        }
    }

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
