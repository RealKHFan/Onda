package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.Discovery
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenBrainzAndServerPlaylistTest {
    @Test fun musicBrainzTakesOnlyTheExactArtist() {
        val json = """{"artists":[{"id":"m-1","name":"Velvet Hours Tribute","score":100},{"id":"m-2","name":"Velvet Hours","score":97}]}"""
        assertEquals("m-2", ListenBrainz.parseMbid(json, "velvet hours"))
        assertNull(ListenBrainz.parseMbid("""{"artists":[{"id":"m-3","name":"Velvet Hours","score":60}]}""", "Velvet Hours"))
        assertEquals(listOf("Lumen Trio", "Coastline Kids"),
            ListenBrainz.parseSimilar("""[{"name":"Lumen Trio","score":900},{"name":"Coastline Kids","score":700},{"name":""}]"""))
    }

    @Test fun serverPlaylistsKeepTheirOrder() {
        val list = Subsonic.parsePlaylistList(JSONObject("""{"playlists":{"playlist":[{"id":"p1","name":"Road trip"},{"id":"p2","name":""}]}}"""))
        assertEquals(listOf("p1" to "Road trip", "p2" to "Playlist"), list)
        assertEquals(listOf("s3", "s1"), Subsonic.parsePlaylistSongs(JSONObject("""{"playlist":{"id":"p1","entry":[{"id":"s3"},{"id":"s1"}]}}""")))
        assertTrue(Subsonic.parsePlaylistSongs(JSONObject("""{"playlist":{"id":"p3"}}""")).isEmpty())
    }

    @Test fun searchWordsCanAskForVersions() {
        assertTrue(Discovery.asksFor("paper moon remix"))
        assertTrue(Discovery.asksFor("night drive sped up"))
        assertFalse(Discovery.asksFor("paper moon"))
    }
}
