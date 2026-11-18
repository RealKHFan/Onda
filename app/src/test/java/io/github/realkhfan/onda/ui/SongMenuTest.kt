package io.github.realkhfan.onda.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The song menu stays put when a swipe up is let go: Material's sheet sprang past its top and back (the menu jumped). */
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h914dp-xxhdpi")
class SongMenuTest {
    @get:Rule val rule = createComposeRule()

    private fun top() = rule.onNodeWithText("Paper Moon").fetchSemanticsNode().positionInWindow.y

    private fun menu(n: Int) {
        rule.setContent { SongMenu("Paper Moon", "Velvet Hours", null, List(n) { MenuAction("Action $it", Icons.Rounded.MusicNote) {} }) {} }
        rule.waitForIdle()
    }

    /** Swipes up on [node] and lets go; where the menu is on each frame after that. */
    private fun afterSwipeUp(node: SemanticsNodeInteraction): List<Float> {
        rule.mainClock.autoAdvance = false
        node.performTouchInput { down(center); repeat(30) { moveBy(Offset(0f, -40f)) }; up() }
        val tops = (1..60).map { rule.mainClock.advanceTimeByFrame(); top() }
        rule.mainClock.autoAdvance = true
        return tops
    }

    private fun assertStill(tops: List<Float>) = assertEquals(tops.drop(2).toSet().toString(), 1, tops.drop(2).toSet().size)

    @Test fun looks() {
        menu(10)
        com.github.takahirom.roborazzi.captureScreenRoboImage("screenshots/menu.png")
    }

    @Test fun aShortMenuDoesntJump() { menu(8); assertStill(afterSwipeUp(rule.onNodeWithText("Action 3"))) }
    @Test fun aLongMenuScrollsToItsEndAndStaysThere() { menu(17); assertStill(afterSwipeUp(rule.onNodeWithText("Action 3"))) }
    @Test fun itsHandleDoesntJumpEither() { menu(8); assertStill(afterSwipeUp(rule.onNodeWithTag("sheet-handle"))) }

    @Test fun aSwipeDownStillClosesIt() {
        var closed = false
        rule.setContent { SongMenu("Paper Moon", "Velvet Hours", null, List(8) { MenuAction("Action $it", Icons.Rounded.MusicNote) {} }) { closed = true } }
        rule.waitForIdle()
        rule.onNodeWithTag("sheet-handle").performTouchInput { swipeDown(startY = centerY, endY = centerY + 1500f, durationMillis = 150) }
        rule.waitForIdle()
        org.junit.Assert.assertTrue(closed)
    }
}
