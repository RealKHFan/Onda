package io.github.realkhfan.onda.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun main() {
    System.setProperty("http.agent", "Onda") // the default would send the Java version and OS to every server
    if (Instance.raiseExisting()) return // Onda is already running: it came to the front instead
    application {
        val scope = rememberCoroutineScope()
        val state = remember { AppState(scope) }
        val player = remember {
            runCatching { Player(scope) }.onFailure { System.err.println("Onda: GStreamer could not start: $it") }.getOrNull()
                ?.also { p -> p.resolver = state::resolve; p.onLastSong = { state.extend(p) }; p.prefetch = state::prefetch
                    p.gain = { if (state.settings.normalize) Loudness.factor(it.id) else 1.0 } }
        }
        val mpris = remember { arrayOfNulls<Mpris>(1) }
        var visible by remember { mutableStateOf(true) }
        fun saveSession() {
            player?.takeIf { it.queue.isNotEmpty() }?.let { Store.saveSession(Store.Session(it.queue, it.index, it.positionMs, it.shuffle, it.repeat)) }
        }
        fun quit() {
            saveSession()
            mpris[0]?.close()
            player?.close()
            exitApplication()
        }
        fun keys(e: KeyEvent): Boolean {
            if (e.type != KeyEventType.KeyDown || player == null) return false
            when {
                e.key == Key.Spacebar -> player.toggle()
                e.isCtrlPressed && e.key == Key.DirectionRight -> player.next()
                e.isCtrlPressed && e.key == Key.DirectionLeft -> player.previous()
                e.key == Key.DirectionRight -> player.seek(player.positionMs + 5000)
                e.key == Key.DirectionLeft -> player.seek(player.positionMs - 5000)
                e.key == Key.DirectionUp -> { player.changeVolume(player.volume + 0.05); state.setVolume(player.volume) }
                e.key == Key.DirectionDown -> { player.changeVolume(player.volume - 0.05); state.setVolume(player.volume) }
                e.isCtrlPressed && e.key == Key.L -> player.current?.id?.takeIf(state::canSave)?.let(state::toggleLike)
                e.isCtrlPressed && e.key == Key.F -> state.navRequest = "search"
                e.isCtrlPressed && e.key == Key.Comma -> state.navRequest = "settings"
                e.key == Key.Escape -> state.navRequest = "back"
                else -> return false
            }
            return true
        }
        Window(
            // With "keep playing" on, closing hides the window; the app menu or the media widget brings it back.
            onCloseRequest = { if (state.settings.background && player?.queue?.isNotEmpty() == true) visible = false else quit() },
            visible = visible,
            title = "Onda",
            icon = painterResource("onda.png"),
            state = rememberWindowState(width = 1280.dp, height = 820.dp),
            onKeyEvent = ::keys,
        ) {
            LaunchedEffect(Unit) {
                state.start()
                if (player == null) return@LaunchedEffect
                player.changeVolume(state.settings.volume)
                // Back where you left off: same queue and song, paused at the same moment.
                withContext(Dispatchers.IO) { Store.session() }?.let { player.restore(it.items, it.index, it.positionMs, it.shuffle, it.repeat) }
                scope.launch { snapshotFlow { player.current }.collect { it?.let(state::played) } } // for Home and the mixes
                val m = Mpris(player, scope, Dispatchers.Main, onRaise = { visible = true; window.toFront() }, onQuit = ::quit)
                withContext(Dispatchers.IO) { m.start() }
                mpris[0] = m
                var tick = 0
                while (true) {
                    // Mixes and new releases once the library has loaded, then every half hour (each checks if it's due).
                    if (tick % 120 == 1) { state.refreshMixes(); state.checkReleases(); withContext(Dispatchers.IO) { state.dailyBackup() } }
                    delay(15_000); tick++
                    saveSession()
                }
            }
            OndaDesktop(state, player)
        }
    }
}
