package io.github.realkhfan.onda.desktop

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuRepresentation
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.rememberPopupPositionProviderAtPosition
import kotlinx.coroutines.delay

/** A right-click menu entry that opens [items] beside it when the pointer rests on it ("Add to playlist ›"). */
class SubMenu(label: String, val items: () -> List<ContextMenuItem>) : ContextMenuItem(label, {})

/**
 * The right-click menu in Onda's colours, with [SubMenu]s: they open on hover (or click, or →), and stay open for a moment
 * while the pointer crosses other entries on its way over. Keys: ↑ ↓ to move, Enter to choose, → ← in and out, Esc to close.
 */
object OndaMenus : ContextMenuRepresentation {
    private val Hover = Color(0xFF2E2E38)

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    override fun Representation(state: ContextMenuState, items: () -> List<ContextMenuItem>) {
        val status = state.status as? ContextMenuState.Status.Open ?: return
        val close = { state.status = ContextMenuState.Status.Closed }
        val list = remember(status) { items() }
        var at by remember(status) { mutableIntStateOf(-1) } // highlighted entry (pointer or keys)
        var open by remember(status) { mutableStateOf<SubMenu?>(null) }
        var subAt by remember(status) { mutableIntStateOf(-1) }
        var inSub by remember(status) { mutableStateOf(false) }
        val gap = with(LocalDensity.current) { 8.dp.roundToPx() }
        val beside = remember(gap) { Beside(gap) }
        val subItems = remember(open) { open?.items?.invoke().orEmpty() }
        // Resting on an entry: a submenu opens at once; any other entry closes the open one after a moment.
        LaunchedEffect(at, inSub) {
            val item = list.getOrNull(at)
            if (item is SubMenu) { if (open !== item) { open = item; subAt = -1 } }
            else if (item != null && !inSub) { delay(250); open = null }
        }
        fun choose(item: ContextMenuItem) { if (item is SubMenu) { open = item; subAt = 0 } else { close(); item.onClick() } }
        Popup(
            popupPositionProvider = rememberPopupPositionProviderAtPosition(status.rect.center),
            onDismissRequest = close,
            properties = PopupProperties(focusable = true),
            onKeyEvent = { e ->
                if (e.type != KeyEventType.KeyDown) return@Popup false
                val sub = subAt >= 0 && subItems.isNotEmpty()
                when (e.key) {
                    Key.Escape -> if (sub) subAt = -1 else close()
                    Key.DirectionDown -> if (sub) subAt = (subAt + 1) % subItems.size else at = (at + 1) % list.size
                    Key.DirectionUp -> if (sub) subAt = if (subAt <= 0) subItems.size - 1 else subAt - 1 else at = if (at <= 0) list.size - 1 else at - 1
                    Key.DirectionRight -> (list.getOrNull(at) as? SubMenu)?.let(::choose)
                    Key.DirectionLeft -> subAt = -1
                    Key.Enter, Key.NumPadEnter, Key.Spacebar -> if (sub) choose(subItems[subAt]) else list.getOrNull(at)?.let(::choose)
                    else -> return@Popup false
                }
                true
            },
        ) {
            Panel(scrolls = false) {
                list.forEachIndexed { i, item ->
                    Box {
                        Entry(item.label, sub = item is SubMenu, lit = i == at || open === item,
                            Modifier.onPointerEvent(PointerEventType.Enter) { at = i; inSub = false }) { choose(item) }
                        // Its own popup beside the entry, so the menu itself never moves when it opens.
                        if (open === item) Popup(popupPositionProvider = beside, onDismissRequest = {}, properties = PopupProperties(focusable = false)) {
                            Panel(scrolls = true, Modifier.onPointerEvent(PointerEventType.Enter) { inSub = true }.onPointerEvent(PointerEventType.Exit) { inSub = false }) {
                                subItems.forEachIndexed { j, sub ->
                                    Entry(sub.label, sub = false, lit = j == subAt, Modifier.onPointerEvent(PointerEventType.Enter) { subAt = j }) { choose(sub) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Right of the entry, its first row level with it; to the left of the menu when there's no room, and kept in the window. */
    internal class Beside(val gap: Int) : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
            val right = anchorBounds.right + gap / 2
            val x = if (right + popupContentSize.width <= windowSize.width) right else anchorBounds.left - gap / 2 - popupContentSize.width
            val y = (anchorBounds.top - gap).coerceAtMost(windowSize.height - popupContentSize.height)
            return IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
        }
    }

    @Composable
    private fun Panel(scrolls: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        val shape = RoundedCornerShape(6.dp)
        // A long submenu (many playlists) scrolls instead of running off the window.
        Column(modifier.padding(2.dp).shadow(8.dp, shape).background(Raised, shape).width(IntrinsicSize.Max).widthIn(min = 180.dp, max = 360.dp)
            .then(if (scrolls) Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()) else Modifier).padding(vertical = 4.dp)) { content() }
    }

    @Composable
    private fun Entry(label: String, sub: Boolean, lit: Boolean, modifier: Modifier, onClick: () -> Unit) {
        Row(modifier.fillMaxWidth().background(if (lit) Hover else Color.Transparent).clickable(onClick = onClick)
            .padding(start = 16.dp, end = if (sub) 8.dp else 16.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = TextMain, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (sub) {
                Spacer(Modifier.weight(1f).widthIn(min = 16.dp))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TextDim)
            }
        }
    }
}
