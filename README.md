# Aria2 Downloader Android

Material 3 Android download manager powered by a bundled aria2 daemon and JSON-RPC.

## Highlights
- Direct HTTP/HTTPS/FTP/SFTP downloads
- Magnet, `.torrent` and Metalink support
- Multi-connection downloads with queue controls
- Foreground service monitoring and persistent sync
- System / Light / Dark theme
- Custom download location with SAF support
- Default public folder: `/storage/emulated/0/Download`
- Cleartext HTTP allowed for arbitrary file hosts
- RPC port scan and daemon reconnect on `6800..6810`

## Build
```bash
./gradlew assembleDebug
```

## Runtime notes
- The bundled daemon targets `arm64-v8a`
- The app first tries the packaged native binary and falls back to the extracted asset copy
- The service owns daemon startup and keeps syncing progress while downloads are active
