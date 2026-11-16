package io.github.realkhfan.onda.player

import android.net.Uri
import android.util.Base64
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.data.Link
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/** A song played straight from your PC over the link ("Play from your PC"), read in encrypted pieces as it plays. */
@UnstableApi
class LinkDataSource : BaseDataSource(true) {
    private var song: LinkSync.Song? = null
    private var uri: Uri? = null
    private var pos = 0L
    private var left = 0L
    private var buf = ByteArray(0)
    private var bufAt = 0L
    private var opened = false

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val s = songOf(dataSpec.uri) ?: throw IOException("Not a song from your PC")
        val size = sizes[s.id] ?: Link.stat(s)?.first?.also { sizes[s.id] = it } ?: throw IOException("This song isn't on your PC (or it can't be reached)")
        song = s; uri = dataSpec.uri
        pos = dataSpec.position
        left = if (dataSpec.length != C.LENGTH_UNSET.toLong()) dataSpec.length else size - pos
        if (left < 0) throw IOException("Past the end")
        opened = true
        transferStarted(dataSpec)
        return left
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (left == 0L) return C.RESULT_END_OF_INPUT
        if (pos < bufAt || pos >= bufAt + buf.size) {
            buf = Link.read(song ?: throw IOException("Closed"), pos, PIECE) ?: throw IOException("Lost the connection to your PC")
            bufAt = pos
            if (buf.isEmpty()) return C.RESULT_END_OF_INPUT
        }
        val from = (pos - bufAt).toInt()
        val n = minOf(length.toLong(), (buf.size - from).toLong(), left).toInt()
        System.arraycopy(buf, from, buffer, offset, n)
        pos += n; left -= n
        bytesTransferred(n)
        return n
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        uri = null
        if (opened) { opened = false; transferEnded() }
    }

    companion object {
        const val SCHEME = "ondalink"
        private const val PIECE = 512 * 1024
        private val sizes = ConcurrentHashMap<String, Long>()

        fun uriFor(s: LinkSync.Song): String =
            "$SCHEME:" + Base64.encodeToString(LinkSync.songToJson(s).toString().toByteArray(), Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)

        fun songOf(u: Uri): LinkSync.Song? = runCatching {
            LinkSync.songFromJson(JSONObject(String(Base64.decode(u.schemeSpecificPart, Base64.URL_SAFE or Base64.NO_PADDING))))
        }.getOrNull()

        /** A greyed-out song (only on your PC) to play from there. */
        fun item(t: io.github.realkhfan.onda.core.Track): MediaItem = MediaItem.Builder().setMediaId(t.id)
            .setUri(uriFor(LinkSync.Song(null, t.title, t.artist)))
            .setMediaMetadata(MediaMetadata.Builder().setTitle(t.title).setArtist(t.artist).build()).build()
    }
}

/** Your PC's songs go through [LinkDataSource]; everything else through the usual sources. */
@UnstableApi
class RoutingDataSource(private val normal: DataSource, private val link: DataSource) : DataSource {
    private var current: DataSource? = null
    override fun addTransferListener(transferListener: TransferListener) { normal.addTransferListener(transferListener); link.addTransferListener(transferListener) }
    override fun open(dataSpec: DataSpec): Long = (if (dataSpec.uri.scheme == LinkDataSource.SCHEME) link else normal).also { current = it }.open(dataSpec)
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = current?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
    override fun getUri(): Uri? = current?.uri
    override fun getResponseHeaders(): Map<String, List<String>> = current?.responseHeaders ?: emptyMap()
    override fun close() { current?.close(); current = null }

    class Factory(private val normal: DataSource.Factory) : DataSource.Factory {
        override fun createDataSource(): DataSource = RoutingDataSource(normal.createDataSource(), LinkDataSource())
    }
}
