package io.github.realkhfan.onda.core

/** Playlist files (.m3u/.m3u8), read and written the same way as on the desktop, so files move between both. */
object M3u {
    /** One line per song: "#EXTINF:seconds,Artist - Title" then the path (or a note when it isn't a file here). */
    fun write(name: String, tracks: List<Track>, pathOf: (Track) -> String?): String = buildString {
        append("#EXTM3U\n#PLAYLIST:").append(name.replace('\n', ' ')).append('\n')
        for (t in tracks) {
            append("#EXTINF:").append(t.durationMs / 1000).append(',').append(t.artist).append(" - ").append(t.title).append('\n')
            append(pathOf(t) ?: "# not a file on this PC").append('\n')
        }
    }

    /** The playlist's name and its entries: (path or null, "Artist - Title" or null). */
    fun parse(text: String, fallbackName: String): Pair<String, List<Pair<String?, String?>>> {
        var name = fallbackName
        var info: String? = null
        val out = ArrayList<Pair<String?, String?>>()
        for (raw in text.lineSequence().take(20_000)) {
            val line = raw.trim().removePrefix("\uFEFF")
            when {
                line.startsWith("#PLAYLIST:") -> name = line.substringAfter(':').trim().ifEmpty { name }
                line.startsWith("#EXTINF:") -> info = line.substringAfter(',', "").trim().ifEmpty { null }
                line == "# not a file on this PC" -> { out += null to info; info = null }
                line.isEmpty() || line.startsWith("#") -> {}
                else -> { out += line to info; info = null }
            }
        }
        return name to out
    }

    /** Finds each entry in your library: by file name, else by "Artist - Title". */
    fun match(entries: List<Pair<String?, String?>>, library: List<Track>): List<String> {
        val byFile = library.groupBy { it.fileName.lowercase() }
        val byName = library.groupBy { Match.norm(it.artist) + "|" + Match.norm(it.title) }
        return entries.mapNotNull { (path, info) ->
            path?.substringAfterLast('/')?.substringAfterLast('\\')?.lowercase()?.let { byFile[it]?.firstOrNull() }
                ?: info?.takeIf { " - " in it }?.let { byName[Match.norm(it.substringBefore(" - ")) + "|" + Match.norm(it.substringAfter(" - "))]?.firstOrNull() }
        }.map { it.id }.distinct()
    }
}
