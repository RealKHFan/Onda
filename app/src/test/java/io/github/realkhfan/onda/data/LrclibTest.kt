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

    @Test fun parseReadsSyncedPlainAndInstrumental() {
        val l = Lrclib.parse(results.getJSONObject(1))
        assertEquals(listOf("synced a", "synced b"), l.synced.map { it.text })
        val none = Lrclib.parse(JSONObject("{}"))
        assertTrue(none.synced.isEmpty())
        assertNull(none.plain)
        assertTrue(Lrclib.parse(JSONObject("""{"instrumental":true}""")).instrumental)
    }
}
