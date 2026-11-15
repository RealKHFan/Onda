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
}
