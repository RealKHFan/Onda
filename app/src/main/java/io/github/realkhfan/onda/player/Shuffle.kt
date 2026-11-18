package io.github.realkhfan.onda.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player

/**
 * Onda shuffles the list itself (ExoPlayer's shuffle stays off), so queued songs land where you expect. Kept here, not in
 * a screen, so the playback service can switch it too (your PC as the remote) and a queue from the PC keeps it.
 */
object Shuffle {
    var on by mutableStateOf(false)
        private set

    /** The playing context in its original order, without queued songs. */
    var context: List<MediaItem> = emptyList()
        private set

    /** Shuffle as it was when Onda closed (the list itself is already in play order). */
    fun restore(context: List<MediaItem>, shuffled: Boolean) { this.context = context; on = shuffled }

    /** A new list from [start]: when [shuffled], the tapped song first and the rest mixed; [keepOrder]: it's already in play order. */
    fun play(p: Player, items: List<MediaItem>, start: Int, shuffled: Boolean, positionMs: Long = 0, keepOrder: Boolean = false) {
        if (items.isEmpty()) return
        val s = start.coerceIn(0, items.lastIndex)
        context = items
        on = shuffled
        val mix = shuffled && !keepOrder
        val list = if (mix) listOf(items[s]) + items.filterIndexed { i, _ -> i != s }.shuffled() else items
        p.setMediaItems(list, if (mix) 0 else s, positionMs)
        p.prepare()
        p.play()
    }

    /** More of the same list at its end (radio). */
    fun append(items: List<MediaItem>) { context = context + items }

    fun set(p: Player, shuffled: Boolean) { if (shuffled != on) toggle(p) }

    fun toggle(p: Player) {
        on = !on
        val cur = p.currentMediaItemIndex
        if (p.mediaItemCount == 0 || cur < 0 || context.isEmpty()) return
        if (cur > 0) p.removeMediaItems(0, cur) // history would duplicate the rebuilt list (Previous remembers what played)
        rebuildUpcoming(p)
        if (!on) { // put back the songs before this one, in the list's order
            val pos = context.indexOfFirst { it.mediaId == p.currentMediaItem?.mediaId }
            if (pos > 0) p.addMediaItems(0, context.take(pos))
        }
    }

    /** Replaces everything after the current song and its queued songs with the rest of the context. */
    fun rebuildUpcoming(p: Player) {
        val end = p.queueEnd()
        if (end < p.mediaItemCount) p.removeMediaItems(end, p.mediaItemCount)
        val pos = context.indexOfFirst { it.mediaId == p.currentMediaItem?.mediaId }
        val rest = when {
            on -> context.filterIndexed { i, _ -> i != pos }.shuffled()
            pos >= 0 -> context.drop(pos + 1)
            else -> context
        }
        if (rest.isNotEmpty()) p.addMediaItems(rest)
    }
}

/** After the current song and the songs queued behind it (Spotify's order for "Add to queue"). */
internal fun Player.queueEnd(): Int {
    var i = currentMediaItemIndex + 1
    while (i < mediaItemCount && getMediaItemAt(i).isQueued) i++
    return i
}

/** Adds songs as queued: right after the current song ([next]) or after the songs already queued. */
internal fun Player.enqueue(items: List<MediaItem>, next: Boolean) {
    if (items.isEmpty()) return
    addMediaItems(if (next) currentMediaItemIndex + 1 else queueEnd(), items.map { it.markQueued() })
}
