package io.github.realkhfan.onda.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PlayCounterTest {
    private val exo = ExoPlayer.Builder(ApplicationProvider.getApplicationContext()).build()
    private var playing = true
    private val p = object : ForwardingPlayer(exo) { override fun isPlaying() = playing }
    private var clock = 0L
    private val logged = mutableListOf<String>()

    @After fun done() = exo.release()

    @Test fun eachLoopOfARepeatedSongCountsAndPausesDont() {
        val s1 = MediaItem.Builder().setMediaId("s1").setUri("file:///nowhere/1.mp3").build()
        exo.setMediaItem(s1)
        val c = PlayCounter(p, { clock }) { logged += it.mediaId }
        clock = 31_000; c.onMediaItemTransition(s1, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
        clock = 62_000; c.onMediaItemTransition(s1, Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT)
        assertEquals(listOf("s1", "s1"), logged)
        clock = 72_000; playing = false; c.onIsPlayingChanged(false) // 10 s, then a long pause
        clock = 500_000; playing = true; c.onIsPlayingChanged(true)
        clock = 510_000; c.onMediaItemTransition(s1, Player.MEDIA_ITEM_TRANSITION_REASON_SEEK) // 20 s in all: no play
        assertEquals(listOf("s1", "s1"), logged)
        clock = 540_000; playing = false; c.onIsPlayingChanged(false) // the last song ends after 30 s
        assertEquals(listOf("s1", "s1", "s1"), logged)
    }
}
