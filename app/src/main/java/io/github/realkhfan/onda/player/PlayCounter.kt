package io.github.realkhfan.onda.player

import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.github.realkhfan.onda.core.PlayRule

/**
 * Logs a play by [PlayRule] (30 s, or most of a short song; each loop again). It listens to the phone's own player in the
 * playback service, so it also counts with Onda's screen closed. While the phone controls your PC that player is paused,
 * and the PC counts what plays there.
 */
class PlayCounter(
    private val player: Player,
    private val now: () -> Long = SystemClock::elapsedRealtime,
    private val log: (MediaItem) -> Unit,
) : Player.Listener {
    private val rule = PlayRule()
    private var item: MediaItem? = null
    private var durationMs = 0L

    init { start(player.currentMediaItem) }

    private fun start(next: MediaItem?) {
        item = next
        durationMs = player.duration.coerceAtLeast(0) // unknown yet for a new song; known again when a song repeats
        rule.start(player.isPlaying, now())
    }

    private fun check() {
        val it = item ?: return
        if (rule.due(now(), durationMs)) log(it)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        player.duration.takeIf { it > 0 }?.let { durationMs = it }
        rule.playing(isPlaying, now())
        check()
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        check() // still the song that just ended (or looped)
        start(mediaItem)
    }
}
