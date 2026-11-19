package io.github.realkhfan.onda.desktop

import coil3.PlatformContext
import coil3.SingletonImageLoader
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** KDE's media controls get a cover the window has shown as a file, so they don't download it again. */
class CoverCacheTest {
    @Test fun aShownCoverIsHandedOverAsItsFile() {
        val url = "https://covers.example.invalid/onda-test-${System.nanoTime()}.jpg"
        assertNull(cachedCover(url)) // never shown: KDE gets the address as before
        val cache = assertNotNull(SingletonImageLoader.get(PlatformContext.INSTANCE).diskCache, "the window keeps covers on disk")
        val bytes = ByteArray(500) { it.toByte() }
        cache.openEditor(url)!!.apply { cache.fileSystem.write(data) { write(bytes) }; commit() }
        val file = assertNotNull(cachedCover(url))
        assertEquals(bytes.toList(), File(file.removePrefix("file://")).readBytes().toList())
        cache.remove(url)
    }
}
