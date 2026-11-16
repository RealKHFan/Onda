package io.github.realkhfan.onda.data

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/** Your profile picture: a copy of a photo or an animated GIF/WebP in Onda's own storage. */
object Profile {
    /** The picture's file, or null for the letter. */
    var picture by mutableStateOf<File?>(null)
        private set

    private const val MAX_BYTES = 20L * 1024 * 1024

    fun load(ctx: Context) {
        picture = Prefs.profilePicture(ctx)?.let(::File)?.takeIf { it.isFile }
    }

    /** Copies the picked picture; false when it isn't an image Onda can show or is over 20 MB. Call off the main thread. */
    fun set(ctx: Context, uri: Uri): Boolean {
        val type = ctx.contentResolver.getType(uri).orEmpty()
        val ext = when (type) { "image/gif" -> "gif"; "image/png" -> "png"; "image/webp" -> "webp"; "image/jpeg" -> "jpg"; else -> return false }
        // A new name each time, so the old picture isn't shown from a cache.
        val out = File(ctx.filesDir, "profile-${System.currentTimeMillis()}.$ext")
        try {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > MAX_BYTES) return false.also { out.delete() }
                        o.write(buf, 0, n)
                    }
                }
            } ?: return false
            val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeFile(out.path, it) }
            if (size.outWidth <= 0) return false.also { out.delete() }
        } catch (_: Exception) {
            out.delete()
            return false
        }
        picture?.delete()
        Prefs.setProfilePicture(ctx, out.path)
        picture = out
        return true
    }

    fun clear(ctx: Context) {
        picture?.delete()
        Prefs.setProfilePicture(ctx, null)
        picture = null
    }
}
