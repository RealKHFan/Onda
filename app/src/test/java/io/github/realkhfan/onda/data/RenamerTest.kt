package io.github.realkhfan.onda.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RenamerTest {
    @Test fun theFileNextToASongIsFoundByItsPath() {
        assertEquals("primary:Music/Spotify/Takayan/Want to Disappear.mp3", Renamer.sibling("primary:Music/Spotify/Takayan/want to disappear.mp3", "Want to Disappear.mp3"))
        assertEquals("primary:New name.flac", Renamer.sibling("primary:old.flac", "New name.flac")) // at the top of the storage
        assertNull(Renamer.sibling("1234", "x.mp3"))
    }
}
