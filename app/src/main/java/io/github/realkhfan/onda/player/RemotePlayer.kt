package io.github.realkhfan.onda.player

import android.net.Uri
import android.os.Looper
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.data.Link
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Whether the phone is a remote for Onda on your PC right now (like Spotify Connect). */
object Remote {
    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on.asStateFlow()
    fun start() { _on.value = true }
    fun stop() { _on.value = false }
}

/**
 * Stands in for the phone's player while music plays on the PC: Onda's player bar, the notification, the lock screen
 * and Bluetooth buttons then show the PC's song and control it. Asks the PC every second what it's playing.
 */
@UnstableApi
class RemotePlayer(looper: Looper, private val scope: CoroutineScope) : SimpleBasePlayer(looper) {
    private var s: JSONObject? = null
    private var at = 0L // when [s] came
    private var job: Job? = null
    private var misses = 0

    fun startPolling() {
        if (job?.isActive == true) return
        misses = 0
        job = scope.launch {
            while (true) {
                val got = Link.state()
                if (got == null) {
                    // Gone for 30 s (PC off, out of Wi-Fi): the phone is the player again.
                    if (++misses >= 30) { Remote.stop(); return@launch }
                } else { misses = 0; show(got) }
                delay(1000)
            }
        }
    }

    fun stopPolling() { job?.cancel(); job = null }

    /** The PC's latest answer (also used by tests). */
    internal fun show(state: JSONObject) { s = state; at = System.currentTimeMillis(); invalidateState() }

    override fun getState(): State {
        val st = s
        val commands = Player.Commands.Builder().addAll(
            COMMAND_PLAY_PAUSE, COMMAND_STOP, COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM, COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
            COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, COMMAND_GET_CURRENT_MEDIA_ITEM, COMMAND_GET_TIMELINE, COMMAND_GET_METADATA,
            COMMAND_SET_MEDIA_ITEM, COMMAND_CHANGE_MEDIA_ITEMS, COMMAND_GET_VOLUME, COMMAND_SET_VOLUME,
            COMMAND_SET_SHUFFLE_MODE, COMMAND_SET_REPEAT_MODE,
        ).build()
        val b = State.Builder().setAvailableCommands(commands).setVolume(st?.optDouble("volume", 1.0)?.toFloat() ?: 1f)
        if (st == null || !st.optBoolean("has")) return b.setPlaybackState(STATE_IDLE).build()
        val playing = st.optBoolean("playing")
        val items = listOf(st) + (st.optJSONArray("next")?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } }.orEmpty())
        val playlist = items.mapIndexed { i, o ->
            val meta = MediaMetadata.Builder().setTitle(o.optString("title")).setArtist(o.optString("artist")).setAlbumTitle(o.optString("album"))
                .setArtworkUri(o.optString("art").takeIf { it.startsWith("http") }?.let(Uri::parse)).build()
            MediaItemData.Builder("pc$i-${o.optString("key")}")
                .setMediaItem(MediaItem.Builder().setMediaId(o.optString("key").ifEmpty { "pc$i" }).setMediaMetadata(meta).build())
                .setDurationUs(o.optLong("dur").takeIf { it > 0 }?.times(1000) ?: C.TIME_UNSET).setIsSeekable(i == 0).build()
        }
        val pos = st.optLong("pos") + if (playing) System.currentTimeMillis() - at else 0
        return b.setPlaylist(playlist).setCurrentMediaItemIndex(0)
            .setShuffleModeEnabled(st.optBoolean("shuffle")).setRepeatMode(exoRepeat(st.optInt("repeat")))
            .setPlaybackState(STATE_READY).setPlayWhenReady(playing, PLAY_WHEN_READY_CHANGE_REASON_REMOTE)
            .setContentPositionMs(if (playing) PositionSupplier.getExtrapolating(pos, 1f) else PositionSupplier.getConstant(pos))
            .build()
    }

    private fun send(cmd: String, extra: JSONObject.() -> Unit = {}): ListenableFuture<*> {
        scope.launch { Link.command(cmd, extra)?.let(::show) }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> =
        if (playWhenReady == s?.optBoolean("playing")) Futures.immediateVoidFuture() else send("toggle")

    override fun handleStop(): ListenableFuture<*> = if (s?.optBoolean("playing") == true) send("toggle") else Futures.immediateVoidFuture()

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> = when (seekCommand) {
        COMMAND_SEEK_TO_NEXT, COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> send("next")
        COMMAND_SEEK_TO_PREVIOUS, COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> send("prev")
        else -> send("seek") { put("ms", positionMs.coerceAtLeast(0)) }
    }

    override fun handleSetVolume(volume: Float): ListenableFuture<*> = send("volume") { put("v", volume.toDouble()) }

    // The PC's shuffle and repeat; shown at once, the PC confirms with its next answer.
    override fun handleSetShuffleModeEnabled(shuffleModeEnabled: Boolean): ListenableFuture<*> {
        s = s?.let { JSONObject(it.toString()).put("shuffle", shuffleModeEnabled) }
        return send("shuffle") { put("on", shuffleModeEnabled) }
    }

    override fun handleSetRepeatMode(repeatMode: Int): ListenableFuture<*> {
        s = s?.let { JSONObject(it.toString()).put("repeat", pcRepeat(repeatMode)) }
        return send("repeat") { put("mode", pcRepeat(repeatMode)) }
    }

    // Playing something in Onda while it's a remote plays it on the PC, as with Spotify Connect, with the PC's shuffle and repeat.
    override fun handleSetMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long): ListenableFuture<*> {
        val from = if (startIndex == C.INDEX_UNSET) 0 else startIndex.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0))
        val shuffle = s?.optBoolean("shuffle") == true
        // Shuffled: the chosen song first and the rest mixed (the PC keeps the order it gets).
        val list = if (shuffle && mediaItems.isNotEmpty()) listOf(mediaItems[from]) + mediaItems.filterIndexed { i, _ -> i != from }.shuffled() else mediaItems
        val q = queueOf(list, if (startPositionMs == C.TIME_UNSET) 0 else startPositionMs)
            .copy(start = if (shuffle) 0 else from, shuffle = shuffle, repeat = s?.optInt("repeat") ?: 0)
        scope.launch { Link.playThere(q) }
        return Futures.immediateVoidFuture()
    }

    override fun handleAddMediaItems(index: Int, mediaItems: MutableList<MediaItem>): ListenableFuture<*> {
        val q = queueOf(mediaItems, 0)
        scope.launch { Link.addThere(q, next = index <= 1) }
        return Futures.immediateVoidFuture()
    }

    // The PC's queue is changed on the PC.
    override fun handleRemoveMediaItems(fromIndex: Int, toIndex: Int): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handleMoveMediaItems(fromIndex: Int, toIndex: Int, newIndex: Int): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handleReplaceMediaItems(fromIndex: Int, toIndex: Int, mediaItems: MutableList<MediaItem>): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handlePrepare(): ListenableFuture<*> = Futures.immediateVoidFuture()
    override fun handleRelease(): ListenableFuture<*> { stopPolling(); return Futures.immediateVoidFuture() }

    companion object {
        /** The PC counts repeat as 0 off, 1 all, 2 one. */
        fun exoRepeat(pc: Int) = when (pc) { 1 -> REPEAT_MODE_ALL; 2 -> REPEAT_MODE_ONE; else -> REPEAT_MODE_OFF }
        fun pcRepeat(exo: Int) = when (exo) { REPEAT_MODE_ALL -> 1; REPEAT_MODE_ONE -> 2; else -> 0 }

        /** Player items as songs the PC can find (catalog songs by key, yours by artist and title). */
        fun queueOf(items: List<MediaItem>, positionMs: Long): LinkSync.Queue {
            val songs = items.map { m ->
                val title = m.mediaMetadata.title?.toString().orEmpty()
                val artist = m.mediaMetadata.artist?.toString().orEmpty()
                LinkSync.Song(m.mediaId.takeIf { Source.of(it) != null }, title, artist, m.mediaMetadata.albumTitle?.toString().orEmpty())
            }
            val catalog = items.mapNotNull { m ->
                LinkSync.remoteOf(m.mediaId, m.mediaMetadata.title?.toString().orEmpty(), m.mediaMetadata.artist?.toString().orEmpty(),
                    m.mediaMetadata.albumTitle?.toString().orEmpty(), m.mediaMetadata.artworkUri?.toString())
            }
            return LinkSync.Queue(songs, catalog, positionMs)
        }
    }
}
