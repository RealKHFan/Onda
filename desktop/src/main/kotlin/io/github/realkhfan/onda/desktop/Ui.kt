package io.github.realkhfan.onda.desktop

import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.realkhfan.onda.core.AutoTree
import io.github.realkhfan.onda.core.Grouping
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Deezer
import io.github.realkhfan.onda.data.FreeCatalogs
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

val Bg = Color(0xFF0F0F12)
val Side = Color(0xFF09090B)
val Raised = Color(0xFF1C1C22)
val Accent = Color(0xFF9D7BFF)
val TextMain = Color(0xFFF2F2F5)
val TextDim = Color(0xFFA7A7B0)

private enum class Page(val label: String, val icon: ImageVector) {
    SONGS("Songs", Icons.Rounded.MusicNote), ALBUMS("Albums", Icons.Rounded.Album), ARTISTS("Artists", Icons.Rounded.Person),
    SEARCH("Search", Icons.Rounded.Search), SETTINGS("Settings", Icons.Rounded.Settings),
}

/** A list opened from Albums or Artists. */
private data class Detail(val title: String, val subtitle: String, val art: String?, val tracks: List<Track>)

@Composable
fun OndaDesktop(state: AppState, player: Player?) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Bg, onPrimary = Color.Black)) {
        var page by remember { mutableStateOf(Page.SONGS) }
        var detail by remember { mutableStateOf<Detail?>(null) }
        var showQueue by remember { mutableStateOf(false) }
        Column(Modifier.fillMaxSize().background(Bg)) {
            Row(Modifier.weight(1f)) {
                Column(Modifier.width(220.dp).fillMaxHeight().background(Side).padding(12.dp)) {
                    Text("Onda", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.padding(8.dp, 4.dp, 8.dp, 16.dp))
                    Page.entries.forEach { p ->
                        val on = page == p && detail == null
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (on) Raised else Color.Transparent)
                                .clickable { page = p; detail = null }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(p.icon, null, tint = if (on) Accent else TextDim)
                            Spacer(Modifier.width(12.dp))
                            Text(p.label, color = if (on) TextMain else TextDim, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (state.scanning || state.syncing) {
                        Text(if (state.scanning) "Scanning your folders…" else "Syncing your server…", color = TextDim, fontSize = 12.sp)
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp), color = Accent, trackColor = Raised)
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    if (player == null) {
                        Note("Onda needs GStreamer to play music. On Bazzite it's already there; on other systems install gstreamer1 and its plugins.")
                    }
                    val play = { list: List<Track>, i: Int -> player?.play(list.map(state::item), i); Unit }
                    val d = detail
                    when {
                        d != null -> SongList(d.title, d.subtitle, d.tracks, player, play, art = d.art)
                        page == Page.SONGS -> Songs(state, player, play)
                        page == Page.ALBUMS -> Albums(state) { a -> detail = Detail(a.title, "Album · ${a.artist}", a.artUri, a.tracks) }
                        page == Page.ARTISTS -> Artists(state) { a -> detail = Detail(a.name, "Artist", a.artUri, a.tracks) }
                        page == Page.SEARCH -> Search(state, player)
                        else -> SettingsPage(state)
                    }
                }
                if (showQueue && player != null) QueuePanel(player) { showQueue = false }
            }
            PlayerBar(player, state) { showQueue = !showQueue }
        }
    }
}

@Composable
private fun Note(text: String) = Text(text, color = TextDim, modifier = Modifier.padding(32.dp))

@Composable
private fun Art(model: String?, size: Int, round: Boolean = false) {
    val shape = if (round) CircleShape else RoundedCornerShape(6.dp)
    Box(Modifier.size(size.dp).clip(shape).background(Raised), contentAlignment = Alignment.Center) {
        if (model == null) Icon(Icons.Rounded.MusicNote, null, tint = Accent)
        else AsyncImage(if (model.startsWith("/")) File(model) else model, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

private fun time(ms: Long): String = (ms / 1000).let { "%d:%02d".format(it / 60, it % 60) }

@Composable
private fun Header(title: String, subtitle: String, art: String? = null, action: @Composable () -> Unit = {}) {
    Row(Modifier.padding(24.dp, 24.dp, 24.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (art != null) { Art(art, 96); Spacer(Modifier.width(20.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = TextDim)
        }
        action()
    }
}

@Composable
private fun SongRow(n: Int?, title: String, artist: String, album: String, durMs: Long, art: String?, playing: Boolean, note: String? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (n != null) Text("$n", color = TextDim, modifier = Modifier.width(36.dp))
        Art(art, 40)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1.4f)) {
            Text(title, color = if (playing) Accent else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(note, artist).joinToString(" · "), color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(album, color = TextDim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        Text(if (durMs > 0) time(durMs) else "", color = TextDim, fontSize = 13.sp, modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun SongList(title: String, subtitle: String, tracks: List<Track>, player: Player?, play: (List<Track>, Int) -> Unit, art: String? = null) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Header(title, subtitle, art) {
                if (tracks.isNotEmpty()) Button(onClick = { play(tracks, 0) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
            }
        }
        itemsIndexed(tracks) { i, t ->
            SongRow(i + 1, t.title, t.artist, t.album, t.durationMs, t.artUri, player?.current?.id == t.id) { play(tracks, i) }
        }
    }
}

@Composable
private fun Songs(state: AppState, player: Player?, play: (List<Track>, Int) -> Unit) {
    var filter by remember { mutableStateOf("") }
    val all = remember(state.library) { Grouping.songsSorted(state.library) }
    val shown = remember(all, filter) { if (filter.isBlank()) all else AutoTree.search(all, filter, limit = 2000) }
    if (all.isEmpty() && !state.scanning) return Note("No music yet. Add a folder or your Navidrome server in Settings.")
    Column {
        Row(Modifier.padding(24.dp, 16.dp, 24.dp, 0.dp)) {
            OutlinedTextField(filter, { filter = it }, placeholder = { Text("Filter your songs") }, singleLine = true, modifier = Modifier.width(360.dp))
        }
        SongList("Songs", "${shown.size} songs", shown, player, play)
    }
}

@Composable
private fun Albums(state: AppState, onOpen: (io.github.realkhfan.onda.core.Album) -> Unit) {
    val albums = remember(state.library) { Grouping.albums(state.library) }
    LazyVerticalGrid(GridCells.Adaptive(180.dp), Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(albums) { a ->
            Column(Modifier.clip(RoundedCornerShape(8.dp)).clickable { onOpen(a) }.padding(8.dp)) {
                Art(a.artUri, 164)
                Text(a.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
                Text(a.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Artists(state: AppState, onOpen: (io.github.realkhfan.onda.core.ArtistGroup) -> Unit) {
    val artists = remember(state.library) { Grouping.artists(state.library) }
    LazyColumn(Modifier.fillMaxSize()) {
        item { Header("Artists", "${artists.size} artists") }
        items(artists) { a ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(a) }.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Art(a.artUri, 48, round = true)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(a.name, color = TextMain)
                    Text("${a.tracks.size} songs", color = TextDim, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun Search(state: AppState, player: Player?) {
    var q by remember { mutableStateOf("") }
    var online by remember { mutableStateOf<List<RemoteTrack>>(emptyList()) }
    var free by remember { mutableStateOf<List<RemoteTrack>>(emptyList()) }
    var problem by remember { mutableStateOf<String?>(null) }
    val mine = remember(state.library, q) { AutoTree.search(state.library, q, limit = 20) }
    LaunchedEffect(q) {
        online = emptyList(); free = emptyList(); problem = null
        if (q.trim().length < 2) return@LaunchedEffect
        delay(350) // wait until typing pauses
        coroutineScope {
            val dz = async { runCatching { Deezer.search(q, 20) } }
            val ov = async { runCatching { FreeCatalogs.openverse(q) } }
            val au = async { runCatching { FreeCatalogs.audius(q) } }
            online = dz.await().getOrDefault(emptyList())
            free = (ov.await().getOrDefault(emptyList()) + au.await().getOrDefault(emptyList())).take(10)
            if (dz.await().isFailure && ov.await().isFailure && au.await().isFailure) problem = "No connection to the online catalogs."
        }
    }
    fun playRemote(list: List<RemoteTrack>, i: Int) {
        val items = list.mapNotNull(state::item)
        val start = items.indexOfFirst { it.id == list[i].key || it.title == list[i].title }.coerceAtLeast(0)
        player?.play(items, start)
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(24.dp)) {
                Text("Search", color = TextMain, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(q, { q = it }, placeholder = { Text("Songs and artists, here and online") }, singleLine = true, modifier = Modifier.width(480.dp))
                problem?.let { Text(it, color = TextDim, modifier = Modifier.padding(top = 8.dp)) }
            }
        }
        if (mine.isNotEmpty()) {
            item { Section("In your library") }
            itemsIndexed(mine) { i, t -> SongRow(null, t.title, t.artist, t.album, t.durationMs, t.artUri, player?.current?.id == t.id) { player?.play(mine.map(state::item), i) } }
        }
        if (online.isNotEmpty()) {
            item { Section("Songs") }
            itemsIndexed(online) { i, r -> SongRow(null, r.title, r.artist, r.album, r.durationSec * 1000L, r.cover, player?.current?.id == r.key, "Preview") { playRemote(online, i) } }
        }
        if (free.isNotEmpty()) {
            item { Section("Free full songs") }
            itemsIndexed(free) { i, r -> SongRow(null, r.title, r.artist, r.album, r.durationSec * 1000L, r.cover, player?.current?.id == r.key, "Full · ${r.source.label}") { playRemote(free, i) } }
        }
    }
}

@Composable
private fun Section(text: String) = Text(text, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(24.dp, 16.dp, 24.dp, 6.dp))

@Composable
private fun SettingsPage(state: AppState) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf("") }
    var user by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // only for connecting; never saved
    var connecting by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<String?>(null) }
    var typedFolder by remember { mutableStateOf("") }
    var folderNote by remember { mutableStateOf<String?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Header("Settings", "Onda for Linux · no account, no tracking") }
        item { Section("Music folders") }
        items(state.settings.folders) { f ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f, color = TextMain, modifier = Modifier.weight(1f))
                IconButton(onClick = { state.removeFolder(f) }) { Icon(Icons.Rounded.Close, "Remove $f", tint = TextDim) }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            when (val r = FolderPicker.pick(System.getProperty("user.home") + "/Music")) {
                                is FolderPicker.Result.Picked -> { state.addFolder(r.path); folderNote = null }
                                FolderPicker.Result.Cancelled -> {}
                                FolderPicker.Result.NoPicker -> folderNote = "No folder picker found on this system. Type the folder's path below."
                            }
                        }
                    }) { Text("Add folder") }
                    OutlinedButton(onClick = { state.rescan() }, enabled = !state.scanning) { Text("Rescan") }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(typedFolder, { typedFolder = it }, label = { Text("Or type a folder path") },
                        placeholder = { Text("~/Music") }, singleLine = true, modifier = Modifier.width(420.dp))
                    OutlinedButton(onClick = {
                        val path = FolderPicker.resolve(typedFolder)
                        if (path != null) { state.addFolder(path); typedFolder = ""; folderNote = null } else folderNote = "That folder doesn't exist."
                    }, enabled = typedFolder.isNotBlank()) { Text("Add") }
                }
                folderNote?.let { Text(it, color = TextDim, fontSize = 13.sp) }
            }
        }
        item { Section("Music server (Navidrome)") }
        item {
            val cfg = state.settings.server
            if (cfg != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connected to ${cfg.url} as ${cfg.user} · ${state.serverSongs.size} songs", color = TextMain)
                    state.serverNote?.let { Text(it, color = TextDim) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { state.sync() }, enabled = !state.syncing) { Text("Sync now") }
                        TextButton(onClick = { state.disconnect() }) { Text("Disconnect", color = TextDim) }
                    }
                    Text("Streaming quality", color = TextDim)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Original", 320 to "320 kbps", 192 to "192 kbps", 128 to "128 kbps").forEach { (k, label) ->
                            FilterChip(state.settings.kbps == k, { state.setKbps(k) }, { Text(label) })
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(url, { url = it }, label = { Text("Server address") }, placeholder = { Text("http://192.168.1.20:4533") }, singleLine = true, modifier = Modifier.width(420.dp))
                    OutlinedTextField(user, { user = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.width(420.dp))
                    OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.width(420.dp),
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                    Button(onClick = {
                        connecting = true; problem = null
                        scope.launch { problem = state.connect(url, user, password); password = ""; connecting = false }
                    }, enabled = !connecting && url.isNotBlank() && user.isNotBlank() && password.isNotEmpty()) { Text(if (connecting) "Connecting…" else "Connect") }
                    problem?.let { Text(it, color = Color(0xFFFF6B6B)) }
                    Text("Onda keeps a login token, not your password.", color = TextDim, fontSize = 12.sp)
                }
            }
        }
        item { Section("Privacy") }
        item {
            Text("Search and previews come from Deezer's public API, free full songs from Openverse and Audius. They see your search " +
                "words and IP address, nothing else. Your files and server stay between you and them.", color = TextDim, fontSize = 13.sp)
        }
    }
}

@Composable
private fun QueuePanel(player: Player, onClose: () -> Unit) {
    val upcoming = player.upcoming()
    Column(Modifier.width(320.dp).fillMaxHeight().background(Side)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Queue", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, "Close queue", tint = TextDim) }
        }
        LazyColumn {
            player.current?.let { c -> item { Section("Now playing") }; item { QueueRow(c, true) {} } }
            if (upcoming.isNotEmpty()) item { Section("Next") }
            items(upcoming) { (i, it) -> QueueRow(it, false) { player.jumpTo(i) } }
        }
    }
}

@Composable
private fun QueueRow(it: Player.Item, now: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(it.art, 36)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(it.title, color = if (now) Accent else TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
            Text(it.artist, color = TextDim, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PlayerBar(player: Player?, state: AppState, onQueue: () -> Unit) {
    val cur = player?.current
    Row(Modifier.fillMaxWidth().height(96.dp).background(Side).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (cur != null) {
                Art(cur.art, 56)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(cur.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(if (cur.preview) "Preview · ${cur.artist}" else cur.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    player.error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        Column(Modifier.weight(1.3f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player?.toggleShuffle() }) { Icon(Icons.Rounded.Shuffle, "Shuffle", tint = if (player?.shuffle == true) Accent else TextDim) }
                IconButton(onClick = { player?.previous() }) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = TextMain) }
                IconButton(onClick = { player?.toggle() }, Modifier.size(44.dp).clip(CircleShape).background(Accent)) {
                    Icon(if (player?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (player?.playing == true) "Pause" else "Play", tint = Color.Black)
                }
                IconButton(onClick = { player?.next() }) { Icon(Icons.Rounded.SkipNext, "Next", tint = TextMain) }
                IconButton(onClick = { player?.cycleRepeat() }) {
                    Icon(if (player?.repeat == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", tint = if ((player?.repeat ?: 0) > 0) Accent else TextDim)
                }
            }
            val dur = player?.durationMs ?: 0L
            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableFloatStateOf(0f) }
            val frac = if (dur > 0) (player?.positionMs ?: 0L).toFloat() / dur else 0f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(time(player?.positionMs ?: 0), color = TextDim, fontSize = 11.sp)
                ThinSlider(if (dragging) dragValue else frac, "Position", { dragging = true; dragValue = it },
                    onDone = { if (dur > 0) player?.seek((dragValue * dur).toLong()); dragging = false },
                    modifier = Modifier.width(420.dp).padding(horizontal = 8.dp))
                Text(time(dur), color = TextDim, fontSize = 11.sp)
            }
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onQueue) { Icon(Icons.AutoMirrored.Rounded.QueueMusic, "Queue", tint = TextDim) }
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, null, tint = TextDim)
            ThinSlider((player?.volume ?: 0.8).toFloat(), "Volume", { player?.changeVolume(it.toDouble()) },
                onDone = { player?.let { state.setVolume(it.volume) } }, modifier = Modifier.width(120.dp).padding(start = 8.dp))
        }
    }
}

/** A thin white bar like the phone's: click or drag to set; screen readers can set it too. */
@Composable
private fun ThinSlider(value: Float, label: String, onChange: (Float) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    var width by remember { mutableIntStateOf(1) }
    val change by rememberUpdatedState(onChange)
    val done by rememberUpdatedState(onDone)
    val v = value.coerceIn(0f, 1f)
    Box(
        modifier.height(24.dp).onSizeChanged { width = it.width.coerceAtLeast(1) }
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(v, 0f..1f)
                setProgress { change(it); done(); true }
            }
            .pointerInput(Unit) { detectTapGestures { change((it.x / width).coerceIn(0f, 1f)); done() } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(onDragEnd = { done() }) { c, _ -> change((c.position.x / width).coerceIn(0f, 1f)) }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Raised))
        Box(Modifier.fillMaxWidth(v).height(4.dp).clip(CircleShape).background(TextMain))
    }
}
