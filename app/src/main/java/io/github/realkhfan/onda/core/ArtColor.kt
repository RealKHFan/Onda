package io.github.realkhfan.onda.core

import kotlin.math.abs

/** Picks a player background colour from cover art, the way Spotify tints its player. Pure Kotlin for JVM tests. */
object ArtColor {

    /** [pixels] are ARGB ints from a tiny scaled bitmap. Returns an opaque ARGB colour dark enough for white text. */
    fun pick(pixels: IntArray): Int {
        val weight = FloatArray(36)
        val rs = FloatArray(36)
        val gs = FloatArray(36)
        val bs = FloatArray(36)
        var ar = 0f
        var ag = 0f
        var ab = 0f
        for (p in pixels) {
            val r = (p shr 16 and 0xFF) / 255f
            val g = (p shr 8 and 0xFF) / 255f
            val b = (p and 0xFF) / 255f
            ar += r; ag += g; ab += b
            val (h, s, v) = hsv(r, g, b)
            if (s < 0.25f || v < 0.15f) continue // greys and near-black would wash the hue out
            val w = s * v
            val k = (h / 10f).toInt().coerceIn(0, 35)
            weight[k] += w; rs[k] += r * w; gs[k] += g * w; bs[k] += b * w
        }
        val n = pixels.size.coerceAtLeast(1).toFloat()
        val best = weight.indices.maxBy { weight[it] }
        val (r, g, b) = if (weight[best] > n * 0.02f) {
            Triple(rs[best] / weight[best], gs[best] / weight[best], bs[best] / weight[best])
        } else {
            Triple(ar / n, ag / n, ab / n) // colourless cover: fall back to its average grey
        }
        val (h, s, _) = hsv(r, g, b)
        return rgb(h, minOf(s, 0.65f), 0.42f)
    }

    fun hsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
        val max = maxOf(r, g, b)
        val d = max - minOf(r, g, b)
        val h = when {
            d == 0f -> 0f
            max == r -> 60f * (((g - b) / d) % 6f)
            max == g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }.let { if (it < 0f) it + 360f else it }
        return Triple(h, if (max == 0f) 0f else d / max, max)
    }

    fun rgb(h: Float, s: Float, v: Float): Int {
        val c = v * s
        val x = c * (1 - abs((h / 60f) % 2f - 1))
        val m = v - c
        val (r, g, b) = when ((h / 60f).toInt().coerceIn(0, 5)) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        fun ch(f: Float) = ((f + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }
}
