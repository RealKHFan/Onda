package io.github.realkhfan.onda.data

import kotlinx.coroutines.delay
import java.io.File
import org.json.JSONObject
import org.json.JSONArray
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import io.github.realkhfan.onda.MainActivity
import io.github.realkhfan.onda.R
import io.github.realkhfan.onda.core.FileNames
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.scan.MusicLibrary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Saves songs as music files in "Onda downloads" inside your first writable music folder, so they join the library.
 * Only from sources that allow it: your own server, and free catalogs where [RemoteTrack.canDownload].
 */
object Downloads {

    /** [replaces]: the server song's id, so playlists and likes move to the file once it's saved. */
    data class Item(val title: String, val artist: String, val url: String, val ext: String?, val replaces: String? = null)

    data class State(
        val running: Boolean = false, val done: Int = 0, val total: Int = 0, val failed: Int = 0, val current: String? = null,
        /** Stopped without a connection; the rest continues the next time Onda opens. */
        val paused: Boolean = false,
    )

    /** A failure that retrying can't fix (not audio, no usable name, the file is gone). */
    private class Permanent(message: String) : IOException(message)

    const val FOLDER = "Onda downloads"
    private const val CHANNEL = "downloads"
    private const val NOTE_ID = 4301

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val queue = ArrayDeque<Item>() // touched on the main thread only
    private var pendingFile: File? = null

    /** Unfinished downloads are kept on disk, so a closed app, a lost connection or Android's daily limit loses nothing. */
    private fun persist() {
        val f = pendingFile ?: return
        runCatching {
            val arr = JSONArray(queue.map {
                JSONObject().put("t", it.title).put("a", it.artist).put("u", it.url).put("e", it.ext).put("r", it.replaces)
            })
            File(f.path + ".tmp").apply { writeText(arr.toString()) }.renameTo(f)
        }
    }

    /** Called when Onda opens: continues downloads that didn't finish (quietly; a reason not to is just kept for later). */
    fun resume(context: Context) {
        pendingFile = File(context.filesDir, "downloads.json")
        if (_state.value.running) return
        val saved = runCatching {
            val arr = JSONArray(pendingFile!!.readText())
            (0 until arr.length()).map { i ->
                arr.getJSONObject(i).let { Item(it.getString("t"), it.getString("a"), it.getString("u"), it.optString("e").ifBlank { null }, it.optString("r").ifBlank { null }) }
            }
        }.getOrDefault(emptyList())
        if (saved.isNotEmpty()) start(context, saved)
    }

    fun forTrack(t: Track, cfg: Subsonic.Config?): Item? =
        if (cfg != null && t.id.startsWith(Server.PREFIX)) {
            Item(t.title, t.artist, Subsonic.downloadUrl(cfg, t.id.removePrefix(Server.PREFIX)), null, replaces = t.id)
        } else null

    fun forRemote(r: RemoteTrack): Item? = if (!r.canDownload) null else FreeCatalogs.stream(r)?.let { url ->
        Item(r.title, r.artist, url, if (r.ref.contains('/')) r.ref.substringAfterLast('.', "mp3") else "mp3")
    }

    /** The first music folder Onda may write to (granted since 0.6.0), or null. */
    fun targetFolder(context: Context): String? = MusicLibrary.folders.value.firstOrNull { Renamer.canWrite(context, it) }

    /** Starts (or extends) the downloads; returns why not when it can't. */
    fun start(context: Context, items: List<Item>): String? {
        if (items.isEmpty()) return "Nothing here can be downloaded."
        if (targetFolder(context) == null) return "Add a music folder first (Settings → Music folders), so downloads have a place to go."
        val cm = context.getSystemService(ConnectivityManager::class.java)
        if (!Prefs.mobileDownloads(context) && cm.isActiveNetworkMetered) {
            return "You're on mobile data. Downloads wait for Wi-Fi; allow mobile data in Settings → Downloads."
        }
        if (pendingFile == null) pendingFile = File(context.filesDir, "downloads.json")
        val new = items.filter { i -> queue.none { it.url == i.url } }.distinctBy { it.url }
        if (new.isEmpty() && _state.value.running) return null
        val fresh = !_state.value.running
        queue.addAll(new)
        persist()
        _state.value = if (fresh) State(running = true, total = queue.size) else _state.value.let { it.copy(total = it.total + new.size) }
        if (fresh) context.startForegroundService(Intent(context, DownloadService::class.java))
        return null
    }

    internal suspend fun run(service: Service) {
        val ctx = service.applicationContext
        val moved = HashMap<String, String>()
        try {
            val tree = targetFolder(ctx)?.let(Uri::parse) ?: throw IOException("No writable music folder")
            val dir = withContext(Dispatchers.IO) { downloadDir(ctx, tree) }
            val existing = withContext(Dispatchers.IO) { names(ctx, tree, dir) }
            outer@ while (queue.isNotEmpty()) {
                val item = queue.first()
                _state.value = _state.value.copy(current = item.title)
                notify(ctx, progress = true)
                var attempt = 0
                while (true) {
                    try {
                        val saved = withContext(Dispatchers.IO) { save(ctx, tree, dir, item, existing) }
                        if (saved != null && item.replaces != null) moved[item.replaces] = saved
                        break
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (e !is Permanent && !online(ctx)) {
                            _state.value = _state.value.copy(paused = true) // keep it and everything after it
                            break@outer
                        }
                        if (e is Permanent || ++attempt >= 3) {
                            _state.value = _state.value.copy(failed = _state.value.failed + 1)
                            break
                        }
                        delay(attempt * 5_000L) // a short hiccup: try again
                    }
                }
                queue.removeFirst()
                persist()
                _state.value = _state.value.copy(done = _state.value.done + 1)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            _state.value = _state.value.let { it.copy(failed = it.failed + it.total - it.done, done = it.total) } // no folder: none could go
            queue.clear()
            persist()
        } finally {
            // Whatever is left (paused, cancelled, Android's time limit) stays on disk for next time.
            queue.clear()
            _state.value = _state.value.copy(running = false, current = null)
            notify(ctx, progress = false)
        }
        MusicLibrary.rescan(ctx)
        // After the scan has found the files: playlists, likes and history that held the server copy move to them.
        PlaylistStore.migrateIds(moved)
        Overrides.migrateIds(moved)
        PlayLog.migrateIds(moved)
    }

    /** "Onda downloads" in [tree], created the first time. */
    private fun downloadDir(ctx: Context, tree: Uri): Uri {
        val rootId = DocumentsContract.getTreeDocumentId(tree)
        val kids = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootId)
        ctx.contentResolver.query(kids, arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
        ), null, null, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.getString(1) == FOLDER && c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                    return DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
                }
            }
        }
        return DocumentsContract.createDocument(ctx.contentResolver, DocumentsContract.buildDocumentUriUsingTree(tree, rootId),
            DocumentsContract.Document.MIME_TYPE_DIR, FOLDER) ?: throw IOException("Can't create $FOLDER")
    }

    private fun names(ctx: Context, tree: Uri, dir: Uri): MutableSet<String> {
        val kids = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(dir))
        val out = HashSet<String>()
        ctx.contentResolver.query(kids, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            while (c.moveToNext()) out += c.getString(0).lowercase()
        }
        return out
    }

    /** The new file's id (the same id the library scan gives it), or null if a file of that name was already there. */
    private fun save(ctx: Context, tree: Uri, dir: Uri, item: Item, existing: MutableSet<String>): String? {
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
            if (name.lowercase() in existing) return null
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
            val doc = DocumentsContract.createDocument(ctx.contentResolver, dir, mime, name) ?: throw IOException("Can't create file")
            try {
                (ctx.contentResolver.openOutputStream(doc) ?: throw IOException("Can't write")).use { out ->
                    conn.inputStream.use { it.copyTo(out, 64 * 1024) }
                }
            } catch (e: Exception) {
                runCatching { DocumentsContract.deleteDocument(ctx.contentResolver, doc) } // no half-written songs
                throw e
            }
            existing += name.lowercase()
            return DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getDocumentId(doc)).toString()
        } finally {
            conn.disconnect()
        }
    }

    internal fun notification(ctx: Context, progress: Boolean): Notification {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "Downloads", NotificationManager.IMPORTANCE_LOW))
        val s = _state.value
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val b = Notification.Builder(ctx, CHANNEL).setSmallIcon(R.drawable.ic_stat_onda).setContentIntent(open)
        if (s.paused) {
            return b.setContentTitle("Downloads paused: no connection")
                .setContentText("${s.total - s.done} left. They continue the next time you open Onda.").setAutoCancel(true).build()
        }
        return if (progress) {
            b.setContentTitle("Downloading ${minOf(s.done + 1, s.total)} of ${s.total}").setContentText(s.current)
                .setProgress(s.total, s.done, false).setOngoing(true).setOnlyAlertOnce(true).build()
        } else {
            val ok = s.done - s.failed
            b.setContentTitle(if (s.failed == 0) "Downloaded $ok songs" else "Downloaded $ok songs, ${s.failed} failed")
                .setContentText("They're in your library, in “$FOLDER”.").setAutoCancel(true).build()
        }
    }

    private fun online(ctx: Context) = ctx.getSystemService(ConnectivityManager::class.java).activeNetwork != null

    private fun notify(ctx: Context, progress: Boolean) {
        runCatching { ctx.getSystemService(NotificationManager::class.java).notify(NOTE_ID, notification(ctx, progress)) }
    }

    internal const val ID = NOTE_ID
}

/** Keeps Onda alive while files download, with the progress notification Android requires. */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val n = Downloads.notification(this, progress = true)
        if (Build.VERSION.SDK_INT >= 29) startForeground(Downloads.ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else startForeground(Downloads.ID, n)
        if (job?.isActive != true) {
            job = scope.launch {
                try {
                    Downloads.run(this@DownloadService)
                } finally {
                    stopForeground(STOP_FOREGROUND_DETACH) // keep the "Downloaded" note
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    // Android 15 limits data transfers to 6 hours a day; stop cleanly when told to.
    override fun onTimeout(startId: Int, fgsType: Int) {
        job?.cancel()
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
