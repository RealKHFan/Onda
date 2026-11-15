package io.github.realkhfan.onda.core

import kotlin.math.abs

enum class Confidence { HIGH, MAYBE }

/** Decides whether a catalog result is the same recording as a local file. Pure, for JVM tests. */
object MetaMatch {
    private val junk =
        Regex("""\s*[(\[][^)\]]*(official|video|audio|lyrics?|\bmv\b|\bhd\b|\bhq\b|visuali[sz]er)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE)

    // Words that make a different recording; Match.norm drops brackets, so check them separately.
    private val versionWords =
        Regex("""\b(sped ?up|speed ?up|slowed|reverb|nightcore|remix|live|karaoke|instrumental|acoustic|8d)\b""", RegexOption.IGNORE_CASE)

    private fun versionOf(title: String): Set<String> =
        versionWords.findAll(title).map { it.value.lowercase().replace(" ", "") }.toSet()

    /** Title and artist as they'd be searched: "Artist - Title" file names split, video junk removed. */
    fun cleaned(t: Track): Pair<String, String> {
        val artistKnown = t.artist.isNotBlank() && t.artist != "Unknown artist"
        var title = t.title
        var artist = if (artistKnown) t.artist else ""
        if (!artistKnown && " - " in title) {
            artist = title.substringBefore(" - ").trim()
            title = title.substringAfter(" - ").trim()
        }
        return title.replace(junk, "").trim() to artist
    }

    /** Both titles name the same version: a remix, live or sped-up take never stands in for the original. */
    fun sameVersion(a: String, b: String): Boolean = versionOf(a) == versionOf(b)

    /**
     * A free whole song may stand in for a catalog preview only if it's clearly the same recording from the artist's
     * own account: same title and version, exactly the same artist name, and about the same length.
     */
    fun isFullCopy(preview: RemoteTrack, c: RemoteTrack): Boolean =
        Match.norm(preview.title).let { it.isNotEmpty() && it == Match.norm(c.title) } &&
            Match.norm(preview.artist) == Match.norm(c.artist) && sameVersion(preview.title, c.title) &&
            (preview.durationSec == 0 || c.durationSec == 0 || abs(preview.durationSec - c.durationSec) <= 8)

    /** "Other Artist - Song" posted by someone else is almost always a re-upload, so free catalogs skip it. */
    fun namesOtherArtist(title: String, uploader: String): Boolean {
        if (" - " !in title) return false
        val named = Match.norm(title.substringBefore(" - "))
        val up = Match.norm(uploader)
        return named.isNotEmpty() && up.isNotEmpty() && !named.contains(up) && !up.contains(named)
    }

    fun query(t: Track): String = cleaned(t).let { (title, artist) -> "$artist $title".trim() }

    /** HIGH: safe to replace in bulk. MAYBE: show for review. null: not the same song. */
    fun judge(local: Track, c: RemoteTrack): Confidence? {
        val (title, artist) = cleaned(local)
        val lt = Match.norm(title)
        val ct = Match.norm(c.title)
        if (lt.isEmpty() || ct.isEmpty()) return null
        val titleSame = lt == ct
        val titleClose = !titleSame && minOf(lt.length, ct.length) >= 3 && (lt.startsWith(ct) || ct.startsWith(lt))
        val la = Match.norm(artist)
        val ca = Match.norm(c.artist)
        val artistKnown = la.isNotEmpty()
        val artistSame = artistKnown && ca.isNotEmpty() && (la.contains(ca) || ca.contains(la))
        val diff = durationDiff(local, c)
        val sameVersion = versionOf(title) == versionOf(c.title)
        return when {
            // "Song" vs "Song (Sped Up)": a different recording; only worth a look if the length is identical.
            !sameVersion -> if (titleSame && artistSame && diff != null && diff <= 3) Confidence.MAYBE else null
            titleSame && artistSame && (diff == null || diff <= 3) -> Confidence.HIGH
            // No artist tag: only trust a near-exact length.
            titleSame && !artistKnown && diff != null && diff <= 2 -> Confidence.HIGH
            // Same title and artist at another length: probably another edit of the song; let the user judge.
            titleSame && artistSame -> Confidence.MAYBE
            (titleSame || titleClose) && (artistSame || (diff != null && diff <= 5)) && (diff == null || diff <= 15) -> Confidence.MAYBE
            else -> null
        }
    }

    /** The most confident candidate; ties go to the closer length, then the catalog's own ranking. */
    fun best(local: Track, candidates: List<RemoteTrack>): Pair<RemoteTrack, Confidence>? =
        candidates.mapNotNull { c -> judge(local, c)?.let { c to it } }
            .minWithOrNull(compareBy({ it.second.ordinal }, { durationDiff(local, it.first) ?: Long.MAX_VALUE }))

    /** Whether replacing would change anything visible. */
    fun differs(local: Track, c: RemoteTrack): Boolean =
        local.title != c.title || local.artist != c.artist ||
            (c.album.isNotBlank() && local.album != c.album) || (local.artUri == null && c.cover != null)

    private fun durationDiff(local: Track, c: RemoteTrack): Long? =
        if (local.durationMs > 0 && c.durationSec > 0) abs(local.durationMs / 1000 - c.durationSec) else null
}
