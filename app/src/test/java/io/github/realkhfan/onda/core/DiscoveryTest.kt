package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DiscoveryTest {

    private val day = 86_400_000L
    private fun r(id: Long, title: String, artist: String, album: String = "A", preview: String? = "p") =
        RemoteTrack(id, title, artist, album, null, preview, 120)
    private fun t(artist: String) = Track("id-$artist", "u", "Song", artist, "A", artist, 0, 1000, "F", null)

    @Test fun periodsAreDailyAndIsoWeekly() {
        assertEquals("2026-11-15", Discovery.period(Discovery.Kind.DAILY, LocalDate.of(2026, 11, 15)))
        // Sunday 15 Nov and Monday 9 Nov share ISO week 46; Monday 16 Nov starts week 47.
        assertEquals("2026-W46", Discovery.period(Discovery.Kind.WEEKLY, LocalDate.of(2026, 11, 15)))
        assertEquals("2026-W46", Discovery.period(Discovery.Kind.WEEKLY, LocalDate.of(2026, 11, 9)))
        assertEquals("2026-W47", Discovery.period(Discovery.Kind.WEEKLY, LocalDate.of(2026, 11, 16)))
    }

    @Test fun recentPlaysAndLikesRankHighest() {
        val now = 100 * day
        val plays = List(5) { Discovery.Play("k$it", "Old Favourite", "x", now - 60 * day) } + // 5 plays, ~2 months old
            List(2) { Discovery.Play("n$it", "New Love", "y", now - day) }                      // 2 plays, yesterday
        val seeds = Discovery.seedArtists(plays, liked = listOf(t("Liked One")), wanted = emptyList(), library = emptyList(), nowMs = now)
        assertEquals(listOf("Liked One", "New Love", "Old Favourite"), seeds)
    }

    @Test fun unknownArtistIsNeverASeed() {
        assertTrue(Discovery.seedArtists(emptyList(), listOf(t("Unknown artist")), emptyList(), emptyList(), 0).isEmpty())
    }

    @Test fun unwantedVersionsAreLeftOutButNormalTitlesStay() {
        assertTrue(Discovery.isUnwanted("Song (Sped Up)", "A"))
        assertTrue(Discovery.isUnwanted("Song - Slowed + Reverb", "A"))
        assertTrue(Discovery.isUnwanted("Song [Nightcore]", "A"))
        assertTrue(Discovery.isUnwanted("Song (Someone Remix)", "A"))
        assertTrue(Discovery.isUnwanted("Song (Live)", "A"))
        assertTrue(Discovery.isUnwanted("Song", "Live at the Arena"))
        assertTrue(Discovery.isUnwanted("Song (Karaoke Version)", "A"))
        assertFalse(Discovery.isUnwanted("Live Forever", "A"))
        assertFalse(Discovery.isUnwanted("Remix Me", "A"))
        assertFalse(Discovery.isUnwanted("Song (Cover)", "A"))
        assertFalse(Discovery.isUnwanted("Song (Acoustic)", "A"))
    }

    @Test fun pickIsStableForAPeriodAndRespectsLimits() {
        val pool = (1L..40L).map { r(it, "Track $it", "Artist ${it % 8}") } +
            r(100, "Fast (Sped Up)", "Artist 1") + r(101, "No Clip", "Artist 2", preview = null)
        val a = Discovery.pick(pool, Discovery.Kind.DAILY, "2026-11-15") { false }
        val b = Discovery.pick(pool, Discovery.Kind.DAILY, "2026-11-15") { false }
        assertEquals(a, b) // the same day gives the same mix, on every device
        assertTrue(a.size <= 10)
        assertEquals(a.size, a.map { it.artist }.distinct().size) // Daily: one song per artist
        assertTrue(a.none { it.id == 100L || it.id == 101L })
        val weekly = Discovery.pick(pool, Discovery.Kind.WEEKLY, "2026-W46") { false }
        assertTrue(weekly.groupBy { it.artist }.values.all { it.size <= 2 })
    }

    @Test fun fewArtistsAllowMoreSongsEach() {
        val sevenArtists = (1L..35L).map { r(it, "T$it", "Artist ${it % 7}") }
        assertEquals(2, Discovery.perArtistFor(Discovery.Kind.DAILY, sevenArtists))
        assertEquals(10, Discovery.pick(sevenArtists, Discovery.Kind.DAILY, "d", 2) { false }.size)
        assertEquals(3, Discovery.perArtistFor(Discovery.Kind.WEEKLY, sevenArtists)) // capped at 3
        val manyArtists = (1L..40L).map { r(it, "T$it", "Artist $it") }
        assertEquals(1, Discovery.perArtistFor(Discovery.Kind.DAILY, manyArtists))
    }

    @Test fun pickSkipsSongsYouHaveOrPlayed() {
        val pool = listOf(r(1, "Owned", "X"), r(2, "Fresh", "Y"))
        assertEquals(listOf(2L), Discovery.pick(pool, Discovery.Kind.DAILY, "d") { it.id == 1L }.map { it.id })
    }
}
