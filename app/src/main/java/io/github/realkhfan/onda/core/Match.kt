package io.github.realkhfan.onda.core

/** Loose title/artist matching between local files and catalog results. Step 3's metadata matcher reuses it. */
object Match {
    private val brackets = Regex("""\(.*?\)|\[.*?]""")
    private val feat = Regex("""\s(feat|ft)\.?\s.*$""")
    private val nonWord = Regex("""[^\p{L}\p{N}]+""")

    /** "Dopa (feat. X) [Remastered]" -> "dopa". Keeps letters of any script, so Japanese titles survive. */
    fun norm(s: String): String =
        s.lowercase()
            .replace(brackets, " ")
            .replace(feat, " ")
            .replace(nonWord, " ")
            .trim()

    /** The library: local files plus server songs you don't also have on the phone (the local copy wins). */
    fun mergeLibrary(local: List<Track>, server: List<Track>): List<Track> {
        if (server.isEmpty()) return local
        val byTitle = local.groupBy { norm(it.title) }
        return local + server.filter { s -> byTitle[norm(s.title)]?.none { same(it.title, it.artist, s.title, s.artist) } ?: true }
    }

    /** [primary] first, then the songs from [extra] it doesn't already have. */
    fun mergeTracks(primary: List<RemoteTrack>, extra: List<RemoteTrack>): List<RemoteTrack> =
        primary + extra.filter { e -> primary.none { same(it.title, it.artist, e.title, e.artist) } }

    fun same(localTitle: String, localArtist: String, remoteTitle: String, remoteArtist: String): Boolean {
        val t = norm(localTitle)
        if (t.isEmpty() || t != norm(remoteTitle)) return false
        val a = norm(localArtist)
        val b = norm(remoteArtist)
        return a.isEmpty() || b.isEmpty() || a.contains(b) || b.contains(a)
    }
}
