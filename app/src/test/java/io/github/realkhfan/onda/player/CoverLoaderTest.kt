package io.github.realkhfan.onda.player

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import coil3.SingletonImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

/** The notification's cover: one the app has shown comes from Coil's disk cache, so a poor network doesn't leave it black. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoverLoaderTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test fun aCoverSeenBeforeLoadsWithoutTheNetwork() {
        val url = "https://covers.example.invalid/album.png" // can't be reached
        val png = ByteArrayOutputStream().also { Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        // As if the app's screens had shown it: in Coil's disk cache under its address.
        val cache = SingletonImageLoader.get(app).diskCache!!
        cache.openEditor(url)!!.apply { cache.fileSystem.write(data) { write(png) }; commit() }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val loader = CoverLoader(app, scope)
            val bmp = loader.loadBitmap(Uri.parse(url)).get(10, TimeUnit.SECONDS)
            assertEquals(16, bmp.width)
            assertEquals(Color.RED, bmp.getPixel(8, 8))
            // Never seen and no network: it fails (no cover), it doesn't hang.
            val e = runCatching { loader.loadBitmap(Uri.parse("https://covers.example.invalid/other.png")).get(10, TimeUnit.SECONDS) }.exceptionOrNull()
            assertTrue(e.toString(), e is ExecutionException || e is java.util.concurrent.TimeoutException)
        } finally {
            scope.cancel()
        }
    }
}
