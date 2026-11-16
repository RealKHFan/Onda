package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class SmartTest {
    private fun t(id: String, title: String, ms: Long = 0) = Track(id, id, title, "A", "B", "A", 0, ms, "M", null)
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    @Test fun smartListsFollowYourPlays() {
        val a = t("a", "Alpha"); val b = t("b", "Bravo"); val c = t("c", "Charlie")
        val plays = listOf(Discovery.Play("a", "A", "Alpha", now - day), Discovery.Play("a", "A", "Alpha", now - 2 * day),
            Discovery.Play("b", "A", "Bravo", now - 90 * day))
        val by = mapOf("a" to a, "b" to b, "c" to c)
        assertEquals(listOf("a", "b"), Smart.list(Smart.Kind.TOP, listOf(a, b, c), plays, emptyList(), by::get, now).map { it.id })
        assertEquals(listOf("a"), Smart.list(Smart.Kind.REPEAT, listOf(a, b, c), plays, emptyList(), by::get, now).map { it.id })
        assertEquals(listOf("c"), Smart.list(Smart.Kind.NEVER, listOf(a, b, c), plays, emptyList(), by::get, now).map { it.id })
        assertEquals(listOf("c", "b"), Smart.list(Smart.Kind.FORGOTTEN, listOf(a, b, c), plays, listOf("a", "b", "c"), by::get, now).map { it.id })
    }

    @Test fun clockSortAndLength() {
        val (hours, days) = Smart.clock(listOf(Discovery.Play("a", "A", "x", 0)), ZoneOffset.UTC) // 1 Jan 1970, a Thursday, midnight
        assertEquals(1, hours[0]); assertEquals(1, days[3])
        val list = listOf(t("1", "b", 300_000), t("2", "a", 3_600_000))
        assertEquals(listOf("2", "1"), Smart.sorted(list, Smart.Sort.TITLE, emptyMap()).map { it.id })
        assertEquals(listOf("2", "1"), Smart.sorted(list, Smart.Sort.ADDED, emptyMap(), mapOf("1" to 100L, "2" to 200L)).map { it.id })
        assertEquals("1 h 5 min", Smart.length(list))
    }
}

class M3uTest {
    private fun t(id: String, title: String, artist: String, file: String) = Track(id, id, title, artist, "B", artist, 0, 200_000, "M", null, file)

    @Test fun roundTripAndMatchByFileOrName() {
        val a = t("content://a", "Paper Moon", "Velvet Hours", "Paper Moon.mp3")
        val b = t("srv:9", "Tides", "Lumen Trio", "tides.flac")
        val text = M3u.write("Night mix", listOf(a, b)) { if (it.id.startsWith("content:")) "Music/${it.fileName}" else null }
        val (name, entries) = M3u.parse(text, "x")
        assertEquals("Night mix", name)
        assertEquals(listOf(a.id, b.id), M3u.match(entries, listOf(a, b)))
        // A desktop file: absolute paths, matched by file name here.
        val desk = "#EXTM3U\n#EXTINF:200,Someone - Else\n/home/me/Music/Paper Moon.mp3\n"
        assertEquals(listOf(a.id), M3u.match(M3u.parse(desk, "d").second, listOf(a, b)))
    }

    @Test fun wantListIsArtistDashTitlePerLine() {
        val songs = listOf(RemoteTrack(1, "Paper Moon", "Velvet Hours", "A", null, null, 200), RemoteTrack(2, "Tides", "Lumen Trio", "B", null, null, 180))
        assertEquals("Velvet Hours - Paper Moon\nLumen Trio - Tides", M3u.wantList(songs))
        assertEquals("", M3u.wantList(emptyList()))
    }
}

class SuggestTest {
    private val now = 1_800_000_000_000L
    private fun play(artist: String, daysAgo: Int) = Discovery.Play("k$artist$daysAgo", artist, "T", now - daysAgo * 86_400_000L)

    @Test fun topArtistsAreRecentNamedAndOrdered() {
        val plays = listOf(play("Lumen Trio", 1), play("lumen trio", 2), play("Velvet Hours", 3), play("Unknown artist", 1),
            play("Unknown artist", 2), play("Old Band", 200), play("Old Band", 201))
        assertEquals(listOf("lumen trio", "Velvet Hours"), Smart.topArtists(plays, now, 5))
        assertEquals(listOf("Velvet Hours"), Smart.topArtists(plays, now, 5) { it.startsWith("lumen", true) })
    }

    @Test fun interleaveKeepsEverything() {
        assertEquals(listOf(1, 10, 11, 2, 12, 3), Smart.interleave(listOf(1, 2, 3), listOf(10, 11, 12)))
        assertEquals(listOf(10, 11), Smart.interleave(emptyList(), listOf(10, 11)))
    }

    @Test fun duplicatesGroupSameSong() {
        fun t(id: String, title: String, artist: String) = Track(id, id, title, artist, "B", artist, 0, 1, "M", null)
        val g = Smart.duplicates(listOf(t("1", "Tides", "Lumen"), t("2", "tides", "LUMEN"), t("3", "Other", "Lumen")))
        assertEquals(listOf(listOf("1", "2")), g.map { l -> l.map { it.id } })
    }
}
