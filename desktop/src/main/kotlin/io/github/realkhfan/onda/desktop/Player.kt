package io.github.realkhfan.onda.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

    data class Item(val id: String, val title: String, val artist: String, val album: String, val art: String?, val uri: String, val preview: Boolean = false)

    var queue by mutableStateOf<List<Item>>(emptyList()); private set
    var index by mutableIntStateOf(-1); private set
    var playing by mutableStateOf(false); private set
    var positionMs by mutableLongStateOf(0L); private set
    var durationMs by mutableLongStateOf(0L); private set
    var shuffle by mutableStateOf(false); private set
    /** 0 off, 1 all, 2 one. */
    var repeat by mutableIntStateOf(0); private set
    var volume by mutableDoubleStateOf(0.8); private set
    var error by mutableStateOf<String?>(null)

    private var order: List<Int> = emptyList() // play order (shuffled or not)
    private val bin: PlayBin

    val current: Item? get() = queue.getOrNull(index)

    init {
        Gst.init(Version.of(1, 16), "Onda")
        bin = PlayBin("onda")
        bin.setVideoSink(ElementFactory.make("fakesink", "novideo"))
        if (audioSink != null) bin.setAudioSink(ElementFactory.make(audioSink, "audio")) // tests
        bin.bus.connect(Bus.EOS { _ -> scope.launch { next(auto = true) } })
        bin.bus.connect(Bus.ERROR { _, _, message ->
            scope.launch {
                error = "Couldn't play “${current?.title.orEmpty()}”: $message"
                next(auto = true, afterError = true)
            }
        })
        scope.launch {
            while (true) {
                delay(400)
                if (index >= 0) {
                    positionMs = bin.queryPosition(TimeUnit.MILLISECONDS).coerceAtLeast(0)
                    bin.queryDuration(TimeUnit.MILLISECONDS).takeIf { it > 0 }?.let { durationMs = it }
                }
            }
        }
    }

    fun play(items: List<Item>, start: Int) {
        if (items.isEmpty()) return
        queue = items
        index = start.coerceIn(items.indices)
        order = makeOrder()
        load()
    }

    /** Adds after the songs already waiting after the current one. */
    fun enqueue(item: Item) {
        if (index < 0) return play(listOf(item), 0)
        val at = order.indexOf(index) + 1
        queue = queue + item
        order = order.toMutableList().apply { add(at.coerceAtMost(size), queue.lastIndex) }
    }

    fun toggle() {
        if (index < 0) return
        if (playing) bin.pause() else bin.play()
        playing = !playing
    }

    fun next(auto: Boolean = false, afterError: Boolean = false) {
        if (index < 0) return
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

    fun jumpTo(i: Int) { if (i in queue.indices) { index = i; load() } }

    fun seek(ms: Long) {
        bin.seekSimple(Format.TIME, EnumSet.of(SeekFlags.FLUSH, SeekFlags.ACCURATE), ms * 1_000_000)
        positionMs = ms
    }

    fun changeVolume(v: Double) { volume = v.coerceIn(0.0, 1.0); bin.volume = volume }

    fun toggleShuffle() { shuffle = !shuffle; order = makeOrder() }
    fun cycleRepeat() { repeat = (repeat + 1) % 3 }

    /** Songs still to come, in play order. */
    fun upcoming(): List<Pair<Int, Item>> = order.drop(order.indexOf(index) + 1).map { it to queue[it] }

    private fun makeOrder(first: Int? = index): List<Int> {
        val all = queue.indices.toList()
        if (!shuffle) return all
        val rest = all.filter { it != first }.shuffled()
        return if (first != null && first >= 0) listOf(first) + rest else rest
    }

    private fun load() {
        val item = current ?: return
        error = null
        bin.stop()
        bin.setURI(URI(item.uri))
        bin.volume = volume
        bin.play()
        playing = true
        positionMs = 0
        durationMs = 0
    }

    private fun stop() {
        bin.stop()
        playing = false
        positionMs = 0
    }

    fun close() { bin.stop(); bin.dispose() }
}
