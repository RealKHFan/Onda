package io.github.realkhfan.onda.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class HoldToMoveTest {
    @get:Rule val rule = createComposeRule()

    private val moves = mutableListOf<Pair<Int, Int>>()
    private val opened = mutableListOf<Int>()

    private fun rows() = rule.setContent {
        val s = rememberReorder()
        Column {
            repeat(4) { i ->
                Text("Row $i", Modifier.reorderRow(s, i, 4).clickable { opened += i }.holdToMove(s, i, 4) { from, to -> moves += from to to }
                    .fillMaxWidth().height(60.dp).testTag("row$i"))
            }
        }
    }

    @Test fun holdThenDragMovesTheRow() {
        rows()
        rule.onNodeWithTag("row0").performTouchInput {
            // In a test the hold is noticed at the next event, so a tiny move first (on a phone it lifts while the finger rests).
            down(center); advanceEventTime(1_000); moveBy(Offset(0f, 1f)); moveBy(Offset(0f, height * 1f)); moveBy(Offset(0f, height * 1f)); up()
        }
        rule.waitForIdle()
        assertEquals(listOf(0 to 2), moves)
        assertEquals(emptyList<Int>(), opened)
    }

    @Test fun aTapStillOpensAndHoldingAloneMovesNothing() {
        rows()
        rule.onNodeWithTag("row1").performTouchInput { down(center); up() }
        rule.onNodeWithTag("row2").performTouchInput { down(center); advanceEventTime(1_000); up() }
        rule.waitForIdle()
        assertEquals(emptyList<Pair<Int, Int>>(), moves)
        assertEquals(listOf(1), opened)
    }
}
