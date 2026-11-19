@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package io.github.realkhfan.onda.ui

import io.github.realkhfan.onda.data.Prefs
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.TextButton
import io.github.realkhfan.onda.core.Lrc
import io.github.realkhfan.onda.data.Lrclib
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import io.github.realkhfan.onda.core.ArtColor
import io.github.realkhfan.onda.player.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.sign

private val Dim = Color.White.copy(alpha = 0.7f)

/** The cover's dominant colour, darkened, animated between songs. */
@Composable
internal fun rememberArtTint(uri: String?): Color {
    val ctx = LocalContext.current
    var target by remember { mutableStateOf(Raised) }
    LaunchedEffect(uri) {
        target = uri?.let { runCatching { artTint(ctx, it) }.getOrNull() }?.let { Color(it) } ?: Raised
    }
    return animateColorAsState(target, tween(600), label = "tint").value
}

private suspend fun artTint(ctx: Context, uri: String): Int? {
    val req = ImageRequest.Builder(ctx).data(uri).size(48).allowHardware(false).build()
    val bmp = (SingletonImageLoader.get(ctx).execute(req) as? SuccessResult)?.image?.toBitmap() ?: return null
    return withContext(Dispatchers.Default) {
        val px = IntArray(bmp.width * bmp.height)
        bmp.getPixels(px, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        ArtColor.pick(px)
    }
}

@Composable
internal fun MiniPlayer(player: PlayerState, tint: Color, onOpen: () -> Unit) {
    var pos by remember { mutableLongStateOf(player.position()) }
    LaunchedEffect(player.isPlaying, player.currentId) {
        pos = player.position()
        while (player.isPlaying) { delay(500); pos = player.position() }
    }
    // Right after a skip the next song's length isn't known yet: start, not "full".
    val frac = if (player.durationMs > 0) (pos.toFloat() / player.durationMs).coerceIn(0f, 1f) else 0f
    val dx = remember { mutableFloatStateOf(0f) }
    val skip: suspend (Boolean) -> Boolean = remember(player) {
        { next ->
            if (next && !player.controller.hasNextMediaItem()) false
            else {
                val was = player.currentId
                if (next) player.next() else player.prevSong()
                withTimeoutOrNull(600) { snapshotFlow { player.currentId }.first { it != was } } // slide the new song in, not the old
                true
            }
        }
    }
    Column(
        Modifier.fillMaxWidth().padding(8.dp, 4.dp).clip(RoundedCornerShape(8.dp))
            .background(tint).clickable(onClick = onOpen).swipeSongs(dx, skip)
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Next song") { player.next(); true },
                    CustomAccessibilityAction("Previous song") { player.prevSong(); true },
                )
            },
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Art(player.artUri, 40.dp) // stays put; only the names slide
            Spacer(Modifier.width(10.dp))
            Column(
                Modifier.weight(1f).clipToBounds()
                    .offset { IntOffset(dx.floatValue.roundToInt(), 0) }
                    .graphicsLayer { alpha = 1f - (abs(dx.floatValue) / size.width).coerceIn(0f, 0.8f) },
            ) {
                Text(player.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                Text(artistLine(player), color = Dim, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
            }
            IconButton(onClick = { player.togglePlay() }) {
                Icon(if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    if (player.isPlaying) "Pause" else "Play", tint = TextMain)
            }
            IconButton(onClick = { player.next() }) { Icon(Icons.Rounded.SkipNext, "Next", tint = TextMain) }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(2.dp).background(Color.White.copy(alpha = 0.2f))) {
            Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Color.White))
        }
    }
}

/**
 * Spotify's mini player: swipe left for the next song, right for the one before. [dx] is how far the song is moved.
 * [go] skips (true = next) and returns once the new song shows, or false when there is none (it springs back).
 */
internal fun Modifier.swipeSongs(dx: MutableFloatState, go: suspend (next: Boolean) -> Boolean) = pointerInput(dx, go) {
    coroutineScope {
        val tracker = VelocityTracker()
        var job: Job? = null
        suspend fun to(x: Float, ms: Int) = animate(dx.floatValue, x, animationSpec = tween(ms)) { v, _ -> dx.floatValue = v }
        detectHorizontalDragGestures(
            onDragStart = { job?.cancel(); tracker.resetTracking() },
            onDragEnd = {
                val w = size.width.toFloat()
                val v = tracker.calculateVelocity().x
                // A light flick, or a drag past an eighth of the bar.
                val dir = when { abs(v) > 150.dp.toPx() -> sign(v); abs(dx.floatValue) > w / 8 -> sign(dx.floatValue); else -> 0f }
                job = launch {
                    if (dir == 0f) return@launch to(0f, 180)
                    // Skips right away; the names slide out meanwhile. No next song: it answers at once and springs back.
                    val moved = async(start = CoroutineStart.UNDISPATCHED) { go(dir < 0) }
                    if (moved.isCompleted && !moved.await()) return@launch to(0f, 180)
                    to(dir * w, 90)
                    if (moved.await()) dx.floatValue = -dir * w // the new song comes in from the other side
                    to(0f, 180)
                }
            },
            onDragCancel = { job = launch { to(0f, 180) } },
        ) { change, amount ->
            change.consume()
            dx.floatValue += amount
            tracker.addPosition(change.uptimeMillis, Offset(dx.floatValue, 0f))
        }
    }
}

private fun artistLine(p: PlayerState) = when {
    io.github.realkhfan.onda.player.Remote.on.value -> "On ${io.github.realkhfan.onda.data.Link.pc.value?.name ?: "your PC"} · ${p.artist}"
    p.isPreview -> "Preview · ${p.artist}"
    p.fullFrom != null -> "${p.fullFrom!!.label} · ${p.artist}"
    else -> p.artist
}

/**
 * Spotify-style full player. [saved] is "liked" for a local song, "in Want to listen" for a preview.
 * Slides in/out via the caller's AnimatedVisibility; dragging it down closes it.
 */
@Composable
internal fun FullPlayer(
    player: PlayerState,
    tint: Color,
    saved: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onMenu: () -> Unit,
    onQueue: () -> Unit,
    onDevices: () -> Unit,
    badge: String? = null,
) {
    val scope = rememberCoroutineScope()
    val close by rememberUpdatedState(onClose)
    var dragY by remember { mutableFloatStateOf(0f) }
    var pos by remember { mutableLongStateOf(player.position()) }
    var seeking by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(player.isPlaying, player.currentId) {
        pos = player.position()
        while (player.isPlaying) { delay(250); pos = player.position() }
    }
    val dur = player.durationMs.coerceAtLeast(1L)
    val shown = seeking ?: pos
    var showLyrics by remember { mutableStateOf(false) }
    BackHandler(enabled = showLyrics) { showLyrics = false } // composed later, so it wins over "close player"

    Box(Modifier.fillMaxSize()) {
    Box(
        Modifier.fillMaxSize()
            .offset { IntOffset(0, dragY.roundToInt()) }
            .background(Brush.verticalGradient(0f to tint, 0.7f to Bg))
            .pointerInput(Unit) {
                val tracker = VelocityTracker()
                detectVerticalDragGestures(
                    onDragStart = { tracker.resetTracking() },
                    onDragEnd = {
                        // Close on a long-enough pull or a quick flick; otherwise spring back.
                        if (dragY > size.height * 0.2f || tracker.calculateVelocity().y > 1500f) close()
                        else scope.launch { animate(dragY, 0f) { v, _ -> dragY = v } }
                    },
                    onDragCancel = { scope.launch { animate(dragY, 0f) { v, _ -> dragY = v } } },
                ) { change, dy ->
                    change.consume()
                    dragY = (dragY + dy).coerceAtLeast(0f)
                    tracker.addPosition(change.uptimeMillis, Offset(0f, dragY))
                }
            },
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Close player", tint = TextMain) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYING FROM ${player.sourceKind.uppercase()}", color = Dim, fontSize = 10.sp,
                        letterSpacing = 1.sp, maxLines = 1)
                    Text(player.sourceLabel, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onMenu) { Icon(Icons.Rounded.MoreVert, "More options", tint = TextMain) }
            }
            Spacer(Modifier.weight(1f))
            Art(player.artUri, 0.dp, Modifier.fillMaxWidth().aspectRatio(1f))
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(player.title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artistLine(player), color = Dim, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    // Always takes its line, so cover and names don't move when the quality is read a moment later.
                    Text(badge.orEmpty(), color = Dim, fontSize = 11.sp, letterSpacing = 0.5.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                }
                IconButton(onClick = onSave) {
                    if (player.catalog != null) { // catalog songs go to Want to listen; Liked Songs holds your own
                        Icon(if (saved) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
                            if (saved) "Remove from Want to listen" else "Add to Want to listen",
                            tint = if (saved) Accent else TextMain)
                    } else {
                        Icon(if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            if (saved) "Remove from Liked Songs" else "Add to Liked Songs",
                            tint = if (saved) Accent else TextMain)
                    }
                }
            }
            SeekBar(
                frac = if (player.durationMs > 0) shown.toFloat() / dur else 0f, // length not known yet (just skipped): at the start
                onSeeking = { seeking = (it * dur).toLong() },
                onSeekDone = { val ms = (it * dur).toLong(); player.seekTo(ms); pos = ms; seeking = null },
            )
            Row(Modifier.fillMaxWidth()) {
                Text(fmt(shown), color = Dim, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Text(if (player.durationMs > 0) fmt(dur) else "–:––", color = Dim, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = { player.toggleShuffle() }) {
                    Icon(Icons.Rounded.Shuffle, if (player.shuffle) "Shuffle on" else "Shuffle off",
                        tint = if (player.shuffle) Accent else TextMain)
                }
                IconButton(onClick = { player.prev() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous", tint = TextMain, modifier = Modifier.size(36.dp))
                }
                Box(
                    Modifier.size(68.dp).clip(CircleShape).background(Accent).clickable { player.togglePlay() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (player.isPlaying) "Pause" else "Play", tint = Color(0xFF141018), modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = { player.next() }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, "Next", tint = TextMain, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = { player.cycleRepeat() }) {
                    Icon(
                        if (player.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        when (player.repeatMode) { Player.REPEAT_MODE_OFF -> "Repeat off"; Player.REPEAT_MODE_ONE -> "Repeat one"; else -> "Repeat all" },
                        tint = if (player.repeatMode == Player.REPEAT_MODE_OFF) TextMain else Accent,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDevices) { Icon(Icons.Rounded.Devices, "Devices", tint = Dim) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { showLyrics = true }) {
                    Icon(Icons.Rounded.Lyrics, null, tint = Dim)
                    Spacer(Modifier.width(6.dp))
                    Text("Lyrics", color = Dim)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onQueue) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", tint = Dim) }
            }
        }
    }
    // A sibling on top, not a child, so scrolling lyrics never triggers the player's swipe-down.
    if (showLyrics) {
        LyricsPanel(player, shown, tint, onClose = { showLyrics = false }, onSeek = { player.seekTo(it); pos = it })
    }
    }
}

private sealed interface LyricsLoad {
    data object Loading : LyricsLoad
    data object Failed : LyricsLoad
    data class Ready(val lyrics: Lrclib.Lyrics) : LyricsLoad
}

/** Full-screen lyrics. Fetched only when opened; synced lines follow the song, tap a line to jump there. */
@Composable
private fun LyricsPanel(player: PlayerState, posMs: Long, tint: Color, onClose: () -> Unit, onSeek: (Long) -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // The version you picked (LRCLIB id, 0 = the one found) and how far the lines are moved (+ = earlier), like on the desktop.
    var pick by remember(player.currentId) { mutableStateOf(player.currentId?.let { Prefs.lyricsPick(ctx, it) } ?: (0L to 0L)) }
    var scale by remember { mutableStateOf(Prefs.lyricsScale(ctx)) }
    var choosing by remember { mutableStateOf(false) }
    fun setPick(id: Long, shift: Long) { pick = id to shift; player.currentId?.let { Prefs.setLyricsPick(ctx, it, id, shift) } }
    var load by remember(player.currentId, pick.first) { mutableStateOf<LyricsLoad>(LyricsLoad.Loading) }
    LaunchedEffect(player.currentId, pick.first) {
        if (player.isPreview) return@LaunchedEffect
        load = try {
            if (pick.first > 0) Lrclib.byId(pick.first)?.let { LyricsLoad.Ready(it) } ?: LyricsLoad.Failed
            else LyricsLoad.Ready(Lrclib.get(player.title, player.artist, player.album, (player.durationMs / 1000).toInt()))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            LyricsLoad.Failed
        }
    }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to tint, 1f to Bg)).pointerInput(Unit) {}) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.KeyboardArrowDown, "Close lyrics", tint = TextMain) }
                Column(Modifier.weight(1f)) {
                    Text(player.title, color = TextMain, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(player.artist, color = Dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(12.dp))
            val l = (load as? LyricsLoad.Ready)?.lyrics
            val hint = when {
                player.isPreview || l == null || l.instrumental -> null
                l.synced.isNotEmpty() -> if (l.verified) null else "These lyrics may be from another version of this song."
                !l.plain.isNullOrBlank() -> if (l.verified) "These lyrics aren't synced to the song." else "These lyrics aren't synced and may be from another version of this song."
                else -> null
            }
            hint?.let { Text(it, color = Dim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp)) }
            if (!player.isPreview) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                val shift = pick.second
                if (l != null && l.synced.isNotEmpty()) {
                    TextButton(onClick = { setPick(pick.first, shift + 500) }) { Text("Earlier", color = Dim, fontSize = 13.sp) }
                    Text(if (shift == 0L) "in time" else "%+.1f s".format(shift / 1000.0), color = Dim, fontSize = 13.sp)
                    TextButton(onClick = { setPick(pick.first, shift - 500) }) { Text("Later", color = Dim, fontSize = 13.sp) }
                }
                TextButton(onClick = { choosing = true }) { Text("Other lyrics…", color = Dim, fontSize = 13.sp) }
                val text = l?.let { it.synced.takeIf { s -> s.isNotEmpty() }?.joinToString("\n") { s -> s.text } ?: it.plain }
                if (!text.isNullOrBlank()) TextButton(onClick = {
                    ctx.getSystemService(android.content.ClipboardManager::class.java)
                        ?.setPrimaryClip(android.content.ClipData.newPlainText("Lyrics", "${player.title} — ${player.artist}\n\n$text"))
                }) { Text("Copy", color = Dim, fontSize = 13.sp) }
                TextButton(onClick = { scale = (scale - 0.1f).coerceAtLeast(0.6f); Prefs.setLyricsScale(ctx, scale) }) { Text("A−", color = Dim, fontSize = 13.sp) }
                TextButton(onClick = { scale = (scale + 0.1f).coerceAtMost(1.8f); Prefs.setLyricsScale(ctx, scale) }) { Text("A+", color = Dim, fontSize = 15.sp) }
            }
            Box(Modifier.weight(1f)) {
                when {
                    player.isPreview -> Note("Lyrics are available for songs you own.")
                    load == LyricsLoad.Loading -> Note("Loading lyrics…")
                    load == LyricsLoad.Failed -> Note("Couldn't load lyrics. Check your connection.")
                    l != null && l.synced.isNotEmpty() -> SyncedLyrics(l.synced, posMs + pick.second, scale) { onSeek(it - pick.second) }
                    l != null && !l.plain.isNullOrBlank() -> Text(
                        l.plain, color = TextMain, fontSize = (20 * scale).sp, lineHeight = (30 * scale).sp,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                    l != null && l.instrumental -> Note("Instrumental")
                    else -> Note("No lyrics found for this song.")
                }
            }
            Text("Lyrics from LRCLIB", color = Dim, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
        }
    }
    if (choosing) LyricsChoiceDialog(player.title, player.artist, player.durationMs, pick.first,
        onPick = { setPick(it, pick.second); choosing = false }, onDismiss = { choosing = false })
}

/** Every version LRCLIB has: pick the one that fits (the length helps). */
@Composable
private fun LyricsChoiceDialog(title0: String, artist0: String, durMs: Long, picked: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf(title0) }
    var artist by remember { mutableStateOf(artist0) }
    var round by remember { mutableIntStateOf(0) }
    var found by remember { mutableStateOf<List<Lrclib.Choice>?>(null) }
    LaunchedEffect(round) { found = null; found = runCatching { Lrclib.choices(title, artist) }.getOrDefault(emptyList()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Other lyrics") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { OutlinedTextField(title, { title = it }, singleLine = true, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    OutlinedTextField(artist, { artist = it }, singleLine = true, label = { Text("Artist") }, modifier = Modifier.fillMaxWidth(),
                        trailingIcon = { TextButton(onClick = { round++ }) { Text("Search", color = Accent) } })
                }
                item { Text("Pick the version that fits your song." + if (durMs > 0) " Your song: ${fmt(durMs)}." else "", color = TextDim, fontSize = 12.sp) }
                val list = found
                when {
                    list == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent, trackColor = Raised) }
                    list.isEmpty() -> item { Text("Nothing found. Try other words.", color = TextDim) }
                    else -> items(list) { c ->
                        Column(Modifier.fillMaxWidth().background(if (c.id == picked) Raised else Color.Transparent).clickable { onPick(c.id) }.padding(6.dp)) {
                            Text("${c.track} — ${c.artist}", color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(listOf(c.album, fmt(c.durationSec * 1000L), if (c.lyrics.synced.isNotEmpty()) "synced" else "not synced").filter { it.isNotBlank() }.joinToString(" · "),
                                color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = TextDim) } },
        dismissButton = if (picked > 0) ({ TextButton(onClick = { onPick(0) }) { Text("Use the one found", color = Accent) } }) else null,
        containerColor = CardBg,
    )
}

@Composable
private fun Note(text: String) {
    Text(text, color = Dim, fontSize = 16.sp, modifier = Modifier.padding(top = 24.dp))
}

@Composable
private fun SyncedLyrics(lines: List<io.github.realkhfan.onda.core.LyricLine>, posMs: Long, scale: Float, onSeek: (Long) -> Unit) {
    val current = Lrc.indexAt(lines, posMs + 250) // light the line a moment early, as it starts
    val listState = rememberLazyListState()
    LaunchedEffect(current) { if (current >= 0) listState.animateScrollToItem((current - 2).coerceAtLeast(0)) }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(lines) { i, line ->
            // The line being sung grows; the size eases between lines so the jump isn't jarring.
            val size by animateFloatAsState((if (i == current) 30f else 21f) * scale, tween(250), label = "lyricSize")
            Text(
                line.text.ifBlank { "♪" },
                color = when {
                    i == current -> Color.White
                    i < current -> Color.White.copy(alpha = 0.45f)
                    else -> Color.White.copy(alpha = 0.7f)
                },
                fontSize = size.sp,
                lineHeight = (size * 1.3f).sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().clickable { onSeek(line.timeMs) }.padding(vertical = 8.dp),
            )
        }
    }
}

/** Thin plain-white seek bar; tap or drag. 44dp tall so it's an easy touch target. */
@Composable
private fun SeekBar(frac: Float, onSeeking: (Float) -> Unit, onSeekDone: (Float) -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    val seeking by rememberUpdatedState(onSeeking)
    val done by rememberUpdatedState(onSeekDone)
    val f = frac.coerceIn(0f, 1f)
    Canvas(
        Modifier.fillMaxWidth().height(44.dp)
            .semantics {
                contentDescription = "Song position"
                progressBarRangeInfo = ProgressBarRangeInfo(f, 0f..1f)
                setProgress { v -> done(v.coerceIn(0f, 1f)); true }
            }
            .pointerInput(Unit) { detectTapGestures { o -> done((o.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                var last = 0f
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragging = true; last = (o.x / size.width).coerceIn(0f, 1f); seeking(last) },
                    onDragEnd = { dragging = false; done(last) },
                    onDragCancel = { dragging = false; done(last) },
                ) { change, _ ->
                    change.consume()
                    last = (change.position.x / size.width).coerceIn(0f, 1f)
                    seeking(last)
                }
            },
    ) {
        val y = size.height / 2
        val stroke = 4.dp.toPx()
        drawLine(Color.White.copy(alpha = 0.3f), Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
        drawLine(Color.White, Offset(0f, y), Offset(size.width * f, y), stroke, StrokeCap.Round)
        drawCircle(Color.White, radius = (if (dragging) 8.dp else 6.dp).toPx(), center = Offset(size.width * f, y))
    }
}

internal fun fmt(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
