package io.github.realkhfan.onda.data

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import io.github.realkhfan.onda.core.R128
import io.github.realkhfan.onda.core.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.nio.ByteOrder

/** How loud each of your songs is (measured once, on this phone), so they can all play at one level. */
object Loudness {
    data class Gain(val lufs: Double, val peak: Double)
    data class Progress(val running: Boolean = false, val done: Int = 0, val total: Int = 0)

    private val _gains = MutableStateFlow<Map<String, Gain>>(emptyMap())
    val gains: StateFlow<Map<String, Gain>> = _gains.asStateFlow()
    private val _progress = MutableStateFlow(Progress())
    val progress: StateFlow<Progress> = _progress.asStateFlow()
    private val failed = HashSet<String>() // unreadable this run: not tried again until Onda restarts
    private var file: File? = null
    private var job: Job? = null

    fun load(context: Context) {
        val f = File(context.filesDir, "loudness.json").also { file = it }
        _gains.value = runCatching {
            JSONObject(f.readText()).let { o -> o.keys().asSequence().associateWith { k -> o.getJSONArray(k).let { Gain(it.getDouble(0), it.getDouble(1)) } } }
        }.getOrDefault(emptyMap())
    }

    /** dB for a song: its album's level when [album] holds 2+ measured songs, else its own; unknown songs get the typical one. */
    fun gainDb(id: String?, album: List<String>): Double {
        val g = _gains.value
        val own = id?.let { g[it] }
        val albumLufs = R128.albumLufs(album.mapNotNull { g[it]?.lufs })
        val lufs = albumLufs ?: own?.lufs ?: g.values.map { it.lufs }.sorted().let { if (it.isEmpty()) return 0.0 else it[it.size / 2] }
        val peak = if (albumLufs != null) album.mapNotNull { g[it]?.peak }.maxOrNull() ?: 1.0 else own?.peak ?: 1.0
        return R128.gainDb(lufs, peak)
    }

    /** Measures your own files not measured yet, one after another in the background. */
    fun measure(context: Context, tracks: List<Track>) {
        if (job?.isActive == true) return
        val todo = tracks.filter { it.uri.startsWith("content:") && it.id !in _gains.value && it.id !in failed }.distinctBy { it.id }
        if (todo.isEmpty()) return
        val c = context.applicationContext
        job = CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            _progress.value = Progress(true, 0, todo.size)
            val found = HashMap<String, Gain>()
            for ((i, t) in todo.withIndex()) {
                if (!isActive) break
                val g = runCatching { analyse(c, Uri.parse(t.uri)) { isActive } }.getOrNull()
                if (g != null) found[t.id] = g else failed += t.id
                _progress.value = Progress(true, i + 1, todo.size)
                if (found.size >= 20) { _gains.value = _gains.value + found; found.clear(); write() }
            }
            _gains.value = _gains.value + found
            write()
            _progress.value = Progress()
        }
    }

    fun stop() { job?.cancel(); _progress.value = Progress() }

    private fun write() {
        val f = file ?: return
        runCatching {
            val o = JSONObject()
            _gains.value.forEach { (k, g) -> o.put(k, org.json.JSONArray().put(g.lufs).put(g.peak)) }
            File(f.path + ".tmp").apply { writeText(o.toString()) }.renameTo(f)
        }
    }

    /** Decodes the file with the phone's own decoders and measures it; null when it can't be read. */
    private fun analyse(context: Context, uri: Uri, alive: () -> Boolean): Gain? {
        val ex = MediaExtractor()
        try {
            ex.setDataSource(context, uri, null)
            val track = (0 until ex.trackCount).firstOrNull { ex.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true } ?: return null
            ex.selectTrack(track)
            val format = ex.getTrackFormat(track)
            val codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            try {
                codec.configure(format, null, null, 0)
                codec.start()
                var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                var float = false
                var meter: R128? = null
                var samples = FloatArray(0)
                val info = MediaCodec.BufferInfo()
                var inputDone = false
                val deadline = System.currentTimeMillis() + 5 * 60_000 // a broken file can't keep the decoder busy forever
                while (alive() && System.currentTimeMillis() < deadline) {
                    if (!inputDone) {
                        val i = codec.dequeueInputBuffer(10_000)
                        if (i >= 0) {
                            val n = ex.readSampleData(codec.getInputBuffer(i)!!, 0)
                            if (n < 0) { codec.queueInputBuffer(i, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true }
                            else { codec.queueInputBuffer(i, 0, n, ex.sampleTime, 0); ex.advance() }
                        }
                    }
                    val o = codec.dequeueOutputBuffer(info, 10_000)
                    if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val f = codec.outputFormat
                        rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        float = f.containsKey(MediaFormat.KEY_PCM_ENCODING) && f.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                    } else if (o >= 0) {
                        if (info.size > 0) {
                            val m = meter ?: R128(rate, channels).also { meter = it }
                            val buf = codec.getOutputBuffer(o)!!.apply { position(info.offset); limit(info.offset + info.size) }.slice().order(ByteOrder.nativeOrder())
                            val n = if (float) info.size / 4 else info.size / 2
                            if (samples.size < n) samples = FloatArray(n)
                            if (float) buf.asFloatBuffer().get(samples, 0, n)
                            else buf.asShortBuffer().let { s -> for (k in 0 until n) samples[k] = s.get(k) / 32768f }
                            m.add(samples, n)
                        }
                        codec.releaseOutputBuffer(o, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            val m = meter ?: return null
                            return m.integrated()?.let { Gain(it, m.peak) }
                        }
                    }
                }
                return null
            } finally {
                runCatching { codec.stop() }
                codec.release()
            }
        } finally {
            ex.release()
        }
    }
}
