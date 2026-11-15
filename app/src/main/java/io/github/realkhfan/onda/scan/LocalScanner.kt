package io.github.realkhfan.onda.scan

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.MetadataRetriever
import java.util.concurrent.TimeUnit
import io.github.realkhfan.onda.core.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Scans the folders the user granted through the Storage Access Framework. No storage permission is
 * needed, so it also works with GrapheneOS Storage Scopes. Reads tags and embedded cover art with
 * the platform's MediaMetadataRetriever.
 */
object LocalScanner {

    private val AUDIO_EXT = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav")

    suspend fun scan(context: Context, trees: List<Uri>): List<Track> =
        withContext(Dispatchers.IO) {
            val artDir = File(context.cacheDir, "art").apply { mkdirs() }
            val out = ArrayList<Track>()
            for (tree in trees) {
                val rootId = try {
                    DocumentsContract.getTreeDocumentId(tree)
                } catch (_: Throwable) {
                    continue
                }
                val rootName = documentName(context, tree, rootId) ?: "Folder"
                traverse(context, tree, rootId, rootName, artDir, out)
            }
            out
        }

    private fun traverse(
        context: Context,
        tree: Uri,
        parentDocId: String,
        folderName: String,
        artDir: File,
        out: MutableList<Track>,
    ) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentDocId)
        val cursor = try {
            context.contentResolver.query(
                children,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null, null, null,
            )
        } catch (_: Throwable) {
            null
        } ?: return

        cursor.use { c ->
            while (c.moveToNext()) {
                val docId = c.getString(0)
                val name = c.getString(1) ?: continue
                val mime = c.getString(2) ?: ""
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    traverse(context, tree, docId, name, artDir, out)
                } else if (isAudio(name, mime)) {
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
                    out += readTrack(context, fileUri, name, folderName, artDir)
                }
            }
        }
    }

    private fun isAudio(name: String, mime: String): Boolean =
        mime.startsWith("audio/") || AUDIO_EXT.contains(name.substringAfterLast('.', "").lowercase())

    private class Tags(
        val title: String?,
        val artist: String?,
        val album: String?,
        val albumArtist: String?,
        val trackNo: Int,
        val durationMs: Long,
        val art: ByteArray?,
    ) {
        val hasText: Boolean get() = !title.isNullOrBlank() || !artist.isNullOrBlank()
    }

    /**
     * Never drops an audio file. The system tag reader fails on some files (notably fragmented .m4a);
     * then Media3's own parser — the code that plays the file — reads the tags, and the file name is the last resort.
     */
    private fun readTrack(context: Context, uri: Uri, fileName: String, folderName: String, artDir: File): Track {
        val platform = platformTags(context, uri)
        val media3 = if (platform?.hasText == true) null else media3Tags(context, uri)
        val (nameArtist, nameTitle) = fromFileName(fileName)
        fun pick(vararg v: String?) = v.firstOrNull { !it.isNullOrBlank() }?.trim()
        val title = pick(platform?.title, media3?.title, nameTitle) ?: fileName
        val artist = pick(platform?.artist, media3?.artist, nameArtist) ?: "Unknown artist"
        val album = pick(platform?.album, media3?.album) ?: "Unknown album"
        val albumArtist = pick(platform?.albumArtist, media3?.albumArtist) ?: artist
        val duration = platform?.durationMs?.takeIf { it > 0 } ?: extractorDuration(context, uri)
        return Track(
            id = uri.toString(),
            uri = uri.toString(),
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            trackNo = platform?.trackNo?.takeIf { it > 0 } ?: media3?.trackNo ?: 0,
            durationMs = duration,
            folderName = folderName,
            artUri = (platform?.art ?: media3?.art)?.let { cacheArt(it, albumArtist, album, artDir) },
            fileName = fileName,
        )
    }

    private fun platformTags(context: Context, uri: Uri): Tags? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            fun m(key: Int) = r.extractMetadata(key)?.trim()
            Tags(
                title = m(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = m(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                album = m(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                albumArtist = m(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                trackNo = m(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.substringBefore('/')?.toIntOrNull() ?: 0,
                durationMs = m(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L,
                art = r.embeddedPicture,
            )
        } catch (_: Throwable) {
            null
        } finally {
            try {
                r.release()
            } catch (_: Throwable) {
            }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun media3Tags(context: Context, uri: Uri): Tags? = try {
        val groups = MetadataRetriever.retrieveMetadata(context, MediaItem.fromUri(uri)).get(10, TimeUnit.SECONDS)
        val b = MediaMetadata.Builder()
        for (g in 0 until groups.length) {
            val group = groups.get(g)
            for (i in 0 until group.length) group.getFormat(i).metadata?.let { b.populateFromMetadata(it) }
        }
        val mm = b.build()
        Tags(
            mm.title?.toString(), mm.artist?.toString(), mm.albumTitle?.toString(), mm.albumArtist?.toString(),
            mm.trackNumber ?: 0, 0L, mm.artworkData,
        )
    } catch (_: Throwable) {
        null
    }

    /** Length from the container when the tag reader can't give it. */
    private fun extractorDuration(context: Context, uri: Uri): Long {
        val ex = MediaExtractor()
        return try {
            ex.setDataSource(context, uri, null)
            (0 until ex.trackCount).maxOfOrNull { i ->
                val f = ex.getTrackFormat(i)
                if (f.containsKey(MediaFormat.KEY_DURATION)) f.getLong(MediaFormat.KEY_DURATION) / 1000 else 0L
            } ?: 0L
        } catch (_: Throwable) {
            0L
        } finally {
            ex.release()
        }
    }

    /** "Artist - Title.m4a" -> (Artist, Title); otherwise (null, name without extension). */
    private fun fromFileName(name: String): Pair<String?, String> {
        val base = name.substringBeforeLast('.')
        val i = base.indexOf(" - ")
        return if (i > 0) base.substring(0, i).trim() to base.substring(i + 3).trim() else null to base
    }

    private fun cacheArt(bytes: ByteArray, albumArtist: String, album: String, artDir: File): String? {
        val key = sha1("${albumArtist.lowercase()}|${album.lowercase()}")
        val file = File(artDir, "$key.jpg")
        if (file.exists()) return Uri.fromFile(file).toString()
        return try {
            file.writeBytes(bytes)
            Uri.fromFile(file).toString()
        } catch (_: Throwable) {
            null
        }
    }

    private fun documentName(context: Context, tree: Uri, docId: String): String? {
        val docUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
        return try {
            context.contentResolver.query(
                docUri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null,
            )?.use { if (it.moveToFirst()) it.getString(0) else null }
        } catch (_: Throwable) {
            null
        }
    }

    private fun sha1(s: String): String =
        MessageDigest.getInstance("SHA-1").digest(s.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
