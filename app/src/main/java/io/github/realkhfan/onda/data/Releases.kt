package io.github.realkhfan.onda.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.github.realkhfan.onda.MainActivity
import io.github.realkhfan.onda.R
import io.github.realkhfan.onda.core.NewReleases
import io.github.realkhfan.onda.core.RemoteAlbum
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

/**
 * New releases from the artists you listen to most (looked up on Deezer by artist id only), shown on Home,
 * with a notification on release day. Checked when you open Onda and twice a day in the background ([ReleaseJob]).
 */
object Releases {
    const val EXTRA_ALBUM = "onda.album"
    private const val CHANNEL = "releases"
    private const val MAX_ARTISTS = 30
    private const val RECHECK_MS = 6 * 60 * 60 * 1000L

    private val _items = MutableStateFlow<List<RemoteAlbum>>(emptyList())
    val items: StateFlow<List<RemoteAlbum>> = _items.asStateFlow()

    /** Set when a release notification is tapped; the app opens that album and clears it. */
    val open = MutableStateFlow<RemoteAlbum?>(null)

    private var artists: List<String> = emptyList()
    private var seen: Set<Long> = emptySet()
    private var lastCheck = 0L
    private var file: File? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    fun load(context: Context) {
        file = File(context.filesDir, "releases.json")
        read()
    }

    /** Your top artists, strongest first (from plays, likes, Want to listen and your library). */
    fun setArtists(names: List<String>) {
        val top = names.take(MAX_ARTISTS)
        if (top == artists) return
        artists = top
        write()
    }

    fun refreshIfDue(context: Context) {
        if (artists.isEmpty() || job?.isActive == true) return
        if (System.currentTimeMillis() - lastCheck < RECHECK_MS) return
        val app = context.applicationContext
        job = scope.launch { check(app) }
    }

    /** Looks up your artists' latest releases and notifies about new ones. Returns false when Deezer wasn't reachable. */
    suspend fun check(context: Context): Boolean {
        if (artists.isEmpty()) return true
        val today = LocalDate.now()
        val found = ArrayList<RemoteAlbum>()
        var ok = 0
        var failed = 0
        for (name in artists) {
            try {
                val id = ArtistIds.cached(name) ?: ArtistIds.resolve(name).also { delay(120) }
                if (id > 0) {
                    found += Deezer.artistAlbums(id, name) // Deezer does not sort by date, so the newest can be far down the list
                    delay(120) // Deezer allows 50 requests per 5 seconds
                }
                ok++
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed++
            }
        }
        if (ok == 0 && failed > 0) return false
        val recent = NewReleases.recent(found, today)
        notifyAbout(context, NewReleases.toNotify(recent, seen, today, firstCheck = lastCheck == 0L))
        seen = recent.map { it.id }.toSet() // older ones drop out of "recent" and never return, so this stays small
        _items.value = recent
        lastCheck = System.currentTimeMillis()
        write()
        return true
    }

    private fun notifyAbout(context: Context, albums: List<RemoteAlbum>) {
        if (albums.isEmpty() || !Prefs.releaseAlerts(context)) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (!nm.areNotificationsEnabled()) return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "New releases", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "When an artist you listen to releases something new" },
        )
        for (a in albums.take(5)) {
            val open = Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_ALBUM, a.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pi = PendingIntent.getActivity(context, a.id.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val kind = when (a.type) { "single" -> "New single"; "ep" -> "New EP"; else -> "New album" }
            nm.notify(
                a.id.hashCode(),
                Notification.Builder(context, CHANNEL)
                    .setSmallIcon(R.drawable.ic_stat_onda)
                    .setContentTitle("New from ${a.artist}")
                    .setContentText("$kind: ${a.title}")
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build(),
            )
        }
    }

    private fun read() {
        val f = file ?: return
        if (!f.exists()) return
        try {
            val o = JSONObject(f.readText())
            val ar = o.optJSONArray("artists") ?: JSONArray()
            artists = (0 until ar.length()).map { ar.getString(it) }
            val sn = o.optJSONArray("seen") ?: JSONArray()
            seen = (0 until sn.length()).map { sn.getLong(it) }.toSet()
            lastCheck = o.optLong("lastCheck")
            val it = o.optJSONArray("items") ?: JSONArray()
            _items.value = (0 until it.length()).map { i ->
                val a = it.getJSONObject(i)
                RemoteAlbum(
                    a.getLong("id"), a.getString("title"), a.optString("artist"), a.optString("cover").ifBlank { null },
                    a.optString("date").ifBlank { null }, a.optString("type").ifBlank { null }, a.optLong("artistId"),
                )
            }
        } catch (_: Exception) {
            // only a cache; the next check rebuilds it (without a burst of notifications: lastCheck stays as read)
        }
    }

    private fun write() {
        val f = file ?: return
        try {
            val items = JSONArray()
            _items.value.forEach { a ->
                items.put(JSONObject().put("id", a.id).put("title", a.title).put("artist", a.artist).put("cover", a.cover)
                    .put("date", a.date).put("type", a.type).put("artistId", a.artistId))
            }
            val o = JSONObject()
                .put("artists", JSONArray(artists))
                .put("seen", JSONArray(seen.toList()))
                .put("lastCheck", lastCheck)
                .put("items", items)
            val tmp = File(f.path + ".tmp")
            tmp.writeText(o.toString())
            tmp.renameTo(f)
        } catch (_: Exception) {
        }
    }
}
