package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LrcTest {

    // Invented placeholder lines, not real lyrics.
    private val sample = """
        [ar:Someone]
        [ti:Test]
        [00:01.50] first line
        [00:05.00][00:20.00] repeated line
        [00:10.123] third line
        [00:12] no fraction
        [00:15.00]
    """.trimIndent()

    @Test fun parsesStampsSortedAndSkipsTags() {
        val lines = Lrc.parse(sample)
        assertEquals(listOf(1500L, 5000L, 10123L, 12000L, 15000L, 20000L), lines.map { it.timeMs })
        assertEquals("repeated line", lines[1].text)
        assertEquals("repeated line", lines[5].text)
        assertEquals("", lines[4].text) // instrumental gap stays as an empty line
    }

    @Test fun indexAtFindsTheCurrentLine() {
        val lines = Lrc.parse(sample)
        assertEquals(-1, Lrc.indexAt(lines, 0))
        assertEquals(0, Lrc.indexAt(lines, 1500))
        assertEquals(1, Lrc.indexAt(lines, 9999))
        assertEquals(5, Lrc.indexAt(lines, 99_000))
        assertEquals(-1, Lrc.indexAt(emptyList(), 5000))
    }
}
