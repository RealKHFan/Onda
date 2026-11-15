package io.github.realkhfan.onda.player

import io.github.realkhfan.onda.core.RemoteTrack
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Server
import io.github.realkhfan.onda.data.Subsonic
import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Covers for Android Auto's menus, which can't open Onda's own files. Not exported: Onda grants each cover to the
 * app that browses it. Server covers are fetched here, so the server login never leaves Onda.
 */
class ArtProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "io.github.realkhfan.onda.art"
        private val NAME = Regex("""[A-Za-z0-9_-]{1,64}(\.jpg)?""")
        private const val MAX_BYTES = 5 shl 20
        private val WEB_NAME = Regex("""[A-Za-z0-9_-]{8,1200}""")
        // Only known cover hosts, so the provider can never be used to fetch anything else.
        private val WEB_HOSTS = listOf(".dzcdn.net", ".mzstatic.com", "archive.org", ".audius.co", ".jamendo.com", ".wikimedia.org", ".openverse.org")

        fun webAllowed(url: String): Boolean {
            val host = runCatching { java.net.URI(url).takeIf { it.scheme == "https" }?.host }.getOrNull()?.lowercase() ?: return false
            return WEB_HOSTS.any { h -> host.endsWith(h) || host == h.trimStart('.') }
        }

        fun uriFor(r: RemoteTrack): Uri? = r.cover?.takeIf(::webAllowed)?.let {
            Uri.parse("content://$AUTHORITY/web/" + android.util.Base64.encodeToString(it.toByteArray(),
                android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING))
        }

        fun uriFor(t: Track): Uri? = when {
            t.artUri?.startsWith("file:") == true ->
                Uri.parse(t.artUri).lastPathSegment?.let { Uri.parse("content://$AUTHORITY/art/$it") }
            t.id.startsWith(Server.PREFIX) && t.artUri != null ->
                Uri.parse("content://$AUTHORITY/srv/" + Uri.encode(t.id.removePrefix(Server.PREFIX)))
            else -> null
        }
    }

    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("Covers are read-only")
        val ctx = context ?: throw FileNotFoundException()
        val seg = uri.pathSegments.getOrNull(1) ?: throw FileNotFoundException()
        val file = when (uri.pathSegments.firstOrNull()) {
            "art" -> seg.takeIf { NAME.matches(it) }?.let { File(File(ctx.cacheDir, "art"), it) }
            "srv" -> seg.takeIf { NAME.matches(it) }?.let { serverCover(File(ctx.cacheDir, "server_art"), it) }
            "web" -> seg.takeIf { WEB_NAME.matches(it) }?.let { webCover(File(ctx.cacheDir, "web_art"), it) }
            else -> null
        }
        if (file == null || !file.isFile) throw FileNotFoundException(uri.toString())
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    private fun webCover(dir: File, encoded: String): File? {
        val url = runCatching { String(android.util.Base64.decode(encoded, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING)) }
            .getOrNull()?.takeIf(::webAllowed) ?: return null
        val name = java.security.MessageDigest.getInstance("SHA-1").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        return fetch(url, File(dir.apply { mkdirs() }, "$name.jpg"))
    }

    /** Runs on a binder thread, so the one-time download may block. */
    private fun serverCover(dir: File, songId: String): File? {
        val cfg = Server.config.value ?: return null
        val cover = Server.songs.value.firstOrNull { it.id == songId }?.coverArt ?: return null
        val file = File(dir.apply { mkdirs() }, cover.replace(Regex("[^A-Za-z0-9_-]"), "_").take(64) + ".jpg")
        return fetch(Subsonic.coverUrl(cfg, cover), file)
    }

    private fun fetch(url: String, file: File): File? {
        if (file.isFile) return file
        val dir = file.parentFile ?: return null
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "Onda")
        return try {
            if (conn.responseCode != 200 || conn.contentType?.startsWith("image/") != true) return null
            val tmp = File(dir, file.name + ".tmp")
            var total = 0
            conn.inputStream.use { input ->
                tmp.outputStream().use { out ->
                    val buf = ByteArray(16 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) break // not a cover
                        out.write(buf, 0, n)
                    }
                }
            }
            if (total > MAX_BYTES) { tmp.delete(); return null }
            if (tmp.renameTo(file)) file else null
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    override fun getType(uri: Uri) = "image/jpeg"
    override fun query(uri: Uri, p: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun update(uri: Uri, values: ContentValues?, s: String?, a: Array<out String>?) = 0
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
}
