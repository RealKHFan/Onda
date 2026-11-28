package io.github.realkhfan.onda.player

import io.github.realkhfan.onda.core.RemoteTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PreviewItemTest {
    @Test fun aSavedDeezerSongWithoutALinkGetsAFreshOneWhenItPlays() {
        val song = RemoteTrack(626262, "Sombrero", "Night Kites", "Dunes", null, null, 120) // from the PC: no link
        assertNull(song.toPreviewItem()) // straight from Deezer this means "not streamed in your country"
        assertEquals(PreviewResolver.uriFor(626262), song.toPreviewItem(saved = true)?.localConfiguration?.uri.toString())
        assertEquals(true, song.savedPlayable)
    }
}
