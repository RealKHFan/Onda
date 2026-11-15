package io.github.realkhfan.onda.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.freedesktop.gstreamer.Bus
import org.freedesktop.gstreamer.ElementFactory
import org.freedesktop.gstreamer.Format
import org.freedesktop.gstreamer.Gst
import org.freedesktop.gstreamer.Version
import org.freedesktop.gstreamer.elements.PlayBin
import org.freedesktop.gstreamer.event.SeekFlags
import java.net.URI
import java.util.EnumSet
import java.util.concurrent.TimeUnit

/** Plays through the system's GStreamer (files, your server, previews). All calls on the UI thread. */
class Player(private val scope: CoroutineScope, audioSink: String? = null) {

    /** [deezerId] > 0: a Deezer song whose link is fetched right before it plays (links expire). */
    data class Item(
        val id: String, val title: String, val artist: String, val album: String, val art: String?, val uri: String,
        val preview: Boolean = false, val deezerId: Long = 0, val durationMs: Long = 0,
    )

    /** What actually plays: the item's own link, a fresh preview link, or a free full copy ([fullFrom] names the catalog). */
    data class Resolved(val uri: String, val fullFrom: String? = null)

    var resolver: suspend (Item) -> Resolved = { Resolved(it.uri) }

    /** Volume factor per song (normalisation); 1 = as recorded. */
    var gain: (Item) -> Double = { 1.0 }

    /** Told about the next song once one starts, so its audio link can be looked up ahead of time. */
    var prefetch: ((Item) -> Unit)? = null

    var queue by mutableStateOf<List<Item>>(emptyList()); private set
    var index by mutableIntStateOf(-1); private set
    var playing by mutableStateOf(false); private set
    var positionMs by mutableLongStateOf(0L); private set
    var durationMs by mutableLongStateOf(0L); private set
    var shuffle by mutableStateOf(false); private set
    /** 0 off, 1 all, 2 one. */
    var repeat by mutableIntStateOf(0); private set
    /** The slider's position (0..1); see [applyVolume] for what reaches the speakers. */
    var volume by mutableDoubleStateOf(0.8); private set
    /** Set while a preview plays as a free full copy. */
    var fullFrom by mutableStateOf<String?>(null); private set
    var error by mutableStateOf<String?>(null)

    private var order by mutableStateOf<List<Int>>(emptyList())

    /** Gapless: the next file starts with no pause between songs (not for previews, whose link is fetched first). */
    var gapless = true
    /** Set on GStreamer's thread when the next song was queued up gaplessly; the switch shows once it plays. */
    @Volatile private var gaplessNext = -1

    /** Sleep timer: pause at this time (ms since epoch), or 0. */
    var sleepAt by mutableLongStateOf(0L)
    /** Sleep timer: stop when the current song ends. */
    var sleepAfterSong by mutableStateOf(false)

    /** Called when the last song starts (repeat off): "similar songs when the queue ends" adds more. */
    var onLastSong: (() -> Unit)? = null
    private var loadToken = 0
    private var pendingSeek = -1L
    private val bin: PlayBin
    /** Null when the system has no equalizer element (gst-plugins-good). */
    private val eq: org.freedesktop.gstreamer.Element?

    val current: Item? get() = queue.getOrNull(index)

    init {
        Gst.init(Version.of(1, 16), "Onda")
        bin = PlayBin("onda")
        bin.setVideoSink(ElementFactory.make("fakesink", "novideo"))
        // Tests: a silent sink that still keeps real time, like speakers do.
        if (audioSink != null) bin.setAudioSink(ElementFactory.make(audioSink, "audio").also { it.set("sync", true) })
        eq = runCatching { ElementFactory.make("equalizer-10bands", "eq") }.getOrNull()?.also { bin.set("audio-filter", it) }
        bin.bus.connect(Bus.EOS { _ -> scope.launch { gaplessNext = -1; next(auto = true) } })
        // Called on GStreamer's thread shortly before the end: the next file is handed over now, so there's no gap.
        bin.connect(PlayBin.ABOUT_TO_FINISH { _ ->
            gaplessTarget()?.let { i -> gaplessNext = i; bin.set("uri", queue[i].uri) }
        })
        bin.bus.connect(Bus.ERROR { _, _, message ->
            scope.launch {
                error = "Couldn't play “${current?.title.orEmpty()}”: $message"
                next(auto = true, afterError = true)
            }
        })
        scope.launch {
            while (true) {
                delay(if (gaplessNext >= 0) 50 else 400)
                if (gaplessNext >= 0 && bin.get("current-uri") == queue.getOrNull(gaplessNext)?.uri) switchGapless()
                if (sleepAt in 1..System.currentTimeMillis()) { sleepAt = 0; pause() }
                if (index < 0) continue
                val d = bin.queryDuration(TimeUnit.MILLISECONDS)
                if (d > 0) durationMs = d
                if (pendingSeek >= 0 && d > 0) { seek(pendingSeek); pendingSeek = -1 } // a resumed song, once it's ready
                else positionMs = bin.queryPosition(TimeUnit.MILLISECONDS).coerceAtLeast(0)
            }
        }
    }

    fun play(items: List<Item>, start: Int) {
        if (items.isEmpty()) return
        queue = items
        index = start.coerceIn(items.indices)
        order = makeOrder()
        playing = true
        load()
    }

    /** Last session, paused at [positionMs]. */
    fun restore(items: List<Item>, at: Int, positionMs: Long, shuffle: Boolean, repeat: Int) {
        if (items.isEmpty()) return
        queue = items; index = at.coerceIn(items.indices); this.shuffle = shuffle; this.repeat = repeat
        order = makeOrder()
        playing = false
        load(seekTo = positionMs)
    }

    /** Adds after the songs already waiting after the current one. */
    fun enqueue(item: Item) = insert(item, atEnd = true)

    /** Plays right after the current song. */
    fun playNext(item: Item) = insert(item, atEnd = false)

    private fun insert(item: Item, atEnd: Boolean) {
        if (index < 0) return play(listOf(item), 0)
        queue = queue + item
        val at = if (atEnd) order.size else order.indexOf(index) + 1
        order = order.toMutableList().apply { add(at.coerceIn(0, size), queue.lastIndex) }
    }

    fun toggle() {
        if (index < 0) return
        playing = !playing
        if (playing) bin.play() else bin.pause()
    }

    fun resume() { if (!playing) toggle() }
    fun pause() { if (playing) toggle() }

    fun next(auto: Boolean = false, afterError: Boolean = false) {
        if (index < 0) return
        if (auto && sleepAfterSong) { sleepAfterSong = false; playing = false } // the next song waits, paused
        if (auto && repeat == 2 && !afterError) return load()
        val pos = order.indexOf(index)
        when {
            pos + 1 < order.size -> index = order[pos + 1]
            repeat == 1 && !afterError -> { order = makeOrder(first = null); index = order.first() } // a new round, reshuffled
            else -> { stop(); return }
        }
        load()
    }

    fun previous() {
        if (index < 0) return
        if (positionMs > 3000) return seek(0)
        val pos = order.indexOf(index)
        if (pos > 0) { index = order[pos - 1]; load() } else seek(0)
    }

    fun jumpTo(i: Int) { if (i in queue.indices) { index = i; playing = true; load() } }

    fun seek(ms: Long) {
        bin.seekSimple(Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE), ms.coerceAtLeast(0) * 1_000_000)
        positionMs = ms.coerceAtLeast(0)
    }

    fun changeVolume(v: Double) { volume = v.coerceIn(0.0, 1.0); applyVolume() }

    // Ears hear loudness logarithmically: like KDE's and GNOME's sliders, the slider is cubic (10% ≈ -60 dB, 50% ≈ -18 dB).
    private fun applyVolume() { bin.volume = volume * volume * volume * (current?.let(gain) ?: 1.0) }

    /** After the normalisation setting changed. */
    fun refreshVolume() = applyVolume()

    fun toggleShuffle() { shuffle = !shuffle; order = makeOrder() }
    fun cycleRepeat() { repeat = (repeat + 1) % 3 }
    fun shuffleOn(on: Boolean) { if (on != shuffle) toggleShuffle() }
    fun repeatMode(r: Int) { repeat = r.coerceIn(0, 2) }

    /** Adds songs at the end (similar songs after the queue). */
    fun append(items: List<Item>) {
        if (items.isEmpty()) return
        val start = queue.size
        queue = queue + items
        order = order + items.indices.map { start + it }
    }

    /** Takes a waiting song out of the queue (never the one playing). */
    fun removeAt(i: Int) {
        if (i == index || i !in queue.indices) return
        queue = queue.filterIndexed { k, _ -> k != i }
        order = order.filter { it != i }.map { if (it > i) it - 1 else it }
        if (index > i) index--
    }

    /** Moves a song within "Next" ([from] and [to] count from the first waiting song). */
    fun moveUpcoming(from: Int, to: Int) {
        val cur = order.indexOf(index) + 1
        val up = order.drop(cur).toMutableList()
        if (from !in up.indices || to !in up.indices) return
        up.add(to, up.removeAt(from))
        order = order.take(cur) + up
    }

    /** Keeps only the song playing. */
    fun clearUpcoming() {
        val cur = current ?: return
        queue = listOf(cur); order = listOf(0); index = 0
    }

    /** Songs still to come, in play order. */
    fun upcoming(): List<Pair<Int, Item>> = order.drop(order.indexOf(index) + 1).map { it to queue[it] }

    private fun makeOrder(first: Int? = index): List<Int> {
        val all = queue.indices.toList()
        if (!shuffle) return all
        val rest = all.filter { it != first }.shuffled()
        return if (first != null && first >= 0) listOf(first) + rest else rest
    }

    private fun load(seekTo: Long = -1) {
        val item = current ?: return
        val token = ++loadToken
        gaplessNext = -1
        error = null; fullFrom = null
        bin.stop()
        positionMs = seekTo.coerceAtLeast(0)
        // A catalog song may turn out to be a 30-second preview: its full length would show until then.
        durationMs = if (item.preview) 0 else item.durationMs
        scope.launch {
            val r = try {
                withContext(Dispatchers.IO) { resolver(item) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (token != loadToken) return@launch // skipped meanwhile
            if (r == null) {
                error = "Couldn't play “${item.title}” right now."
                return@launch next(auto = true, afterError = true)
            }
            fullFrom = r.fullFrom
            if (item.preview && durationMs == 0L) durationMs = if (r.fullFrom != null) item.durationMs else 30_000 // GStreamer then gives the exact length
            if (repeat == 0 && order.lastOrNull() == index) onLastSong?.invoke()
            bin.setURI(URI(r.uri))
            applyVolume()
            pendingSeek = seekTo
            if (playing) bin.play() else bin.pause()
            upcoming().firstOrNull()?.second?.let { prefetch?.invoke(it) }
        }
    }

    private fun stop() {
        bin.stop()
        playing = false
        positionMs = 0
    }

    /** What GStreamer gets (tests). */
    internal val outputVolume: Double get() = bin.volume

    /** The song to hand over gaplessly, or null when the usual way (end, then load) is needed. */
    private fun gaplessTarget(): Int? {
        if (!gapless || sleepAfterSong || repeat == 2 || index < 0) return null
        val pos = order.indexOf(index)
        val next = order.getOrNull(pos + 1) ?: return null // a new round (repeat all) reshuffles first
        return next.takeIf { queue.getOrNull(it)?.let { i -> !i.preview && i.deezerId <= 0 && i.uri.isNotEmpty() } == true }
    }

    /** The handed-over song is playing now: show it, and do what starting a song does. */
    private fun switchGapless() {
        index = gaplessNext
        gaplessNext = -1
        val item = current ?: return
        error = null; fullFrom = null
        durationMs = item.durationMs; positionMs = 0
        applyVolume()
        if (repeat == 0 && order.lastOrNull() == index) onLastSong?.invoke()
        upcoming().firstOrNull()?.second?.let { prefetch?.invoke(it) }
    }

    /** 10 bands (31 Hz to 16 kHz), each -12..+12 dB. */
    fun equalize(gains: List<Double>) {
        val e = eq ?: return
        gains.take(10).forEachIndexed { i, g -> e.set("band$i", g.coerceIn(-12.0, 12.0)) }
    }

    val hasEqualizer get() = eq != null

    fun close() { bin.stop(); bin.dispose() }
}
