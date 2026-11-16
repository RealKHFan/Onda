package io.github.realkhfan.onda.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.realkhfan.onda.core.LinkSync
import io.github.realkhfan.onda.core.Source
import io.github.realkhfan.onda.core.Track
import io.github.realkhfan.onda.data.Link
import io.github.realkhfan.onda.player.PlayerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/** What plays on the phone, from the current song on, for the PC. */
internal fun queueHere(p: PlayerState, byId: Map<String, Track>): LinkSync.Queue? {
    val items = p.queue().takeIf { it.isNotEmpty() } ?: return null
    val songs = items.map { q ->
        if (Source.of(q.id) != null) LinkSync.Song(q.id, q.title, q.artist)
        else byId[q.id]?.let { LinkSync.Song(null, it.title, it.artist, it.album, it.durationMs) } ?: LinkSync.Song(null, q.title, q.artist)
    }
    return LinkSync.Queue(songs, items.mapNotNull { LinkSync.remoteOf(it.id, it.title, it.artist, "", it.artUri) }, p.position())
}

/** Plays a queue from the PC here (songs this phone doesn't have are left out); returns what to tell you. */
internal fun playFromPc(p: PlayerState, q: LinkSync.Queue, library: List<Track>, pcName: String): String {
    val (items, missing) = Link.resolve(q, library)
    if (items.isEmpty()) return "None of those songs are on this phone."
    p.playItems(items, 0, "Your PC", pcName, shuffled = false)
    if (q.positionMs > 3000) p.seekTo(q.positionMs)
    return if (missing > 0) "Playing here · ${songCount(missing)} not on this phone" else "Playing here"
}

/** "Play on…": this phone or Onda on your PC (and the PC's controls while it plays there). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayOnSheet(player: PlayerState?, library: List<Track>, byId: Map<String, Track>, onMessage: (String) -> Unit, onDismiss: () -> Unit) {
    val pc by Link.pc.collectAsState()
    val scope = rememberCoroutineScope()
    var there by remember { mutableStateOf<JSONObject?>(null) }
    var busy by remember { mutableStateOf(false) }
    // The PC's player, every 1.5 s while this is open.
    LaunchedEffect(pc) { while (pc != null) { there = Link.state(); delay(1500) } }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = CardBg) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Play on", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            val pcPlaying = there?.optBoolean("has") == true && there?.optBoolean("playing") == true
            DeviceRow(Icons.Rounded.PhoneAndroid, "This phone", if (pcPlaying) "Tap to take over what plays on the PC" else "Playing here", !pcPlaying) {
                val p = player ?: return@DeviceRow
                if (!pcPlaying || busy) return@DeviceRow
                busy = true
                scope.launch {
                    val q = Link.take()
                    onMessage(if (q == null) "Couldn't reach your PC" else playFromPc(p, q, library, pc?.name ?: "Your PC"))
                    busy = false; onDismiss()
                }
            }
            HorizontalDivider(color = Raised)
            val c = pc
            if (c == null) {
                DeviceRow(Icons.Rounded.Computer, "Onda on your PC", "Link it in Settings → Your PC, then play there from here", false) {}
            } else {
                DeviceRow(Icons.Rounded.Computer, c.name, when {
                    there == null -> "Not reachable · is Onda open on it, with the link on?"
                    pcPlaying -> "Playing there"
                    else -> "Tap to play what plays here on the PC"
                }, pcPlaying) {
                    val p = player ?: return@DeviceRow
                    val q = queueHere(p, byId) ?: return@DeviceRow onMessage("Nothing is playing here")
                    if (busy || pcPlaying) return@DeviceRow
                    busy = true
                    scope.launch {
                        val missing = Link.playThere(q)
                        if (missing != null && missing < q.songs.size) p.controller.pause()
                        onMessage(when (missing) {
                            null -> "Couldn't reach ${c.name}"
                            q.songs.size -> "None of those songs are on ${c.name}"
                            0 -> "Playing on ${c.name}"
                            else -> "Playing on ${c.name} · ${songCount(missing)} not there"
                        })
                        busy = false
                    }
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent, trackColor = Raised)
                there?.takeIf { it.optBoolean("has") }?.let { s -> RemoteControls(s) { cmd, extra -> scope.launch { Link.command(cmd, extra)?.let { there = it } } } }
            }
        }
    }
}

@Composable
private fun DeviceRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, about: String, active: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (active) Accent else TextDim)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, color = if (active) Accent else TextMain, fontWeight = FontWeight.SemiBold)
            Text(about, color = TextDim, fontSize = 12.sp)
        }
    }
}

/** The PC's song and its controls (like a remote). */
@Composable
private fun RemoteControls(s: JSONObject, send: (String, JSONObject.() -> Unit) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Art(s.optString("art").ifBlank { null }, 48.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(s.optString("title"), color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(s.optString("artist"), color = TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { send("prev") {} }) { Icon(Icons.Rounded.SkipPrevious, "Previous on the PC", tint = TextMain, modifier = Modifier.size(32.dp)) }
            IconButton(onClick = { send("toggle") {} }) {
                val playing = s.optBoolean("playing")
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (playing) "Pause on the PC" else "Play on the PC", tint = Accent, modifier = Modifier.size(40.dp))
            }
            IconButton(onClick = { send("next") {} }) { Icon(Icons.Rounded.SkipNext, "Next on the PC", tint = TextMain, modifier = Modifier.size(32.dp)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.VolumeUp, null, tint = TextDim)
            var v by remember(s.optDouble("volume")) { mutableStateOf(s.optDouble("volume", 0.8).toFloat()) }
            Slider(v, { v = it }, onValueChangeFinished = { send("volume") { put("v", v.toDouble()) } }, modifier = Modifier.weight(1f).padding(start = 8.dp),
                colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Raised))
        }
    }
}

/** Settings → Your PC: link with Onda on your PC, sync, unlink. */
@Composable
internal fun PcSettings(onMessage: (String) -> Unit) {
    val pc by Link.pc.collectAsState()
    val status by Link.status.collectAsState()
    val syncing by Link.syncing.collectAsState()
    var linking by remember { mutableStateOf(false) }
    Text("Sync playlists, Liked Songs, Want to listen, follows, history and edited details with Onda on your PC, and play on either. " +
        "Over your Wi-Fi only, encrypted; nothing goes through the internet.", color = TextDim, fontSize = 12.sp)
    val c = pc
    if (c == null) {
        Button(onClick = { linking = true }) { Text("Link with your PC") }
    } else {
        Text("Linked with ${c.name} (${c.address})", color = TextMain)
        status?.let { Text(it, color = TextDim, fontSize = 12.sp) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { Link.syncLater(onMessage) }, enabled = !syncing) { Text(if (syncing) "Syncing…" else "Sync now") }
            OutlinedButton(onClick = { Link.unlink() }) { Text("Unlink") }
        }
    }
    if (linking) LinkDialog(onMessage, onDone = { msg -> linking = false; msg?.let(onMessage) })
}

/** Finds Onda on the PCs of this network (or takes an address) and pairs with the PC's code. */
@Composable
private fun LinkDialog(onMessage: (String) -> Unit, onDone: (String?) -> Unit) {
    var found by remember { mutableStateOf<List<Link.Pc>?>(null) }
    var address by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var problem by remember { mutableStateOf<String?>(null) }
    var pairing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { found = Link.discover(); found?.firstOrNull()?.let { if (address.isBlank()) address = it.address } }
    AlertDialog(
        onDismissRequest = { onDone(null) },
        title = { Text("Link with your PC") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("On the PC: Onda → Settings → Your phone → turn on the link, then Pair a phone.", color = TextDim, fontSize = 13.sp) }
                when {
                    found == null -> item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent, trackColor = Raised) }
                    found!!.isEmpty() -> item { Text("No PC found on this Wi-Fi. Type the address Onda shows on the PC.", color = TextDim, fontSize = 12.sp) }
                    else -> items(found!!) { p ->
                        Row(Modifier.fillMaxWidth().clickable { address = p.address }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Computer, null, tint = if (address == p.address) Accent else TextDim)
                            Text("${p.name} · ${p.address}", color = if (address == p.address) Accent else TextMain, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
                item { OutlinedTextField(address, { address = it.trim() }, singleLine = true, label = { Text("PC address") }, modifier = Modifier.fillMaxWidth()) }
                item {
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, singleLine = true, label = { Text("Code shown on the PC") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())
                }
                problem?.let { item { Text(it, color = ErrorRed, fontSize = 12.sp) } }
            }
        },
        confirmButton = {
            TextButton(enabled = !pairing && address.isNotBlank() && code.length == 6, onClick = {
                pairing = true; problem = null
                scope.launch {
                    val err = Link.pair(address, code, android.os.Build.MODEL.takeIf { it.isNotBlank() } ?: "Phone")
                    pairing = false
                    if (err == null) { Link.syncLater(onMessage); onDone("Linked with your PC. Syncing…") } else problem = err
                }
            }) { Text(if (pairing) "Linking…" else "Link", color = Accent) }
        },
        dismissButton = { TextButton(onClick = { onDone(null) }) { Text("Cancel", color = TextDim) } },
        containerColor = CardBg,
    )
}
