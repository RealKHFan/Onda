package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

class LoudnessTest {
    /** Stereo sine, [db] dBFS peak on each channel, [seconds] long, then [silence] seconds of nothing. */
    private fun measure(rate: Int, db: Double, seconds: Double, silence: Double = 0.0): R128 {
        val m = R128(rate, 2)
        val a = 10.0.pow(db / 20)
        val n = (rate * seconds).toInt()
        val buf = FloatArray(4096)
        var i = 0
        while (i < n) {
            val frames = minOf(2048, n - i)
            for (f in 0 until frames) { val v = (a * sin(2 * PI * 1000 * (i + f) / rate)).toFloat(); buf[2 * f] = v; buf[2 * f + 1] = v }
            m.add(buf, frames * 2); i += frames
        }
        val zeros = FloatArray(4096)
        repeat((rate * silence / 2048).toInt()) { m.add(zeros, 4096) }
        return m
    }

    // EBU Tech 3341 case 1: a -23 dBFS stereo sine reads -23 LUFS.
    @Test fun sineReadsTheReferenceLevel() {
        assertEquals(-23.0, measure(48_000, -23.0, 10.0).integrated()!!, 0.1)
        assertEquals(-23.0, measure(44_100, -23.0, 10.0).integrated()!!, 0.1)
        assertEquals(-33.0, measure(44_100, -33.0, 10.0).integrated()!!, 0.1)
    }

    @Test fun silenceIsGatedOut() {
        assertEquals(-20.0, measure(48_000, -20.0, 5.0, silence = 20.0).integrated()!!, 0.25) // blocks across the end count, as in the standard
        assertNull(measure(48_000, -20.0, 0.0, silence = 5.0).integrated())
    }

    @Test fun gainKeepsPeaksAndRange() {
        assertEquals(-6.0, R128.gainDb(-8.0, 0.5), 0.01) // a loud song comes down
        assertEquals(3.0, R128.gainDb(-17.0, 0.5), 0.01) // a quiet one goes up…
        assertEquals(20 * kotlin.math.log10(1 / 0.9), R128.gainDb(-30.0, 0.9), 0.01) // …but never past clipping
        assertEquals(-14.0, R128.albumLufs(listOf(-14.0, -14.0))!!, 0.001)
        assertNull(R128.albumLufs(listOf(-10.0)))
    }

    @Test fun qualityBadge() {
        assertEquals("FLAC · 24-bit · 96 kHz", Quality.badge("flac", 2000, 96_000, 24))
        assertEquals("FLAC · 44.1 kHz", Quality.badge("FLAC", 0, 44_100, 0))
        assertEquals("MP3 · 320 kbps", Quality.badge("MP3", 320, 44_100, 0))
        assertEquals("AAC", Quality.formatOf("audio/mp4", "a.m4a"))
        assertEquals("OPUS", Quality.formatOf(null, "a.opus"))
    }
}
