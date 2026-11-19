package io.github.realkhfan.onda.ui

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import io.github.realkhfan.onda.core.Track
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** A song row: right adds it to the queue, left to a playlist. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SwipeTrackRowTest {
    @get:Rule val rule = createComposeRule()
    private val did = mutableListOf<String>()
    private val song = Track("s1", "content://s1", "Tides", "Lumen Trio", "Sea", "Lumen Trio", 0, 180_000, "", null, "")

    private fun row(missing: Boolean = false) = rule.setContent {
        Box(Modifier.width(360.dp).testTag("row")) {
            SwipeTrackRow(song, false, onQueue = { did += "queue" }, onMenu = {}, missing = missing, onAddTo = { did += "playlist" }) { did += "play" }
        }
    }

    @Test fun rightQueuesLeftAddsToAPlaylist() {
        row()
        rule.onNodeWithTag("row").performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.onNodeWithTag("row").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(listOf("queue", "playlist"), did)
    }

    @Test fun aSongOnlyOnYourPcCanOnlyGoIntoAPlaylist() {
        row(missing = true)
        rule.onNodeWithTag("row").performTouchInput { swipeRight() }
        rule.waitForIdle()
        rule.onNodeWithTag("row").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(listOf("playlist"), did)
    }
}
