package io.github.realkhfan.onda.ui

import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.core.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtonTracksTest {
    @Test fun driveSongsYouHaveHerePlayAsYoursTheRestThroughThePc() {
        val mine = Track("content://glass", "content://glass", "Glass", "Dunes", "Singles", "Dunes", 1, 200_000, "Music", null, "Dunes - Glass.mp3")
        val drive = listOf(LinkSync.Song(null, "Glass", "Dunes"), LinkSync.Song(null, "Sombrero", "Night Kites", "Sombrero", 180_000))
        val rows = protonTracks(drive, listOf(mine))
        assertEquals("content://glass", rows[0].id) // got onto the phone before: plays anywhere
        assertTrue(LinkSync.isMissing(rows[1].id)) // only on the PC's Drive: through the PC
        assertEquals(listOf("Sombrero", "Night Kites", "Sombrero", "180000"), rows[1].let { listOf(it.title, it.artist, it.album, it.durationMs.toString()) })
    }

    @Test fun thePcsDriveListTravelsInTheSync() {
        val list = listOf(LinkSync.Song(null, "Sombrero", "Night Kites", "Sombrero", 180_000))
        assertEquals(list, LinkSync.fromJson(LinkSync.toJson(LinkSync.Snapshot(proton = list))).proton)
        assertEquals(null, LinkSync.fromJson(LinkSync.toJson(LinkSync.Snapshot())).proton) // an older PC: nothing changes
    }
}
