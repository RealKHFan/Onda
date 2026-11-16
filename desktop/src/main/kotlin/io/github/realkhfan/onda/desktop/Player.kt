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
    /** The song handed over, so a queue edit in the meantime (an index moving) can't show the wrong song. */
    @Volatile private var gaplessItem: Item? = null

    /** Long songs (DJ mixes) pick up where you left them: [resumeAt] gives the spot, [onLeave] hears where you stopped. */
    var resumeAt: (Item) -> Long = { 0 }
    var onLeave: ((Item, Long, Long) -> Unit)? = null
    private var playingItem: Item? = null

    /** Sleep timer: pause at this time (ms since epoch), or 0. */
    var sleepAt by mutableLongStateOf(0L)
    /** Sleep timer: stop when the current song ends. */
    var sleepAfterSong by mutableStateOf(false)

    /** Called when the last song starts (repeat off): "similar songs when the queue ends" adds more. */
    var onLastSong: (() -> Unit)? = null
    private var loadToken = 0
    private var pendingSeek = -1L
    /** The player you hear; [spare] plays the next song's start during a crossfade, then they swap. */
    private var bin: PlayBin
    private var spare: PlayBin
    /** Each player's equalizer; empty when the system has none (gst-plugins-good). */
    private val eqs = HashMap<PlayBin, org.freedesktop.gstreamer.Element>()
    private var eqGains: List<Double> = List(10) { 0.0 }

    /** Crossfade length in seconds (0 = off); songs of one album still play gaplessly. */
    var crossfade = 0
    private class Crossing(val target: Int, val item: Item, val startedAt: Long, val fadeMs: Long)
    private var crossing: Crossing? = null
    private var crossStarting = false
    private var crossOut = 1.0
    internal val crossfading: Boolean get() = crossing != null

    val current: Item? get() = queue.getOrNull(index)

    private fun makeBin(name: String, audioSink: String?): PlayBin {
        val b = PlayBin(name)
        b.setVideoSink(ElementFactory.make("fakesink", "novideo-$name"))
        // Tests: a silent sink that still keeps real time, like speakers do.
        if (audioSink != null) b.setAudioSink(ElementFactory.make(audioSink, "audio-$name").also { it.set("sync", true) })
        runCatching { ElementFactory.make("equalizer-10bands", "eq-$name") }.getOrNull()?.also { b.set("audio-filter", it); eqs[b] = it }
        b.bus.connect(Bus.EOS { _ -> scope.launch { if (b !== bin) return@launch; gaplessNext = -1; if (crossing != null) finishCrossfade() else next(auto = true) } })
        // Called on GStreamer's thread shortly before the end: the next file is handed over now, so there's no gap.
        b.connect(PlayBin.ABOUT_TO_FINISH { _ ->
            if (b === bin) gaplessTarget()?.let { i -> gaplessItem = queue[i]; gaplessNext = i; b.set("uri", queue[i].uri) }
        })
        b.bus.connect(Bus.ERROR { _, _, message ->
            scope.launch {
                if (b !== bin) { cancelCrossfade(); return@launch } // the next song's start failed: it just starts normally
                error = "Couldn't play “${current?.title.orEmpty()}”: $message"
                next(auto = true, afterError = true)
            }
        })
        return b
    }

    init {
        Gst.init(Version.of(1, 16), "Onda")
        bin = makeBin("onda", audioSink)
        spare = makeBin("onda2", audioSink)
        scope.launch {
            while (true) {
                delay(if (gaplessNext >= 0 || crossing != null || nearCrossfade()) 50 else 400)
                tickCrossfade()
                if (gaplessNext >= 0 && bin.get("current-uri") == gaplessItem?.uri) switchGapless()
                if (sleepAt in 1..System.currentTimeMillis()) { sleepAt = 0; pause(); applyVolume() } // back to full for next time
                else if (sleepAt > 0 && sleepAt - System.currentTimeMillis() < 30_000) applyVolume()
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
        cancelCrossfade()
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
        cancelCrossfade()
        playing = !playing
        if (playing) bin.play() else bin.pause()
    }

    fun resume() { if (!playing) toggle() }
    fun pause() { if (playing) toggle() }

    fun next(auto: Boolean = false, afterError: Boolean = false) {
        if (index < 0) return
        cancelCrossfade()
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
        cancelCrossfade()
        if (positionMs > 3000) return seek(0)
        val pos = order.indexOf(index)
        if (pos > 0) { index = order[pos - 1]; load() } else seek(0)
    }

    fun jumpTo(i: Int) { if (i in queue.indices) { cancelCrossfade(); index = i; playing = true; load() } }

    fun seek(ms: Long) {
        cancelCrossfade()
        bin.seekSimple(Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE), ms.coerceAtLeast(0) * 1_000_000)
        positionMs = ms.coerceAtLeast(0)
    }

    fun changeVolume(v: Double) { volume = v.coerceIn(0.0, 1.0); applyVolume() }

    // Ears hear loudness logarithmically: like KDE's and GNOME's sliders, the slider is cubic (10% ≈ -60 dB, 50% ≈ -18 dB).
    private fun applyVolume() { bin.volume = volume * volume * volume * (current?.let(gain) ?: 1.0) * fade() * crossOut }

    /** The sleep timer fades out over its last 30 seconds instead of stopping mid-sound. */
    private fun fade(): Double {
        if (sleepAt <= 0) return 1.0
        return ((sleepAt - System.currentTimeMillis()) / 30_000.0).coerceIn(0.0, 1.0)
    }

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
        cancelCrossfade()
        leaving()
        playingItem = item
        @Suppress("NAME_SHADOWING") val seekTo = if (seekTo >= 0) seekTo else resumeAt(item).takeIf { it > 0 } ?: -1
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
        if (crossing != null || crossStarting || crossTarget() != null) return null // this change crossfades instead
        val pos = order.indexOf(index)
        val next = order.getOrNull(pos + 1) ?: return null // a new round (repeat all) reshuffles first
        return next.takeIf { queue.getOrNull(it)?.let { i -> !i.preview && i.deezerId <= 0 && i.uri.isNotEmpty() } == true }
    }

    /** The handed-over song is playing now: show it, and do what starting a song does. */
    private fun switchGapless() {
        leaving()
        val handed = gaplessItem
        val at = handed?.let { h -> queue.indexOfFirst { it === h } } ?: -1
        if (at < 0) { gaplessNext = -1; gaplessItem = null; return next() } // removed from the queue meanwhile: play what's next instead
        index = at
        playingItem = current
        gaplessNext = -1; gaplessItem = null
        val item = current ?: return
        error = null; fullFrom = null
        durationMs = item.durationMs; positionMs = 0
        applyVolume()
        if (repeat == 0 && order.lastOrNull() == index) onLastSong?.invoke()
        upcoming().firstOrNull()?.second?.let { prefetch?.invoke(it) }
    }

    private fun leaving() { playingItem?.let { onLeave?.invoke(it, positionMs, durationMs) } }

    /** Plays through another output (headphones, HDMI…; a PulseAudio/PipeWire sink name), or the system's default (null). */
    fun setOutput(device: String?) {
        cancelCrossfade()
        val pos = positionMs
        bin.stop()
        for (b in listOf(bin, spare)) b.setAudioSink(if (device == null) ElementFactory.make("autoaudiosink", null)
            else ElementFactory.make("pulsesink", null).also { it.set("device", device) })
        if (current != null) load(seekTo = pos)
    }

    /** 10 bands (31 Hz to 16 kHz), each -12..+12 dB. */
    fun equalize(gains: List<Double>) {
        eqGains = gains
        eqs.values.forEach { e -> gains.take(10).forEachIndexed { i, g -> e.set("band$i", g.coerceIn(-12.0, 12.0)) } }
    }

    val hasEqualizer get() = eqs.isNotEmpty()

    fun close() { leaving(); spare.stop(); spare.dispose(); bin.stop(); bin.dispose() }

    /* ---------- crossfade ---------- */

    /** The song to crossfade into, or null: a normal change (same album, repeat one, sleep timer, a new round, nothing next). */
    private fun crossTarget(): Int? {
        if (crossfade <= 0 || sleepAfterSong || repeat == 2 || index < 0) return null
        val next = order.getOrNull(order.indexOf(index) + 1) ?: return null
        val a = current ?: return null
        val b = queue.getOrNull(next) ?: return null
        if (a.album.isNotBlank() && a.album == b.album && a.artist == b.artist) return null // an album plays as it was made
        return next
    }

    private fun nearCrossfade() = crossfade > 0 && playing && durationMs > 0 && durationMs - positionMs < crossfade * 1000L + 600

    private fun tickCrossfade() {
        val c = crossing
        if (c == null) {
            if (crossStarting || !playing || !nearCrossfade()) return
            val f = crossfade * 1000L
            val left = durationMs - positionMs
            if (durationMs < f * 2 || left > f || left < 400) return
            val target = crossTarget() ?: return
            val item = queue[target]
            crossStarting = true
            val token = loadToken
            scope.launch {
                val r = runCatching { withContext(Dispatchers.IO) { resolver(item) } }.getOrNull()
                crossStarting = false
                if (r == null || token != loadToken || !playing || queue.getOrNull(target) !== item) return@launch
                spare.stop()
                spare.setURI(URI(r.uri))
                spare.volume = 0.0
                spare.play()
                crossing = Crossing(target, item, System.currentTimeMillis(), (durationMs - positionMs).coerceAtLeast(400))
            }
        } else {
            val t = ((System.currentTimeMillis() - c.startedAt).toDouble() / c.fadeMs).coerceIn(0.0, 1.0)
            crossOut = kotlin.math.cos(t * Math.PI / 2)
            applyVolume()
            spare.volume = volume * volume * volume * gain(c.item) * fade() * kotlin.math.sin(t * Math.PI / 2)
            if (t >= 1.0) finishCrossfade()
        }
    }

    /** The faded-in song is the one playing now: the two players swap. */
    private fun finishCrossfade() {
        val c = crossing ?: return
        crossing = null
        leaving()
        bin.stop()
        val old = bin; bin = spare; spare = old
        index = c.target
        playingItem = current
        gaplessNext = -1; gaplessItem = null
        error = null; fullFrom = null
        durationMs = c.item.durationMs
        positionMs = bin.queryPosition(TimeUnit.MILLISECONDS).coerceAtLeast(0)
        crossOut = 1.0
        applyVolume()
        if (repeat == 0 && order.lastOrNull() == index) onLastSong?.invoke()
        upcoming().firstOrNull()?.second?.let { prefetch?.invoke(it) }
    }

    /** You skipped, paused or changed something: the next song's start stops; the current one plays as it is. */
    private fun cancelCrossfade() {
        if (crossing == null && !crossStarting) return
        crossing = null; crossStarting = false
        spare.stop()
        crossOut = 1.0
        applyVolume()
    }
}
