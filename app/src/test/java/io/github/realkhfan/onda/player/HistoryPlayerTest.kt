package io.github.realkhfan.onda.player

import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HistoryPlayerTest {
    private val exo = ExoPlayer.Builder(ApplicationProvider.getApplicationContext()).build()
    private val p = HistoryPlayer(exo)
    private fun song(n: Int) = MediaItem.Builder().setMediaId("s$n").setUri("file:///nowhere/$n.mp3").build()
    private fun cur() = p.currentMediaItem?.mediaId
    private fun list() = (0 until p.mediaItemCount).map { p.getMediaItemAt(it).mediaId }

    @After fun done() = exo.release()

    @Test fun previousGoesBackToWhatActuallyPlayed() {
        p.setMediaItems((1..6).map(::song), 0, 0)
        p.seekTo(4, 0) // a jump in the queue: s2-s4 never played
        p.seekToPrevious()
        assertEquals("s1", cur())
        p.seekToNext()
        assertEquals("s5", cur()) // Next comes back to where it was
        // Shuffle rebuilt the list: what played before is gone from it, and Previous still finds it.
        Shuffle.play(p, (1..6).map(::song), 4, shuffled = false)
        Shuffle.toggle(p)
        assertEquals(listOf("s5"), list().take(1))
        p.seekToNext()
        val shuffled = cur()
        p.seekToPrevious()
        assertEquals("s5", cur())
        p.seekToNext()
        assertEquals(shuffled, cur())
    }

    @Test fun withNothingPlayedBeforeItWalksTheListOnce() {
        Shuffle.play(p, (1..3).map(::song), 2, shuffled = false)
        p.seekToPrevious()
        assertEquals("s2", cur())
        p.seekToPrevious() // not back to s3: going back isn't playing something new
        assertEquals("s1", cur())
        p.seekToPrevious()
        assertEquals("s1", cur())
    }

    @Test fun aQueueFromThePcKeepsItsOrder() {
        Shuffle.play(p, listOf(song(9), song(8), song(7)), 1, shuffled = true, keepOrder = true)
        assertEquals(listOf("s9", "s8", "s7"), list())
        assertEquals("s8", cur())
        assertTrue(Shuffle.on)
        Shuffle.set(p, false) // the list's own order (it came already in play order)
        assertEquals("s8", cur())
        p.enqueue(listOf(song(1)), next = true)
        assertEquals("s1", p.getMediaItemAt(p.currentMediaItemIndex + 1).mediaId)
    }
}
