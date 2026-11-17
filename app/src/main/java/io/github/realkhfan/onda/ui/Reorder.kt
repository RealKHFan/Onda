package io.github.realkhfan.onda.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/** Drag-to-reorder for a list: the dragged row follows the finger, the rows it passes make room. */
class ReorderState {
    var dragging by mutableIntStateOf(-1)
    var offset by mutableFloatStateOf(0f)
    var rowPx = 0f

    fun target(count: Int): Int =
        if (rowPx <= 0f) dragging else (dragging + (offset / rowPx).roundToInt()).coerceIn(0, count - 1)

    internal fun start(index: Int) { dragging = index; offset = 0f }

    internal fun finish(count: Int, move: (from: Int, to: Int) -> Unit) {
        val from = dragging
        val to = target(count)
        dragging = -1; offset = 0f
        if (from >= 0 && to != from) move(from, to)
    }

    internal fun cancel() { dragging = -1; offset = 0f }
}

@Composable
fun rememberReorder() = remember { ReorderState() }

fun Modifier.reorderRow(s: ReorderState, index: Int, count: Int): Modifier = this
    .zIndex(if (index == s.dragging) 1f else 0f)
    .onSizeChanged { if (s.rowPx == 0f || index == s.dragging) s.rowPx = it.height.toFloat() }
    .graphicsLayer {
        val d = s.dragging
        translationY = when {
            d < 0 -> 0f
            index == d -> s.offset
            else -> {
                val t = s.target(count)
                when {
                    d < t && index in d + 1..t -> -s.rowPx
                    t < d && index in t until d -> s.rowPx
                    else -> 0f
                }
            }
        }
    }

/** The handle; TalkBack gets "Move up" / "Move down" since it can't drag. */
@Composable
fun DragHandle(s: ReorderState, index: Int, count: Int, label: String, onMove: (from: Int, to: Int) -> Unit) {
    val move by rememberUpdatedState(onMove) // the gesture outlives recompositions; a move must see the list as it is now
    Icon(
        Icons.Rounded.DragHandle, "Move $label", tint = TextDim,
        modifier = Modifier.size(48.dp).padding(12.dp)
            .semantics {
                customActions = listOfNotNull(
                    if (index > 0) CustomAccessibilityAction("Move up") { move(index, index - 1); true } else null,
                    if (index < count - 1) CustomAccessibilityAction("Move down") { move(index, index + 1); true } else null,
                )
            }
            .pointerInput(index, count) {
                detectDragGestures(
                    onDragStart = { s.start(index) },
                    onDrag = { change, d -> change.consume(); s.offset += d.y },
                    onDragEnd = { s.finish(count, move) },
                    onDragCancel = { s.cancel() },
                )
            },
    )
}

/**
 * The whole row: hold it, then drag (it lifts with a tick). Put it after the row's clickable (nearer the content): it
 * then sees the finger lift first and takes it, so holding without moving doesn't also open the row.
 */
@Composable
fun Modifier.holdToMove(s: ReorderState, index: Int, count: Int, onMove: (from: Int, to: Int) -> Unit): Modifier {
    val move by rememberUpdatedState(onMove)
    val haptic = LocalHapticFeedback.current
    return pointerInput(index, count) {
        detectDragGesturesAfterLongPress(
            onDragStart = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); s.start(index) },
            onDrag = { change, d -> change.consume(); s.offset += d.y },
            onDragEnd = { s.finish(count, move) },
            onDragCancel = { s.cancel() },
        )
    }
}
