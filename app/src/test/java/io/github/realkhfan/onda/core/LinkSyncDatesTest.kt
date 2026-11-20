package io.github.realkhfan.onda.core

import io.github.realkhfan.onda.core.LinkSync.Edit
import io.github.realkhfan.onda.core.LinkSync.Name
import io.github.realkhfan.onda.core.LinkSync.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Edits and renames: the newest wins, by date, whichever device made it. */
class LinkSyncDatesTest {
    private val k = LinkSync.idOf("Lumen Trio", "Tides")
    private fun e(title: String, at: Long) = Edit(title, "Lumen Trio", "Sea", null, 0, at)

    @Test fun theNewerEditWinsWhicheverSideMadeIt() {
        val base = Snapshot(edits = mapOf(k to e("Tides", 100)))
        // Edited on the PC; the phone still has the old one (even one it never took in): the PC's is newer.
        val pcNewer = LinkSync.merge(base, Snapshot(edits = mapOf(k to e("Tides (old)", 50))), Snapshot(edits = mapOf(k to e("Tides (PC)", 200))))
        assertEquals("Tides (PC)", pcNewer.edits[k]?.title)
        // Edited on both: the later one.
        assertEquals("Tides (phone)", LinkSync.merge(base, Snapshot(edits = mapOf(k to e("Tides (phone)", 300))), Snapshot(edits = mapOf(k to e("Tides (PC)", 200)))).edits[k]?.title)
        assertEquals("Tides (PC)", LinkSync.merge(base, Snapshot(edits = mapOf(k to e("Tides (phone)", 200))), Snapshot(edits = mapOf(k to e("Tides (PC)", 300)))).edits[k]?.title)
    }

    @Test fun aRestoreWinsOnlyOverOlderEdits() {
        val base = Snapshot(edits = mapOf(k to e("Tides", 100)))
        val restoredHere = Snapshot(restores = mapOf(k to 500L))
        assertNull(LinkSync.merge(base, restoredHere, Snapshot(edits = mapOf(k to e("Tides (PC)", 400)))).edits[k])
        assertEquals("Tides (PC)", LinkSync.merge(base, restoredHere, Snapshot(edits = mapOf(k to e("Tides (PC)", 600)))).edits[k]?.title)
    }

    @Test fun editsWithoutDatesKeepTheOldRule() {
        val base = Snapshot(edits = mapOf(k to e("Tides", 0)))
        assertEquals("PC", LinkSync.merge(base, Snapshot(edits = mapOf(k to e("Tides", 0))), Snapshot(edits = mapOf(k to e("PC", 0)))).edits[k]?.title)
        assertEquals("phone", LinkSync.merge(base, Snapshot(edits = mapOf(k to e("phone", 0))), Snapshot(edits = mapOf(k to e("PC", 0)))).edits[k]?.title)
        // Removed on one side (restored by an older Onda): gone.
        assertNull(LinkSync.merge(base, Snapshot(), Snapshot(edits = mapOf(k to e("Tides", 0)))).edits[k])
        // A dated edit is newer than any undated one.
        assertEquals("new", LinkSync.merge(base, Snapshot(edits = mapOf(k to e("Tides", 0))), Snapshot(edits = mapOf(k to e("new", 5)))).edits[k]?.title)
    }

    @Test fun theNewestRenameWinsAndAllOfItTravels() {
        val a = Snapshot(names = mapOf(k to Name("Lumen Trio - Tides", "Sea", 100)), edits = mapOf(k to e("Tides", 7)), restores = mapOf("x" to 9L))
        val b = Snapshot(names = mapOf(k to Name("Tides (Live)", "Sea", 200), "y" to Name("Other", "", 50)))
        val m = LinkSync.merge(null, a, b)
        assertEquals(mapOf(k to Name("Tides (Live)", "Sea", 200), "y" to Name("Other", "", 50)), m.names)
        assertEquals(a.names, LinkSync.merge(null, a, Snapshot()).names) // an older Onda sends none: ours are kept
        val round = LinkSync.fromJson(LinkSync.toJson(a))
        assertEquals(a.names, round.names)
        assertEquals(7L, round.edits[k]?.at)
        assertEquals(a.restores, round.restores)
    }

    @Test fun renamesAreDoneOnTheRightSongHere() {
        fun t(id: String, name: String, album: String = "Sea") = Track(id, id, "Tides", "Lumen Trio", album, "Lumen Trio", 0, 1, "", null, name)
        val mine = t("file:/m/01.flac", "01 tides.flac")
        val name = Name("Lumen Trio - Tides", "Sea", 200)
        // Its own extension, the same folder.
        assertEquals(listOf(FileNames.Rename(mine.id, "01 tides.flac", "Lumen Trio - Tides.flac")), LinkSync.renamesFor(mapOf(k to name), emptyMap(), listOf(mine)).map { it.first })
        // Done already (known as new as this), or named so already: nothing.
        assertEquals(0, LinkSync.renamesFor(mapOf(k to name), mapOf(k to name), listOf(mine)).size)
        assertEquals(0, LinkSync.renamesFor(mapOf(k to name), emptyMap(), listOf(t("file:/m/x.flac", "Lumen Trio - Tides.flac"))).size)
        // Two songs with that artist and title: the album tells them apart; can't tell: left alone.
        val live = t("file:/m/live.flac", "tides live.flac", album = "Live")
        assertEquals(listOf(mine.id), LinkSync.renamesFor(mapOf(k to name), emptyMap(), listOf(mine, live)).map { it.first.id })
        assertEquals(0, LinkSync.renamesFor(mapOf(k to name), emptyMap(), listOf(mine, t("file:/n/01.flac", "copy.flac"))).size)
    }
}
