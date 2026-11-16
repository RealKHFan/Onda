package io.github.realkhfan.onda.core

/** Playlists made from your library and listening history (the desktop's smart playlists). */
object Smart {
    enum class Kind(val title: String, val about: String) {
        TOP("Most played", "Your 100 most played songs"),
        REPEAT("On repeat", "What you've played most in the last 30 days"),
        NEVER("Never played", "Songs in your library you haven't played yet"),
        FORGOTTEN("Forgotten favourites", "Liked songs you haven't played in two months"),
    }

    fun list(kind: Kind, library: List<Track>, plays: List<Discovery.Play>, liked: List<String>, trackFor: (String) -> Track?, now: Long): List<Track> {
        val counts = plays.groupingBy { it.key }.eachCount()
        val last = plays.groupBy { it.key }.mapValues { e -> e.value.maxOf { it.atMs } }
        return when (kind) {
            Kind.TOP -> library.filter { (counts[it.id] ?: 0) > 0 }.sortedByDescending { counts[it.id] }.take(100)
            Kind.REPEAT -> plays.filter { it.atMs >= now - 30 * DAY }.groupingBy { it.key }.eachCount().entries
                .filter { it.value >= 2 }.sortedByDescending { it.value }.take(30).mapNotNull { trackFor(it.key) }
            Kind.NEVER -> library.filter { it.id !in counts }.sortedBy { it.title.lowercase() }
            Kind.FORGOTTEN -> liked.mapNotNull(trackFor).filter { (last[it.id] ?: 0) < now - 60 * DAY }.sortedBy { last[it.id] ?: 0 }
        }
    }

    /** Plays per hour of the day (0–23) and per weekday (Monday first), in [zone]. */
    fun clock(plays: List<Discovery.Play>, zone: java.time.ZoneId): Pair<IntArray, IntArray> {
        val hours = IntArray(24); val days = IntArray(7)
        plays.forEach { p -> java.time.Instant.ofEpochMilli(p.atMs).atZone(zone).let { hours[it.hour]++; days[it.dayOfWeek.value - 1]++ } }
        return hours to days
    }

    /** How a list is ordered (the desktop's sort menu). */
    enum class Sort(val label: String) { CUSTOM("Custom order"), TITLE("Title"), ARTIST("Artist"), ALBUM("Album"), PLAYS("Most played"), LENGTH("Length") }

    fun sorted(tracks: List<Track>, sort: Sort, plays: Map<String, Int>): List<Track> = when (sort) {
        Sort.CUSTOM -> tracks
        Sort.TITLE -> tracks.sortedBy { it.title.lowercase() }
        Sort.ARTIST -> tracks.sortedWith(compareBy({ it.artist.lowercase() }, { it.album.lowercase() }, { it.trackNo }))
        Sort.ALBUM -> tracks.sortedWith(compareBy({ it.album.lowercase() }, { it.trackNo }))
        Sort.PLAYS -> tracks.sortedByDescending { plays[it.id] ?: 0 }
        Sort.LENGTH -> tracks.sortedBy { it.durationMs }
    }

    /** "48 min", "1 h 12 min". */
    fun length(tracks: List<Track>): String {
        val min = tracks.sumOf { it.durationMs } / 60_000
        return if (min >= 60) "${min / 60} h ${min % 60} min" else "$min min"
    }

    private const val DAY = 86_400_000L

    /** Your most played artists of the last three months, for "Your top mixes" (files without an artist tag are left out). */
    fun topArtists(plays: List<Discovery.Play>, now: Long, n: Int, skip: (String) -> Boolean = { false }): List<String> {
        val since = now - 90 * 86_400_000L
        return plays.filter { it.atMs >= since && Match.norm(it.artist) !in NO_ARTIST && !skip(it.artist) }
            .groupBy { Match.norm(it.artist) }.entries.sortedByDescending { it.value.size }.take(n).map { it.value.last().artist }
    }
    private val NO_ARTIST = setOf("", "unknown artist", "unknown", "various artists", "<unknown>")

    /** One of yours, then two others, like the desktop's artist mix. */
    fun <T> interleave(own: List<T>, others: List<T>): List<T> {
        val out = ArrayList<T>(own.size + others.size)
        val a = own.iterator(); val b = others.iterator()
        while (a.hasNext() || b.hasNext()) { if (a.hasNext()) out += a.next(); repeat(2) { if (b.hasNext()) out += b.next() } }
        return out
    }

    /** Songs in your library more than once (same artist and title), sorted by title. */
    fun duplicates(library: List<Track>): List<List<Track>> =
        library.groupBy { Match.norm(it.artist) + "|" + Match.norm(it.title) }.values.filter { it.size > 1 }.sortedBy { it[0].title.lowercase() }
}
