package io.github.realkhfan.onda.core

import io.github.realkhfan.onda.data.UserCollection
import io.github.realkhfan.onda.ui.ReorderState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullCopyAndReorderTest {
    private fun r(title: String, artist: String, sec: Int, src: Source = Source.DEEZER, id: Long = 1) =
        RemoteTrack(id, title, artist, "", null, null, sec, src, ref = if (src.full) "x$id" else "")

    @Test fun onlyTheArtistsOwnSameRecordingCounts() {
        val p = r("Paper Moon (feat. Guest)", "Velvet Hours", 201)
        assertTrue(MetaMatch.isFullCopy(p, r("Paper Moon", "Velvet Hours", 204, Source.AUDIUS)))
        assertFalse(MetaMatch.isFullCopy(p, r("Paper Moon", "Velvet Hours Fan Page", 201, Source.AUDIUS))) // someone else's account
        assertFalse(MetaMatch.isFullCopy(p, r("Paper Moon (Remix)", "Velvet Hours", 201, Source.AUDIUS)))
        assertFalse(MetaMatch.isFullCopy(p, r("Paper Moon", "Velvet Hours", 260, Source.AUDIUS))) // another cut
        assertFalse(MetaMatch.isFullCopy(p, r("Paper Moons", "Velvet Hours", 201, Source.AUDIUS)))
    }

    @Test fun notInterestedHidesSongsAndArtists() {
        val song = r("Night Drive", "Lumen Trio", 180, id = 77)
        assertFalse(UserCollection.notWanted(song))
        UserCollection.toggleHidden(song.key)
        assertTrue(UserCollection.notWanted(song))
        UserCollection.toggleHidden(song.key)
        UserCollection.toggleBlocked("LUMEN trio")
        assertTrue(UserCollection.notWanted(r("Other", "Lumen Trio", 100, id = 78)))
        UserCollection.toggleBlocked("Lumen Trio")
        assertFalse(UserCollection.notWanted(song))
    }

    @Test fun dragTargetFollowsRowsAndStaysInTheList() {
        val s = ReorderState().apply { rowPx = 100f; dragging = 2 }
        s.offset = 140f; assertEquals(3, s.target(5))
        s.offset = -260f; assertEquals(0, s.target(5))
        s.offset = 900f; assertEquals(4, s.target(5))
    }
}
