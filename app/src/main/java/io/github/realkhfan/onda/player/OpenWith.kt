package io.github.realkhfan.onda.player

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

/** "Open with Onda" from a file manager or a chat app: the song plays here. */
object OpenWith {
    /** Set by the activity; the app plays it and clears it. */
    val uri = MutableStateFlow<Uri?>(null)

    /** Only a shared audio file (content://) is taken; anything else in the intent is ignored. */
    fun from(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val u = intent.data?.takeIf { it.scheme == "content" } ?: return
        val type = intent.type ?: runCatching { context.contentResolver.getType(u) }.getOrNull()
        if (type != null && !type.startsWith("audio/") && type != "application/ogg") return
        // Kept across restarts when the sending app allows it; otherwise it plays while Onda is open.
        runCatching { context.contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        intent.data = null // not again after rotation
        uri.value = u
    }

    /** A player item with the file's own tags (or its name). */
    suspend fun item(context: Context, u: Uri): MediaItem = withContext(Dispatchers.IO) {
        val name = runCatching {
            context.contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()?.substringBeforeLast('.')
        val r = MediaMetadataRetriever()
        val tags = try {
            r.setDataSource(context, u)
            Triple(r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE), r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
        } catch (_: Exception) {
            Triple(null, null, null)
        } finally {
            runCatching { r.release() }
        }
        MediaItem.Builder().setMediaId(u.toString()).setUri(u)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(tags.first?.ifBlank { null } ?: name ?: "Song")
                .setArtist(tags.second?.ifBlank { null } ?: "Unknown artist").setAlbumTitle(tags.third).build())
            .build()
    }
}
