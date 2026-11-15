package io.github.realkhfan.onda.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNamesTest {

    private val tree = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
    private fun doc(path: String) = "$tree/document/" + java.net.URLEncoder.encode("primary:Music/$path", "UTF-8")
    private fun t(path: String, title: String, artist: String) =
        Track(doc(path), doc(path), title, artist, "A", artist, 0, 1000, "F", null, fileName = path.substringAfterLast('/'))

    @Test fun nameIsArtistDashTitleWithTheOriginalExtension() {
        assertEquals("takayan - Dopa.m4a", FileNames.fromTags(t("x/abc123.m4a", "Dopa", "takayan")))
        assertEquals("Dopa.mp3", FileNames.fromTags(t("x/1.mp3", "Dopa", "Unknown artist")))
    }

    @Test fun illegalCharactersAndTrailingDotsAreCleaned() {
        assertEquals("AC_DC - What_ Why_.flac", FileNames.fromTags(t("x/1.flac", "What? Why?", "AC/DC")))
        assertEquals("Band - Ending.mp3", FileNames.fromTags(t("x/1.mp3", "Ending...", "Band")))
    }

    @Test fun longJapaneseNamesStayUnderTheByteLimit() {
        val name = FileNames.fromTags(t("x/1.mp3", "あ".repeat(120), "アーティスト"))!!
        assertTrue(name.toByteArray().size <= 204) // 200 for the name + ".mp3"
        assertTrue(name.endsWith(".mp3"))
    }

    @Test fun noExtensionOrTitleMeansNoRename() {
        assertNull(FileNames.fromTags(t("x/noext", "Song", "Band")))
        assertNull(FileNames.fromTags(t("x/1.mp3", "  ", "Band")))
    }

    @Test fun planSkipsCorrectNamesAndNumbersClashesPerFolder() {
        val plan = FileNames.plan(listOf(
            t("A/takayan - Dopa.m4a", "Dopa", "takayan"),   // already right
            t("A/track01.mp3", "Toy", "takayan"),
            t("A/track02.mp3", "Toy", "takayan"),            // same tags in the same folder
            t("B/track03.mp3", "Toy", "takayan"),            // other folder: no clash
        ))
        assertEquals(listOf("takayan - Toy.mp3", "takayan - Toy (2).mp3", "takayan - Toy.mp3"), plan.map { it.to })
        assertEquals(listOf("track01.mp3", "track02.mp3", "track03.mp3"), plan.map { it.from })
    }

    @Test fun clashIsCaseInsensitiveForSdCards() {
        val plan = FileNames.plan(listOf(t("A/TAKAYAN - TOY.mp3", "Other", "Band"), t("A/x.mp3", "Toy", "takayan")))
        assertEquals("takayan - Toy.mp3", plan.first { it.from == "x.mp3" }.to) // the upper-case file is renamed away first
    }

    @Test fun parentFolderComesFromTheDocumentId() {
        assertEquals("primary:Music/A", FileNames.parentOf(doc("A/x.mp3")))
        assertNull(FileNames.parentOf("srv:123"))
        assertEquals("file:/home/me/Music/A", FileNames.parentOf("file:/home/me/Music/A/x.mp3"))
    }
}
