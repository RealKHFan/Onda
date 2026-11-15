package io.github.realkhfan.onda.desktop

import io.github.realkhfan.onda.core.Discovery
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.RemoteAlbum
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.data.Deezer
import io.github.realkhfan.onda.data.FreeCatalogs
import io.github.realkhfan.onda.data.FullCopies
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File

/** Daily 10 / Weekly 30, similar songs for autoplay and new releases: the phone's rules (core Discovery), on Deezer's catalog. */
object Discover {
    private val idsFile = File(Store.data, "artist_ids.json")
    private val ids: MutableMap<String, Long> = runCatching {
        JSONObject(idsFile.readText()).let { o -> o.keySet().associateWith { o.getLong(it) }.toMutableMap() }
    }.getOrDefault(HashMap())
    private val lock = Mutex()

    /** Deezer's id for an artist name (exact name only; 0 = none). Cached, so each name is looked up once. */
    suspend fun artistId(name: String): Long = lock.withLock {
        val k = Match.norm(name)
        if (k.isEmpty()) return 0
        ids[k]?.let { return it }
        val id = runCatching { Deezer.searchArtists(name).firstOrNull { Match.norm(it.name) == k }?.id ?: 0L }.getOrElse { return 0 }
        ids[k] = id
        runCatching { File(Store.data, "artist_ids.json.tmp").apply { writeText(JSONObject(ids as Map<*, *>).toString()) }.renameTo(idsFile) }
        delay(120) // Deezer allows about 50 requests in 5 seconds
        id
    }

    data class Mix(val kind: Discovery.Kind, val period: String, val tracks: List<RemoteTrack>)

    /** [known]: artists you already have or play (Weekly is new artists only); [skip]: songs to leave out. */
    suspend fun mix(kind: Discovery.Kind, period: String, seeds: List<String>, known: Set<String>, skip: (RemoteTrack) -> Boolean): Mix {
        val seedIds = seeds.take(kind.seeds).map { artistId(it) }.filter { it > 0 }
        val perSeed = if (seedIds.size < kind.seeds / 2) kind.relatedPerSeed * 2 else kind.relatedPerSeed
        val related = LinkedHashSet<Long>()
        for (id in seedIds) {
            runCatching { Deezer.related(id, perSeed * 2) }.getOrDefault(emptyList())
                .filter { !kind.newArtistsOnly || Match.norm(it.name) !in known }.take(perSeed).forEach { related += it.id }
            delay(120)
        }
        val sources = ((if (kind.newArtistsOnly) emptyList() else seedIds) + related).distinct().take(30)
        val per = if (sources.size < 12) 10 else 5
        val candidates = ArrayList<RemoteTrack>()
        for (id in sources) { candidates += runCatching { Deezer.artistTop(id, per) }.getOrDefault(emptyList()); delay(120) }
        val picked = Discovery.pick(candidates, kind, period, Discovery.perArtistFor(kind, candidates), skip)
        // Songs with a free full copy become that copy and lead the mix.
        val withFull = picked.map { t -> runCatching { FullCopies.find(t.id, audius = true, jamendoId = "") }.getOrNull()?.takeIf { FreeCatalogs.stream(it) != null } ?: t }
        return Mix(kind, period, withFull.sortedBy { !it.source.full })
    }

    /** For "similar songs when the queue ends": popular songs of artists related to the last ones played. */
    suspend fun similar(lastArtists: List<String>, skip: (RemoteTrack) -> Boolean, n: Int = 10): List<RemoteTrack> {
        val seeds = lastArtists.distinctBy(Match::norm).take(5).map { artistId(it) }.filter { it > 0 }
        val related = seeds.flatMap { runCatching { Deezer.related(it, 4) }.getOrDefault(emptyList()) }.distinctBy { it.id }.shuffled().take(8)
        val per = HashMap<String, Int>()
        return (related.map { it.id } + seeds).distinct()
            .flatMap { runCatching { Deezer.artistTop(it, 5) }.getOrDefault(emptyList()) }
            .filter { it.preview != null && !skip(it) && !Discovery.isUnwanted(it.title, it.album) }
            .distinctBy { Match.norm(it.artist) + "|" + Match.norm(it.title) }
            .shuffled()
            .filter { t -> val a = Match.norm(t.artist); ((per[a] ?: 0) < 2).also { ok -> if (ok) per[a] = (per[a] ?: 0) + 1 } }
            .take(n)
    }

    /** Recent releases (not compilations) of up to 30 of your artists. */
    suspend fun releases(artists: List<String>): List<RemoteAlbum> {
        val all = ArrayList<RemoteAlbum>()
        for (name in artists.take(30)) {
            val id = artistId(name)
            if (id > 0) { all += runCatching { Deezer.artistAlbums(id, name) }.getOrDefault(emptyList()); delay(120) }
        }
        return all
    }

    /** A desktop notification (KDE, GNOME…); silently nothing if the system has no notify-send. */
    fun notify(title: String, body: String) {
        runCatching { ProcessBuilder("notify-send", "-a", "Onda", title, body).start() }
    }
}
