package io.github.realkhfan.onda.player

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RemotePlayerTest {
    @Test fun showsWhatThePcPlays() {
        val p = RemotePlayer(Looper.getMainLooper(), CoroutineScope(Dispatchers.Unconfined))
        p.show(JSONObject().put("has", true).put("playing", true).put("key", "file:///a.flac").put("title", "Tides").put("artist", "Lumen Trio")
            .put("pos", 61_000).put("dur", 200_000).put("volume", 0.5)
            .put("next", JSONArray().put(JSONObject().put("key", "dz:7").put("title", "Paper Moon").put("artist", "Velvet Hours"))))
        assertEquals("Tides", p.mediaMetadata.title.toString())
        assertTrue(p.isPlaying)
        assertEquals(2, p.mediaItemCount)
        assertTrue(p.currentPosition >= 61_000)
        assertEquals(200_000L, p.duration)
        assertEquals(0.5f, p.volume, 0.001f)
        assertTrue(p.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT))
        p.show(JSONObject().put("has", false))
        assertEquals(Player.STATE_IDLE, p.playbackState)
    }

    @Test fun playerItemsBecomeSongsThePcCanFind() {
        val mine = MediaItem.Builder().setMediaId("content://9").setMediaMetadata(MediaMetadata.Builder().setTitle("Glass").setArtist("Other Band").build()).build()
        val preview = MediaItem.Builder().setMediaId("dz:7").setMediaMetadata(MediaMetadata.Builder().setTitle("Paper Moon").setArtist("Velvet Hours").build()).build()
        val q = RemotePlayer.queueOf(listOf(mine, preview), 5000)
        assertEquals(listOf(null, "dz:7"), q.songs.map { it.key })
        assertEquals("Glass", q.songs[0].title)
        assertEquals(listOf("dz:7"), q.catalog.map { it.key })
        assertEquals(5000L, q.positionMs)
    }
}
