package io.github.realkhfan.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubsonicTest {

    private val cfg = Subsonic.Config("http://192.168.1.20:4533", "rkf", "c19b2d", "tok")

    @Test fun tokenMatchesTheSpecExample() {
        // Worked example from the OpenSubsonic docs: password "sesame", salt "c19b2d".
        assertEquals("26719a1196d2a940705a59634eb18eab", Subsonic.token("sesame", "c19b2d"))
    }

    @Test fun urlsCarryAuthAndQuality() {
        val orig = Subsonic.streamUrl(cfg, "abc", 0)
        assertTrue(orig.startsWith("http://192.168.1.20:4533/rest/stream?id=abc&u=rkf&t=tok&s=c19b2d"))
        assertFalse(orig.contains("maxBitRate"))
        assertTrue(Subsonic.streamUrl(cfg, "abc", 128).contains("maxBitRate=128&format=mp3"))
        assertTrue(Subsonic.coverUrl(cfg, "mf-1").contains("/rest/getCoverArt?id=mf-1&size=600"))
    }

    @Test fun parsesSongsAndSkipsFolders() {
        val r = Subsonic.unwrap("""
            {"subsonic-response":{"status":"ok","version":"1.16.1","searchResult3":{"song":[
              {"id":"s1","title":"First","artist":"Band","album":"Record","displayAlbumArtist":"Band & Friends",
               "track":3,"duration":201,"coverArt":"mf-1"},
              {"id":"d1","title":"Folder","isDir":true},
              {"id":"s2","title":"Second","displayArtist":"Solo"}
            ]}}}
        """.trimIndent())
        val songs = Subsonic.parseSongs(r)
        assertEquals(listOf("s1", "s2"), songs.map { it.id })
        assertEquals("Band & Friends", songs[0].albumArtist)
        assertEquals(201, songs[0].durationSec)
        assertEquals("Solo", songs[1].artist)
        assertNull(songs[1].coverArt)
    }

    @Test fun emptyLibraryIsEmpty() {
        assertTrue(Subsonic.parseSongs(Subsonic.unwrap("""{"subsonic-response":{"status":"ok","searchResult3":{}}}""")).isEmpty())
    }

    @Test fun wrongLoginIsCode40() {
        val e = runCatching {
            Subsonic.unwrap("""{"subsonic-response":{"status":"failed","error":{"code":40,"message":"Wrong username or password"}}}""")
        }.exceptionOrNull()
        assertEquals(40, (e as Subsonic.ApiError).code)
    }

    @Test fun normalizesPastedAddresses() {
        assertEquals("http://192.168.1.20:4533", Subsonic.normalizeUrl(" 192.168.1.20:4533/ "))
        assertEquals("https://music.example.org", Subsonic.normalizeUrl("https://music.example.org/app/#/album/123"))
        assertEquals("http://nas:4533", Subsonic.normalizeUrl("http://nas:4533/rest"))
    }

    @Test fun privateHostsAreRecognised() {
        listOf("http://192.168.1.20:4533", "http://10.0.0.2", "http://172.20.1.1", "http://100.101.1.2",
            "http://raspberrypi:4533", "http://pi.local:4533", "http://[fd00::5]:4533").forEach { assertTrue(it, Subsonic.isPrivateHost(it)) }
        listOf("http://203.0.113.9:4533", "http://music.example.org", "http://172.40.1.1").forEach { assertFalse(it, Subsonic.isPrivateHost(it)) }
    }
}
