package io.github.realkhfan.onda.core

/** Pure grouping/sorting logic, kept off Android so it can be unit-tested on the JVM. */
object Grouping {

    fun songsSorted(tracks: List<Track>): List<Track> =
        tracks.sortedWith(compareBy({ it.title.lowercase() }, { it.artist.lowercase() }))

    fun albums(tracks: List<Track>): List<Album> =
        tracks.groupBy { it.albumKey }
            .map { (key, group) ->
                val first = group.first()
                Album(
                    key = key,
                    title = first.album.ifBlank { "Unknown album" },
                    artist = first.albumArtist.ifBlank { first.artist }.ifBlank { "Unknown artist" },
                    artUri = group.firstNotNullOfOrNull { it.artUri },
                    tracks = group.sortedWith(compareBy({ it.trackNo }, { it.title.lowercase() })),
                )
            }
            .sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))

    fun artists(tracks: List<Track>): List<ArtistGroup> =
        tracks.groupBy { it.artistKey }
            .map { (_, group) ->
                ArtistGroup(
                    name = group.first().artist.ifBlank { "Unknown artist" },
                    artUri = group.firstNotNullOfOrNull { it.artUri },
                    tracks = group.sortedWith(
                        compareBy({ it.album.lowercase() }, { it.trackNo }, { it.title.lowercase() }),
                    ),
                )
            }
            .sortedBy { it.name.lowercase() }

    fun folders(tracks: List<Track>): List<FolderGroup> =
        tracks.groupBy { it.folderName }
            .map { (name, group) -> FolderGroup(name, songsSorted(group)) }
            .sortedBy { it.name.lowercase() }

    /** A drag in a list that shows some of [all] ([shown]): [shown]`[from]` lands where [shown]`[to]` was, and that one moves aside. */
    fun <T> moved(all: List<T>, shown: List<T>, from: Int, to: Int): List<T> {
        if (from !in shown.indices || to !in shown.indices || from == to) return all
        val at = all.indexOf(shown[from])
        val target = all.indexOf(shown[to])
        if (at < 0 || target < 0) return all
        return all.toMutableList().apply { add(target, removeAt(at)) }
    }

    /**
     * Your playlists' own order after a drag in [shown] (playlist ids as listed: pinned ones first). Pinned ones move among
     * themselves (in [pins]), the rest in [order]; a drag across that line stops at it. Returns the new order and pins.
     */
    fun dragPlaylist(order: List<String>, pins: List<String>, shown: List<String>, from: Int, to: Int): Pair<List<String>, List<String>> {
        val pinned = shown.count { it in pins }
        return when {
            from !in shown.indices -> order to pins
            from < pinned -> order to moved(pins, shown, from, to.coerceAtMost(pinned - 1))
            else -> moved(order, shown, from, to.coerceAtLeast(pinned)) to pins
        }
    }
}
