package io.github.realkhfan.onda.desktop

import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBus

/** One Onda at a time: a second start brings the running one forward (through its media-controls name) and ends. */
object Instance {
    /** [files]: file URIs to hand to the running Onda ("Open with Onda"). */
    fun raiseExisting(files: List<String> = emptyList()): Boolean = runCatching {
        DBusConnectionBuilder.forSessionBus().build().use { c ->
            val bus = c.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus::class.java)
            if (!bus.NameHasOwner(Mpris.BUS)) return false
            files.forEach { c.getRemoteObject(Mpris.BUS, Mpris.PATH, MprisPlayer::class.java).OpenUri(it) }
            c.getRemoteObject(Mpris.BUS, Mpris.PATH, MprisRoot::class.java).Raise()
            true
        }
    }.getOrDefault(false)
}
