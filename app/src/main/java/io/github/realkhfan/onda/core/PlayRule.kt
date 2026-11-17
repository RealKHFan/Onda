package io.github.realkhfan.onda.core

/**
 * When listening counts as a play, the same on the phone and the PC: once a song has played 30 s, or most of a short one
 * (like a preview). A song that loops starts over, so each loop counts again; pauses don't count as listening.
 */
class PlayRule {
    private var playedMs = 0L
    private var since = -1L
    private var counted = false

    /** A song starts: a new one, or the same one again from the top. */
    fun start(playing: Boolean, now: Long) {
        playedMs = 0; counted = false
        since = if (playing) now else -1
    }

    /** Playing (or paused) from [now] on. */
    fun playing(on: Boolean, now: Long) {
        if (since >= 0) playedMs += now - since
        since = if (on) now else -1
    }

    /** True once per start, when the song has played long enough; [durationMs] 0 or less: not known yet. */
    fun due(now: Long, durationMs: Long): Boolean {
        playing(since >= 0, now)
        val need = if (durationMs > 0) minOf(30_000L, durationMs * 8 / 10) else 30_000L
        if (counted || playedMs < need) return false
        counted = true
        return true
    }
}
