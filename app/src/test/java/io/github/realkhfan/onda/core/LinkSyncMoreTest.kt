package io.github.realkhfan.onda.core

import io.github.realkhfan.onda.core.LinkSync.Snapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LinkSyncMoreTest {
    private fun round(s: Snapshot) = LinkSync.fromJson(LinkSync.toJson(s))

    @Test fun pinsHiddenAndSearchesMergeLikeLists() {
        val song = LinkSync.Song(null, "Paper Moon", "Velvet Hours")
        val base = Snapshot(pins = listOf("p1"), hidden = emptyList(), searches = listOf("tides"))
        val phone = Snapshot(pins = listOf("p1", "p2"), hidden = listOf(song), searches = listOf("lumen", "tides"))
        val pc = Snapshot(pins = emptyList(), hidden = emptyList(), searches = listOf("velvet", "tides"))
        val m = LinkSync.merge(base, phone, pc)
        assertEquals(listOf("p2"), m.pins) // p1 unpinned on the PC, p2 pinned on the phone
        assertEquals(listOf(song), m.hidden)
        assertEquals(listOf("velvet", "lumen", "tides"), m.searches)
    }

    @Test fun settingsTakeThePcsTheFirstTimeThenWhoeverChangedOne() {
        val phone = mapOf("crossfade" to "0", "accent" to "1")
        val pc = mapOf("crossfade" to "6", "accent" to "2")
        val first = LinkSync.merge(Snapshot(), Snapshot(settings = phone), Snapshot(settings = pc)).settings!!
        assertEquals(pc, first)
        val base = Snapshot(settings = first)
        val later = LinkSync.merge(base, Snapshot(settings = first + ("accent" to "3")), Snapshot(settings = first + ("crossfade" to "4")))
        assertEquals(mapOf("crossfade" to "4", "accent" to "3"), later.settings)
        // A server login is only sent when there is one: the PC without one doesn't log the phone out; a logout does.
        val login = """{"url":"http://nas"}"""
        assertEquals(login, LinkSync.merge(Snapshot(), Snapshot(settings = phone + ("server" to login)), Snapshot(settings = pc)).settings!!["server"])
        val both = Snapshot(settings = pc + ("server" to login))
        assertNull(LinkSync.merge(both, both, Snapshot(settings = pc)).settings!!["server"])
    }

    @Test fun lyricsPicksMergePerSongAndARestoreRemovesOne() {
        val base = Snapshot(lyrics = mapOf("~a|x" to LinkSync.Lyric(5, 200), "~a|y" to LinkSync.Lyric(0, 300)))
        val phone = Snapshot(lyrics = mapOf("~a|x" to LinkSync.Lyric(5, 200))) // y put back as found
        val pc = Snapshot(lyrics = mapOf("~a|x" to LinkSync.Lyric(7, 0), "~a|y" to LinkSync.Lyric(0, 300)))
        assertEquals(mapOf("~a|x" to LinkSync.Lyric(7, 0)), LinkSync.merge(base, phone, pc).lyrics)
    }

    @Test fun addedKeepsTheEarliestDate() {
        val m = LinkSync.merge(null, Snapshot(added = mapOf("~a|x" to 500L, "~a|y" to 100L)), Snapshot(added = mapOf("~a|x" to 300L, "~b|z" to 50L)))
        assertEquals(mapOf("~a|x" to 300L, "~a|y" to 100L, "~b|z" to 50L), m.added)
    }

    @Test fun bothPlayTheSameMix() {
        fun mix(period: String, vararg titles: String) = LinkSync.Mix("DAILY", period,
            titles.mapIndexed { i, t -> RemoteTrack(i.toLong() + 1, t, "Lumen Trio", "Night", null, null, 180) })
        val yesterday = mix("2026-11-16", "old")
        val phone = mix("2026-11-17", "a"); val pc = mix("2026-11-17", "b")
        // Both built today's before syncing: the phone's. A newer day beats an older one.
        assertEquals(listOf(phone), LinkSync.merge(Snapshot(mixes = listOf(yesterday)), Snapshot(mixes = listOf(phone)), Snapshot(mixes = listOf(pc))).mixes)
        assertEquals(listOf(pc), LinkSync.merge(null, Snapshot(mixes = listOf(yesterday)), Snapshot(mixes = listOf(pc))).mixes)
        // Agreed on the phone's, then the PC built it anew: the PC's.
        assertEquals(listOf(pc), LinkSync.merge(Snapshot(mixes = listOf(phone)), Snapshot(mixes = listOf(phone)), Snapshot(mixes = listOf(pc))).mixes)
    }

    @Test fun theNewerLastPlayWinsAndSurvivesJson() {
        fun last(title: String, at: Long) = LinkSync.LastPlay(LinkSync.Queue(listOf(LinkSync.Song(null, title, "Lumen Trio")), emptyList(), 61_000, 0, true, 2), at)
        val m = LinkSync.merge(null, Snapshot(last = last("Tides", 100)), Snapshot(last = last("Glass", 200)))
        assertEquals("Glass", m.last!!.queue.songs.single().title)
        val r = round(m).last!!
        assertEquals(200L, r.at); assertEquals(61_000L, r.queue.positionMs); assertEquals(true, r.queue.shuffle); assertEquals(2, r.queue.repeat)
        assertNull(LinkSync.merge(null, Snapshot(), Snapshot()).last)
    }

    @Test fun aLongQueueIsSentAroundItsSong() {
        val songs = (0 until 2000).map { LinkSync.Song(null, "S$it", "A") }
        val (part, start) = LinkSync.around(songs, 1000)
        assertEquals(500, part.size); assertEquals("S950", part.first().title); assertEquals("S1000", part[start].title)
        val (end, s2) = LinkSync.around(songs, 1990)
        assertEquals("S1990", end[s2].title); assertEquals("S1999", end.last().title); assertEquals(500, end.size)
        val (short, s3) = LinkSync.around(songs.take(10), 3)
        assertEquals(10, short.size); assertEquals(3, s3)
    }

    @Test fun anOlderOndaLeavesThemAsTheyAre() {
        val phone = Snapshot(pins = listOf("p1"), settings = mapOf("crossfade" to "4"), searches = listOf("x"))
        val old = round(Snapshot()) // an Onda that doesn't send these
        assertNull(old.pins); assertNull(old.settings); assertNull(old.mixes)
        val m = LinkSync.merge(null, phone, old)
        assertEquals(listOf("p1"), m.pins)
        assertEquals(mapOf("crossfade" to "4"), m.settings)
    }

    @Test fun everythingSurvivesJson() {
        val s = Snapshot(
            pins = listOf("p1"), hidden = listOf(LinkSync.Song(null, "Paper Moon", "Velvet Hours")), searches = listOf("tides"),
            settings = mapOf("eq" to "0.0,1.5"), lyrics = mapOf("~a|x" to LinkSync.Lyric(7, -250)), added = mapOf("~a|x" to 42L),
            mixes = listOf(LinkSync.Mix("WEEKLY", "2026-W47", listOf(RemoteTrack(1, "A", "B", "C", null, null, 100)), listOf("Lumen Trio"))),
        )
        val r = round(s)
        assertEquals(s.pins, r.pins); assertEquals(s.hidden, r.hidden); assertEquals(s.searches, r.searches)
        assertEquals(s.settings, r.settings); assertEquals(s.lyrics, r.lyrics); assertEquals(s.added, r.added)
        assertEquals(s.mixes!!.single().basedOn, r.mixes!!.single().basedOn)
        assertEquals(s.mixes!!.single().tracks.map { it.title }, r.mixes!!.single().tracks.map { it.title })
    }
}
