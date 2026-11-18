package io.github.realkhfan.onda.player

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LastSessionTest {
    private fun song(id: String, title: String) = MediaItem.Builder().setMediaId(id).setUri("content://music/$id")
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist("Lumen Trio").setAlbumTitle("Night").build()).build()

    @Test fun whatPlayedComesBackAsItWas() {
        val a = song("a", "Tides"); val b = song("b", "Paper Moon").markQueued(); val c = song("c", "Glass")
        val saved = LastSession.Saved(listOf(a, b, c), 1, 61_000, 2, true, listOf(c, a), LastSession.Source("Playlist", "Road trip", "p1"))
        val f = File.createTempFile("session", ".json")
        LastSession.save(f, saved)
        val back = LastSession.load(f)!!
        assertEquals(listOf("a", "b", "c"), back.items.map { it.mediaId })
        assertEquals(listOf(false, true, false), back.items.map { it.isQueued })
        assertEquals("Paper Moon", back.items[1].mediaMetadata.title.toString())
        assertEquals("content://music/b", back.items[1].localConfiguration?.uri.toString())
        assertEquals(1, back.index); assertEquals(61_000L, back.positionMs); assertEquals(2, back.repeat)
        assertEquals(true, back.shuffle); assertEquals(listOf("c", "a"), back.context.map { it.mediaId })
        assertEquals(LastSession.Source("Playlist", "Road trip", "p1"), back.source)
    }

    @Test fun nothingToPutBackGivesNothing() {
        assertNull(LastSession.fromJson(JSONObject().put("items", JSONArray())))
        assertNull(LastSession.load(File("/nowhere/session.json")))
        // A song without an address is left out; the index stays in range.
        val o = JSONObject().put("items", JSONArray().put(JSONObject().put("id", "x")).put(JSONObject().put("id", "y").put("uri", "content://y"))).put("index", 5)
        val s = LastSession.fromJson(o)!!
        assertEquals(listOf("y"), s.items.map { it.mediaId })
        assertEquals(0, s.index)
    }
}
