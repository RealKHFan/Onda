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
        val bytes = try {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    if (out.size() > MAX_BYTES) return false
                }
                out.toByteArray()
            } ?: return false
        } catch (_: Exception) {
            return false
        }
        return set(ctx, bytes)
    }

    /** A picture's bytes (picked here, or from your PC); false when it isn't a JPG, PNG, WebP or GIF. */
    fun set(ctx: Context, bytes: ByteArray): Boolean {
        val ext = io.github.realkhfan.onda.core.LinkSync.pictureType(bytes) ?: return false
        if (BitmapFactory.Options().apply { inJustDecodeBounds = true }.also { BitmapFactory.decodeByteArray(bytes, 0, bytes.size, it) }.outWidth <= 0) return false
        // A new name each time, so the old picture isn't shown from a cache.
        val out = File(ctx.filesDir, "profile-${System.currentTimeMillis()}.$ext")
        try {
            out.writeBytes(bytes)
        } catch (_: Exception) {
            out.delete()
            return false
        }
        val old = picture
        Prefs.setProfilePicture(ctx, out.path)
        picture = out
        old?.delete()
        return true
    }

    fun clear(ctx: Context) {
        picture?.delete()
        Prefs.setProfilePicture(ctx, null)
        picture = null
    }

    private var hashed: Pair<String, String>? = null

    /** The picture's hash for the sync ("" for none). */
    fun hash(): String {
        val f = picture ?: return ""
        hashed?.takeIf { it.first == f.path }?.let { return it.second }
        return runCatching { io.github.realkhfan.onda.core.LinkSync.hashOf(f.readBytes()) }.getOrDefault("").also { hashed = f.path to it }
    }
}
