package io.github.realkhfan.onda.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ResumeTest {
    @Test fun onlyLongSongsAndNotNearTheEnds() {
        val long = 30 * 60_000L
        Resume.left("mix", 10 * 60_000L, long)
        assertEquals(10 * 60_000L, Resume.at("mix"))
        Resume.left("mix", long - 30_000, long) // nearly done: start over next time
        assertEquals(0L, Resume.at("mix"))
        Resume.left("song", 120_000, 4 * 60_000L) // a normal song is never resumed
        assertEquals(0L, Resume.at("song"))
    }
}
