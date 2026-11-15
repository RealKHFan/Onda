package io.github.realkhfan.onda.core

/** Menu ids for Android Auto and car search. Pure, for JVM tests. */
object AutoTree {
    const val ROOT = "root"
    const val PLAYLISTS = "playlists"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val LIKED = "liked"
    const val ALL = "all"

    // Album keys contain "|", so a song's id inside a list uses a character no id ever holds.
    private const val SEP = '\u001F'

    /** A song as an entry of list [parent]: playing it plays the list from there. */
    fun entry(parent: String, songId: String): String = "$parent$SEP$songId"

    /** (list, song id), or null for a menu id. */
    fun split(mediaId: String): Pair<String, String>? {
        val i = mediaId.indexOf(SEP)
        return if (i <= 0) null else mediaId.substring(0, i) to mediaId.substring(i + 1)
    }

    /** Songs whose title, artist or album hold every word, exact titles first, then exact artists. */
    fun search(tracks: List<Track>, query: String, limit: Int = 100): List<Track> {
        val q = Match.norm(query)
        val words = q.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        return tracks
            .filter { t -> Match.norm("${t.title} ${t.artist} ${t.album}").let { hay -> words.all { it in hay } } }
            .sortedWith(compareBy({ Match.norm(it.title) != q }, { Match.norm(it.artist) != q }))
            .take(limit)
    }
}
