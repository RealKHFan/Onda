package io.github.realkhfan.onda.core

import java.net.URLDecoder

/** File names from tags: "Artist - Title.ext". Pure, for JVM tests. */
object FileNames {

    data class Rename(val id: String, val from: String, val to: String)

    // Not allowed on Android's FAT/exFAT SD cards (and Windows): \ / : * ? " < > | and control characters.
    private val illegal = Regex("""[\\/:*?"<>|\u0000-\u001F]""")
    private val spaces = Regex("""\s+""")
    private const val MAX_BYTES = 200 // file systems allow 255 bytes; Japanese characters take 3 each

    fun clean(s: String): String = s.replace(illegal, "_").replace(spaces, " ").trim()

    /** "Artist - Title.ext", or "Title.ext" for an unknown artist; null when there's no usable title or extension. */
    fun fromTags(t: Track): String? = forSong(t.title, t.artist, t.fileName.substringAfterLast('.', ""))

    fun forSong(title: String, artist: String, extension: String): String? {
        val ext = clean(extension).lowercase().takeIf { it.isNotEmpty() && it.length <= 5 } ?: return null
        val title = clean(title).takeIf { it.isNotEmpty() } ?: return null
        val artist = clean(artist).takeUnless { it.isEmpty() || artist == "Unknown artist" }
        val base = trimToBytes(if (artist != null) "$artist - $title" else title, MAX_BYTES).trimEnd('.', ' ')
        return if (base.isEmpty()) null else "$base.$ext"
    }

    /** File extension of a download, from its Content-Disposition name or else its type ("mp3" if unknown). */
    fun extFrom(disposition: String?, contentType: String?): String {
        val named = disposition?.let { Regex("""filename\*?=(?:UTF-8'')?"?([^";]+)"?""", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) }
            ?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.length in 2..4 && it.all(Char::isLetterOrDigit) }
        if (named != null) return named
        return when (contentType?.substringBefore(';')?.trim()?.lowercase()) {
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/mp4", "audio/x-m4a", "audio/m4a" -> "m4a"
            "audio/ogg", "application/ogg" -> "ogg"
            "audio/opus" -> "opus"
            "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
            else -> "mp3"
        }
    }

    /**
     * Renames needed so files match their tags: unchanged names are skipped, and two songs that would get
     * the same name in one folder become "Name.ext" and "Name (2).ext". Compared case-insensitively (SD cards).
     */
    fun plan(tracks: List<Track>): List<Rename> {
        val out = ArrayList<Rename>()
        tracks.filter { it.fileName.isNotEmpty() }.groupBy { parentOf(it.id) ?: it.folderName }.values.forEach { folder ->
            val taken = folder.map { it.fileName.lowercase() }.toMutableSet()
            for (t in folder) {
                val target = fromTags(t) ?: continue
                if (target == t.fileName) continue
                taken -= t.fileName.lowercase()
                val dot = target.lastIndexOf('.')
                var name = target
                var n = 2
                while (name.lowercase() in taken) name = target.substring(0, dot) + " (${n++})" + target.substring(dot)
                taken += name.lowercase()
                if (name != t.fileName) out += Rename(t.id, t.fileName, name)
            }
        }
        return out
    }

    /** The folder part of a document id inside a SAF document URI ("primary:Music/A/x.m4a" -> "primary:Music/A"), or of a file URI. */
    fun parentOf(documentUri: String): String? {
        if (documentUri.startsWith("file:")) return documentUri.substringBeforeLast('/', "").ifEmpty { null }
        val encoded = documentUri.substringAfterLast("/document/", "").ifEmpty { return null }
        val docId = URLDecoder.decode(encoded, "UTF-8")
        return docId.substringBeforeLast('/', "").ifEmpty { null }
    }

    private fun trimToBytes(s: String, max: Int): String {
        var r = s
        while (r.toByteArray().size > max) r = r.dropLast(1)
        return r
    }
}
