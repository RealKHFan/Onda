package io.github.realkhfan.onda.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.freedesktop.gstreamer.Bus
import org.freedesktop.gstreamer.Gst
import org.freedesktop.gstreamer.Pipeline
import org.json.JSONObject
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.pow

/**
 * Volume normalisation: each song's loudness is measured once (ReplayGain analysis by GStreamer, decoding the whole
 * song faster than real time) and every song then plays at about the same level. Your files are never changed.
 */
object Loudness {
    /** [db]: ReplayGain track gain; [peak]: the loudest sample (1.0 = full scale). */
    data class Gain(val db: Double, val peak: Double)
    class State(val running: Boolean = false, val done: Int = 0, val total: Int = 0)

    // ReplayGain's 89 dB reference is about -18 LUFS; +4 dB lands at -14 LUFS, Spotify's "normal" level.
    private const val PREAMP = 4.0
    private val file = File(Store.data, "loudness.json")
    var gains by mutableStateOf(read()); private set
    var state by mutableStateOf(State()); private set
    private val failed = HashSet<String>() // unreadable this run: not tried again until Onda restarts
    private var job: Job? = null

    /**
     * The volume factor for a song: turned to about -14 LUFS, and boosted only as far as its peak allows (never clipping).
     * Songs not measured (previews, free songs) get the typical gain of your measured songs, so they fit in too.
     */
    fun factor(id: String): Double {
        val g = gains[id]
        val db = g?.db ?: typical() ?: return 1.0
        return factorFor(db, g?.peak ?: 1.0)
    }

    fun factorFor(db: Double, peak: Double): Double = minOf(10.0.pow((db + PREAMP) / 20), 1.0 / peak.coerceAtLeast(0.05)).coerceIn(0.05, 4.0)

    private fun typical(): Double? = gains.values.map { it.db }.sorted().takeIf { it.isNotEmpty() }?.let { it[it.size / 2] }

    /** Measures, one at a time in the background, the songs not measured yet. [songs]: id to a URI GStreamer can open. */
    fun measure(scope: CoroutineScope, songs: List<Pair<String, String>>) {
        if (job?.isActive == true) return
        val todo = songs.filter { it.first !in gains && it.first !in failed }.distinctBy { it.first }
        if (todo.isEmpty()) return
        state = State(true, 0, todo.size)
        job = scope.launch {
            val found = HashMap<String, Gain>()
            try {
                for ((i, s) in todo.withIndex()) {
                    val g = withContext(Dispatchers.IO) { runCatching { analyse(s.second) }.getOrNull() }
                    if (g != null) found[s.first] = g else failed += s.first
                    state = State(true, i + 1, todo.size)
                    if (found.size >= 25) { gains = gains + found; found.clear(); save() }
                }
            } finally {
                gains = gains + found
                save()
                state = State()
            }
        }
    }

    fun stop() { job?.cancel() }

    /** Decodes [uri] and returns its ReplayGain track gain and peak; null if it can't be read. */
    fun analyse(uri: String): Gain? {
        if (!Gst.isInitialized()) Gst.init(org.freedesktop.gstreamer.Version.of(1, 16), "Onda")
        val p = Gst.parseLaunch("uridecodebin name=src ! audioconvert ! audioresample ! rganalysis ! fakesink sync=false") as Pipeline
        try {
            p.getElementByName("src").set("uri", uri)
            var gain: Double? = null
            var peak: Double? = null
            val done = CountDownLatch(1)
            // The analysis is posted at the end, after any ReplayGain tags the file had: the last value wins.
            p.bus.connect(Bus.TAG { _, tags ->
                tags.getValues("replaygain-track-gain").lastOrNull()?.let { gain = (it as Number).toDouble() }
                tags.getValues("replaygain-track-peak").lastOrNull()?.let { peak = (it as Number).toDouble() }
            })
            p.bus.connect(Bus.EOS { done.countDown() })
            p.bus.connect(Bus.ERROR { _, _, _ -> gain = null; done.countDown() })
            p.play()
            if (!done.await(180, TimeUnit.SECONDS)) return null
            return gain?.takeIf { !it.isNaN() }?.let { Gain(it, peak?.takeIf { x -> x > 0 && !x.isNaN() } ?: 1.0) }
        } finally {
            p.stop()
            p.dispose()
        }
    }

    private fun read(): Map<String, Gain> = runCatching {
        val o = JSONObject(file.readText())
        o.keySet().associateWith { k -> o.getJSONArray(k).let { Gain(it.getDouble(0), it.getDouble(1)) } }
    }.getOrDefault(emptyMap())

    private fun save() {
        runCatching {
            val o = JSONObject()
            gains.forEach { (k, g) -> o.put(k, org.json.JSONArray().put(g.db).put(g.peak)) }
            File(file.path + ".tmp").apply { writeText(o.toString()) }.renameTo(file)
        }
    }
}
