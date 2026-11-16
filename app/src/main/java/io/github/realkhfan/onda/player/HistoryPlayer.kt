package io.github.realkhfan.onda.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * The player your controls reach (Onda's buttons, the notification, the lock screen, Bluetooth): Previous goes back to the
 * song that actually played before, not the one before it in the list (shuffle, jumps in the queue and new lists reorder that).
 */
@UnstableApi
class HistoryPlayer(private val inner: Player) : ForwardingPlayer(inner) {
    private val played = ArrayDeque<MediaItem>()
    private var last: MediaItem? = inner.currentMediaItem
    private var goingBack = false

    init {
        inner.addListener(object : Player.Listener {
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                val was = last
                last = item
                if (goingBack) { goingBack = false; return }
                if (was != null && item != null && was.mediaId != item.mediaId) {
                    played.addLast(was)
                    if (played.size > 200) played.removeFirst()
                }
            }
        })
    }

    override fun seekToPrevious() {
        if (currentPosition > 3000) seekTo(0) else seekToPreviousMediaItem()
    }

    override fun seekToPreviousMediaItem() {
        val back = played.removeLastOrNull()
        if (back == null) {
            // Nothing played before (just opened): the list's previous song, unless it's shuffled (that one never played).
            if (!Shuffle.on && hasPreviousMediaItem()) { goingBack = true; super.seekToPreviousMediaItem() } else seekTo(0)
            return
        }
        val cur = currentMediaItemIndex
        goingBack = true
        if (cur > 0 && getMediaItemAt(cur - 1).mediaId == back.mediaId) return super.seekToPreviousMediaItem()
        // Just before this song, so Next comes back here.
        addMediaItem(cur, back)
        seekTo(cur, 0)
    }
}
