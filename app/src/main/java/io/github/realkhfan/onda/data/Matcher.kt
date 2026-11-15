package io.github.realkhfan.onda.data

import io.github.realkhfan.onda.core.Confidence
import io.github.realkhfan.onda.core.Match
import io.github.realkhfan.onda.core.MetaMatch
import io.github.realkhfan.onda.core.RemoteTrack
import io.github.realkhfan.onda.core.Track
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

/**
 * Looks songs up on Deezer and collects proposals for the user to Keep or Replace.
 * Runs outside any screen, so leaving the review page doesn't stop it.
 */
object Matcher {

    data class Proposal(val track: Track, val match: RemoteTrack, val confidence: Confidence)

    data class State(
        val running: Boolean = false,
        val done: Int = 0,
        val total: Int = 0,
        val proposals: List<Proposal> = emptyList(),
        val alreadyRight: Int = 0,
        val noMatch: Int = 0,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var job: Job? = null

    /** [tracks] are the songs as currently shown (with earlier matches applied). */
    fun start(tracks: List<Track>, onlyNew: Boolean) {
        if (job?.isActive == true) return
        val reviewed = Overrides.reviewed.value
        val todo = if (onlyNew) tracks.filter { it.id !in reviewed } else tracks
        _state.value = State(running = true, total = todo.size)
        job = scope.launch {
            val kept = ArrayList<String>()
            var failuresInARow = 0
            for ((i, t) in todo.withIndex()) {
                val candidates = try {
                    Deezer.search(MetaMatch.query(t), limit = 10).also { failuresInARow = 0 }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Not marked reviewed, so it's tried again next time.
                    if (++failuresInARow >= 5) {
                        _state.update { it.copy(error = "Can't reach Deezer. Check your connection or Onda's Network permission.") }
                        break
                    }
                    null
                }
                if (candidates != null) {
                    val best = MetaMatch.best(t, candidates)
                    when {
                        best == null -> { kept += t.id; _state.update { it.copy(noMatch = it.noMatch + 1) } }
                        !MetaMatch.differs(t, best.first) -> { kept += t.id; _state.update { it.copy(alreadyRight = it.alreadyRight + 1) } }
                        else -> _state.update { it.copy(proposals = it.proposals + Proposal(t, best.first, best.second)) }
                    }
                }
                _state.update { it.copy(done = i + 1) }
                if (kept.size >= 25) { Overrides.keep(kept.toList()); kept.clear() }
                delay(120) // Deezer allows 50 requests per 5 seconds; stay well under it
            }
            Overrides.keep(kept)
            fetchArtistPictures(tracks, retryMissing = !onlyNew)
            _state.update { it.copy(running = false) }
        }
    }

    fun stop() {
        job?.cancel()
        _state.update { it.copy(running = false) }
    }

    fun decide(p: Proposal, replace: Boolean) {
        if (replace) Overrides.replace(p.track, p.match) else Overrides.keep(listOf(p.track.id))
        _state.update { s -> s.copy(proposals = s.proposals.filterNot { it.track.id == p.track.id }) }
    }

    fun replaceAllConfident() {
        _state.value.proposals.filter { it.confidence == Confidence.HIGH }.forEach { decide(it, true) }
    }

    fun keepAll() {
        Overrides.keep(_state.value.proposals.map { it.track.id })
        _state.update { it.copy(proposals = emptyList()) }
    }

    /** Artist photos from Deezer for library artists that don't have one yet (only an exact name match counts). */
    private suspend fun fetchArtistPictures(tracks: List<Track>, retryMissing: Boolean) {
        val known = Overrides.artistPics.value
        val names = tracks.map { it.artist }.filter { it.isNotBlank() && it != "Unknown artist" }
            .distinctBy { it.lowercase() }
            .filter { n -> known[n.lowercase()].let { it == null || (retryMissing && it.isEmpty()) } }
        val found = HashMap<String, String>()
        for (name in names) {
            val hit = try {
                Deezer.searchArtists(name).firstOrNull { Match.norm(it.name) == Match.norm(name) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                break // offline: try again next run
            }
            found[name.lowercase()] = hit?.picture.orEmpty()
            delay(120)
        }
        Overrides.setArtistPics(found)
    }
}
