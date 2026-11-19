package io.github.realkhfan.onda.player

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import coil3.SingletonImageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.size.Precision
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Covers for the notification, lock screen and car, loaded like the app's own screens (Coil): a cover the app has shown
 * comes from its disk cache, without the network. Media3's own loader downloaded web covers (matched songs) every time.
 */
@UnstableApi
class CoverLoader(private val context: Context, private val scope: CoroutineScope) : BitmapLoader {
    private val bytes = DataSourceBitmapLoader(context) // covers inside the metadata itself

    override fun supportsMimeType(mimeType: String) = bytes.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = bytes.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> = SettableFuture.create<Bitmap>().also { f ->
        scope.launch {
            try {
                // Software bitmaps: the notification sends them to the system. At most the width of a phone screen.
                val req = ImageRequest.Builder(context).data(uri.toString()).size(MAX).precision(Precision.INEXACT).allowHardware(false).build()
                when (val r = SingletonImageLoader.get(context).execute(req)) {
                    is SuccessResult -> f.set(r.image.toBitmap())
                    is ErrorResult -> f.setException(r.throwable)
                }
            } catch (e: CancellationException) {
                f.cancel(false)
            } catch (e: Exception) {
                f.setException(e)
            }
        }
    }

    private companion object { const val MAX = 1080 }
}
