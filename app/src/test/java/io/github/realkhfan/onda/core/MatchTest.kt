package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchTest {

    @Test fun normStripsBracketsFeatAndPunctuation() {
        assertEquals("dopa", Match.norm("Dopa (feat. Someone) [Remastered]"))
        assertEquals("song", Match.norm("Song feat. Other Artist"))
        assertEquals("crystals", Match.norm("CRYSTALS!"))
    }

    @Test fun normKeepsNonLatinScripts() {
        assertEquals("ドーパ", Match.norm("ドーパ"))
    }

    @Test fun sameMatchesCaseAndArtistLists() {
        assertTrue(Match.same("CRYSTALS", "PR1SVX", "Crystals", "PR1SVX"))
        assertTrue(Match.same("Dopa", "takayan", "Dopa", "takayan, Other"))
    }

    @Test fun mergeKeepsPrimaryAndAddsOnlyMissingSongs() {
        val dz = listOf(RemoteTrack(1, "Toy", "takayan", "", null, "p", 0))
        val apple = listOf(
            RemoteTrack(9, "Toy", "Takayan", "", null, "p", 0, Source.ITUNES), // same song, different catalog
            RemoteTrack(10, "Slave Dog", "Takayan", "", null, "p", 0, Source.ITUNES),
        )
        assertEquals(listOf("dz:1", "it:10"), Match.mergeTracks(dz, apple).map { it.key })
    }

    @Test fun localCopyWinsOverServerCopy() {
        fun t(id: String, title: String, artist: String) = Track(id, "u", title, artist, "A", artist, 0, 1000, "F", null)
        val local = listOf(t("content://1", "Toy", "takayan"))
        val server = listOf(t("srv:9", "Toy", "Takayan"), t("srv:10", "Underdog", "takayan"))
        assertEquals(listOf("content://1", "srv:10"), Match.mergeLibrary(local, server).map { it.id })
        assertEquals(local, Match.mergeLibrary(local, emptyList()))
    }

    @Test fun sourceKeysRoundTripAndIgnoreLocalIds() {
        assertEquals(Source.ITUNES to 10L, Source.parse("it:10"))
        assertEquals(Source.DEEZER to 5L, Source.parse("dz:5"))
        assertEquals(null, Source.parse("content://com.android.externalstorage/doc/1"))
    }

    @Test fun sameRejectsDifferentTitlesOrArtists() {
        assertFalse(Match.same("Dopa", "takayan", "Dopamine", "takayan"))
        assertFalse(Match.same("Dopa", "takayan", "Dopa", "Someone Else"))
        assertFalse(Match.same("", "takayan", "", "takayan"))
    }
}
