package io.github.realkhfan.onda.data

import android.content.Context
import io.github.realkhfan.onda.core.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.SecureRandom

/** The user's Navidrome / OpenSubsonic server: login, a cached list of every song, and full-length streams. */
object Server {
    /** Server songs' ids start with this, so they never collide with local files or catalog previews. */
    const val PREFIX = "srv:"
    private const val PAGE = 500

    private val _config = MutableStateFlow<Subsonic.Config?>(null)
    val config: StateFlow<Subsonic.Config?> = _config.asStateFlow()

    private val _songs = MutableStateFlow<List<Subsonic.Song>>(emptyList())
    val songs: StateFlow<List<Subsonic.Song>> = _songs.asStateFlow()

    /** Max streaming kbps; 0 = original file. */
    private val _kbps = MutableStateFlow(0)
    val kbps: StateFlow<Int> = _kbps.asStateFlow()

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing.asStateFlow()

    /** Last sync problem, shown in Settings; null when fine. */
    private val _problem = MutableStateFlow<String?>(null)
    val problem: StateFlow<String?> = _problem.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var cache: File? = null

    fun load(ctx: Context) {
        cache = File(ctx.filesDir, "server_songs.json")
        _config.value = Prefs.server(ctx)
        _kbps.value = Prefs.streamKbps(ctx)
        _songs.value = if (_config.value != null) readCache() else emptyList()
    }

    /** Logs in with a fresh salt; on success keeps only the token and lists the library. Returns an error message, or null. */
    suspend fun connect(ctx: Context, url: String, user: String, password: String): String? {
        val salt = ByteArray(8).also { SecureRandom().nextBytes(it) }.joinToString("") { "%02x".format(it) }
        val cfg = Subsonic.Config(Subsonic.normalizeUrl(url), user.trim(), salt, Subsonic.token(password, salt))
        return try {
            withContext(Dispatchers.IO) { Subsonic.unwrap(Http.get(Subsonic.url(cfg, "ping"))) }
            Prefs.saveServer(ctx, cfg)
            _config.value = cfg
            sync()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Subsonic.ApiError) {
            when (e.code) {
                40 -> "Wrong username or password."
                41 -> "This server doesn't accept token login (LDAP or proxy login setups aren't supported yet)."
                else -> e.message
            }
        } catch (_: Exception) {
            "Can't reach ${cfg.url}. Check the address and port, that you're on your home Wi-Fi or WireGuard, " +
                "and Onda's Network permission."
        }
    }

    fun disconnect(ctx: Context) {
        job?.cancel()
        Prefs.clearServer(ctx)
        _config.value = null
        _songs.value = emptyList()
        _problem.value = null
        cache?.delete()
    }

    fun setKbps(ctx: Context, kbps: Int) {
        Prefs.setStreamKbps(ctx, kbps)
        _kbps.value = kbps
    }

    /** Lists every song on the server, [PAGE] per request (Navidrome's empty-query search). */
    fun sync() {
        val cfg = _config.value ?: return
        if (job?.isActive == true) return
        job = scope.launch {
            _syncing.value = true
            _problem.value = null
            try {
                val all = LinkedHashMap<String, Subsonic.Song>()
                var offset = 0
                while (true) {
                    val page = withContext(Dispatchers.IO) {
                        val q = "query=%22%22&songCount=$PAGE&songOffset=$offset&artistCount=0&albumCount=0"
                        Subsonic.parseSongs(Subsonic.unwrap(Http.get(Subsonic.url(cfg, "search3", q), readTimeoutMs = 30_000)))
                    }
                    // A server that ignores the offset would repeat page one forever; stop on repeats.
                    if (page.isEmpty() || page.all { it.id in all }) break
                    page.forEach { all[it.id] = it }
                    if (page.size < PAGE) break
                    offset += PAGE
                }
                _songs.value = all.values.toList()
                withContext(Dispatchers.IO) { writeCache(_songs.value) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _problem.value = "Couldn't sync: your server isn't reachable right now. Showing the last list."
            } finally {
                _syncing.value = false
            }
        }
    }

    /** Server songs as library tracks; stream and cover URLs carry the login token, so they're built in memory only. */
    fun toTracks(songs: List<Subsonic.Song>, cfg: Subsonic.Config?, kbps: Int): List<Track> {
        if (cfg == null) return emptyList()
        return songs.map { s ->
            Track(
                id = PREFIX + s.id,
                uri = Subsonic.streamUrl(cfg, s.id, kbps),
                title = s.title,
                artist = s.artist.ifBlank { "Unknown artist" },
                album = s.album.ifBlank { "Unknown album" },
                albumArtist = s.albumArtist.ifBlank { s.artist.ifBlank { "Unknown artist" } },
                trackNo = s.track,
                durationMs = s.durationSec * 1000L,
                folderName = "Music server",
                artUri = s.coverArt?.let { Subsonic.coverUrl(cfg, it) },
            )
        }
    }

    private fun readCache(): List<Subsonic.Song> = try {
        val arr = JSONArray(cache?.takeIf { it.exists() }?.readText() ?: "[]")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Subsonic.Song(
                o.getString("id"), o.getString("title"), o.optString("artist"), o.optString("album"),
                o.optString("albumArtist"), o.optInt("track"), o.optInt("duration"), o.optString("cover").ifBlank { null },
            )
        }
    } catch (_: Exception) {
        emptyList() // only a cache: the next sync rebuilds it
    }

    private fun writeCache(songs: List<Subsonic.Song>) {
        val f = cache ?: return
        val arr = JSONArray()
        songs.forEach { s ->
            arr.put(JSONObject().put("id", s.id).put("title", s.title).put("artist", s.artist).put("album", s.album)
                .put("albumArtist", s.albumArtist).put("track", s.track).put("duration", s.durationSec).put("cover", s.coverArt))
        }
        val tmp = File(f.path + ".tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(f)
    }
}
