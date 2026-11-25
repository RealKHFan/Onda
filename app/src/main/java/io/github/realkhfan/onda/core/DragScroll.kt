package io.github.realkhfan.onda.core

/** How fast a list scrolls while a row is dragged near its edge (phone and PC). */
object DragScroll {
    /**
     * Pixels to scroll this frame for a pointer at [y] in a list from [top] to [bottom]: nothing in the middle, then faster
     * the deeper into the [zone] at an edge (or past it), up to [max]; negative scrolls up.
     */
    fun speed(y: Float, top: Float, bottom: Float, zone: Float, max: Float): Float {
        if (y.isNaN() || bottom - top < 2 * zone) return 0f
        val up = top + zone - y
        val down = y - (bottom - zone)
        return when {
            up > 0 -> -max * (up / zone).coerceAtMost(1f)
            down > 0 -> max * (down / zone).coerceAtMost(1f)
            else -> 0f
        }
    }
}
