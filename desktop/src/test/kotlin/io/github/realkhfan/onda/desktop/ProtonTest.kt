package io.github.realkhfan.onda.desktop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProtonTest {
    /** A stand-in for Proton's Drive tool: answers like it does (JSON lists, downloads into a folder, "login first"). */
    private fun fakeCli(): File {
        val dir = Files.createTempDirectory("proton-cli").toFile()
        val f = File(dir, "proton-drive")
        f.writeText("""
            #!/bin/sh
            n() { printf '{"uid":"%s","name":{"ok":true,"value":"%s"},"type":"%s","activeRevision":{"claimedSize":%s}}' "${'$'}1" "${'$'}2" "${'$'}3" "${'$'}4"; }
            case "${'$'}1 ${'$'}2" in
              "filesystem list")
                case "${'$'}3" in
                  /my-files) printf '[\n'; n v~1 Music folder 0; printf '\n]\n' ;;
                  /my-files/Music) printf '[\n'; n v~2 "Night Kites" folder 0; printf ',\n'; n v~3 "Dunes - Glass.mp3" file 1000; printf ',\n'; n v~4 cover.jpg file 50
                    printf ',\n{"uid":"v~9","name":{"ok":false,"error":{}},"type":"file"}\n]\n' ;;
                  "/my-files/Music/Night Kites") printf '[\n'; n v~5 Sombrero folder 0; printf '\n]\n' ;;
                  "/my-files/Music/Night Kites/Sombrero") printf '[\n'; n v~6 "01 Sombrero.flac" file 2000; printf '\n]\n' ;;
                  /my-files/Out) echo "You need to login first" >&2; exit 1 ;;
                  *) echo "Node not found" >&2; exit 1 ;;
                esac ;;
              "filesystem download") printf 'fLaC-song-bytes' > "${'$'}4/$(basename "${'$'}3")" ;;
              *) echo "Unknown command" >&2; exit 1 ;;
            esac
        """.trimIndent() + "\n")
        f.setExecutable(true)
        return f
    }

    @Test fun songsAreListedByNameAndDownloadedWhenTheyPlay() {
        val cli = fakeCli()
        val songs = Proton.scan(cli, listOf("/my-files/Music"))
        // Folders inside the chosen one too; only audio files; a name the tool couldn't read is left out.
        assertEquals(listOf("Dunes - Glass.mp3", "Night Kites/Sombrero/01 Sombrero.flac"), songs.map { it.rel })
        val glass = Proton.track(songs[0])
        assertEquals(listOf("Glass", "Dunes", ""), listOf(glass.title, glass.artist, glass.album))
        val sombrero = Proton.track(songs[1])
        assertEquals(listOf("Sombrero", "Night Kites", "Sombrero", "1"), listOf(sombrero.title, sombrero.artist, sombrero.album, sombrero.trackNo.toString()))
        assertEquals("proton:v~6", sombrero.id)

        val id = Proton.PREFIX + songs[1].uid
        try {
            val f = Proton.fetch(cli, songs[1])
            assertEquals("fLaC-song-bytes", f.readText())
            assertTrue(SongCache.has(id)) // kept: it plays without the internet next time
            assertEquals(f, Proton.fetch(File("/nonexistent"), songs[1])) // and isn't downloaded again
        } finally { SongCache.get(id)?.delete() }
    }

    @Test fun signedOutIsToldApartFromOtherProblems() {
        val cli = fakeCli()
        val out = runCatching { Proton.list(cli, "/my-files/Out") }.exceptionOrNull() as Proton.Failed
        assertTrue(out.signedOut)
        val missing = runCatching { Proton.list(cli, "/my-files/Nope") }.exceptionOrNull() as Proton.Failed
        assertEquals(false, missing.signedOut)
        assertEquals("Node not found", missing.message)
    }

    @Test fun namesWithoutADashTakeTheArtistFromTheFolders() {
        fun t(rel: String) = Proton.track(Proton.Song("u", "/my-files/x/$rel", rel, 0)).let { listOf(it.title, it.artist, it.album, it.trackNo.toString()) }
        assertEquals(listOf("Tides", "Lumen Trio", "Blue", "3"), t("Lumen Trio/Blue/03 - Tides.mp3"))
        assertEquals(listOf("Tides", "Lumen Trio", "", "0"), t("Lumen Trio/Tides.mp3")) // one folder: the artist's
        assertEquals(listOf("2026!", "Unknown artist", "", "0"), t("2026!.mp3")) // a number that's the title stays
        assertEquals(listOf("Glass", "Dunes", "Singles", "0"), t("Singles/Dunes - Glass.m4a"))
    }
}
