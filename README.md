# Aria2 Downloader Android

Production-style Android download manager built with Kotlin, Jetpack Compose, Material 3, Room, DataStore and a bundled `aria2c` daemon controlled over local JSON-RPC.

## Highlights
- Direct links with pause, resume, retry and duplicate handling
- Multi-connection downloads via aria2 split and per-server tuning
- Magnet links, `.torrent` import and Metalink support
- Active, queued and completed sections
- Queue reordering through `aria2.changePosition`
- Foreground-service syncing and progress UI
- System / Light / Dark theme selection
- Download-location export using SAF tree picker and persisted URI permissions
- Startup permission gate for notifications and legacy storage where applicable

## Project structure
- `app/src/main/java/com/aria2/downloader` — Android app source
- `app/src/main/assets/bin/aria2c-arm64-v8a` — bundled aria2 binary
- `app/src/main/res` — Material 3 resources and icons
- `.github/workflows` — GitHub Actions build and packaging workflows

## Build
```bash
./gradlew assembleDebug
```

APK output:
`app/build/outputs/apk/debug/`

## Notes
- The bundled aria2 binary in this project is for `arm64-v8a`
- Downloads are written to the app-managed staging folder first; if the user chooses a custom folder in settings, completed files are copied there automatically
