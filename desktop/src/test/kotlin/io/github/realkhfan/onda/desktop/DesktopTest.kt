package io.github.realkhfan.onda.desktop

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import io.github.realkhfan.onda.core.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

class DesktopTest {
    /** One second of a quiet tone as WAV (no tags), invented for the test. */
    private fun wav(f: File, seconds: Double = 1.0): File {
        val rate = 22050; val n = (rate * seconds).toInt()
        val b = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(rate).putInt(rate * 2).putShort(2).putShort(16).put("data".toByteArray()).putInt(n * 2)
        repeat(n) { b.putShort((Math.sin(it * 2 * Math.PI * 440 / rate) * 2000).toInt().toShort()) }
        f.writeBytes(b.array()); return f
    }

    @Test fun filesWithoutTagsAreNamedFromTheFileName() {
        val dir = createTempDir()
        wav(File(dir, "Velvet Hours - Paper Moon.wav"))
        val t = Scanner.scan(listOf(dir.path), emptyMap()).values.single().second
        assertEquals("Paper Moon", t.title)
        assertEquals("Velvet Hours", t.artist)
        assertEquals(1000L, t.durationMs)
        assertEquals(dir.name, t.folderName)
    }

    @Test fun playsThroughGStreamerAndMovesOn() = runBlocking {
        val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val dir = createTempDir()
        val songs = listOf(wav(File(dir, "a.wav"), 0.6), wav(File(dir, "b.wav"), 0.6)).mapIndexed { i, f ->
            Player.Item("id$i", "Song $i", "Artist", "Album", null, f.toURI().toString())
        }
        val player = withContext(ui) { Player(CoroutineScope(ui), audioSink = "fakesink") }
        withContext(ui) { player.play(songs, 0) }
        assertTrue(withContext(ui) { player.playing })
        var waited = 0
        while (withContext(ui) { player.playing } && waited < 8000) { Thread.sleep(100); waited += 100 }
        assertFalse("both songs should have ended", withContext(ui) { player.playing })
        assertEquals(1, withContext(ui) { player.index }) // moved on to the second, then stopped (repeat off)
        withContext(ui) { player.close() }
    }

    @Test fun screenshots() {
        val state = AppState(CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher()))
        state.local = listOf(
            Track("file:///m/1.flac", "file:///m/1.flac", "Paper Moon", "Velvet Hours", "Tides", "Velvet Hours", 1, 201_000, "Music", null),
            Track("file:///m/2.flac", "file:///m/2.flac", "Night Drive", "Velvet Hours", "Tides", "Velvet Hours", 2, 187_000, "Music", null),
            Track("file:///m/3.mp3", "file:///m/3.mp3", "Open Fields", "Lumen Trio", "Meadows", "Lumen Trio", 1, 243_000, "Music", null),
        )
        if (state.collection.playlists.none { it.name == "Road trip" }) state.createPlaylist("Road trip", state.local.map { it.id })
        if (!state.isLiked(state.local[0].id)) state.toggleLike(state.local[0].id)
        val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val player = runBlocking { withContext(ui) { Player(CoroutineScope(ui), audioSink = "fakesink") } }
        runBlocking { withContext(ui) { player.restore(listOf(state.item(state.local[0]).copy(uri = wav(File.createTempFile("onda", ".wav"), 3.0).toURI().toString())), 0, 0, false, 0) } }
        Thread.sleep(800)
        val scene = ImageComposeScene(1240, 800, Density(1f)) { OndaDesktop(state, player) }
        repeat(3) { scene.render(it * 16_000_000L) }
        File("build/screenshots").mkdirs()
        File("build/screenshots/desktop-home.png").writeBytes(scene.render(64_000_000L).encodeToData(EncodedImageFormat.PNG)!!.bytes)
        scene.close()
    }
}

class FolderPickerTest {
    @Test fun typedPathsResolve() {
        val dir = createTempDir()
        assertEquals(dir.canonicalPath, FolderPicker.resolve("  \"${dir.path}\" "))
        assertEquals(dir.canonicalPath, FolderPicker.resolve("file://${dir.path}"))
        assertEquals(java.io.File(System.getProperty("user.home")).canonicalPath, FolderPicker.resolve("~"))
        assertEquals(null, FolderPicker.resolve("/no/such/folder"))
        assertEquals(null, FolderPicker.resolve(" "))
    }

    @Test fun noPickerInstalledIsReported() = kotlinx.coroutines.runBlocking {
        // This build machine has neither kdialog nor zenity.
        assertEquals(FolderPicker.Result.NoPicker, FolderPicker.pick("/tmp"))
    }
}
