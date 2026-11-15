package io.github.realkhfan.onda.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

fun main() {
    System.setProperty("http.agent", "Onda") // the default would send the Java version and OS to every server
    application {
        val scope = rememberCoroutineScope()
        val state = remember { AppState(scope) }
        val player = remember {
            runCatching { Player(scope) }.onFailure { System.err.println("Onda: GStreamer could not start: $it") }.getOrNull()
                ?.also { it.resolver = state::resolve }
        }
        val mpris = remember { arrayOfNulls<Mpris>(1) }
        fun saveSession() {
            player?.takeIf { it.queue.isNotEmpty() }?.let { Store.saveSession(Store.Session(it.queue, it.index, it.positionMs, it.shuffle, it.repeat)) }
        }
        fun quit() {
            saveSession()
            mpris[0]?.close()
            player?.close()
            exitApplication()
        }
        Window(
            onCloseRequest = ::quit,
            title = "Onda",
            icon = painterResource("onda.png"),
            state = rememberWindowState(width = 1280.dp, height = 820.dp),
            onKeyEvent = { e ->
                // Space plays/pauses (text fields keep their own spaces).
                if (e.type == KeyEventType.KeyDown && e.key == Key.Spacebar && player != null) { player.toggle(); true } else false
            },
        ) {
            LaunchedEffect(Unit) {
                state.start()
                if (player == null) return@LaunchedEffect
                player.changeVolume(state.settings.volume)
                // Back where you left off: same queue and song, paused at the same moment.
                withContext(Dispatchers.IO) { Store.session() }?.let { player.restore(it.items, it.index, it.positionMs, it.shuffle, it.repeat) }
                val m = Mpris(player, scope, Dispatchers.Main, onRaise = { window.toFront() }, onQuit = ::quit)
                withContext(Dispatchers.IO) { m.start() }
                mpris[0] = m
                while (true) { delay(15_000); saveSession() }
            }
            OndaDesktop(state, player)
        }
    }
}
