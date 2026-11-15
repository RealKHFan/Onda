package io.github.realkhfan.onda.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrclibTest {

    // Invented placeholder text, not real lyrics.
    private val results = JSONArray("""
        [
          {"id":1,"duration":200.0,"plainLyrics":"plain only","syncedLyrics":null,"instrumental":false},
          {"id":2,"duration":185.0,"plainLyrics":"x","syncedLyrics":"[00:01.00] synced a\n[00:03.00] synced b","instrumental":false},
          {"id":3,"duration":181.0,"plainLyrics":"y","syncedLyrics":null,"instrumental":false}
        ]
    """.trimIndent())

    @Test fun pickPrefersSyncedWithinTenSeconds() {
        assertEquals(2, Lrclib.pick(results, 180)?.optInt("id"))
    }

    @Test fun pickRejectsFarLengths() {
        assertNull(Lrclib.pick(results, 400))
    }

    @Test fun closestNeedsSameTitleAndArtist() {
        val arr = JSONArray("""[
          {"id":7,"trackName":"Other Song","artistName":"Band","duration":300.0,"syncedLyrics":"[00:01.00] a"},
          {"id":8,"trackName":"Song","artistName":"Band feat. Guest","duration":320.0,"plainLyrics":"p"},
          {"id":9,"trackName":"Song","artistName":"Band","duration":330.0,"syncedLyrics":"[00:01.00] b"}
        ]""")
        assertEquals(9, Lrclib.closest(arr, "Song", "Band")?.optInt("id"))
        assertNull(Lrclib.closest(arr, "Song", "Someone Else"))
    }

    @Test fun parseReadsVerifiedFlag() {
        assertTrue(Lrclib.parse(JSONObject("{}")).verified)
        assertEquals(false, Lrclib.parse(JSONObject("""{"ondaVerified":false}""")).verified)
    }

    @Test fun parseReadsSyncedPlainAndInstrumental() {
        val l = Lrclib.parse(results.getJSONObject(1))
        assertEquals(listOf("synced a", "synced b"), l.synced.map { it.text })
        val none = Lrclib.parse(JSONObject("{}"))
        assertTrue(none.synced.isEmpty())
        assertNull(none.plain)
        assertTrue(Lrclib.parse(JSONObject("""{"instrumental":true}""")).instrumental)
    }
}
