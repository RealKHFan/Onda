package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Test

class DragScrollTest {
    private fun at(y: Float) = DragScroll.speed(y, top = 100f, bottom = 900f, zone = 80f, max = 20f)

    @Test fun aRowDraggedNearAnEdgeScrollsTheListFasterTheCloserItGets() {
        assertEquals(0f, at(500f), 0f) // the middle: no scrolling
        assertEquals(0f, at(180f), 0f) // just outside the zone
        assertEquals(-10f, at(140f), 0.01f) // halfway into the top zone: up at half speed
        assertEquals(-20f, at(50f), 0f) // above the list: full speed, no more
        assertEquals(10f, at(860f), 0.01f)
        assertEquals(20f, at(990f), 0f)
        assertEquals(0f, DragScroll.speed(Float.NaN, 100f, 900f, 80f, 20f), 0f) // no pointer yet
        assertEquals(0f, DragScroll.speed(120f, 100f, 200f, 80f, 20f), 0f) // a list too short for two zones
    }
}
