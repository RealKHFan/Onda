package io.github.realkhfan.onda.player

import android.net.Uri
import android.os.Bundle
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
fun RemoteTrack.toPreviewItem(saved: Boolean = false): MediaItem? {
    // A song from Deezer itself without a link isn't streamed in your country; a saved one (playlists, Liked, mixes, songs
    // from the PC) has none because links expire, and gets a fresh one when it plays.
    if (source == Source.DEEZER && (preview != null || saved)) return toMediaItem(PreviewResolver.uriFor(id))
    return (preview ?: FreeCatalogs.stream(this))?.let { toMediaItem(it) }
}

/** For a saved song's row: a Deezer one can play (see [toPreviewItem]); null lets the row decide from what it has. */
val RemoteTrack.savedPlayable: Boolean? get() = if (source == Source.DEEZER) true else null

internal fun MediaItem.markQueued(): MediaItem =
    buildUpon()
        .setMediaMetadata(mediaMetadata.buildUpon().setExtras(Bundle().apply { putBoolean(QUEUED, true) }).build())
        .build()

internal val MediaItem.isQueued: Boolean get() = mediaMetadata.extras?.getBoolean(QUEUED) == true

data class QueueItem(
    val index: Int,
    /** A library id or a catalog key ("dz:123"). */
    val id: String,
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

    /** Shuffle here ([Shuffle]), or the PC's while this phone is its remote. */
    val shuffle: Boolean get() = if (Remote.on.value) remoteShuffle else Shuffle.on
    private var remoteShuffle by mutableStateOf(false)

    /** "Playing from <kind>" / <label>, and the playlist id when playing from one (as it was when Onda closed, at first). */
    var sourceKind by mutableStateOf(LastSession.source.kind)
        private set
    var sourceLabel by mutableStateOf(LastSession.source.label)
        private set
    var sourceId by mutableStateOf(LastSession.source.id)
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

    private var lastIndex = 0

    init {
        if (!Remote.on.value) controller.shuffleModeEnabled = false // Onda shuffles the list itself
        remoteShuffle = controller.shuffleModeEnabled
        controller.addListener(object : Player.Listener {
            override fun onShuffleModeEnabledChanged(enabled: Boolean) { remoteShuffle = enabled }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) { refresh() }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
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
                if (auto && now < prev && shuffle && !Remote.on.value) Shuffle.rebuildUpcoming(controller) // reshuffle after each full loop
                refresh()
            }
            override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) refresh() } // the length is known now
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
        id: String? = null, shuffled: Boolean = shuffle, keepOrder: Boolean = false,
    ) {
        if (items.isEmpty()) return
        sourceKind = kind; sourceLabel = label; sourceId = id
        LastSession.source = LastSession.Source(kind, label, id)
        if (Remote.on.value) { // plays on the PC, with the PC's own shuffle
            controller.setMediaItems(items, start.coerceIn(0, items.lastIndex), 0L)
            controller.prepare(); controller.play()
            return
        }
        Shuffle.play(controller, items, start, shuffled, keepOrder = keepOrder)
    }

    /** More of the same list at its end (radio): not "queued", so they play after your own queue. */
    fun append(items: List<MediaItem>) {
        if (items.isEmpty() || controller.mediaItemCount == 0) return
        Shuffle.append(items)
        controller.addMediaItems(items)
    }

    /** Spotify order: after the current song and anything already queued, before the rest. */
    fun addToQueue(item: MediaItem) {
        if (controller.mediaItemCount == 0) return playItems(listOf(item), 0, "Queue", "Your queue")
        controller.addMediaItem(controller.queueEnd(), item.markQueued())
    }

    fun playNext(item: MediaItem) {
        if (controller.mediaItemCount == 0) return playItems(listOf(item), 0, "Queue", "Your queue")
        controller.addMediaItem(controller.currentMediaItemIndex + 1, item.markQueued())
    }

    /** The current song and everything after it; history is hidden, like Spotify. */
    fun queue(): List<QueueItem> {
        val cur = controller.currentMediaItemIndex
        if (controller.mediaItemCount == 0 || cur < 0) return emptyList()
        return (cur until controller.mediaItemCount).map { i ->
            val mi = controller.getMediaItemAt(i)
            QueueItem(
                index = i,
                id = mi.mediaId,
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

    /** The song that played before (or the start of this one after 3 s); see [HistoryPlayer]. */
    fun prev() = controller.seekToPrevious()

    /** The song that played before, even after 3 s (a swipe on the mini player). */
    fun prevSong() = controller.seekToPreviousMediaItem()

    fun seekTo(ms: Long) = controller.seekTo(ms)

    fun toggleShuffle() {
        if (Remote.on.value) controller.shuffleModeEnabled = !remoteShuffle // the PC's shuffle
        else Shuffle.toggle(controller)
    }

    fun cycleRepeat() {
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }
}
