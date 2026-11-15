package io.github.realkhfan.onda.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/** Streamed songs are kept (up to 1 GB, oldest out first) so replays and seeking cost no data. Android may clear it. */
@UnstableApi
object PlaybackCache {
    const val MAX_BYTES = 1L shl 30
    private var cache: SimpleCache? = null

    /** One instance per folder for the whole run (a second one on the same folder would throw). */
    @Synchronized
    fun get(context: Context): SimpleCache = cache ?: SimpleCache(
        File(context.cacheDir, "media"), LeastRecentlyUsedCacheEvictor(MAX_BYTES), StandaloneDatabaseProvider(context.applicationContext),
    ).also { cache = it }

    /** Deezer preview links carry a fresh signature each time; the same song is the same file without it. */
    fun key(url: String): String = if (".dzcdn.net/" in url) url.substringBefore('?') else url

    fun clear(context: Context) {
        val c = get(context)
        c.keys.toList().forEach { runCatching { c.removeResource(it) } }
    }
}
