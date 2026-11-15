package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class SmartTest {
    private fun t(id: String, title: String, ms: Long = 0) = Track(id, id, title, "A", "B", "A", 0, ms, "M", null)
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    @Test fun smartListsFollowYourPlays() {
        val a = t("a", "Alpha"); val b = t("b", "Bravo"); val c = t("c", "Charlie")
        val plays = listOf(Discovery.Play("a", "A", "Alpha", now - day), Discovery.Play("a", "A", "Alpha", now - 2 * day),
            Discovery.Play("b", "A", "Bravo", now - 90 * day))
        val by = mapOf("a" to a, "b" to b, "c" to c)
        assertEquals(listOf("a", "b"), Smart.list(Smart.Kind.TOP, listOf(a, b, c), plays, emptyList(), by::get, now).map { it.id })
        assertEquals(listOf("a"), Smart.list(Smart.Kind.REPEAT, listOf(a, b, c), plays, emptyList(), by::get, now).map { it.id })
        assertEquals(listOf("c"), Smart.list(Smart.Kind.NEVER, listOf(a, b, c), plays, emptyList(), by::get, now).map { it.id })
        assertEquals(listOf("c", "b"), Smart.list(Smart.Kind.FORGOTTEN, listOf(a, b, c), plays, listOf("a", "b", "c"), by::get, now).map { it.id })
    }

    @Test fun clockSortAndLength() {
        val (hours, days) = Smart.clock(listOf(Discovery.Play("a", "A", "x", 0)), ZoneOffset.UTC) // 1 Jan 1970, a Thursday, midnight
        assertEquals(1, hours[0]); assertEquals(1, days[3])
        val list = listOf(t("1", "b", 300_000), t("2", "a", 3_600_000))
        assertEquals(listOf("2", "1"), Smart.sorted(list, Smart.Sort.TITLE, emptyMap()).map { it.id })
        assertEquals("1 h 5 min", Smart.length(list))
    }
}
