package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.core.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files

class OverridesSyncTest {
    private fun song(id: String) = Track(id, id, "tides demo", "lumen trio", "", "lumen trio", 0, 200_000, "audio/mpeg", null, "t.mp3")

    @Test fun onlyARestoreRemovesAnEditFromTheSync() {
        val dir = Files.createTempDirectory("ov").toFile()
        Overrides.load(dir)
        val t = song("content://a")
        val key = LinkSync.fileKey(t)
        val agreed = mapOf(key to LinkSync.Edit("Tides", "Lumen Trio", "Sea", null, 0))
        // This phone doesn't have the edit right now (the song got a new id, say): the agreed one goes out, not a removal.
        assertEquals("Tides", Overrides.forSync(listOf(t), agreed)[key]?.title)
        // An edit here wins over the agreed one.
        Overrides.edit(t, "Tides (Live)", "Lumen Trio", "Sea")
        assertEquals("Tides (Live)", Overrides.forSync(listOf(t), agreed)[key]?.title)
        // Restored on purpose: left out until a sync takes it in, even after Onda restarts.
        Overrides.restore(t.id)
        Overrides.load(dir)
        assertNull(Overrides.forSync(listOf(t), agreed)[key])
        Overrides.synced()
        assertEquals("Tides", Overrides.forSync(listOf(t), agreed)[key]?.title) // a later sync's agreed edits decide again
    }

    @Test fun aStaleEditHereDoesntBeatANewerAgreedOne() {
        val dir = Files.createTempDirectory("ov3").toFile()
        Overrides.load(dir)
        val t = song("content://b")
        val key = LinkSync.fileKey(t)
        Overrides.edit(t, "Tides (old)", "Lumen Trio", "Sea")
        val mine = Overrides.meta.value.getValue(t.id).at
        // The PC's newer edit was agreed while this phone's songs weren't loaded: this phone's older one isn't news.
        val agreed = mapOf(key to LinkSync.Edit("Tides (PC)", "Lumen Trio", "Sea", null, 0, mine + 1000))
        assertEquals("Tides (PC)", Overrides.forSync(listOf(t), agreed)[key]?.title)
        // Edited here after that: this one goes.
        Thread.sleep(2); Overrides.edit(t, "Tides (phone)", "Lumen Trio", "Sea")
        assertEquals("Tides (phone)", Overrides.forSync(listOf(t), mapOf(key to agreed.getValue(key).copy(at = mine)))[key]?.title)
        // A restore carries its date, kept over a restart; a sync takes in only the restores it sent.
        Overrides.restore(t.id)
        Overrides.load(dir)
        val at = Overrides.restoresForSync(listOf(t)).getValue(key)
        Overrides.synced(upTo = at - 1)
        assertEquals(at, Overrides.restoresForSync(listOf(t))[key])
        Overrides.synced(upTo = at)
        assertNull(Overrides.restoresForSync(listOf(t))[key])
        // Renames known here survive a restart too, and only a newer one replaces one.
        Overrides.knowNames(mapOf(key to LinkSync.Name("Lumen Trio - Tides", "Sea", 200)))
        Overrides.knowNames(mapOf(key to LinkSync.Name("Older", "Sea", 100)))
        Overrides.load(dir)
        assertEquals("Lumen Trio - Tides", Overrides.names.value[key]?.stem)
    }

    @Test fun editsFollowAMovedFileByItsName() {
        Overrides.load(Files.createTempDirectory("ov2").toFile())
        fun file(path: String, name: String) = Track("file:$path", "file:$path", "tides demo", "lumen trio", "", "lumen trio", 0, 200_000, "audio/mpeg", null, name)
        val old = file("/home/me/Music/Lumen%20Trio/01%20Tides.mp3", "01 Tides.mp3")
        val twinOld = file("/home/me/Music/A/Intro.mp3", "Intro.mp3")
        Overrides.edit(old, "Tides", "Lumen Trio", "Sea")
        Overrides.edit(twinOld, "Intro", "A", "")
        // The music folder moved: new paths, so new ids. "Intro.mp3" is now in two places: too unsure, it stays.
        val moved = file("/home/me/Music%20Downloads/Lumen%20Trio/01%20Tides.mp3", "01 Tides.mp3")
        val intros = listOf(file("/home/me/Music%20Downloads/A/Intro.mp3", "Intro.mp3"), file("/home/me/Music%20Downloads/B/Intro.mp3", "Intro.mp3"))
        assertEquals(1, Overrides.relink(listOf(moved) + intros))
        assertEquals("Tides", Overrides.meta.value[moved.id]?.title)
        assertNull(Overrides.meta.value[old.id])
        assertEquals("Intro", Overrides.meta.value[twinOld.id]?.title)
        // Phone ids (documents in a folder you picked) work the same way.
        val doc = "content://com.android.externalstorage.documents/tree/primary%3AMusic/document/primary%3AMusic%2FTides.mp3"
        Overrides.edit(Track(doc, doc, "t", "a", "", "a", 0, 1, "", null, "Tides.mp3"), "Tides", "Lumen Trio", "")
        val newDoc = "content://com.android.externalstorage.documents/tree/primary%3ASongs/document/primary%3ASongs%2FTides.mp3"
        assertEquals(1, Overrides.relink(listOf(Track(newDoc, newDoc, "t", "a", "", "a", 0, 1, "", null, "Tides.mp3"))))
        assertEquals("Tides", Overrides.meta.value[newDoc]?.title)
    }
}
