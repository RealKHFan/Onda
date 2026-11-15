package io.github.realkhfan.onda.data

import android.content.Context
import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.RemoteArtist
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.time.LocalDate

/**
 * Builds and keeps the Daily 10 and Weekly 30. Your history stays on the phone: only artist names
 * and Deezer ids are looked up (related artists, their top songs). Each mix is fixed for its day / week.
 */
object Mixes {

    data class Mix(val kind: Discovery.Kind, val period: String, val tracks: List<RemoteTrack>, val basedOn: List<String>)

    /** [seeds]: your artists, strongest first. [known]: artists you already have or play (normalised). */
    class Taste(
        val seeds: List<String>, val known: Set<String>, val skip: (RemoteTrack) -> Boolean,
        /** Free sources for full copies (the "Free full songs" switch, Jamendo ID) and ListenBrainz on/off. */
        val audius: Boolean = false, val jamendoId: String = "", val listenBrainz: Boolean = false,
    )

    private val _mixes = MutableStateFlow<Map<Discovery.Kind, Mix>>(emptyMap())
    val mixes: StateFlow<Map<Discovery.Kind, Mix>> = _mixes.asStateFlow()

    private val _building = MutableStateFlow<Set<Discovery.Kind>>(emptySet())
    val building: StateFlow<Set<Discovery.Kind>> = _building.asStateFlow()

    private val _problem = MutableStateFlow<String?>(null)
    val problem: StateFlow<String?> = _problem.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null
    private var file: File? = null

    fun load(ctx: Context) {
        file = File(ctx.filesDir, "mixes.json")
        read()
    }

    /** Builds each mix whose day / week has changed (or all, with [force]). Cheap to call often. */
    fun refresh(taste: Taste, today: LocalDate = LocalDate.now(), force: Boolean = false) {
        if (job?.isActive == true || taste.seeds.isEmpty()) return
        val stale = Discovery.Kind.entries.filter { force || _mixes.value[it]?.period != Discovery.period(it, today) }
        if (stale.isEmpty()) return
        job = scope.launch {
            _problem.value = null
            for (kind in stale) {
                _building.update { it + kind }
                try {
                    val mix = build(kind, taste, Discovery.period(kind, today))
                    _mixes.update { it + (kind to mix) }
                    write()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _problem.value = "Couldn't build your mixes: Deezer isn't reachable. They'll be built next time you're online."
                } finally {
                    _building.update { it - kind }
                }
            }
        }
    }

    private suspend fun build(kind: Discovery.Kind, taste: Taste, period: String): Mix {
        var calls = 0
        var failures = 0
        // One failed lookup shouldn't sink the mix; only "everything failed" (offline) is an error.
        suspend fun <T> soft(block: suspend () -> T): T? {
            calls++
            return try {
                block().also { delay(120) } // Deezer allows 50 requests per 5 s
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failures++; null
            }
        }

        val seeds = taste.seeds.take(kind.seeds)
        val seedIds = seeds.mapNotNull { name -> (ArtistIds.cached(name) ?: soft { ArtistIds.resolve(name) })?.takeIf { it > 0 } }
        // Few artists to start from (a new library): reach further so the mix still fills up.
        val perSeed = if (seedIds.size < kind.seeds / 2) kind.relatedPerSeed * 2 else kind.relatedPerSeed
        val related = LinkedHashMap<Long, RemoteArtist>()
        for (id in seedIds) {
            soft { Deezer.related(id, perSeed * 2) }.orEmpty()
                .filter { !kind.newArtistsOnly || Match.norm(it.name) !in taste.known }
                .take(perSeed)
                .forEach { related.putIfAbsent(it.id, it) }
        }
        // ListenBrainz's similar artists for the strongest seeds, as Deezer artists (it can't sink the mix: errors are ignored).
        if (taste.listenBrainz) {
            for (name in seeds.take(4)) {
                for (similar in ListenBrainz.similar(name, perSeed * 2)) {
                    if (UserCollection.isBlocked(similar) || UserCollection.isAi(similar)) continue
                    if (kind.newArtistsOnly && Match.norm(similar) in taste.known) continue
                    val id = ArtistIds.cached(similar) ?: soft { ArtistIds.resolve(similar) } ?: 0
                    if (id > 0) related.putIfAbsent(id, RemoteArtist(id, similar, null))
                    if (related.size >= seedIds.size * perSeed * 2) break
                }
            }
        }
        // Daily also brings songs you don't have yet from your own artists; Weekly is new artists only.
        val sources = ((if (kind.newArtistsOnly) emptyList() else seedIds) + related.keys).distinct().take(30)
        val topPerArtist = if (sources.size < 12) 10 else 5
        val candidates = ArrayList<RemoteTrack>()
        for (id in sources) candidates += soft { Deezer.artistTop(id, topPerArtist) }.orEmpty()
        if (calls > 0 && failures == calls) throw IOException("offline")
        val picked = Discovery.pick(candidates, kind, period, Discovery.perArtistFor(kind, candidates), taste.skip)
        // Playable first: songs with a free full copy become that copy and lead the mix.
        val withFull = if (!taste.audius && taste.jamendoId.isBlank()) picked else picked.map { t ->
            if (t.source != Source.DEEZER) t
            else runCatching { FullCopies.find(t.id, taste.audius, taste.jamendoId) }.getOrNull()?.also { delay(120) } ?: t
        }
        return Mix(kind, period, withFull.sortedBy { !it.source.full }, seeds.take(3))
    }

    private fun read() {
        val f = file ?: return
        if (!f.exists()) return
        try {
            val root = JSONObject(f.readText())
            val arr = root.optJSONArray("mixes") ?: JSONArray()
            _mixes.value = (0 until arr.length()).associate { i ->
                val o = arr.getJSONObject(i)
                val kind = Discovery.Kind.valueOf(o.getString("kind"))
                val ts = o.getJSONArray("tracks")
                val based = o.optJSONArray("basedOn") ?: JSONArray()
                kind to Mix(
                    kind, o.getString("period"),
                    (0 until ts.length()).map { j ->
                        val t = ts.getJSONObject(j)
                        // Mixes saved before 0.14.0 used "dur"; now the same format as playlists.
                        remoteFromJson(t).let { r -> if (r.durationSec == 0) r.copy(durationSec = t.optInt("dur")) else r }
                    },
                    (0 until based.length()).map { based.getString(it) },
                )
            }
        } catch (_: Exception) {
            _mixes.value = emptyMap() // only a cache: the mixes are rebuilt
        }
    }

    private fun write() {
        val f = file ?: return
        try {
            val arr = JSONArray()
            _mixes.value.values.forEach { m ->
                val ts = JSONArray()
                // The preview link is stored only as "has a preview": the player fetches a fresh one when it plays.
                m.tracks.forEach { t -> ts.put(t.toJson()) }
                arr.put(JSONObject().put("kind", m.kind.name).put("period", m.period).put("tracks", ts).put("basedOn", JSONArray(m.basedOn)))
            }
            val tmp = File(f.path + ".tmp")
            tmp.writeText(JSONObject().put("mixes", arr).toString())
            tmp.renameTo(f)
        } catch (_: Exception) {
        }
    }
}
