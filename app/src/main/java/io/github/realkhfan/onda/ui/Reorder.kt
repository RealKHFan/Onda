package io.github.realkhfan.onda.ui

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import io.github.realkhfan.onda.core.DragScroll
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import androidx.compose.ui.draw.drawBehind
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

/** The list a dragged row scrolls when the finger nears its top or bottom ([dragScrollArea] tells it where that is). */
class DragScroller(val list: ScrollableState) {
    internal var top = 0f
    internal var bottom = 0f

    /** Scrolls while [s] drags; the row moves with the finger, not with the list. Ends when the drag does. */
    internal suspend fun follow(s: ReorderState, zone: Float, max: Float) {
        while (true) {
            val v = DragScroll.speed(s.pointerY, top, bottom, zone, max)
            if (v != 0f) s.offset += list.scrollBy(v)
            delay(16)
        }
    }
}

@Composable
fun rememberDragScroller(list: ScrollableState) = remember(list) { DragScroller(list) }

/** On the list itself: where its edges are on screen. */
fun Modifier.dragScrollArea(d: DragScroller) = onGloballyPositioned { val b = it.boundsInRoot(); d.top = b.top; d.bottom = b.bottom }

/** Drag-to-reorder for a list: the dragged row follows the finger, the rows it passes make room. */
class ReorderState(internal val scroller: DragScroller? = null) {
    var dragging by mutableIntStateOf(-1)
    var offset by mutableFloatStateOf(0f)
    var rowPx = 0f
    // On screen: the finger, where it holds the row, and where the list has the dragged row now. The row is drawn under the
    // finger from these, since a list that scrolled its place away keeps it just outside the screen.
    internal var pointerY by mutableFloatStateOf(Float.NaN)
    internal var grab = Float.NaN
    internal var rowTop by mutableFloatStateOf(Float.NaN)
    internal val tops = HashMap<Int, Float>()

    internal fun lift(index: Int) = (pointerY - (tops[index] ?: Float.NaN)).also { grab = it; rowTop = tops[index] ?: Float.NaN }

    fun target(count: Int): Int =
        if (rowPx <= 0f) dragging else (dragging + (offset / rowPx).roundToInt()).coerceIn(0, count - 1)

    internal fun start(index: Int) { dragging = index; offset = 0f; pointerY = Float.NaN; grab = Float.NaN }

    internal fun finish(count: Int, move: (from: Int, to: Int) -> Unit) {
        val from = dragging
        val to = target(count)
        dragging = -1; offset = 0f
        if (from >= 0 && to != from) move(from, to)
    }

    internal fun cancel() { dragging = -1; offset = 0f }
}

@Composable
fun rememberReorder(scroller: DragScroller? = null) = remember(scroller) { ReorderState(scroller) }

fun Modifier.reorderRow(s: ReorderState, index: Int, count: Int): Modifier = this
    .zIndex(if (index == s.dragging) 1f else 0f)
    .onSizeChanged { if (s.rowPx == 0f || index == s.dragging) s.rowPx = it.height.toFloat() }
    .onGloballyPositioned { val y = it.positionInRoot().y; s.tops[index] = y; if (index == s.dragging) s.rowTop = y }
    .graphicsLayer {
        val d = s.dragging
        translationY = when {
            d < 0 -> 0f
            index == d -> (s.pointerY - s.grab - s.rowTop).takeUnless { it.isNaN() } ?: s.offset
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
    .drawBehind { if (index == s.dragging) drawRect(Raised) } // the lifted row is solid, readable over the rows it passes

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
            .dragRow(s, index, count, onMove, hold = false),
    )
}

/**
 * The whole row: hold it, then drag (it lifts with a tick). Put it after the row's clickable (nearer the content): it
 * then sees the finger lift first and takes it, so holding without moving doesn't also open the row.
 */
@Composable
fun Modifier.holdToMove(s: ReorderState, index: Int, count: Int, onMove: (from: Int, to: Int) -> Unit): Modifier {
    val haptic = LocalHapticFeedback.current
    return dragRow(s, index, count, onMove, hold = true) { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
}

/**
 * The drag itself. Near the list's top or bottom it scrolls the list, and the row is pinned meanwhile: a lazy list would
 * otherwise drop it once its place scrolls away, and the drag with it.
 */
@Composable
private fun Modifier.dragRow(s: ReorderState, index: Int, count: Int, onMove: (from: Int, to: Int) -> Unit, hold: Boolean, onLift: () -> Unit = {}): Modifier {
    val move by rememberUpdatedState(onMove) // the gesture outlives recompositions; a move must see the list as it is now
    val pins = LocalPinnableContainer.current
    val density = LocalDensity.current
    val where = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val pin = remember { arrayOfNulls<PinnableContainer.PinnedHandle>(1) }
    DisposableEffect(Unit) { onDispose { pin[0]?.release(); pin[0] = null } }
    return onGloballyPositioned { where[0] = it }.pointerInput(index, count, s) {
        coroutineScope {
            var scrolling: Job? = null
            fun at(p: Offset) = where[0]?.takeIf { it.isAttached }?.localToRoot(p)?.y ?: Float.NaN
            fun stop() { scrolling?.cancel(); scrolling = null; pin[0]?.release(); pin[0] = null }
            val start = { p: Offset ->
                onLift(); s.start(index); s.pointerY = at(p); s.lift(index); pin[0] = pins?.pin()
                s.scroller?.let { d -> scrolling = launch { with(density) { d.follow(s, zone = 72.dp.toPx(), max = 18.dp.toPx()) } } }
                Unit
            }
            val drag = { c: PointerInputChange, d: Offset -> c.consume(); s.offset += d.y; s.pointerY = at(c.position) }
            val end = { stop(); s.finish(count, move) }
            val cancel = { stop(); s.cancel() }
            if (hold) detectDragGesturesAfterLongPress(onDragStart = start, onDrag = drag, onDragEnd = end, onDragCancel = cancel)
            else detectDragGestures(onDragStart = start, onDrag = drag, onDragEnd = end, onDragCancel = cancel)
        }
    }
}
