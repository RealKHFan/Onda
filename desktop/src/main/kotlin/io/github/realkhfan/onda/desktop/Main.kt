package io.github.realkhfan.onda.desktop

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.WindowPosition
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

/** Errors also go to ~/.cache/onda/onda.log (this start only), so a window that never shows can be looked into. */
private fun startLog() = runCatching {
    val out = java.io.FileOutputStream(java.io.File(Store.cache.apply { mkdirs() }, "onda.log"))
    val err = System.err
    System.setErr(java.io.PrintStream(object : java.io.OutputStream() {
        override fun write(b: Int) { err.write(b); out.write(b) }
        override fun write(b: ByteArray, off: Int, len: Int) { err.write(b, off, len); out.write(b, off, len) }
        override fun flush() { err.flush(); out.flush() }
    }, true))
    Thread.setDefaultUncaughtExceptionHandler { t, e -> System.err.println("Onda: error in ${t.name}"); e.printStackTrace() }
    System.err.println("Onda started ${java.time.LocalDateTime.now()} · drawing: ${System.getProperty("skiko.renderApi") ?: "graphics card"}")
}

fun main(args: Array<String>) {
    System.setProperty("http.agent", "Onda") // the default would send the Java version and OS to every server
    // A game can hold the graphics card so Onda's window never appears: drawing on the CPU avoids it.
    if (Store.settings().software) System.setProperty("skiko.renderApi", "SOFTWARE")
    if (Instance.raiseExisting()) return // Onda is already running: it came to the front instead
    startLog()
    val started = System.currentTimeMillis()
    application {
        val scope = rememberCoroutineScope()
        val state = remember { AppState(scope) }
        val player = remember {
            runCatching { Player(scope) }.onFailure { System.err.println("Onda: GStreamer could not start: $it") }.getOrNull()
                ?.also { p -> p.gapless = state.settings.gapless; p.resolver = state::resolve; p.onLastSong = { state.extend(p) }; p.prefetch = state::prefetch
                    p.gain = { if (state.settings.normalize) Loudness.factor(it.id, state.albumMates(it.id)) else 1.0 }
                    p.resumeAt = { state.resumeAt(it.id) }; p.onLeave = { item, pos, dur -> state.leftAt(item.id, pos, dur) }
                    state.settings.output?.let { o -> if (Outputs.list().any { it.name == o }) p.setOutput(o) } }
        }
        val mpris = remember { arrayOfNulls<Mpris>(1) }
        // Started with your desktop session: no window until you open Onda (from the app menu, or the media widget).
        var visible by remember { mutableStateOf("--background" !in args) }
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
                e.isCtrlPressed && (e.key == Key.Equals || e.key == Key.Plus || e.key == Key.NumPadAdd) -> state.setZoom(state.settings.zoom + 0.1f)
                e.isCtrlPressed && (e.key == Key.Minus || e.key == Key.NumPadSubtract) -> state.setZoom(state.settings.zoom - 0.1f)
                e.isCtrlPressed && (e.key == Key.Zero || e.key == Key.NumPad0) -> state.setZoom(1f)
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
                e.isCtrlPressed && e.key == Key.K -> state.navRequest = "quick"
                e.key == Key.F1 || (e.isCtrlPressed && e.key == Key.Slash) -> state.navRequest = "keys"
                e.key == Key.Escape -> state.navRequest = "back"
                else -> return false
            }
            return true
        }
        // A full-screen game keeps a window that comes up behind it: Onda stays on top for a moment, un-minimized and focused.
        fun bringToFront(w: java.awt.Frame) {
            visible = true
            scope.launch {
                delay(80)
                w.extendedState = w.extendedState and java.awt.Frame.ICONIFIED.inv()
                w.isAlwaysOnTop = true; w.toFront(); w.requestFocus()
                delay(1500)
                w.isAlwaysOnTop = false
            }
        }
        val mainWindow = arrayOfNulls<java.awt.Frame>(1)
        if (state.mini && player != null) Window(
            onCloseRequest = { state.mini = false }, title = "Onda mini player", icon = painterResource("onda.png"), alwaysOnTop = true, resizable = false,
            state = rememberWindowState(width = 440.dp, height = 150.dp, position = WindowPosition(Alignment.BottomEnd)), onKeyEvent = ::keys,
        ) { MiniPlayer(state, player) { mainWindow[0]?.let(::bringToFront) } }
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
                mainWindow[0] = window
                System.err.println("Onda: window ready after ${System.currentTimeMillis() - started} ms")
                if (visible) bringToFront(window)
                state.start()
                if (player == null) return@LaunchedEffect
                player.changeVolume(state.settings.volume)
                player.equalize(state.settings.eq)
                // Back where you left off: same queue and song, paused at the same moment.
                withContext(Dispatchers.IO) { Store.session() }?.let { player.restore(it.items, it.index, it.positionMs, it.shuffle, it.repeat) }
                scope.launch { snapshotFlow { player.current }.collect { it?.let(state::played) } } // for Home and the mixes
                scope.launch {
                    var restored = true // the song from last time isn't news
                    snapshotFlow { player.current }.collect { cur ->
                        if (!restored && cur != null && state.settings.songNotify && player.playing && !window.isFocused) {
                            val icon = cur.art?.let { a -> if (a.startsWith("/")) a else if (a.startsWith("file:")) runCatching { java.io.File(java.net.URI(a)).path }.getOrNull() else null }
                            Discover.notify(cur.title, cur.artist, icon ?: "io.github.realkhfan.Onda")
                        }
                        restored = false
                    }
                }
                val m = Mpris(player, scope, Dispatchers.Main, onRaise = { bringToFront(window) }, onQuit = ::quit)
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
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density * state.settings.zoom, base.fontScale)) { OndaDesktop(state, player) }
        }
    }
}
