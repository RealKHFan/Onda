package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtColorTest {

    private fun r(c: Int) = c shr 16 and 0xFF
    private fun g(c: Int) = c shr 8 and 0xFF
    private fun b(c: Int) = c and 0xFF

    @Test fun greenCoverGivesDarkGreen() {
        val c = ArtColor.pick(IntArray(100) { 0xFF00C000.toInt() })
        assertTrue(g(c) > r(c) && g(c) > b(c))
        assertTrue("dark enough for white text", g(c) < 140)
    }

    @Test fun greyCoverGivesGrey() {
        val c = ArtColor.pick(IntArray(100) { 0xFF808080.toInt() })
        assertEquals(r(c), g(c))
        assertEquals(g(c), b(c))
    }

    @Test fun dominantHueWins() {
        val px = IntArray(100) { if (it < 70) 0xFFD02020.toInt() else 0xFF2020D0.toInt() }
        val c = ArtColor.pick(px)
        assertTrue(r(c) > b(c))
    }

    @Test fun emptyInputDoesNotCrash() {
        ArtColor.pick(IntArray(0))
    }
}
