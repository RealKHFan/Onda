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

fun main() {
    System.setProperty("http.agent", "Onda") // the default would send the Java version and OS to every server
    application {
        val scope = rememberCoroutineScope()
        val state = remember { AppState(scope) }
        val player = remember {
            runCatching { Player(scope) }.onFailure { System.err.println("Onda: GStreamer could not start: $it") }.getOrNull()
        }
        LaunchedEffect(Unit) {
            state.start()
            player?.changeVolume(state.settings.volume)
        }
        Window(
            onCloseRequest = { player?.close(); exitApplication() },
            title = "Onda",
            icon = painterResource("onda.png"),
            state = rememberWindowState(width = 1240.dp, height = 800.dp),
            onKeyEvent = { e ->
                // Space plays/pauses (text fields keep their own spaces).
                if (e.type == KeyEventType.KeyDown && e.key == Key.Spacebar && player != null) { player.toggle(); true } else false
            },
        ) {
            OndaDesktop(state, player)
        }
    }
}
