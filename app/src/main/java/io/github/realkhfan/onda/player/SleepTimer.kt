package io.github.realkhfan.onda.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Pause after some minutes (fading out over the last 30 seconds) or when the song ends, like the desktop. */
object SleepTimer {
    var endsAt by mutableLongStateOf(0L); private set
    var afterSong by mutableStateOf(false); private set
    private var job: Job? = null

    fun start(scope: CoroutineScope, player: PlayerState, minutes: Int) {
        cancel(player)
        endsAt = System.currentTimeMillis() + minutes * 60_000L
        job = scope.launch {
            while (true) {
                val left = endsAt - System.currentTimeMillis()
                if (left <= 0) { player.controller.pause(); break }
                player.controller.volume = (left / 30_000f).coerceIn(0f, 1f)
                delay(if (left < 30_000) 250 else 5_000)
            }
            endsAt = 0
            player.controller.volume = 1f // full again for next time
        }
    }

    fun endOfSong(player: PlayerState) { cancel(player); afterSong = true }

    /** Called when a song ends by itself: the next one waits, paused. */
    fun songEnded(player: PlayerState) { if (afterSong) { afterSong = false; player.controller.pause() } }

    fun cancel(player: PlayerState) {
        job?.cancel(); job = null
        endsAt = 0; afterSong = false
        player.controller.volume = 1f
    }

    val on: Boolean get() = endsAt > 0 || afterSong
}
