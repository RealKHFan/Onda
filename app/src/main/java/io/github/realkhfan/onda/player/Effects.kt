package io.github.realkhfan.onda.player

import android.media.audiofx.Equalizer
import kotlin.math.abs
import kotlin.math.ln

/**
 * The equalizer: the desktop's 10 bands (30 Hz … 15 kHz) mapped onto the phone's own bands (often 5),
 * through Android's audio effect on the player's audio session.
 */
object Effects {
    val bandHz = listOf(30, 60, 120, 240, 480, 950, 1900, 3800, 7500, 15000)
    private var eq: Equalizer? = null
    private var gains: List<Double> = List(10) { 0.0 }

    /** Called by the playback service whenever the player's audio session starts or changes. */
    @Synchronized fun attach(sessionId: Int) {
        eq?.release()
        eq = runCatching { Equalizer(0, sessionId) }.getOrNull()
        apply(gains)
    }

    @Synchronized fun apply(newGains: List<Double>) {
        gains = newGains
        val e = eq ?: return
        runCatching {
            val flat = gains.all { it == 0.0 }
            e.enabled = !flat
            if (flat) return
            val (min, max) = e.bandLevelRange.let { it[0].toInt() to it[1].toInt() }
            for (b in 0 until e.numberOfBands) {
                val hz = e.getCenterFreq(b.toShort()) / 1000.0 // milliHertz
                e.setBandLevel(b.toShort(), (gainAt(hz, gains) * 100).toInt().coerceIn(min, max).toShort())
            }
        }
    }

    /** The 10-band curve at any frequency, read on a log scale between the two nearest bands. */
    fun gainAt(hz: Double, gains: List<Double>): Double {
        if (hz <= bandHz.first()) return gains.first()
        if (hz >= bandHz.last()) return gains.last()
        val i = bandHz.indexOfLast { it <= hz }
        val (a, b) = bandHz[i].toDouble() to bandHz[i + 1].toDouble()
        val t = (ln(hz) - ln(a)) / (ln(b) - ln(a))
        return gains[i] + (gains[i + 1] - gains[i]) * t
    }

    val active: Boolean get() = eq != null && gains.any { abs(it) > 0.01 }

    fun release() { eq?.release(); eq = null }
}
