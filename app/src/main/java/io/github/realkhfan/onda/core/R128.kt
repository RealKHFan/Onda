package io.github.realkhfan.onda.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.tan

/**
 * Integrated loudness (EBU R128 / ITU-R BS.1770: K-weighting, 400 ms blocks, absolute and relative gate),
 * fed decoded samples a chunk at a time, so a whole song never has to sit in memory.
 */
class R128(private val rate: Int, private val channels: Int) {
    private val pre: DoubleArray
    private val rlb: DoubleArray
    private val state = Array(channels) { DoubleArray(8) } // two biquads (x1, x2, y1, y2) per channel
    private val step = rate / 10 // 100 ms
    private var inStep = 0
    private var sum = 0.0
    private val quarters = ArrayList<Double>() // mean square of each 100 ms, all channels summed
    var peak = 0.0; private set

    init {
        // libebur128's formulas, so any sample rate gets the right curve.
        var k = tan(PI * 1681.974450955533 / rate)
        val vh = 10.0.pow(3.999843853973347 / 20)
        val vb = vh.pow(0.4996667741545416)
        var q = 0.7071752369554196
        var a0 = 1 + k / q + k * k
        pre = doubleArrayOf((vh + vb * k / q + k * k) / a0, 2 * (k * k - vh) / a0, (vh - vb * k / q + k * k) / a0, 2 * (k * k - 1) / a0, (1 - k / q + k * k) / a0)
        k = tan(PI * 38.13547087602444 / rate)
        q = 0.5003270373238773
        a0 = 1 + k / q + k * k
        rlb = doubleArrayOf(1.0, -2.0, 1.0, 2 * (k * k - 1) / a0, (1 - k / q + k * k) / a0)
    }

    /** Interleaved samples in -1..1; [count] values (frames × channels). */
    fun add(samples: FloatArray, count: Int) {
        var i = 0
        while (i + channels <= count) {
            for (c in 0 until channels) {
                val x = samples[i + c].toDouble()
                if (abs(x) > peak) peak = abs(x)
                val s = state[c]
                val y1 = pre[0] * x + pre[1] * s[0] + pre[2] * s[1] - pre[3] * s[2] - pre[4] * s[3]
                s[1] = s[0]; s[0] = x; s[3] = s[2]; s[2] = y1
                val y2 = rlb[0] * y1 + rlb[1] * s[4] + rlb[2] * s[5] - rlb[3] * s[6] - rlb[4] * s[7]
                s[5] = s[4]; s[4] = y1; s[7] = s[6]; s[6] = y2
                sum += y2 * y2
            }
            if (++inStep == step) { quarters += sum / step; sum = 0.0; inStep = 0 }
            i += channels
        }
    }

    /** LUFS, or null for silence / less than 400 ms. */
    fun integrated(): Double? {
        if (quarters.size < 4) return null
        val blocks = DoubleArray(quarters.size - 3) { (quarters[it] + quarters[it + 1] + quarters[it + 2] + quarters[it + 3]) / 4 }
        fun lufs(e: Double) = -0.691 + 10 * log10(e)
        val loud = blocks.filter { it > 0 && lufs(it) > -70 }
        if (loud.isEmpty()) return null
        val gate = lufs(loud.average()) - 10
        val kept = loud.filter { lufs(it) > gate }
        return lufs(kept.average())
    }

    companion object {
        /** Onda's level: ReplayGain's reference (-18 LUFS) plus 4 dB, the same as the desktop. */
        const val TARGET = -14.0

        /** dB to apply to a song: to the target, never so much that its loudest sample clips. */
        fun gainDb(lufs: Double, peak: Double): Double =
            minOf(TARGET - lufs, -20 * log10(peak.coerceIn(0.05, 1.0))).coerceIn(-20.0, 12.0)

        /** An album's loudness: the power mean of its songs, so its quiet and loud songs keep their difference. */
        fun albumLufs(songs: List<Double>): Double? =
            songs.takeIf { it.size >= 2 }?.let { l -> 10 * log10(l.sumOf { 10.0.pow(it / 10) } / l.size) }
    }
}
