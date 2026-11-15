package io.github.realkhfan.onda.player

import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import io.github.realkhfan.onda.data.Deezer
import java.io.IOException

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

    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        if (dataSpec.uri.scheme != SCHEME) return dataSpec
        val id = dataSpec.uri.schemeSpecificPart.toLongOrNull() ?: throw IOException("Bad preview id")
        val now = SystemClock.elapsedRealtime()
        val cached = synchronized(cache) { cache[id]?.takeIf { now - it.second < REUSE_MS }?.first }
        val url = cached ?: (Deezer.previewNow(id) ?: throw IOException("No preview for this song"))
            .also { synchronized(cache) { cache[id] = it to now } }
        return dataSpec.withUri(Uri.parse(url))
    }
}
