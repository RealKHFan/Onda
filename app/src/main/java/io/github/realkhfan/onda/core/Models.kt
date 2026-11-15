package io.github.realkhfan.onda.core

/** A playable local track. [id] is stable (the document URI string). */
data class Track(
    val id: String,
    val uri: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val trackNo: Int,
    val durationMs: Long,
    val folderName: String,
    val artUri: String?,
    /** The file's own name, e.g. "song.m4a" ("" for server songs). Used by "Rename files". */
    val fileName: String = "",
) {
    val albumKey: String get() = "${albumArtist.lowercase()}|${album.lowercase()}"
    val artistKey: String get() = artist.lowercase()
}

data class Album(
    val key: String,
    val title: String,
    val artist: String,
    val artUri: String?,
    val tracks: List<Track>,
)

data class ArtistGroup(
    val name: String,
    val artUri: String?,
    val tracks: List<Track>,
) {
    val albumCount: Int get() = tracks.map { it.albumKey }.distinct().size
}

data class FolderGroup(
    val name: String,
    val tracks: List<Track>,
)

/** Where a catalog item comes from. The prefix keeps ids apart; [full] catalogs stream whole songs for free. */
enum class Source(val prefix: String, val label: String, val full: Boolean) {
    DEEZER("dz:", "Deezer", false), ITUNES("it:", "Apple", false),
    AUDIUS("au:", "Audius", true), JAMENDO("ja:", "Jamendo", true), ARCHIVE("ia:", "Internet Archive", true),
    OPENVERSE("ov:", "Openverse", true);

    companion object {
        fun of(key: String): Source? = entries.firstOrNull { key.startsWith(it.prefix) }

        /** "dz:123" -> (DEEZER, 123); null for anything else, e.g. a local file id or a text id. */
        fun parse(key: String): Pair<Source, Long>? =
            of(key)?.let { s -> key.removePrefix(s.prefix).toLongOrNull()?.let { s to it } }
    }
}

/**
 * A song from an online catalog. [preview] is the audio URL: a 30-second clip from Deezer/Apple (null when there's
 * none for us), the whole song from a [Source.full] catalog. Those use the text id [ref] instead of [id].
 */
data class RemoteTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val cover: String?,
    val preview: String?,
    val durationSec: Int,
    val source: Source = Source.DEEZER,
    /** Deezer includes the artist's photo with each track; used when a match is accepted. */
    val artistPicture: String? = null,
    val ref: String = "",
    /** The catalog lets you keep a copy: Internet Archive always, Audius and Jamendo when the artist allows it. */
    val canDownload: Boolean = false,
) {
    val key: String get() = source.prefix + ref.ifEmpty { id.toString() }
}

/** Deezer artist. */
data class RemoteArtist(val id: Long, val name: String, val picture: String?)

/** Deezer album or single, or an Internet Archive item ([ref] = its identifier). [date] is unknown in Deezer album search. */
data class RemoteAlbum(
    val id: Long,
    val title: String,
    val artist: String,
    val cover: String?,
    val date: String?,
    val type: String?,
    val artistId: Long = 0,
    val source: Source = Source.DEEZER,
    val ref: String = "",
) {
    val year: String? get() = date?.take(4)
}

data class RemoteResults(
    val tracks: List<RemoteTrack> = emptyList(),
    val artists: List<RemoteArtist> = emptyList(),
    val albums: List<RemoteAlbum> = emptyList(),
    /** Whole songs from free catalogs (Audius, Jamendo) and free albums (Internet Archive). */
    val free: List<RemoteTrack> = emptyList(),
    val freeAlbums: List<RemoteAlbum> = emptyList(),
) {
    val isEmpty: Boolean get() = tracks.isEmpty() && artists.isEmpty() && albums.isEmpty() && free.isEmpty() && freeAlbums.isEmpty()
}
