package io.github.realkhfan.onda.data

import android.content.Context
import io.github.realkhfan.onda.core.Match
import org.json.JSONObject
import java.io.File

/** Artist name -> Deezer id (0 = not on Deezer), looked up once and remembered. Shared by the mixes and new releases. */
object ArtistIds {
    private val ids = HashMap<String, Long>()
    private var file: File? = null

    fun load(context: Context) {
        file = File(context.filesDir, "artist_ids.json")
        try {
            val o = JSONObject(file?.takeIf { it.exists() }?.readText() ?: "{}")
            o.keys().forEach { ids[it] = o.getLong(it) }
        } catch (_: Exception) {
            // only a cache: names are looked up again
        }
    }

    fun cached(name: String): Long? = ids[Match.norm(name)]

    /** Only an exact (normalised) name match counts, so a different artist with a similar name isn't picked. Throws when offline. */
    suspend fun resolve(name: String): Long {
        val k = Match.norm(name)
        ids[k]?.let { return it }
        val id = Deezer.searchArtists(name).firstOrNull { Match.norm(it.name) == k }?.id ?: 0L
        ids[k] = id
        save()
        return id
    }

    private fun save() {
        val f = file ?: return
        try {
            val o = JSONObject()
            ids.forEach { (k, v) -> o.put(k, v) }
            val tmp = File(f.path + ".tmp")
            tmp.writeText(o.toString())
            tmp.renameTo(f)
        } catch (_: Exception) {
        }
    }
}
