package io.github.realkhfan.onda.data

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.realkhfan.onda.scan.MusicLibrary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

/**
 * "Copy new songs to a folder", as on the desktop: every 30 minutes, songs that are new in your music folders are copied
 * into a folder you picked, in the same folders below it. Each song once; nothing there is deleted or replaced.
 */
object SongCopy {
    /** How far the copy is, or what the last one did. */
    var note by mutableStateOf<String?>(null)
        private set
    private val running = Mutex()

    private val AUDIO = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav")

    private fun copiedFile(ctx: Context) = File(ctx.filesDir, "copied.json")
    private fun readCopied(ctx: Context): MutableSet<String> = runCatching {
        JSONArray(copiedFile(ctx).readText()).let { a -> (0 until a.length()).mapTo(HashSet()) { a.getString(it) } }
    }.getOrDefault(HashSet())
    private fun writeCopied(ctx: Context, ids: Set<String>) = runCatching {
        File(ctx.filesDir, "copied.json.tmp").apply { writeText(JSONArray(ids).toString()) }.renameTo(copiedFile(ctx))
    }

    /** A song file in your music folders, by where it is below its folder ("Artist/Album/song.flac"). */
    private class Song(val uri: Uri, val path: String)

    /** Your music folders' audio files (names only, no tags: quick enough for every 30 minutes). */
    private fun songs(ctx: Context): List<Song> {
        val out = ArrayList<Song>()
        for (t in MusicLibrary.folders.value) {
            val tree = Uri.parse(t)
            val root = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: continue
            fun walk(docId: String, prefix: String) {
                val kids = DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
                runCatching {
                    ctx.contentResolver.query(kids, arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE), null, null, null)
                }.getOrNull()?.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getString(0); val name = c.getString(1) ?: continue
                        if (c.getString(2) == Document.MIME_TYPE_DIR) walk(id, "$prefix$name/")
                        else if (name.substringAfterLast('.', "").lowercase() in AUDIO) out += Song(DocumentsContract.buildDocumentUriUsingTree(tree, id), "$prefix$name")
                    }
                }
            }
            walk(root, "")
        }
        return out
    }

    /**
     * [tree]: the folder to copy into (null: off). [everything]: also the songs already here (those not there yet);
     * otherwise only songs added from now on. Returns why it can't, or null.
     */
    suspend fun setFolder(ctx: Context, tree: Uri?, everything: Boolean): String? = withContext(Dispatchers.IO) {
        if (tree == null) { Prefs.setCopyTree(ctx, null); note = null; SongCopyJob.cancel(ctx); return@withContext null }
        // Inside a music folder, every copy would be a new song too.
        val target = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: return@withContext "Can't use that folder."
        if (MusicLibrary.folders.value.any { m ->
                val music = runCatching { DocumentsContract.getTreeDocumentId(Uri.parse(m)) }.getOrNull()
                Uri.parse(m).authority == tree.authority && music != null && (target == music || target.startsWith("$music/"))
            }) return@withContext "Pick a folder outside your music folders, or every song would show up twice."
        runCatching { ctx.contentResolver.takePersistableUriPermission(tree, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        writeCopied(ctx, if (everything) emptySet() else songs(ctx).mapTo(HashSet()) { it.uri.toString() })
        Prefs.setCopyTree(ctx, tree.toString())
        note = null
        SongCopyJob.schedule(ctx)
        null
    }

    /** Copies the songs added since last time. Safe to call from the screen and the 30-minute job at once (one runs). */
    suspend fun run(ctx: Context) = withContext(Dispatchers.IO) {
        val tree = Prefs.copyTree(ctx)?.let(Uri::parse) ?: return@withContext
        if (!running.tryLock()) return@withContext
        try {
            val cr = ctx.contentResolver
            val root = runCatching { DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)) }.getOrNull()
            if (root == null || runCatching { cr.query(root, arrayOf(Document.COLUMN_DOCUMENT_ID), null, null, null)?.use { it.count } }.getOrNull() == null) {
                note = "The folder isn't there right now (a card or drive not connected?). Onda tries again in 30 minutes."
                return@withContext
            }
            val done = readCopied(ctx)
            val todo = songs(ctx).filter { it.uri.toString() !in done }
            var copied = 0; var failed = 0
            for ((i, s) in todo.withIndex()) {
                if (todo.size > 3) note = "Copying new songs: ${i + 1} of ${todo.size}…"
                val r = runCatching { copyOne(ctx, s, tree, root) }
                if (r.isSuccess) { done += s.uri.toString(); if (r.getOrThrow()) copied++ } else failed++
                if (i % 25 == 24) writeCopied(ctx, done)
            }
            writeCopied(ctx, done)
            note = "Checked at ${java.time.LocalTime.now().withSecond(0).withNano(0)}: " +
                (if (copied == 0) "no new songs" else if (copied == 1) "1 new song copied" else "$copied new songs copied") +
                (if (failed > 0) " · $failed couldn't be copied (Onda tries again in 30 minutes)" else "")
        } finally {
            running.unlock()
        }
    }

    /**
     * Songs renamed in Onda: not copied again as new ones, and their copy in the folder gets the same name, in place (your
     * backup sees a rename, not a new song).
     */
    suspend fun renamed(ctx: Context, moved: Map<String, String>) = withContext(Dispatchers.IO) {
        if (moved.isEmpty()) return@withContext
        val tree = Prefs.copyTree(ctx)?.let(Uri::parse) ?: return@withContext
        running.withLock {
            val done = readCopied(ctx)
            if (done.any { it in moved }) writeCopied(ctx, done.mapTo(HashSet()) { moved[it] ?: it })
            val root = runCatching { DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)) }.getOrNull() ?: return@withLock
            for ((a, b) in moved) runCatching {
                val parts = pathOf(Uri.parse(a))?.split('/') ?: return@runCatching
                val name = pathOf(Uri.parse(b))?.substringAfterLast('/') ?: return@runCatching
                var dir = root
                for (folder in parts.dropLast(1)) dir = child(ctx, tree, dir, folder) ?: return@runCatching
                val copy = child(ctx, tree, dir, parts.last()) ?: return@runCatching
                if (child(ctx, tree, dir, name) == null) DocumentsContract.renameDocument(ctx.contentResolver, copy, name)
            }
        }
    }

    /** Where a song is below its music folder ("Artist/Album/song.flac", as in [songs]); null when that can't be told. */
    private fun pathOf(uri: Uri): String? {
        val id = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return null
        for (t in MusicLibrary.folders.value) {
            val tree = Uri.parse(t)
            if (tree.authority != uri.authority) continue
            val root = runCatching { DocumentsContract.getTreeDocumentId(tree) }.getOrNull() ?: continue
            val prefix = if (root.endsWith(":")) root else "$root/" // the whole card ("primary:") or a folder on it
            if (id.startsWith(prefix)) return id.removePrefix(prefix)
        }
        return null
    }

    /** True when copied; false when it's there already. */
    private fun copyOne(ctx: Context, s: Song, tree: Uri, root: Uri): Boolean {
        val cr = ctx.contentResolver
        var dir = root
        val parts = s.path.split('/')
        for (folder in parts.dropLast(1)) dir = child(ctx, tree, dir, folder) ?: DocumentsContract.createDocument(cr, dir, Document.MIME_TYPE_DIR, folder)
            ?: throw java.io.IOException("Can't make $folder")
        val name = parts.last()
        if (child(ctx, tree, dir, name) != null) return false
        val mime = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.').lowercase()) ?: "application/octet-stream"
        fun write(doc: Uri) = try {
            cr.openInputStream(s.uri)!!.use { input -> cr.openOutputStream(doc, "w")!!.use { input.copyTo(it, 64 * 1024) } }
        } catch (e: Exception) {
            runCatching { DocumentsContract.deleteDocument(cr, doc) } // no half-copied songs
            throw e
        }
        // Written under a hidden name, then renamed: a song never shows up half-copied.
        val part = DocumentsContract.createDocument(cr, dir, mime, ".$name.part") ?: throw java.io.IOException("Can't create $name")
        write(part)
        if (runCatching { DocumentsContract.renameDocument(cr, part, name) }.getOrNull() == null) {
            // This folder can't rename: the song goes straight in under its name.
            runCatching { DocumentsContract.deleteDocument(cr, part) }
            write(DocumentsContract.createDocument(cr, dir, mime, name) ?: throw java.io.IOException("Can't create $name"))
        }
        return true
    }

    private fun child(ctx: Context, tree: Uri, dir: Uri, name: String): Uri? {
        val kids = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(dir))
        return ctx.contentResolver.query(kids, arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            while (c.moveToNext()) if (c.getString(1) == name) return@use DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
            null
        }
    }
}

/** Runs [SongCopy] every 30 minutes (Android's JobScheduler, while the battery isn't low). */
class SongCopyJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            try {
                MusicLibrary.load(applicationContext)
                SongCopy.run(applicationContext)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        scope.cancel()
        return true
    }

    companion object {
        private const val ID = 4202

        fun schedule(context: Context) {
            val js = context.getSystemService(JobScheduler::class.java) ?: return
            if (js.getPendingJob(ID) != null) return
            js.schedule(
                JobInfo.Builder(ID, ComponentName(context, SongCopyJob::class.java))
                    .setPeriodic(30 * 60 * 1000L)
                    .setRequiresBatteryNotLow(true)
                    .setPersisted(true)
                    .build(),
            )
        }

        fun cancel(context: Context) {
            context.getSystemService(JobScheduler::class.java)?.cancel(ID)
        }
    }
}
