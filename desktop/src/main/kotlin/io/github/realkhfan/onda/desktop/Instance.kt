package io.github.realkhfan.onda.desktop

import org.freedesktop.dbus.connections.impl.DBusConnectionBuilder
import org.freedesktop.dbus.interfaces.DBus

/** One Onda at a time: a second start brings the running one forward (through its media-controls name) and ends. */
object Instance {
    fun raiseExisting(): Boolean = runCatching {
        DBusConnectionBuilder.forSessionBus().build().use { c ->
            val bus = c.getRemoteObject("org.freedesktop.DBus", "/org/freedesktop/DBus", DBus::class.java)
            if (!bus.NameHasOwner(Mpris.BUS)) return false
            c.getRemoteObject(Mpris.BUS, Mpris.PATH, MprisRoot::class.java).Raise()
            true
        }
    }.getOrDefault(false)
}
