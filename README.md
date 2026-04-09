# Aria2 Downloader Android

Material 3 Android download manager rebuilt around a bundled `aria2c` daemon and local JSON-RPC.

## What this build fixes

The old app validated links in one code path and downloaded them in another. That caused many real hosts to get stuck on **"validating URL"** when they rejected `HEAD` requests, redirected unexpectedly, or needed a ranged `GET`.

This build fixes that by:

- validating HTTP(S)/FTP/SFTP links with a tolerant `HEAD` → `GET bytes=0-0` fallback
- routing the final request into the same aria2 JSON-RPC pipeline that performs the actual download
- using a foreground service to keep the daemon alive and sync progress back into Room
- bundling the Android-friendly `aria2c` binary and wrapper script inside the app

## Features

- direct links
- magnet links
- `.torrent` import
- Metalink import
- multi-connection acceleration
- configurable queue and split tuning
- DHT / PEX / local peer discovery / optional torrent encryption
- System / Light / Dark theme
- launcher icon switching
- Room-backed history
- foreground progress syncing

## Bundled aria2 binary

The included binary is the supplied **arm64-v8a Android build**. On non-arm64 devices, the engine will fail to start.

## Build

```bash
./gradlew assembleDebug
```

APK output:

```bash
app/build/outputs/apk/debug/app-debug.apk
```
