package io.github.realkhfan.onda.desktop

import io.github.realkhfan.onda.core.Track
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.security.MessageDigest
import java.util.logging.Level
import java.util.logging.Logger

/** Finds music files in your folders and reads their tags and covers. */
object Scanner {
    /** What the player bar and "Song info" show about a file. */
    data class AudioInfo(val format: String, val bitrateKbps: Long, val sampleRate: Int, val bits: Int, val channels: String, val bytes: Long, val modified: Long) {
        /** "FLAC · 24-bit · 96 kHz", "MP3 · 320 kbps". */
        val badge: String get() = io.github.realkhfan.onda.core.Quality.badge(format, bitrateKbps, sampleRate, bits)
    }

    fun audioInfo(f: File): AudioInfo? = runCatching {
        val h = org.jaudiotagger.audio.AudioFileIO.read(f).audioHeader
        AudioInfo(h.format.substringBefore(' ').ifBlank { f.extension }, h.bitRateAsNumber, h.sampleRateAsNumber, h.bitsPerSample, h.channels.orEmpty(), f.length(), f.lastModified())
    }.getOrNull()

    private val exts = setOf("mp3", "m4a", "flac", "ogg", "oga", "opus", "wav", "aac")
    fun isAudio(f: File) = f.isFile && f.extension.lowercase() in exts
    private val artDir = File(Store.cache, "art").apply { mkdirs() }

    init {
        Logger.getLogger("org.jaudiotagger").level = Level.OFF // it logs every file it reads
    }

    /** Files that didn't change since [known] are not read again. */
    fun scan(folders: List<String>, known: Map<String, Pair<Long, Track>>): Map<String, Pair<Long, Track>> {
        val out = LinkedHashMap<String, Pair<Long, Track>>()
        for (root in folders.map(::File).filter { it.isDirectory }) {
            root.walkTopDown().onEnter { !it.name.startsWith(".") }
                .filter { it.isFile && it.extension.lowercase() in exts }
                .forEach { f ->
                    val m = f.lastModified()
                    out[f.absolutePath] = known[f.absolutePath]?.takeIf { it.first == m } ?: (m to read(f, root.name))
                }
        }
        return out
    }

    fun read(f: File, folderName: String): Track {
        var title = ""; var artist = ""; var album = ""; var albumArtist = ""; var trackNo = 0; var durMs = 0L
        var art: ByteArray? = null
        runCatching {
            val af = AudioFileIO.read(f)
            durMs = af.audioHeader.trackLength * 1000L
            af.tag?.let { tag ->
                title = tag.getFirst(FieldKey.TITLE).trim()
                artist = tag.getFirst(FieldKey.ARTIST).trim()
                album = tag.getFirst(FieldKey.ALBUM).trim()
                albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST).trim()
                trackNo = tag.getFirst(FieldKey.TRACK).substringBefore('/').trim().toIntOrNull() ?: 0
                art = tag.firstArtwork?.binaryData
            }
        } // unreadable tags: the file still counts, named from its file name
        if (title.isEmpty()) {
            val base = f.nameWithoutExtension
            if (" - " in base && artist.isEmpty()) { artist = base.substringBefore(" - ").trim(); title = base.substringAfter(" - ").trim() } else title = base
        }
        val a = artist.ifEmpty { "Unknown artist" }
        val alb = album.ifEmpty { "Unknown album" }
        val aa = albumArtist.ifEmpty { a }
        val uri = f.toURI().toString()
        return Track(uri, uri, title, a, alb, aa, trackNo, durMs, folderName, art?.let { cover(it, aa, alb) }, f.name)
    }

    private fun cover(bytes: ByteArray, albumArtist: String, album: String): String? = runCatching {
        val key = MessageDigest.getInstance("SHA-1").digest("${albumArtist.lowercase()}|${album.lowercase()}".toByteArray())
            .joinToString("") { "%02x".format(it) }
        val file = File(artDir, "$key.jpg")
        if (!file.exists()) file.writeBytes(bytes)
        file.absolutePath
    }.getOrNull()
}
