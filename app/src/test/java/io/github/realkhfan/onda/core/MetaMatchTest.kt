package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetaMatchTest {

    private fun local(title: String, artist: String = "Unknown artist", album: String = "Unknown album", sec: Long = 180, art: String? = null) =
        Track("id", "uri", title, artist, album, artist, 0, sec * 1000, "Music", art)

    private fun remote(title: String, artist: String, sec: Int = 180, album: String = "Real Album", id: Long = 1) =
        RemoteTrack(id, title, artist, album, "https://img.example/c.jpg", "p", sec)

    @Test fun cleanedSplitsFileNamesAndDropsVideoJunk() {
        assertEquals("Dopa" to "takayan", MetaMatch.cleaned(local("takayan - Dopa (Official Video)")))
        assertEquals("Song [Live]" to "Band", MetaMatch.cleaned(local("Song [Live]", "Band")))
        assertEquals("takayan Dopa", MetaMatch.query(local("takayan - Dopa (Official Video)")))
    }

    @Test fun sameTitleArtistAndLengthIsConfident() {
        assertEquals(Confidence.HIGH, MetaMatch.judge(local("Dopa", "takayan", sec = 103), remote("Dopa", "Takayan", 104)))
    }

    @Test fun unknownArtistNeedsANearExactLength() {
        assertEquals(Confidence.HIGH, MetaMatch.judge(local("Dopa", sec = 103), remote("Dopa", "Takayan", 104)))
        assertEquals(Confidence.MAYBE, MetaMatch.judge(local("Dopa", sec = 103), remote("Dopa", "Takayan", 107)))
        assertNull(MetaMatch.judge(local("Dopa", sec = 103), remote("Dopa", "Takayan", 140)))
    }

    @Test fun sameSongAtAnotherLengthIsForReview() {
        assertEquals(Confidence.MAYBE, MetaMatch.judge(local("CRYSTALS", "PR1SVX", sec = 165), remote("CRYSTALS", "PR1SVX", 68)))
    }

    @Test fun spedUpOrRemixIsNeverConfident() {
        assertNull(MetaMatch.judge(local("Crystals", "Band", sec = 165), remote("Crystals (Sped Up)", "Band", 124)))
        assertEquals(Confidence.MAYBE, MetaMatch.judge(local("Crystals", "Band", sec = 165), remote("Crystals (Remix)", "Band", 166)))
        assertEquals(Confidence.HIGH, MetaMatch.judge(local("Crystals (Remix)", "Band", sec = 200), remote("Crystals (Remix)", "Band", 201)))
    }

    @Test fun differentSongIsRejected() {
        assertNull(MetaMatch.judge(local("Dopa", "takayan"), remote("Toy", "takayan")))
        assertNull(MetaMatch.judge(local("Dopa", "takayan", sec = 103), remote("Dopa", "Someone Else", 300)))
    }

    @Test fun bestPrefersConfidentThenCloserLength() {
        val l = local("Dopa", "takayan", sec = 103)
        val pick = MetaMatch.best(l, listOf(remote("Dopa", "Other", 106, id = 1), remote("Dopa", "takayan", 110, id = 2), remote("Dopa", "takayan", 104, id = 3)))
        assertEquals(3L, pick?.first?.id)
        assertEquals(Confidence.HIGH, pick?.second)
    }

    @Test fun differsOnlyWhenSomethingVisibleChanges() {
        val same = local("Dopa", "takayan", album = "Real Album", art = "file://art.jpg")
        assertFalse(MetaMatch.differs(same, remote("Dopa", "takayan")))
        assertTrue(MetaMatch.differs(local("Dopa", "takayan", album = "Unknown album", art = "file://a"), remote("Dopa", "takayan")))
        assertTrue(MetaMatch.differs(local("Dopa", "takayan", album = "Real Album"), remote("Dopa", "takayan"))) // gains a cover
    }
}
