package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class NewReleasesTest {

    private val today = LocalDate.of(2026, 11, 15)
    private fun a(id: Long, date: String?, type: String = "album") = RemoteAlbum(id, "R$id", "Band", null, date, type)

    @Test fun recentKeepsLastSixtyDaysNewestFirstWithoutCompilations() {
        val list = listOf(
            a(1, "2026-11-14"), a(2, "2026-09-01"), a(3, "2026-11-20"), // future-dated: not yet
            a(4, "2026-11-10", "compile"), a(5, "2026-10-30", "single"), a(6, null), a(1, "2026-11-14"),
        )
        assertEquals(listOf(1L, 5L), NewReleases.recent(list, today).map { it.id })
    }

    @Test fun notifiesOnlyUnseenReleasesFromTheLastThreeDays() {
        val recent = listOf(a(1, "2026-11-15"), a(2, "2026-11-13"), a(3, "2026-11-01"), a(4, "2026-11-14"))
        assertEquals(listOf(1L, 2L), NewReleases.toNotify(recent, alreadySeen = setOf(4L), today, firstCheck = false).map { it.id })
    }

    @Test fun firstCheckAnnouncesNothing() {
        assertTrue(NewReleases.toNotify(listOf(a(1, "2026-11-15")), emptySet(), today, firstCheck = true).isEmpty())
    }

    @Test fun agoReadsNaturally() {
        assertEquals("Today", NewReleases.ago(a(1, "2026-11-15"), today))
        assertEquals("Yesterday", NewReleases.ago(a(1, "2026-11-14"), today))
        assertEquals("4 days ago", NewReleases.ago(a(1, "2026-11-11"), today))
        assertEquals("30 Oct", NewReleases.ago(a(1, "2026-10-30"), today))
    }
}
