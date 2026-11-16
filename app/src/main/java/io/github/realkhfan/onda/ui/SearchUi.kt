package io.github.realkhfan.onda.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import io.github.realkhfan.onda.data.UserCollection
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.rounded.Download
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.core.RemoteArtist
import io.github.realkhfan.onda.core.RemoteResults
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Deezer
import io.github.realkhfan.onda.data.FreeCatalogs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal enum class SearchState { Idle, Loading, Done, Error }

/** What a catalog row needs from the app: owned copies, the Want list, and what's playing. */
internal class RemoteRowContext(
    val localFor: (RemoteTrack) -> Track?,
    val wantKeys: Set<String>,
    val currentId: String?,
    val onToggleWant: (RemoteTrack) -> Unit,
)

@Composable
internal fun SearchField(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = false,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    TextField(
        value = value,
        onValueChange = onValue,
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton(onClick = { onValue("") }) { Icon(Icons.Rounded.Close, "Clear search") }
        },
        shape = RoundedCornerShape(8.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Raised,
            unfocusedContainerColor = Raised,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Accent,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        modifier = modifier.fillMaxWidth().focusRequester(focus),
    )
    if (autoFocus) LaunchedEffect(Unit) { focus.requestFocus() }
}

/** Online catalog search. Local-file search lives in Your Library. */
@Composable
internal fun SearchScreen(
    query: String,
    onQuery: (String) -> Unit,
    state: SearchState,
    results: RemoteResults,
    row: RemoteRowContext,
    errorDetail: String?,
    onOpenAppInfo: () -> Unit,
    onPlay: (RemoteTrack) -> Unit,
    onOpenArtist: (RemoteArtist) -> Unit,
    onOpenAlbum: (RemoteAlbum) -> Unit,
    recent: List<String> = emptyList(),
    onForget: (String?) -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf("All") }
    Column(Modifier.fillMaxSize()) {
        Text("Search", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 24.sp,
            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp))
        SearchField(query, onQuery, "What do you want to listen to?", Modifier.padding(horizontal = 16.dp))
        // Same tabs as the desktop's search.
        if (query.isNotBlank()) Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp, 10.dp, 16.dp, 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) { listOf("All", "Songs", "Artists", "Albums", "Free songs").forEach { t -> Chip(t, t == tab) { tab = t } } }
        if (state == SearchState.Loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp, 8.dp), color = Accent, trackColor = Raised)
        }
        val q = query.trim()
        when {
            q.isEmpty() && recent.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                item {
                    Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent searches", color = TextMain, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onForget(null) }) { Text("Clear", color = TextDim) }
                    }
                }
                items(recent) { r ->
                    Row(Modifier.fillMaxWidth().clickable { onQuery(r) }.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, null, tint = TextDim, modifier = Modifier.size(20.dp))
                        Text(r, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 14.dp))
                        IconButton(onClick = { onForget(r) }) { Icon(Icons.Rounded.Close, "Remove $r from recent searches", tint = TextDim) }
                    }
                }
            }
            q.length < 2 -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Find any song", color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(
                    "Searches Deezer (and Apple, if on in Settings) anonymously — no account. Tap a song for a " +
                        "30-second preview, or + to save it to Want to listen. Songs you already own play in full.",
                    color = TextDim, fontSize = 14.sp,
                )
            }
            state == SearchState.Error -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Can't reach the catalog", color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                Text(
                    "If the internet works in other apps, Onda's Network permission is off. GrapheneOS switches " +
                        "it off when an update adds network access. Open Onda's app info → Permissions → Network → Allow.",
                    color = TextDim, fontSize = 14.sp,
                )
                Button(onClick = onOpenAppInfo) { Text("Open Onda's app info") }
                if (errorDetail != null) Text("Details: $errorDetail", color = TextDim, fontSize = 11.sp)
            }
            state == SearchState.Done && results.isEmpty ->
                Text("No results for “$q”.", color = TextDim, modifier = Modifier.padding(24.dp))
            else -> ResultsList(q, results, row, onPlay, onOpenArtist, onOpenAlbum, tab)
        }
    }
}

@Composable
private fun ResultsList(
    q: String,
    results: RemoteResults,
    row: RemoteRowContext,
    onPlay: (RemoteTrack) -> Unit,
    onOpenArtist: (RemoteArtist) -> Unit,
    onOpenAlbum: (RemoteAlbum) -> Unit,
    tab: String = "All",
) {
    val qn = Match.norm(q)
    val all = tab == "All"
    val top = results.artists.firstOrNull { Match.norm(it.name) == qn }?.takeIf { all }
    val songs = if (all || tab == "Songs") results.tracks else emptyList()
    val artists = if (all || tab == "Artists") results.artists else emptyList()
    val albums = if (all || tab == "Albums") results.albums else emptyList()
    val free = if (all || tab == "Free songs") results.free else emptyList()
    val freeAlbums = if (all || tab == "Free songs" || tab == "Albums") results.freeAlbums else emptyList()
    // A tab shows all of its kind; All keeps the short overview.
    val firstSongs = if (all) 5 else Int.MAX_VALUE
    LazyColumn(Modifier.fillMaxSize().padding(top = 4.dp)) {
        if (top != null) {
            item { SectionLabel("Top result") }
            item { TopArtistCard(top) { onOpenArtist(top) } }
        }
        if (songs.isNotEmpty()) item { SectionLabel("Songs") }
        items(songs.take(firstSongs)) { t -> RemoteRow(t, row, onPlay = { onPlay(t) }) }
        if (artists.isNotEmpty()) {
            item { SectionLabel("Artists") }
            if (all) item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(artists) { a -> ArtistBubble(a) { onOpenArtist(a) } }
                }
            } else items(artists) { a ->
                Row(Modifier.fillMaxWidth().clickable { onOpenArtist(a) }.padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art(a.picture, 52.dp, round = true)
                    Text(a.name, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        if (albums.isNotEmpty()) {
            item { SectionLabel("Albums") }
            if (all) item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(albums) { a -> AlbumCard(a) { onOpenAlbum(a) } }
                }
            } else items(albums) { a -> AlbumListRow(a) { onOpenAlbum(a) } }
        }
        if (free.isNotEmpty()) {
            item { SectionLabel("Free full songs") }
            items(free.take(firstSongs)) { t -> RemoteRow(t, row, onPlay = { onPlay(t) }) }
        }
        if (freeAlbums.isNotEmpty()) {
            item { SectionLabel("Free albums") }
            if (all) item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(freeAlbums) { a -> AlbumCard(a) { onOpenAlbum(a) } }
                }
            } else items(freeAlbums) { a -> AlbumListRow(a) { onOpenAlbum(a) } }
        }
        if (!all && songs.isEmpty() && artists.isEmpty() && albums.isEmpty() && free.isEmpty() && freeAlbums.isEmpty()) {
            item { Text("Nothing under $tab for “$q”.", color = TextDim, modifier = Modifier.padding(24.dp)) }
        }
        if (all && songs.size > 5) {
            item { SectionLabel("More songs") }
            items(songs.drop(5)) { t -> RemoteRow(t, row, onPlay = { onPlay(t) }) }
        }
    }
}

/**
 * A catalog song. [available] is false when there's no preview for us and no owned copy;
 * saved songs pass true because their preview is fetched fresh on play.
 */
@Composable
internal fun RemoteRow(
    r: RemoteTrack,
    row: RemoteRowContext,
    onPlay: () -> Unit,
    number: Int? = null,
    available: Boolean? = null,
    onMenu: (() -> Unit)? = null,
) {
    val local = row.localFor(r)
    val wanted = r.key in row.wantKeys
    val playing = row.currentId == r.key || (local != null && row.currentId == local.id)
    val canPlay = available ?: (local != null || r.preview != null || r.source.full)
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            Text("$number", color = TextDim, textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
        } else {
            Art(r.cover, 48.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(r.title, color = when { playing -> Accent; canPlay -> TextMain; else -> TextDim },
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (local != null) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                val what = when {
                    local != null -> "In your library"
                    r.source.full -> "Full · ${r.source.label}"
                    canPlay -> "Preview"
                    else -> "No preview here"
                }
                Text("$what · ${r.artist}", color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        // Owned songs don't need saving, unless they're already in the list (so they can be removed).
        if (local == null || wanted) {
            IconButton(onClick = { row.onToggleWant(r) }) {
                Icon(
                    if (wanted) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
                    if (wanted) "Remove ${r.title} from Want to listen" else "Add ${r.title} to Want to listen",
                    tint = if (wanted) Accent else TextDim,
                )
            }
        }
        if (onMenu != null) IconButton(onClick = onMenu) { Icon(Icons.Rounded.MoreVert, "More options for ${r.title}", tint = TextDim) }
    }
}

@Composable
private fun AlbumListRow(a: RemoteAlbum, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(a.cover, 52.dp)
        Column(Modifier.padding(start = 12.dp)) {
            Text(a.title, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(a.artist, color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TopArtistCard(a: RemoteArtist, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp)).background(Raised)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Art(a.picture, 88.dp, round = true)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(a.name, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Artist", color = TextDim, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ArtistBubble(a: RemoteArtist, onClick: () -> Unit) {
    Column(Modifier.width(104.dp).clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Art(a.picture, 104.dp, round = true)
        Spacer(Modifier.height(6.dp))
        Text(a.name, color = TextMain, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text("Artist", color = TextDim, fontSize = 11.sp)
    }
}

@Composable
private fun AlbumCard(a: RemoteAlbum, onClick: () -> Unit) {
    Column(Modifier.width(140.dp).clickable(onClick = onClick)) {
        Art(a.cover, 140.dp)
        Spacer(Modifier.height(6.dp))
        Text(a.title, color = TextMain, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(listOfNotNull(typeLabel(a.type), a.year, a.artist).joinToString(" · "),
            color = TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AlbumRow(a: RemoteAlbum, big: Boolean = false, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Art(a.cover, if (big) 88.dp else 64.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.title, color = TextMain, fontWeight = if (big) FontWeight.SemiBold else null,
                maxLines = if (big) 2 else 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(if (big) prettyDate(a.date) else a.year, typeLabel(a.type)).joinToString(" · "),
                color = TextDim, fontSize = 12.sp)
        }
    }
}

/** Artist page: Latest release, Popular (previews), then every release newest first. */
@Composable
internal fun RemoteArtistScreen(
    artist: RemoteArtist,
    row: RemoteRowContext,
    onBack: () -> Unit,
    onPlayList: (List<RemoteTrack>, Int, String) -> Unit,
    onOpenAlbum: (RemoteAlbum) -> Unit,
    onRadio: () -> Unit = {},
) {
    var picture by remember(artist.id) { mutableStateOf(artist.picture) }
    var top by remember(artist.id) { mutableStateOf<List<RemoteTrack>?>(null) }
    var releases by remember(artist.id) { mutableStateOf<List<RemoteAlbum>?>(null) }
    var failed by remember(artist.id) { mutableStateOf(false) }
    LaunchedEffect(artist.id) {
        coroutineScope {
            val t = async { runCatching { Deezer.artistTop(artist.id) }.getOrNull() }
            val a = async { runCatching { Deezer.artistAlbums(artist.id, artist.name) }.getOrNull() }
            val p = async { picture ?: runCatching { Deezer.artist(artist.id)?.picture }.getOrNull() }
            top = t.await(); releases = a.await(); picture = p.await()
            failed = top == null && releases == null
        }
    }
    val popular = top.orEmpty()
    val latest = releases?.firstOrNull()
    val rest = releases?.drop(1).orEmpty()
    Column(Modifier.fillMaxSize()) {
        BackTopBar(artist.name, onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp, 0.dp, 16.dp, 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Art(picture, 160.dp, round = true)
                    Spacer(Modifier.height(12.dp))
                    Text(artist.name, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 26.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (popular.any { it.preview != null || row.localFor(it) != null }) {
                            Button(onClick = { onPlayList(popular, -1, artist.name) }) {
                                Icon(Icons.Rounded.PlayArrow, null); Text("Play popular")
                            }
                        }
                        OutlinedButton(onClick = onRadio) { Icon(Icons.Rounded.Radio, null); Text(" Radio") }
                    }
                    val followed by UserCollection.followed.collectAsState()
                    val marked by UserCollection.aiMarked.collectAsState()
                    val following = followed.any { it.id == artist.id }
                    val ai = remember(marked) { UserCollection.isAi(artist.name) }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { UserCollection.toggleFollow(artist.copy(picture = picture)) }) {
                            Text(if (following) "Following" else "Follow", color = if (following) Accent else TextMain)
                        }
                        val blockedNow by UserCollection.blocked.collectAsState()
                        val noSuggest = remember(blockedNow) { UserCollection.isBlocked(artist.name) }
                        TextButton(onClick = { UserCollection.toggleBlocked(artist.name) }) {
                            Text(if (noSuggest) "Suggest again" else "Don't suggest", color = TextDim)
                        }
                        if (!UserCollection.onCommunityList(artist.name)) {
                            TextButton(onClick = { UserCollection.toggleAi(artist.name) }) {
                                Text(if (ai) "Not AI after all" else "This is AI", color = TextDim)
                            }
                        }
                    }
                    if (ai) {
                        Text("AI-generated: hidden from search, mixes and new releases.", color = TextDim, fontSize = 12.sp,
                            textAlign = TextAlign.Center)
                    }
                }
            }
            if (top == null && releases == null && !failed) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp), color = Accent, trackColor = Raised) }
            }
            if (failed) item { Text("Couldn't load this artist. Check your connection.", color = TextDim, modifier = Modifier.padding(24.dp)) }
            if (latest != null) {
                item { SectionLabel("Latest release") }
                item { AlbumRow(latest, big = true) { onOpenAlbum(latest) } }
            }
            if (popular.isNotEmpty()) item { SectionLabel("Popular") }
            itemsIndexed(popular) { i, t -> RemoteRow(t, row, onPlay = { onPlayList(popular, i, artist.name) }, number = i + 1) }
            if (rest.isNotEmpty()) item { SectionLabel("Releases") }
            items(rest) { a -> AlbumRow(a) { onOpenAlbum(a) } }
        }
    }
}

/** Album page: every track, previews play through in order like a normal album. */
@Composable
internal fun RemoteAlbumScreen(
    album: RemoteAlbum,
    row: RemoteRowContext,
    onBack: () -> Unit,
    onPlayList: (List<RemoteTrack>, Int, String) -> Unit,
    onOpenArtist: (RemoteArtist) -> Unit,
    onDownload: (List<RemoteTrack>) -> Unit = {},
) {
    var info by remember(album) { mutableStateOf(album) }
    var tracks by remember(album) { mutableStateOf<List<RemoteTrack>?>(null) }
    var failed by remember(album) { mutableStateOf(false) }
    LaunchedEffect(album) {
        try {
            val (a, t) = if (album.source == Source.ARCHIVE) FreeCatalogs.archiveItem(album.ref) else Deezer.album(album.id)
            info = a; tracks = t
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            failed = true
        }
    }
    val list = tracks.orEmpty()
    Column(Modifier.fillMaxSize()) {
        BackTopBar(info.title, onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp, 0.dp, 16.dp, 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Art(info.cover, 200.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(info.title, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
                    Text(
                        info.artist,
                        color = if (info.artistId > 0) Accent else TextDim,
                        fontWeight = FontWeight.SemiBold,
                        modifier = if (info.artistId > 0) {
                            Modifier.clickable { onOpenArtist(RemoteArtist(info.artistId, info.artist, null)) }.padding(8.dp)
                        } else {
                            Modifier.padding(8.dp)
                        },
                    )
                    Text(listOfNotNull(typeLabel(info.type), prettyDate(info.date), tracks?.let { songCount(it.size) }).joinToString(" · "),
                        color = TextDim, fontSize = 13.sp)
                    if (list.any { it.preview != null || row.localFor(it) != null }) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onPlayList(list, -1, info.title) }) { Icon(Icons.Rounded.PlayArrow, null); Text("Play") }
                            if (list.any { it.canDownload }) {
                                OutlinedButton(onClick = { onDownload(list) }) { Icon(Icons.Rounded.Download, null); Text("Download") }
                            }
                        }
                    }
                    val saved by UserCollection.albums.collectAsState()
                    val isSaved = remember(saved, info) { UserCollection.isSaved(info) }
                    TextButton(onClick = { UserCollection.toggleAlbum(info) }) {
                        Icon(if (isSaved) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline, null, tint = if (isSaved) Accent else TextDim)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isSaved) "Saved to your library" else "Save to your library", color = if (isSaved) Accent else TextDim)
                    }
                }
            }
            if (tracks == null && !failed) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp), color = Accent, trackColor = Raised) }
            }
            if (failed) item { Text("Couldn't load this album. Check your connection.", color = TextDim, modifier = Modifier.padding(24.dp)) }
            itemsIndexed(list) { i, t -> RemoteRow(t, row, onPlay = { onPlayList(list, i, info.title) }, number = i + 1) }
        }
    }
}

@Composable
internal fun WantScreen(
    items: List<RemoteTrack>,
    row: RemoteRowContext,
    onBack: () -> Unit,
    onPlay: (RemoteTrack) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Want to listen", onBack)
        Row(Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.Bookmark, 72.dp)
            Spacer(Modifier.width(12.dp))
            Text(songCount(items.size), color = TextDim, fontSize = 13.sp)
        }
        if (items.isEmpty()) {
            Text("Nothing saved yet. Tap + on a search result to keep it here.", color = TextDim, modifier = Modifier.padding(24.dp))
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(items) { r -> RemoteRow(r, row, onPlay = { onPlay(r) }, available = true) }
        }
    }
}

private fun typeLabel(type: String?): String? = when (type) {
    "album" -> "Album"
    "single" -> "Single"
    "ep" -> "EP"
    "compile" -> "Compilation"
    else -> null
}

/** "2026-11-14" -> "14 Nov 2026"; unknown formats pass through. */
private fun prettyDate(date: String?): String? =
    date?.let { runCatching { LocalDate.parse(it).format(DateTimeFormatter.ofPattern("d MMM yyyy")) }.getOrDefault(it) }
