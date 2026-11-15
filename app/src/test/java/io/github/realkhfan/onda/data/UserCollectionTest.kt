package io.github.realkhfan.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UserCollectionTest {
    @Test fun aiListSkipsHeaderAndNormalises() {
        assertEquals(setOf("velvet echo", "the dream machine", "neon inc"), UserCollection.parseAiList("artist\nVelvet Echo\n\"The Dream Machine\"\n\"Neon, Inc.\"\n\n"))
    }
}
