package io.github.realkhfan.onda.desktop

import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.freedesktop.dbus.DBusPath
import org.freedesktop.dbus.annotations.DBusInterfaceName
import org.freedesktop.dbus.connections.impl.DBusConnection
import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBusInterface
import org.freedesktop.dbus.interfaces.Properties
import org.freedesktop.dbus.types.Variant

@DBusInterfaceName("org.mpris.MediaPlayer2")
interface MprisRoot : DBusInterface {
    fun Raise()
    fun Quit()
}

@DBusInterfaceName("org.mpris.MediaPlayer2.Player")
interface MprisPlayer : DBusInterface {
    fun Next()
    fun Previous()
    fun Pause()
    fun PlayPause()
    fun Stop()
    fun Play()
    fun Seek(offset: Long)
    fun SetPosition(trackId: DBusPath, position: Long)
    fun OpenUri(uri: String)
}

/**
 * MPRIS on the session bus: media keys, KDE's and GNOME's media controls and the lock screen. Only what's playing is
 * shared, and only with this computer's desktop. Calls arrive on D-Bus threads and run on the UI thread.
 */
class Mpris(
    private val player: Player,
    private val scope: CoroutineScope,
    private val ui: CoroutineDispatcher,
    private val onRaise: () -> Unit,
    private val onQuit: () -> Unit,
    /** "Open with Onda" from the file manager, through a second start: plays the file. */
    private val onOpen: (String) -> Unit = {},
) : MprisRoot, MprisPlayer, Properties {

    private var conn: DBusConnection? = null

    fun start() {
        try {
            conn = DBusConnectionBuilder.forSessionBus().build().also {
                it.requestBusName(BUS)
                it.exportObject(PATH, this)
            }
        } catch (e: Exception) {
            System.err.println("Onda: media controls unavailable ($e)")
            return
        } catch (e: LinkageError) { // a missing piece of Java must not take the player down with it
            System.err.println("Onda: media controls unavailable ($e)")
            return
        }
        // Tell the desktop when the song, play state or modes change.
        scope.launch(ui) {
            snapshotFlow { listOf(player.current, player.playing, player.shuffle, player.repeat, player.volume, player.fullFrom) }
                .distinctUntilChanged()
                .collect { runCatching { conn?.sendMessage(Properties.PropertiesChanged(PATH, PLAYER, playerProps(), emptyList())) } }
        }
    }

    fun close() { runCatching { conn?.releaseBusName(BUS); conn?.close() } }

    override fun getObjectPath() = PATH

    private fun <T> onUi(block: () -> T): T = runBlocking(ui) { block() }

    override fun Raise() { scope.launch(ui) { onRaise() } }
    override fun Quit() { scope.launch(ui) { onQuit() } }
    override fun Next() = onUi { player.next() }
    override fun Previous() = onUi { player.previous() }
    override fun Pause() = onUi { player.pause() }
    override fun PlayPause() = onUi { player.toggle() }
    override fun Stop() = onUi { player.pause(); player.seek(0) }
    override fun Play() = onUi { player.resume() }
    override fun Seek(offset: Long) = onUi { player.seek(player.positionMs + offset / 1000) }
    override fun SetPosition(trackId: DBusPath, position: Long) = onUi { player.seek(position / 1000) }
    override fun OpenUri(uri: String) { if (uri.startsWith("file:")) scope.launch(ui) { onOpen(uri) } }

    @Suppress("UNCHECKED_CAST")
    override fun <A : Any?> Get(iface: String, prop: String): A = GetAll(iface)[prop] as A // typed, so maps (Metadata) travel too

    override fun <A : Any?> Set(iface: String, prop: String, value: A) {
        onUi { applySetting(prop, value) }
    }

    private fun applySetting(prop: String, value: Any?) {
        when (prop) {
            "Volume" -> (value as? Double)?.let { player.changeVolume(it) }
            "Shuffle" -> (value as? Boolean)?.let { player.shuffleOn(it) }
            "LoopStatus" -> player.repeatMode(when (value) { "Track" -> 2; "Playlist" -> 1; else -> 0 })
            else -> Unit
        }
    }

    override fun GetAll(iface: String): Map<String, Variant<*>> = when (iface) {
        ROOT -> mapOf(
            "CanQuit" to Variant(true), "CanRaise" to Variant(true), "HasTrackList" to Variant(false),
            "Identity" to Variant("Onda"), "DesktopEntry" to Variant("io.github.realkhfan.Onda"),
            "SupportedUriSchemes" to Variant(arrayOf("file"), "as"), "SupportedMimeTypes" to Variant(arrayOf("audio/mpeg", "audio/flac", "audio/ogg", "audio/opus", "audio/wav", "audio/mp4", "audio/aac"), "as"),
        )
        PLAYER -> onUi { playerProps() }
        else -> emptyMap()
    }

    private fun playerProps(): Map<String, Variant<*>> {
        val cur = player.current
        val meta = LinkedHashMap<String, Variant<*>>()
        meta["mpris:trackid"] = Variant(DBusPath(if (cur == null) "/org/mpris/MediaPlayer2/TrackList/NoTrack" else "/io/github/realkhfan/onda/track/${index()}"))
        if (cur != null) {
            meta["xesam:title"] = Variant(cur.title)
            meta["xesam:artist"] = Variant(arrayOf(cur.artist), "as")
            meta["xesam:album"] = Variant(cur.album)
            val len = player.durationMs.takeIf { it > 0 } ?: cur.durationMs
            if (len > 0) meta["mpris:length"] = Variant(len * 1000)
            artUrl(cur.art)?.let { meta["mpris:artUrl"] = Variant(it) }
        }
        return mapOf(
            "PlaybackStatus" to Variant(if (cur == null) "Stopped" else if (player.playing) "Playing" else "Paused"),
            "LoopStatus" to Variant(when (player.repeat) { 2 -> "Track"; 1 -> "Playlist"; else -> "None" }),
            "Rate" to Variant(1.0), "MinimumRate" to Variant(1.0), "MaximumRate" to Variant(1.0),
            "Shuffle" to Variant(player.shuffle),
            "Metadata" to Variant(meta, "a{sv}"),
            "Volume" to Variant(player.volume),
            "Position" to Variant(player.positionMs * 1000),
            "CanGoNext" to Variant(cur != null), "CanGoPrevious" to Variant(cur != null),
            "CanPlay" to Variant(cur != null), "CanPause" to Variant(cur != null), "CanSeek" to Variant(cur != null),
            "CanControl" to Variant(true),
        )
    }

    private fun index() = player.index.coerceAtLeast(0)

    // Covers from your server carry its login in the link, so they're not handed to the desktop.
    private fun artUrl(art: String?): String? = when {
        art == null -> null
        art.startsWith("/") -> "file://$art"
        "/rest/" in art -> null
        else -> cachedCover(art) ?: art
    }

    companion object {
        const val BUS = "org.mpris.MediaPlayer2.onda"
        const val PATH = "/org/mpris/MediaPlayer2"
        const val ROOT = "org.mpris.MediaPlayer2"
        const val PLAYER = "org.mpris.MediaPlayer2.Player"
    }
}

/** A web cover Onda's window has shown, as the file in its image cache: KDE needn't download it (black on a poor network). */
internal fun cachedCover(url: String): String? = runCatching {
    coil3.SingletonImageLoader.get(coil3.PlatformContext.INSTANCE).diskCache?.openSnapshot(url)?.use { "file://${it.data}" }
}.getOrNull()
