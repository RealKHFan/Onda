package io.github.realkhfan.onda.desktop

import io.github.realkhfan.onda.core.FileNames
import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.data.Overrides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Renames go everywhere: the copy in the copy folder follows in place, and a newer rename from the phone is done here. */
class RenameSyncTest {
    private val ui = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private fun <T> onUi(block: suspend () -> T): T = runBlocking { withContext(ui) { block() } }

    private fun wav(): ByteArray {
        val n = 800
        val b = ByteBuffer.allocate(44 + n * 2).order(ByteOrder.LITTLE_ENDIAN)
        b.put("RIFF".toByteArray()).putInt(36 + n * 2).put("WAVEfmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(8000).putInt(16000).putShort(2).putShort(16).put("data".toByteArray()).putInt(n * 2)
        return b.array()
    }

    private fun waitFor(what: String, check: () -> Boolean) {
        val until = System.currentTimeMillis() + 10_000
        while (!check()) { assertTrue(System.currentTimeMillis() < until, what); Thread.sleep(50) }
    }

    @Test fun aRenameReachesTheCopyFolderAndThePhonesNewerRenameIsDoneHere() {
        val music = Files.createTempDirectory("onda-music").toFile()
        val copies = Files.createTempDirectory("onda-copies").toFile()
        val song = File(music, "Lumen Trio/01 tides.wav").apply { parentFile.mkdirs(); writeBytes(wav()) }
        // Tagged like real music: the song is known by its tags on both devices, whatever its file is called.
        org.jaudiotagger.audio.AudioFileIO.read(song).apply {
            tagOrCreateAndSetDefault.apply { setField(org.jaudiotagger.tag.FieldKey.ARTIST, "Lumen Trio"); setField(org.jaudiotagger.tag.FieldKey.TITLE, "Tides"); setField(org.jaudiotagger.tag.FieldKey.ALBUM, "Sea") }
            commit()
        }
        val copy = File(copies, "Lumen Trio/01 tides.wav").apply { parentFile.mkdirs(); writeBytes(song.readBytes()) }
        val s = onUi { AppState(CoroutineScope(ui)) }
        val folders = s.settings.folders
        val copyDir = s.settings.copyDir
        try {
            onUi { s.addFolder(music.path); s.rescanNow(); s.setCopyDir(copies.path) }
            val t = onUi { s.local.single { it.id == song.toURI().toString() } }
            val key = LinkSync.nameKey(t)

            // Renamed here: the file and its copy keep their content and get the new name, no second file anywhere.
            val r = onUi { s.renameFiles(listOf(FileNames.Rename(t.id, t.fileName, "Lumen Trio - Tides.wav"))) }
            assertEquals(1, r.moved.size)
            waitFor("the copy is renamed") { File(copies, "Lumen Trio/Lumen Trio - Tides.wav").isFile }
            assertEquals(listOf("Lumen Trio - Tides.wav"), File(copies, "Lumen Trio").list()!!.toList())
            assertEquals(listOf("Lumen Trio - Tides.wav"), File(music, "Lumen Trio").list()!!.toList())
            val mine = Overrides.names.value.getValue(key)
            assertEquals("Lumen Trio - Tides", mine.stem) // what the phone gets: the name without the extension

            // An older rename of the old file from the phone loses.
            onUi { s.adoptNames(mapOf(key to LinkSync.Name("Old idea", t.album, mine.at - 1000))) }
            Thread.sleep(300)
            assertEquals(listOf("Lumen Trio - Tides.wav"), File(music, "Lumen Trio").list()!!.toList())
            // Renamed again on the phone (the file as it's named now): done here, copy included.
            val now = onUi { s.local.single { it.fileName == "Lumen Trio - Tides.wav" } }
            onUi { s.adoptNames(mapOf(LinkSync.nameKey(now) to LinkSync.Name("Tides (Live)", t.album, mine.at + 1000))) }
            waitFor("the phone's newer name is used") { File(music, "Lumen Trio/Tides (Live).wav").isFile }
            waitFor("its copy follows") { File(copies, "Lumen Trio/Tides (Live).wav").isFile }
            assertEquals(1, File(copies, "Lumen Trio").list()!!.size)
            assertEquals(mine.at + 1000, Overrides.names.value.getValue(LinkSync.nameKey(now)).at) // known now: not done again
            assertTrue(!copy.exists())
        } finally {
            onUi { s.setCopyDir(copyDir); s.removeFolder(music.path); folders.filter { it !in s.settings.folders }.forEach(s::addFolder) }
            music.deleteRecursively(); copies.deleteRecursively()
        }
    }
}
