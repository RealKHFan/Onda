package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistOrderTest {
    private val order = listOf("a", "b", "c", "d", "e") // d is in a folder, so not shown at the top level

    @Test fun movesWithinWhatIsShownAndSkipsHiddenOnes() {
        val shown = listOf("a", "b", "c", "e")
        assertEquals(listOf("b", "c", "d", "e", "a"), Grouping.dragPlaylist(order, emptyList(), shown, 0, 3).first) // down: after e
        assertEquals(listOf("e", "a", "b", "c", "d"), Grouping.dragPlaylist(order, emptyList(), shown, 3, 0).first) // up: before a
        assertEquals(listOf("a", "c", "b", "d", "e"), Grouping.dragPlaylist(order, emptyList(), shown, 1, 2).first)
    }

    @Test fun pinnedOnesMoveAmongThemselvesAndTheLineHolds() {
        val pins = listOf("c", "a") // shown first, in pin order
        val shown = listOf("c", "a", "b", "e")
        assertEquals(order to listOf("a", "c"), Grouping.dragPlaylist(order, pins, shown, 0, 1))
        assertEquals(order to listOf("a", "c"), Grouping.dragPlaylist(order, pins, shown, 0, 3)) // stops at the last pinned one
        assertEquals(listOf("a", "c", "d", "e", "b") to pins, Grouping.dragPlaylist(order, pins, shown, 2, 3))
        assertEquals(listOf("a", "e", "b", "c", "d") to pins, Grouping.dragPlaylist(order, pins, shown, 3, 0)) // stops at the first unpinned one
    }

    @Test fun nothingChangesForAMoveOutOfRange() {
        assertEquals(order to emptyList<String>(), Grouping.dragPlaylist(order, emptyList(), listOf("a", "b"), 5, 0))
        assertEquals(order, Grouping.moved(order, listOf("a", "b"), 0, 9))
    }

    @Test fun aReorderOnOneSideSyncs() {
        fun snap(vararg ids: String) = LinkSync.Snapshot(playlists = ids.map { LinkSync.Playlist(it, it, "", emptyList(), null, null) })
        val base = snap("a", "b", "c")
        assertEquals(listOf("c", "a", "b"), LinkSync.merge(base, snap("a", "b", "c"), snap("c", "a", "b")).playlists.map { it.id })
        assertEquals(listOf("b", "c", "a"), LinkSync.merge(base, snap("b", "c", "a"), snap("a", "b", "c")).playlists.map { it.id })
    }
}
