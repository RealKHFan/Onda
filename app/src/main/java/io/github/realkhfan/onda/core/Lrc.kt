package io.github.realkhfan.onda.core

data class LyricLine(val timeMs: Long, val text: String)

/** Parses LRC: "[mm:ss.xx] text", several stamps per line allowed; tags like [ar:...] are skipped. */
object Lrc {
    private val stamp = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    fun parse(lrc: String): List<LyricLine> {
        val out = ArrayList<LyricLine>()
        for (raw in lrc.lineSequence()) {
            val stamps = stamp.findAll(raw).toList()
            if (stamps.isEmpty()) continue
            val text = raw.substring(stamps.last().range.last + 1).trim()
            for (m in stamps) {
                val (mm, ss, frac) = m.destructured
                val fracMs = when (frac.length) {
                    0 -> 0
                    1 -> frac.toInt() * 100
                    2 -> frac.toInt() * 10
                    else -> frac.toInt()
                }
                out += LyricLine(mm.toLong() * 60_000 + ss.toLong() * 1000 + fracMs, text)
            }
        }
        return out.sortedBy { it.timeMs }
    }

    /** Index of the line being sung at [posMs], or -1 before the first line. */
    fun indexAt(lines: List<LyricLine>, posMs: Long): Int {
        var lo = 0
        var hi = lines.lastIndex
        var ans = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (lines[mid].timeMs <= posMs) { ans = mid; lo = mid + 1 } else hi = mid - 1
        }
        return ans
    }
}
