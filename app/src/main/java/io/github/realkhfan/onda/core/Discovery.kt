package io.github.realkhfan.onda.core

import java.time.LocalDate
import java.time.temporal.IsoFields
import kotlin.math.pow
import kotlin.random.Random

/** Daily 10 / Weekly 30: who you like, what to leave out, and a date-seeded pick. Pure, for JVM tests. */
object Discovery {

    data class Play(val key: String, val artist: String, val title: String, val atMs: Long)

    /**
     * [seeds]: how many of your top artists to start from. [perArtist]: songs per artist in the mix.
     * [newArtistsOnly]: Weekly 30 only suggests artists you don't have or play yet.
     */
    enum class Kind(val label: String, val size: Int, val seeds: Int, val relatedPerSeed: Int, val perArtist: Int, val newArtistsOnly: Boolean) {
        DAILY("Daily 10", 10, 6, 4, 1, false),
        WEEKLY("Weekly 30", 30, 12, 6, 2, true),
    }

    /** One mix per day, or per ISO week (Monday to Sunday). */
    fun period(kind: Kind, date: LocalDate): String = when (kind) {
        Kind.DAILY -> date.toString()
        Kind.WEEKLY -> "%d-W%02d".format(date.get(IsoFields.WEEK_BASED_YEAR), date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
    }

    /**
     * Your artists, strongest first. A play counts 1 and halves every 2 weeks; a liked song counts 3,
     * a Want-to-listen song 2, and library size adds up to 2.
     */
    fun seedArtists(
        plays: List<Play>,
        liked: List<Track>,
        wanted: List<RemoteTrack>,
        library: List<Track>,
        nowMs: Long,
    ): List<String> {
        val score = HashMap<String, Double>()
        val name = HashMap<String, String>()
        fun add(artist: String, w: Double) {
            if (artist.isBlank() || artist == "Unknown artist") return
            val k = Match.norm(artist)
            if (k.isEmpty()) return
            score[k] = (score[k] ?: 0.0) + w
            name.putIfAbsent(k, artist)
        }
        plays.forEach { add(it.artist, 0.5.pow((nowMs - it.atMs).coerceAtLeast(0) / DAY_MS / 14.0)) }
        liked.forEach { add(it.artist, 3.0) }
        wanted.forEach { add(it.artist, 2.0) }
        library.groupBy { Match.norm(it.artist) }.forEach { (_, ts) -> add(ts.first().artist, minOf(ts.size * 0.1, 2.0)) }
        return score.entries.sortedByDescending { it.value }.map { name.getValue(it.key) }
    }

    private val tagged = Regex("""[(\[]([^)\]]*)[)\]]|\s-\s(.*)$""")
    private val unwantedWords =
        Regex("""\b(remix|rmx|sped ?up|speed ?up|slowed|reverb|nightcore|karaoke|8d|live)\b""", RegexOption.IGNORE_CASE)
    private val liveAlbum = Regex("""\blive (at|in|from)\b|\(live\)|\[live]""", RegexOption.IGNORE_CASE)

    /**
     * Versions left out of discovery: remixes, sped up / slowed / nightcore, live, karaoke.
     * Only words in brackets or after " - " count, so "Live Forever" or "Remix Me" stay in. Covers and singles stay too.
     */
    fun isUnwanted(title: String, album: String): Boolean =
        tagged.findAll(title).any { m -> unwantedWords.containsMatchIn(m.value) } || liveAlbum.containsMatchIn(album)

    /** Date-seeded pick: [Kind.size] songs, at most [Kind.perArtist] per artist, skipping unwanted versions and [skip]. */
    fun pick(
        candidates: List<RemoteTrack>,
        kind: Kind,
        period: String,
        perArtistMax: Int = kind.perArtist,
        skip: (RemoteTrack) -> Boolean,
    ): List<RemoteTrack> {
        val usable = candidates
            .filter { it.preview != null && !isUnwanted(it.title, it.album) && !skip(it) }
            .distinctBy { Match.norm(it.title) + "|" + Match.norm(it.artist) }
        val perArtist = HashMap<String, Int>()
        val out = ArrayList<RemoteTrack>()
        for (t in usable.shuffled(Random(period.hashCode().toLong()))) {
            val k = Match.norm(t.artist)
            if ((perArtist[k] ?: 0) >= perArtistMax) continue
            perArtist[k] = (perArtist[k] ?: 0) + 1
            out += t
            if (out.size == kind.size) break
        }
        return out
    }

    /** With few artists in the pool, allow more songs each (up to 3) so the mix still reaches its size. */
    fun perArtistFor(kind: Kind, candidates: List<RemoteTrack>): Int {
        val artists = candidates.map { Match.norm(it.artist) }.distinct().size.coerceAtLeast(1)
        return maxOf(kind.perArtist, minOf(3, (kind.size + artists - 1) / artists))
    }

    private const val DAY_MS = 86_400_000.0
}
