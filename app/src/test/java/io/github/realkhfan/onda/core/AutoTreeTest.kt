package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoTreeTest {

    private fun t(id: String, title: String, artist: String, album: String = "A") =
        Track(id, "u", title, artist, album, artist, 0, 1000, "F", null)

    @Test fun entriesKeepTheirListEvenWithSeparatorsInKeys() {
        val album = "al:velvet hours|tides"
        val id = AutoTree.entry(album, "content://x/tree/a%3AMusic/document/a%3AMusic%2F1.mp3")
        assertEquals(album to "content://x/tree/a%3AMusic/document/a%3AMusic%2F1.mp3", AutoTree.split(id))
        assertNull(AutoTree.split("playlists"))
    }

    @Test fun searchNeedsEveryWordAndPutsExactTitlesFirst() {
        val lib = listOf(
            t("1", "Paper Moon Reprise", "Velvet Hours"),
            t("2", "Paper Moon", "Velvet Hours"),
            t("3", "Night Drive", "Velvet Hours", album = "Paper Moon"),
            t("4", "Paper", "Other Band"),
        )
        assertEquals(listOf("2", "1", "3"), AutoTree.search(lib, "paper moon").map { it.id })
        assertEquals(listOf("1", "2", "3"), AutoTree.search(lib, "Velvet Hours").map { it.id })
        assertTrue(AutoTree.search(lib, "  ").isEmpty())
    }
}
