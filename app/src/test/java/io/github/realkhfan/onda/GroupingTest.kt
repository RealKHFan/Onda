package io.github.realkhfan.onda

import io.github.realkhfan.onda.core.Grouping
import io.github.realkhfan.onda.core.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupingTest {
    private fun t(title: String, artist: String, album: String, albumArtist: String = artist, no: Int = 0, folder: String = "Music") =
        Track("u:$title", "u:$title", title, artist, album, albumArtist, no, 1000, folder, null)

    private val tracks = listOf(
        t("B", "Alice", "First", no = 2),
        t("A", "Alice", "First", no = 1),
        t("C", "Bob", "Second"),
        t("D", "Alice", "Third", albumArtist = "Alice", folder = "Other"),
    )

    @Test fun songs_sorted_by_title() {
        assertEquals(listOf("A", "B", "C", "D"), Grouping.songsSorted(tracks).map { it.title })
    }

    @Test fun albums_group_and_order_tracks_by_number() {
        val albums = Grouping.albums(tracks)
        assertEquals(3, albums.size)
        val first = albums.first { it.title == "First" }
        assertEquals(listOf("A", "B"), first.tracks.map { it.title })
    }

    @Test fun artists_group() {
        val artists = Grouping.artists(tracks)
        assertEquals(2, artists.size)
        val alice = artists.first { it.name == "Alice" }
        assertEquals(2, alice.albumCount)
        assertEquals(3, alice.tracks.size)
    }

    @Test fun folders_group() {
        val folders = Grouping.folders(tracks)
        assertEquals(setOf("Music", "Other"), folders.map { it.name }.toSet())
    }
}
