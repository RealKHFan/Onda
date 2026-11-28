package io.github.realkhfan.onda.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Link
import io.github.realkhfan.onda.player.LinkDataSource
import io.github.realkhfan.onda.player.PlayerState
import io.github.realkhfan.onda.player.toMediaItem
import kotlinx.coroutines.delay

/** Your PC's Proton Drive songs on the phone: a copy you have here plays as yours, the rest one by one; mapped by name. */
internal fun protonTracks(songs: List<LinkSync.Song>, library: List<Track>): List<Track> {
    val lib = LinkSync.Library(library, library, emptyMap())
    val byId = library.associateBy { it.id }
    return songs.mapNotNull { s ->
        lib.local(s.id)?.let(byId::get)
            ?: LinkSync.missingTrack(LinkSync.Library.PLACEHOLDER + s.artist + '\u001F' + s.title)?.copy(album = s.album, albumArtist = s.artist, durationMs = s.ms)
    }
}

/**
 * The songs in your PC's Proton Drive folders. The phone can't reach Proton Drive itself, so they play through your PC
 * while both are on your Wi-Fi (the PC downloads each one from Drive); away from home they're greyed out, except the ones
 * you got onto this phone (⋮ → Get it, or Get all).
 */
@Composable
internal fun ProtonScreen(
    songs: List<LinkSync.Song>, library: List<Track>, pc: Link.Pc?, player: PlayerState?,
    onBack: () -> Unit, onMenu: (Track) -> Unit, onQueue: (Track) -> Unit, onGetAll: (List<Track>) -> Unit,
) {
    val tracks = remember(songs, library) { protonTracks(songs, library) }
    var reachable by remember { mutableStateOf<Boolean?>(null) }
    // The PC is asked while this page is open (the link's own listening runs only while music plays).
    LaunchedEffect(pc) {
        if (pc == null) { reachable = false; return@LaunchedEffect }
        while (true) { reachable = Link.state() != null; delay(20_000) }
    }
    val onPc = tracks.filter { LinkSync.isMissing(it.id) }
    fun play(i: Int) {
        val items = tracks.mapIndexedNotNull { j, t ->
            when {
                !LinkSync.isMissing(t.id) -> j to t.toMediaItem()
                reachable == true -> j to LinkDataSource.item(t)
                else -> null
            }
        }
        val at = items.indexOfFirst { it.first == i }.takeIf { it >= 0 } ?: items.indices.firstOrNull() ?: return
        player?.playItems(items.map { it.second }, at, "Proton Drive", pc?.name ?: "Your PC", shuffled = false)
    }
    Column(Modifier.fillMaxSize()) {
        BackTopBar("Proton Drive", onBack)
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Column(Modifier.padding(16.dp, 0.dp, 16.dp, 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(Icons.Rounded.Cloud, 72.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(songCount(tracks.size) + " · on ${pc?.name ?: "your PC"}'s Proton Drive", color = TextDim, fontSize = 13.sp)
                    }
                    Text(
                        when {
                            pc == null -> "Link your PC (Settings → Your PC) to play these."
                            reachable == null -> "Checking ${pc.name}…"
                            reachable == true -> "These play through ${pc.name}. To have one away from home too: ⋮ → Get it from ${pc.name}."
                            else -> "Out of reach: these play through ${pc.name} when you're on the same Wi-Fi with Onda open there. " +
                                "Songs you got onto this phone still play."
                        },
                        color = TextDim, fontSize = 13.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { play(0) }, enabled = tracks.any { !LinkSync.isMissing(it.id) } || reachable == true,
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                            Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play")
                        }
                        if (reachable == true && onPc.isNotEmpty()) OutlinedButton(onClick = { onGetAll(onPc) }) {
                            Icon(Icons.Rounded.Download, null, tint = TextMain); Spacer(Modifier.width(6.dp))
                            Text("Get ${songCount(onPc.size)}", color = TextMain)
                        }
                    }
                }
            }
            if (tracks.isEmpty()) item {
                Text("No songs yet: choose Proton Drive folders on your PC (Settings → Proton Drive), then sync.", color = TextDim, modifier = Modifier.padding(24.dp))
            }
            itemsIndexed(tracks) { i, t ->
                val here = !LinkSync.isMissing(t.id)
                SwipeTrackRow(
                    t, player?.currentId == t.id,
                    onQueue = { if (here) onQueue(t) else player?.addToQueue(LinkDataSource.item(t)) },
                    onMenu = { onMenu(t) },
                    missing = !here && reachable != true,
                ) { play(i) }
            }
        }
    }
}
