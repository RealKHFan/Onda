package io.github.realkhfan.onda.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Which releases from your artists to show and which to notify about. Pure, for JVM tests. */
object NewReleases {

    /** Out in the last [days] days (not future-dated), compilations left out, newest first, each album once. */
    fun recent(albums: List<RemoteAlbum>, today: LocalDate, days: Long = 60): List<RemoteAlbum> {
        val from = today.minusDays(days)
        return albums
            .filter { a -> a.type != "compile" && dateOf(a)?.let { !it.isAfter(today) && !it.isBefore(from) } == true }
            .distinctBy { it.id }
            .sortedByDescending { it.date }
    }

    /**
     * New since the last check and out within the last 3 days. On the very first check nothing is announced,
     * so turning the feature on doesn't bring a burst of old releases.
     */
    fun toNotify(recent: List<RemoteAlbum>, alreadySeen: Set<Long>, today: LocalDate, firstCheck: Boolean): List<RemoteAlbum> =
        if (firstCheck) emptyList()
        else recent.filter { it.id !in alreadySeen && dateOf(it)?.let { d -> !d.isBefore(today.minusDays(3)) } == true }

    /** "Today", "Yesterday", "3 days ago", or a date for older ones. */
    fun ago(a: RemoteAlbum, today: LocalDate): String {
        val d = dateOf(a) ?: return ""
        return when (val n = ChronoUnit.DAYS.between(d, today)) {
            0L -> "Today"
            1L -> "Yesterday"
            in 2..6 -> "$n days ago"
            else -> "%d %s".format(d.dayOfMonth, d.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() })
        }
    }

    private fun dateOf(a: RemoteAlbum): LocalDate? = a.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
