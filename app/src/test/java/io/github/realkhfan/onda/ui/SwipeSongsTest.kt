package io.github.realkhfan.onda.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SwipeSongsTest {
    @get:Rule val rule = createComposeRule()

    private val skips = mutableListOf<Boolean>()
    private val dx = mutableFloatStateOf(0f)
    private var opened = 0

    private fun bar(hasNext: Boolean = true) = rule.setContent {
        // As in the mini player: a tap opens the full player.
        Box(Modifier.width(360.dp).height(56.dp).testTag("bar").clickable { opened++ }.swipeSongs(dx) { next -> if (next && !hasNext) false else { skips += next; true } })
    }

    @Test fun leftIsNextRightIsBack() {
        bar()
        rule.onNodeWithTag("bar").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        rule.onNodeWithTag("bar").performTouchInput { swipeRight() }
        rule.waitForIdle()
        assertEquals(listOf(true, false), skips)
        assertEquals(0f, dx.floatValue) // the song slides back into place
        assertEquals("a swipe doesn't open the player", 0, opened)
        rule.onNodeWithTag("bar").performClick()
        assertEquals(1, opened)
    }

    @Test fun aShortSlowDragDoesNothing() {
        bar()
        rule.onNodeWithTag("bar").performTouchInput { swipe(center, center.copy(x = center.x - width * 0.1f), durationMillis = 1000) }
        rule.waitForIdle()
        assertEquals(emptyList<Boolean>(), skips)
        assertEquals(0f, dx.floatValue)
    }

    @Test fun noNextSongSpringsBack() {
        bar(hasNext = false)
        rule.onNodeWithTag("bar").performTouchInput { swipeLeft() }
        rule.waitForIdle()
        assertEquals(emptyList<Boolean>(), skips)
        assertEquals(0f, dx.floatValue)
    }
}
