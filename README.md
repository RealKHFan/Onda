# Onda

A Spotify-style music player for Android (built for GrapheneOS) that needs no account, sends no tracking data and contains no Google code.

Onda is offline-first: it plays music you own or can legally stream, and works fully offline with your own files.

## What it does
- **Your music:** local folders (Storage Access Framework, no storage permission) and your own Navidrome / OpenSubsonic server, including its playlists ([setup note](docs/navidrome.md)).
- **Free full songs:** Audius, Jamendo, Openverse (Creative Commons: Wikimedia, ccMixter) and the Internet Archive (netlabels and the Live Music Archive). A Deezer preview plays as a free full copy when the artist's own account has one.
- **Discover:** search the Deezer and Apple catalogs with 30-second previews, artist and album pages, a "Want to listen" list, Daily 10 and Weekly 30 mixes (similar artists from Deezer and ListenBrainz), similar songs when the queue ends, and new-release notifications for your artists.
- **Library:** playlists (rename, drag to reorder), Liked Songs, followed artists, saved albums, "not interested", metadata matching, synced lyrics (LRCLIB), renaming files from tags, backup and restore.
- **Filters:** remixes, sped-up/slowed, live, karaoke and AI-generated "artists" are kept out of discovery. The AI list comes from [spotify-ai-blocker](https://github.com/CennoxX/spotify-ai-blocker) (MIT), and you can mark more artists yourself.
- **Spotify import:** playlists, liked songs and recent listening history from Spotify's data export or Exportify CSVs.
- **Downloads:** saved as real music files in your music folder, only from sources that allow it (your server, free catalogs).
- **Android Auto:** playlists, albums, artists, search and voice.

## Privacy
- No account, no analytics, no crash reporting, no device ID. Your history never leaves the phone.
- Online services (Deezer, Apple, Audius, Jamendo, Openverse, Internet Archive, ListenBrainz/MusicBrainz, LRCLIB) only see search words, song names and your IP address, and each can be turned off. Deny Onda's Network permission and it keeps working offline.
- Your server password is never stored, only a salted token.
- No Google Play Services, Firebase or ML Kit. Every build is checked for them.

## What Onda will not do
No downloading or decoding YouTube, no ripping paid services, no bypassing paid features. Previews stay previews.

## Install
Releases will be published here with their APK and SHA-256. Until then, builds are debug builds for testing.

## License
GPL-3.0, see [LICENSE](LICENSE). The bundled AI artist list is MIT-licensed, see `app/src/main/assets/ai_artists_LICENSE.txt`.
