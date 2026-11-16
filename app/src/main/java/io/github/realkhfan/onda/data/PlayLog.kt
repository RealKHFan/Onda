package io.github.realkhfan.onda.data

import android.content.Context
import io.github.realkhfan.onda.core.Discovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** What you've listened to (30 s or most of a song), kept on the phone only. Feeds the Daily and Weekly mixes. */
object PlayLog {
    private const val MAX = io.github.realkhfan.onda.core.LinkSync.MAX_PLAYS

    private val _plays = MutableStateFlow<List<Discovery.Play>>(emptyList())
    val plays: StateFlow<List<Discovery.Play>> = _plays.asStateFlow()

    private var file: File? = null

    fun load(context: Context) {
        file = File(context.filesDir, "plays.json")
        _plays.value = try {
            val arr = JSONArray(file?.takeIf { it.exists() }?.readText() ?: "[]")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Discovery.Play(o.getString("k"), o.optString("a"), o.optString("t"), o.getLong("at"))
            }
        } catch (_: Exception) {
            emptyList() // history only shapes suggestions; starting over is harmless
        }
    }

    fun add(key: String, artist: String, title: String) {
        _plays.value = (_plays.value + Discovery.Play(key, artist, title, System.currentTimeMillis())).takeLast(MAX)
        write()
    }

    /** Spotify listening history: merged in time order, duplicates (same song, same moment) dropped. */
    fun import(list: List<Discovery.Play>) {
        _plays.value = (_plays.value + list).distinctBy { Triple(it.artist, it.title, it.atMs / 60_000) }.sortedBy { it.atMs }.takeLast(MAX)
        write()
    }

    /** History after syncing with your PC. */
    fun replace(list: List<Discovery.Play>) {
        _plays.value = list.sortedBy { it.atMs }.takeLast(MAX)
        write()
    }

    /** After files are renamed (their ids change), their history follows them. */
    fun migrateIds(map: Map<String, String>) {
        if (map.isEmpty()) return
        _plays.value = _plays.value.map { p -> map[p.key]?.let { p.copy(key = it) } ?: p }
        write()
    }

    private fun write() {
        val f = file ?: return
        try {
            val arr = JSONArray()
            _plays.value.forEach { arr.put(JSONObject().put("k", it.key).put("a", it.artist).put("t", it.title).put("at", it.atMs)) }
            val tmp = File(f.path + ".tmp")
            tmp.writeText(arr.toString())
            tmp.renameTo(f)
        } catch (_: Exception) {
        }
    }
}
