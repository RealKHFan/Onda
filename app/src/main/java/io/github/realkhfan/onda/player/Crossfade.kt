package io.github.realkhfan.onda.player

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Crossfade: the next song starts on a second player while the current one fades out; when the current one ends,
 * the main player takes over at the same spot and the second one fades away (a fraction of a second).
 * Songs of the same album play gaplessly instead, as they were made.
 */
class Crossfade(
    private val main: ExoPlayer,
    private val helper: ExoPlayer,
    private val scope: CoroutineScope,
    private val seconds: () -> Int,
    /** The next song's volume (its loudness level). */
    private val levelOf: (MediaItem) -> Float,
) {
    private enum class Phase { IDLE, FADING, HANDOFF }
    private var phase = Phase.IDLE
    private var from = -1
    private var target = -1
    private var startedAt = 0L
    private var fadeMs = 0L
    private var level = 1f
    private var job: Job? = null

    fun start() {
        job = scope.launch { while (true) { delay(if (phase == Phase.IDLE) 250 else 30); runCatching { tick() } } }
    }

    fun release() { job?.cancel(); cancel(); helper.release() }

    /** You skipped, paused or changed the queue: the fade stops, the current song plays as it is. */
    fun cancel() {
        if (phase == Phase.IDLE) return
        helper.stop()
        Effects.crossOut = 1f
        phase = Phase.IDLE
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        when (phase) {
            Phase.IDLE -> {
                val f = seconds() * 1000L
                if (f <= 0 || !main.isPlaying || Remote.on.value || SleepTimer.afterSong) return
                val d = main.duration
                if (d == C.TIME_UNSET || d < f * 2) return
                val left = d - main.currentPosition
                if (left > f || left < 500) return
                val next = main.nextMediaItemIndex
                if (next == C.INDEX_UNSET || main.repeatMode == Player.REPEAT_MODE_ONE) return
                val a = main.currentMediaItem ?: return
                val b = main.getMediaItemAt(next)
                val album = a.mediaMetadata.albumTitle?.toString().orEmpty()
                if (album.isNotBlank() && album == b.mediaMetadata.albumTitle?.toString() && a.mediaMetadata.artist == b.mediaMetadata.artist) return
                level = levelOf(b)
                helper.setMediaItem(b)
                helper.volume = 0f
                helper.prepare()
                helper.play()
                from = main.currentMediaItemIndex; target = next; startedAt = now; fadeMs = left; phase = Phase.FADING
            }
            Phase.FADING -> {
                if (main.currentMediaItemIndex == target && main.currentMediaItemIndex != from) return handoff()
                if (!main.isPlaying || main.currentMediaItemIndex != from) return cancel()
                val t = ((now - startedAt).toFloat() / fadeMs).coerceIn(0f, 1f)
                Effects.crossOut = cos(t * PI / 2).toFloat()
                helper.volume = sin(t * PI / 2).toFloat() * level
            }
            Phase.HANDOFF -> {
                if (main.currentMediaItemIndex != target) return cancel()
                if (main.playbackState != Player.STATE_READY || !main.isPlaying) return
                // Both play the same spot now: swap over quickly.
                val t = ((now - startedAt) / 150f).coerceIn(0f, 1f)
                Effects.crossOut = t
                helper.volume = (1 - t) * level
                if (t >= 1f) { helper.stop(); Effects.crossOut = 1f; phase = Phase.IDLE }
            }
        }
    }

    private fun handoff() {
        val at = helper.currentPosition
        if (helper.playbackState != Player.STATE_READY || at < 300) return cancel() // the next song wasn't ready: it just starts
        Effects.crossOut = 0f
        main.seekTo(at + 60) // ~ the time the seek takes
        startedAt = System.currentTimeMillis() + 120
        phase = Phase.HANDOFF
    }
}
