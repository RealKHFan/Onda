package io.github.realkhfan.onda.core

/** The quality badge for a file, the same on the phone and the desktop: "FLAC · 24-bit · 96 kHz", "MP3 · 320 kbps". */
object Quality {
    private val LOSSLESS = setOf("FLAC", "ALAC", "WAV", "AIFF", "APE", "WAVPACK")

    fun badge(format: String, kbps: Long, sampleRate: Int, bits: Int): String {
        val f = format.uppercase()
        val lossless = f in LOSSLESS
        val khz = if (sampleRate > 0) (if (sampleRate % 1000 == 0) "${sampleRate / 1000}" else "%.1f".format(java.util.Locale.ROOT, sampleRate / 1000.0)) + " kHz" else null
        return listOfNotNull(f, if (lossless && bits > 0) "$bits-bit" else if (kbps > 0) "$kbps kbps" else null, if (lossless) khz else null).joinToString(" · ")
    }

    /** A short format name from a file's MIME type. */
    fun formatOf(mime: String?, fileName: String): String = when (mime?.lowercase()) {
        "audio/flac", "audio/x-flac" -> "FLAC"
        "audio/mpeg", "audio/mp3" -> "MP3"
        "audio/mp4", "audio/m4a", "audio/x-m4a", "audio/aac", "audio/mp4a-latm" -> "AAC"
        "audio/ogg", "application/ogg", "audio/vorbis" -> "OGG"
        "audio/opus" -> "OPUS"
        "audio/wav", "audio/x-wav", "audio/wave" -> "WAV"
        else -> fileName.substringAfterLast('.', "").uppercase()
    }
}
