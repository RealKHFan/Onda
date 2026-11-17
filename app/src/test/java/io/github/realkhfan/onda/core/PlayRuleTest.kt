package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayRuleTest {
    @Test fun countsOnceAfterThirtySecondsOfListening() {
        val r = PlayRule()
        r.start(playing = true, now = 0)
        assertFalse(r.due(29_000, 200_000))
        assertTrue(r.due(30_000, 200_000))
        assertFalse(r.due(90_000, 200_000)) // once per start
    }

    @Test fun pausesDontCountAndShortSongsNeedMostOfThemselves() {
        val r = PlayRule()
        r.start(playing = true, now = 0)
        r.playing(false, 10_000)
        r.playing(true, 500_000) // paused for a long time
        assertFalse(r.due(519_000, 200_000)) // 29 s of listening
        assertTrue(r.due(520_000, 200_000))
        r.start(playing = true, now = 0) // a 20 s song: 16 s are enough
        assertFalse(r.due(15_000, 20_000))
        assertTrue(r.due(16_000, 20_000))
    }

    @Test fun eachLoopCountsAgain() {
        val r = PlayRule()
        var count = 0
        repeat(3) { loop -> r.start(playing = true, now = loop * 200_000L); if (r.due(loop * 200_000L + 199_000, 200_000)) count++ }
        assertEquals(3, count)
    }
}

class PlaysMergeTest {
    private fun play(at: Long) = Discovery.Play("", "Lumen Trio", "Short Song", at)

    @Test fun loopsOfAShortSongSurviveASyncAndTheSamePlayStaysOne() {
        val both = play(1_000)
        val merged = LinkSync.merge(null, LinkSync.Snapshot(plays = listOf(both, play(25_000))), LinkSync.Snapshot(plays = listOf(both, play(49_000))))
        assertEquals(listOf(1_000L, 25_000L, 49_000L), merged.plays.map { it.atMs })
    }
}
