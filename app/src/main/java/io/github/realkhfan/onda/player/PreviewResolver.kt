package io.github.realkhfan.onda.player

import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.data.Deezer
import io.github.realkhfan.onda.data.FreeCatalogs
import io.github.realkhfan.onda.data.FullCopies
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Deezer preview links expire after ~12 minutes, so queued previews are stored as "dzpreview:<id>" and swapped
 * for a fresh link just before they load. Runs on the player's loading thread, so a blocking request is fine.
 */
@UnstableApi
internal object PreviewResolver : ResolvingDataSource.Resolver {
    const val SCHEME = "dzpreview"
    private const val REUSE_MS = 8 * 60_000L // well inside the link's lifetime; avoids a request on every seek
    private val cache = HashMap<Long, Pair<String, Long>>()

    fun uriFor(deezerId: Long): String = "$SCHEME:$deezerId"

    /** (Audius on, Jamendo client ID), read when a preview loads; set by the player service from Settings. */
    @Volatile var freeSources: () -> Pair<Boolean, String> = { false to "" }
    private val full = HashMap<Long, Pair<String, Source>?>() // this run's answers: a full copy, or null for none
    private val upgraded = ConcurrentHashMap<String, Source>()

    /** The catalog a "dz:" song is playing from in full, or null while it's a preview. */
    fun fullCopyOf(key: String?): Source? = key?.let { upgraded[it] }

    private fun fullCopy(id: Long): Pair<String, Source>? {
        synchronized(full) { if (full.containsKey(id)) return full[id] }
        val (audius, jamendo) = freeSources()
        if (!audius && jamendo.isBlank()) return null
        val found = try {
            runBlocking { FullCopies.find(id, audius, jamendo) }?.let { c -> FreeCatalogs.stream(c)?.let { it to c.source } }
        } catch (_: Exception) {
            return null // unreachable now: play the preview, ask again next time
        }
        synchronized(full) { full[id] = found }
        return found
    }

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        if (dataSpec.uri.scheme != SCHEME) return dataSpec
        val id = dataSpec.uri.schemeSpecificPart.toLongOrNull() ?: throw IOException("Bad preview id")
        fullCopy(id)?.let { (url, source) ->
            upgraded["dz:$id"] = source
            return dataSpec.withUri(Uri.parse(url))
        }
        upgraded.remove("dz:$id")
        val now = SystemClock.elapsedRealtime()
        val cached = synchronized(cache) { cache[id]?.takeIf { now - it.second < REUSE_MS }?.first }
        val url = cached ?: (Deezer.previewNow(id) ?: throw IOException("No preview for this song"))
            .also { synchronized(cache) { cache[id] = it to now } }
        return dataSpec.withUri(Uri.parse(url))
    }
}
