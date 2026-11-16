package io.github.realkhfan.onda.desktop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PictureTest {
    /** A two-frame GIF: red, then blue. */
    private fun gif(): File {
        val f = File.createTempFile("onda-pic", ".gif")
        val w = ImageIO.getImageWritersByFormatName("gif").next()
        ImageIO.createImageOutputStream(f).use { out ->
            w.output = out
            w.prepareWriteSequence(null)
            for (c in listOf(Color.RED, Color.BLUE)) {
                val img = BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB).apply { createGraphics().apply { color = c; fillRect(0, 0, 8, 8); dispose() } }
                w.writeToSequence(IIOImage(img, null, w.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img), null)), null)
            }
            w.endWriteSequence()
        }
        return f
    }

    @Test fun aGifPlaysFrameByFrame() {
        val codec = Codec.makeFromData(Data.makeFromBytes(gif().readBytes()))
        assertEquals(2, codec.frameCount)
        val bitmap = Bitmap().apply { allocPixels(codec.imageInfo) }
        codec.readPixels(bitmap, 0, -1)
        assertEquals(0xFFFF0000.toInt(), bitmap.getColor(4, 4))
        codec.readPixels(bitmap, 1, 0) // drawn over the frame before, as the picture does
        assertEquals(0xFF0000FF.toInt(), bitmap.getColor(4, 4))
    }

    @Test fun thePictureIsCopiedAndReplaced() = runBlocking(Dispatchers.Swing) {
        val state = AppState(CoroutineScope(Dispatchers.Swing))
        val before = state.settings.picture
        try {
            assertFalse(state.setPicture(File.createTempFile("onda-not", ".gif").apply { writeText("not a picture") }))
            assertTrue(state.setPicture(gif()))
            val first = File(state.settings.picture!!)
            assertTrue(first.isFile && first.parentFile == Store.data)
            val png = File.createTempFile("onda-pic", ".png").apply { ImageIO.write(BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB), "png", this) }
            assertTrue(state.setPicture(png))
            assertFalse(first.exists()) // the old copy is gone
            assertTrue(state.setPicture(null))
            assertEquals(null, state.settings.picture)
        } finally {
            if (before == null) state.setPicture(null)
        }
    }

    @Test fun aPictureFromThePhoneIsTakenOverAndRemovedWithTheSync() = runBlocking(Dispatchers.Swing) {
        val state = AppState(CoroutineScope(Dispatchers.Swing))
        val before = state.settings.picture
        try {
            val bytes = gif().readBytes()
            val hash = io.github.realkhfan.onda.core.LinkSync.hashOf(bytes)
            assertTrue(state.saveCover(hash, bytes)) // as the phone sends it, just before the sync
            val snap = state.linkSnapshot(emptyMap(), HashMap())
            state.applyLink(snap.copy(picture = hash), HashMap())
            assertEquals(hash, state.settings.picture?.let(state::coverHash))
            assertEquals(hash, state.linkSnapshot(emptyMap(), HashMap()).picture) // and it goes back out the same
            assertEquals(hash, state.coverFiles()[hash]?.let { io.github.realkhfan.onda.core.LinkSync.hashOf(File(it).readBytes()) }) // the phone can fetch it
            state.applyLink(snap.copy(picture = ""), HashMap()) // removed on the phone
            assertEquals(null, state.settings.picture)
            assertEquals("", state.linkSnapshot(emptyMap(), HashMap()).picture)
        } finally {
            if (before == null) state.setPicture(null as File?)
        }
    }
}
