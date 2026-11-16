package io.github.realkhfan.onda.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkSyncTest {
    private fun s(title: String, artist: String = "Lumen Trio") = LinkSync.Song(null, title, artist)
    private fun pl(id: String, name: String, vararg songs: LinkSync.Song) = LinkSync.Playlist(id, name, "", songs.toList())

    @Test fun firstSyncKeepsEverything() {
        val a = LinkSync.Snapshot(playlists = listOf(pl("p1", "Road trip", s("Tides"))), liked = listOf(s("Tides")))
        val b = LinkSync.Snapshot(playlists = listOf(pl("p2", "Focus", s("Glass"))), liked = listOf(s("Glass")))
        val m = LinkSync.merge(null, a, b)
        assertEquals(listOf("p1", "p2"), m.playlists.map { it.id })
        assertEquals(listOf("Glass", "Tides"), m.liked.map { it.title }) // the other side's new likes come first
    }

    @Test fun addsAndRemovalsOnEitherSideWin() {
        val base = LinkSync.Snapshot(playlists = listOf(pl("p1", "Road trip", s("A"), s("B"))), liked = listOf(s("A"), s("B")))
        // Phone: removed B from the playlist, renamed it. PC: added C, unliked A.
        val phone = base.copy(playlists = listOf(pl("p1", "Road trip 2", s("A"))))
        val pc = base.copy(playlists = listOf(pl("p1", "Road trip", s("A"), s("B"), s("C"))), liked = listOf(s("B")))
        val m = LinkSync.merge(base, phone, pc)
        assertEquals("Road trip 2", m.playlists.single().name)
        assertEquals(listOf("A", "C"), m.playlists.single().songs.map { it.title })
        assertEquals(listOf("B"), m.liked.map { it.title })
    }

    @Test fun deletedPlaylistStaysDeletedAndOrderFollowsTheSideThatChanged() {
        val base = LinkSync.Snapshot(playlists = listOf(pl("p1", "One", s("A"), s("B"), s("C")), pl("p2", "Two")))
        val phone = base.copy(playlists = listOf(base.playlists[0]))                       // deleted p2
        val pc = base.copy(playlists = listOf(pl("p1", "One", s("C"), s("A"), s("B")), base.playlists[1])) // reordered p1
        val m = LinkSync.merge(base, phone, pc)
        assertEquals(listOf("p1"), m.playlists.map { it.id })
        assertEquals(listOf("C", "A", "B"), m.playlists[0].songs.map { it.title })
    }

    @Test fun playsMergeOnceAndEditsFollowTheChange() {
        val p1 = Discovery.Play("", "Lumen Trio", "Tides", 1_000_000)
        val e0 = LinkSync.Edit("Tides", "Lumen Trio", "Sea", null, 0)
        val e1 = e0.copy(album = "Sea (Deluxe)")
        val base = LinkSync.Snapshot(edits = mapOf("~lumen trio|tides" to e0))
        val m = LinkSync.merge(base, base.copy(plays = listOf(p1)), base.copy(plays = listOf(p1.copy(key = "x")), edits = mapOf("~lumen trio|tides" to e1)))
        assertEquals(1, m.plays.size)
        assertEquals("Sea (Deluxe)", m.edits.values.single().album)
    }

    @Test fun jsonRoundTrip() {
        val snap = LinkSync.Snapshot(
            playlists = listOf(LinkSync.Playlist("p1", "Road trip", "for the drive", listOf(s("Tides"), LinkSync.Song("dz:7", "Paper Moon", "Velvet Hours")))),
            catalog = listOf(RemoteTrack(7, "Paper Moon", "Velvet Hours", "Moons", null, null, 180)),
            followed = listOf(RemoteArtist(3, "Velvet Hours", null)), blocked = listOf("someone"),
            plays = listOf(Discovery.Play("", "A", "B", 5)), edits = mapOf("~a|b" to LinkSync.Edit("B", "A", "C", null, 9)),
        )
        assertEquals(snap, LinkSync.fromJson(org.json.JSONObject(LinkSync.toJson(snap).toString())))
    }

    @Test fun libraryFindsSongsByNameAndKeepsMissingOnes() {
        val file = Track("content://1", "content://1", "Tides", "Lumen Trio", "Sea", "Lumen Trio", 1, 200_000, "M", null, "tides.flac")
        val server = file.copy(id = "srv:1", uri = "http://pi/stream")
        val lib = LinkSync.Library(listOf(server, file), listOf(file), emptyMap())
        assertEquals("content://1", lib.id(LinkSync.Song(null, "TIDES", "lumen trio")))
        val missing = lib.id(LinkSync.Song(null, "Glass", "Other Band"))
        assertEquals(LinkSync.Song(null, "Glass", "Other Band"), lib.song(missing))
        assertTrue(LinkSync.isMissing(missing))
        assertEquals("Glass" to "Other Band", LinkSync.missingTrack(missing)!!.let { it.title to it.artist })
        assertNull(LinkSync.missingTrack("content://1"))
        assertEquals("Tides", lib.song("content://1")?.title)
    }

    @Test fun cryptoRoundTripAndTamper() {
        val key = LinkCrypto.linkKey(LinkCrypto.newToken())
        val sealed = LinkCrypto.seal(key, "hello".toByteArray(), "/v1/x")
        assertArrayEquals("hello".toByteArray(), LinkCrypto.open(key, sealed, "/v1/x"))
        assertNull(LinkCrypto.open(key, sealed, "/v1/y")) // for another request
        assertNull(LinkCrypto.open(LinkCrypto.pairKey("123456"), sealed, "/v1/x"))
        sealed[sealed.size - 1] = (sealed.last() + 1).toByte()
        assertNull(LinkCrypto.open(key, sealed, "/v1/x"))
    }
}

class LinkSyncLibraryTest {
    @Test fun aSongMissingForAMomentIsStillSent() {
        val file = Track("content://1", "content://1", "Tides", "Lumen Trio", "Sea", "Lumen Trio", 1, 200_000, "M", null, "tides.flac")
        val first = LinkSync.Library(listOf(file), listOf(file), emptyMap())
        first.song("content://1")
        // Next time the scan isn't done yet: the song comes from what was learned.
        val later = LinkSync.Library(emptyList(), emptyList(), emptyMap(), known = first.learned)
        assertEquals("Tides", later.song("content://1")?.title)
        val json = LinkSync.songsToJson(first.learned)
        assertEquals(first.learned, LinkSync.songsFromJson(org.json.JSONObject(json.toString())))
    }
}
