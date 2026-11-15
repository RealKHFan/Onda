package io.github.realkhfan.onda.player

import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import io.github.realkhfan.onda.data.PlayLog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import io.github.realkhfan.onda.data.Server
import androidx.media3.common.Timeline
import androidx.media3.session.MediaController
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.data.FreeCatalogs
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.core.Track

private const val QUEUED = "onda.queued"

fun Track.toMediaItem(): MediaItem =
    MediaItem.Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(artUri?.let { Uri.parse(it) })
                .build(),
        )
        .build()

/** A 30-second catalog preview. Its id is the source-prefixed key, so it never collides with a local file. */
fun RemoteTrack.toMediaItem(previewUrl: String): MediaItem =
    MediaItem.Builder()
        .setMediaId(key)
        .setUri(previewUrl)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .setArtworkUri(cover?.let { Uri.parse(it) })
                .build(),
        )
        .build()

/**
 * A preview ready for a queue, or null if this song has none. Deezer previews go through [PreviewResolver]
 * (fresh link at play time); Apple's links don't expire, so they're used as they are.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun RemoteTrack.toPreviewItem(): MediaItem? {
    val p = preview ?: FreeCatalogs.stream(this) ?: return null
    return if (source == Source.DEEZER) toMediaItem(PreviewResolver.uriFor(id)) else toMediaItem(p)
}

private fun MediaItem.markQueued(): MediaItem =
    buildUpon()
        .setMediaMetadata(mediaMetadata.buildUpon().setExtras(Bundle().apply { putBoolean(QUEUED, true) }).build())
        .build()

private val MediaItem.isQueued: Boolean get() = mediaMetadata.extras?.getBoolean(QUEUED) == true

data class QueueItem(
    val index: Int,
    val title: String,
    val artist: String,
    val artUri: String?,
    val queued: Boolean,
    val isCurrent: Boolean,
)

/** Compose-friendly wrapper around the session's [MediaController]. */
class PlayerState(val controller: MediaController) {

    var isPlaying by mutableStateOf(false)
        private set
    var hasItem by mutableStateOf(false)
        private set
    var currentId by mutableStateOf<String?>(null)
        private set
    var title by mutableStateOf("")
        private set
    var artist by mutableStateOf("")
        private set
    var album by mutableStateOf("")
        private set
    var artUri by mutableStateOf<String?>(null)
        private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF)
        private set
    var durationMs by mutableLongStateOf(0L)
        private set

    /** Onda shuffles the list itself (ExoPlayer's shuffle stays off), so queued songs land where you expect. */
    var shuffle by mutableStateOf(false)
        private set

    /** "Playing from <kind>" / <label>, and the playlist id when playing from one. */
    var sourceKind by mutableStateOf("Library")
        private set
    var sourceLabel by mutableStateOf("Your library")
        private set
    var sourceId by mutableStateOf<String?>(null)
        private set

    /** A short reason when a song can't play; the UI shows it once, then calls [clearError]. */
    var error by mutableStateOf<String?>(null)
        private set

    fun clearError() { error = null }

    /** Changes whenever the list changes, so the queue screen refreshes. */
    var queueVersion by mutableIntStateOf(0)
        private set

    /** The online catalog the current song comes from; null for your own files and server. */
    val catalog: Source? get() = currentId?.let { Source.of(it) }
    /** A Deezer preview may be playing as a free full copy (see [PreviewResolver]). */
    val fullFrom: Source? get() = catalog?.takeIf { it.full } ?: PreviewResolver.fullCopyOf(currentId)
    val isPreview: Boolean get() = catalog != null && fullFrom == null

    /** The playing context in its original order, without queued songs. */
    private var context: List<MediaItem> = emptyList()
    private var lastIndex = 0

    // Play log for the mixes: a song counts once it has played 30 s, or most of a short one (like a preview).
    private var logKey: String? = null
    private var logArtist = ""
    private var logTitle = ""
    private var playedMs = 0L
    private var since = -1L
    private var logged = false

    private fun tally() {
        if (since >= 0) {
            val now = SystemClock.elapsedRealtime()
            playedMs += now - since
            since = now
        }
    }

    private fun maybeLog() {
        val key = logKey ?: return
        if (logged) return
        val need = if (durationMs > 0) minOf(30_000L, durationMs * 8 / 10) else 30_000L
        if (playedMs >= need) {
            PlayLog.add(key, logArtist, logTitle)
            logged = true
        }
    }

    private fun startTally(item: MediaItem?) {
        logKey = item?.mediaId
        logArtist = item?.mediaMetadata?.artist?.toString().orEmpty()
        logTitle = item?.mediaMetadata?.title?.toString().orEmpty()
        playedMs = 0
        logged = false
        since = if (controller.isPlaying) SystemClock.elapsedRealtime() else -1
    }

    init {
        controller.shuffleModeEnabled = false
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                tally()
                since = if (playing) SystemClock.elapsedRealtime() else -1
                maybeLog()
                isPlaying = playing
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { refresh() }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                tally(); maybeLog() // durationMs still belongs to the song that just ended here
                val now = controller.currentMediaItemIndex
                val prev = lastIndex
                val auto = reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
                if (auto) SleepTimer.songEnded(this@PlayerState)
                // A queued song that finished (or was skipped past) leaves the queue, like Spotify.
                // Otherwise it would come round again on every repeat of the playlist.
                if ((auto || now > prev) && prev != now && prev in 0 until controller.mediaItemCount &&
                    controller.getMediaItemAt(prev).isQueued
                ) {
                    controller.removeMediaItem(prev)
                }
                if (auto && now < prev && shuffle) rebuildUpcoming() // reshuffle after each full loop
                refresh()
                startTally(mediaItem)
            }
            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                refresh(); queueVersion++
            }
            override fun onRepeatModeChanged(mode: Int) { repeatMode = mode }
            override fun onPlayerError(e: PlaybackException) {
                error = when {
                    currentId?.startsWith(Server.PREFIX) == true -> "Can't reach your music server"
                    isPreview -> "Preview unavailable right now"
                    catalog != null -> "Can't stream this song right now"
                    else -> "Can't play this file"
                }
            }
        })
        refresh()
        isPlaying = controller.isPlaying
        repeatMode = controller.repeatMode
        startTally(controller.currentMediaItem)
    }

    private fun refresh() {
        hasItem = controller.mediaItemCount > 0
        currentId = controller.currentMediaItem?.mediaId
        lastIndex = controller.currentMediaItemIndex
        val mm = controller.mediaMetadata
        title = mm.title?.toString() ?: ""
        artist = mm.artist?.toString() ?: ""
        album = mm.albumTitle?.toString() ?: ""
        artUri = mm.artworkUri?.toString()
        durationMs = controller.duration.coerceAtLeast(0L)
    }

    fun position(): Long = controller.currentPosition.coerceAtLeast(0L)

    fun play(
        tracks: List<Track>, start: Int, kind: String, label: String,
        id: String? = null, shuffled: Boolean = shuffle,
    ) = playItems(tracks.map { it.toMediaItem() }, start, kind, label, id, shuffled)

    fun playItems(
        items: List<MediaItem>, start: Int, kind: String, label: String,
        id: String? = null, shuffled: Boolean = shuffle,
    ) {
        if (items.isEmpty()) return
        val s = start.coerceIn(0, items.lastIndex)
        context = items
        shuffle = shuffled
        sourceKind = kind; sourceLabel = label; sourceId = id
        // Shuffled: the tapped song first, the rest in random order.
        val list = if (shuffled) listOf(items[s]) + items.filterIndexed { i, _ -> i != s }.shuffled() else items
        controller.setMediaItems(list, if (shuffled) 0 else s, 0L)
        controller.prepare()
        controller.play()
    }

    /** Spotify order: after the current song and anything already queued, before the rest. */
    fun addToQueue(item: MediaItem) {
        if (controller.mediaItemCount == 0) return playItems(listOf(item), 0, "Queue", "Your queue")
        controller.addMediaItem(queueEnd(), item.markQueued())
    }

    fun playNext(item: MediaItem) {
        if (controller.mediaItemCount == 0) return playItems(listOf(item), 0, "Queue", "Your queue")
        controller.addMediaItem(controller.currentMediaItemIndex + 1, item.markQueued())
    }

    private fun queueEnd(): Int {
        var i = controller.currentMediaItemIndex + 1
        while (i < controller.mediaItemCount && controller.getMediaItemAt(i).isQueued) i++
        return i
    }

    /** The current song and everything after it; history is hidden, like Spotify. */
    fun queue(): List<QueueItem> {
        val cur = controller.currentMediaItemIndex
        if (controller.mediaItemCount == 0 || cur < 0) return emptyList()
        return (cur until controller.mediaItemCount).map { i ->
            val mi = controller.getMediaItemAt(i)
            QueueItem(
                index = i,
                title = mi.mediaMetadata.title?.toString() ?: "",
                artist = mi.mediaMetadata.artist?.toString() ?: "",
                artUri = mi.mediaMetadata.artworkUri?.toString(),
                queued = mi.isQueued,
                isCurrent = i == cur,
            )
        }
    }

    fun jumpTo(index: Int) {
        controller.seekTo(index, 0L)
        controller.play()
    }

    /** Drag in the queue screen; the playing song stays put. */
    fun move(from: Int, to: Int) {
        val cur = controller.currentMediaItemIndex
        if (from == cur || to == cur || from !in 0 until controller.mediaItemCount || to !in 0 until controller.mediaItemCount) return
        controller.moveMediaItem(from, to)
    }

    fun removeAt(index: Int) {
        if (index in 0 until controller.mediaItemCount && index != controller.currentMediaItemIndex) {
            controller.removeMediaItem(index)
        }
    }

    fun clearQueue() {
        val cur = controller.currentMediaItemIndex
        for (i in controller.mediaItemCount - 1 downTo cur + 1) {
            if (controller.getMediaItemAt(i).isQueued) controller.removeMediaItem(i)
        }
    }

    fun togglePlay() {
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun next() = controller.seekToNextMediaItem()

    fun prev() {
        if (controller.currentPosition > 3000) controller.seekTo(0) else controller.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) = controller.seekTo(ms)

    fun toggleShuffle() {
        shuffle = !shuffle
        val cur = controller.currentMediaItemIndex
        if (controller.mediaItemCount == 0 || cur < 0 || context.isEmpty()) return
        if (cur > 0) controller.removeMediaItems(0, cur) // history would duplicate the rebuilt list
        rebuildUpcoming()
        if (!shuffle) { // put back the songs before this one so Previous walks the original order
            val pos = context.indexOfFirst { it.mediaId == controller.currentMediaItem?.mediaId }
            if (pos > 0) controller.addMediaItems(0, context.take(pos))
        }
    }

    /** Replaces everything after the current song and its queued songs with the rest of the context. */
    private fun rebuildUpcoming() {
        val end = queueEnd()
        if (end < controller.mediaItemCount) controller.removeMediaItems(end, controller.mediaItemCount)
        val pos = context.indexOfFirst { it.mediaId == controller.currentMediaItem?.mediaId }
        val rest = when {
            shuffle -> context.filterIndexed { i, _ -> i != pos }.shuffled()
            pos >= 0 -> context.drop(pos + 1)
            else -> context
        }
        if (rest.isNotEmpty()) controller.addMediaItems(rest)
    }

    fun cycleRepeat() {
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}
