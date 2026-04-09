# Build notes

## Requirements

- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34
- arm64 Android device for runtime testing of the bundled aria2 binary

## Build

1. Sync Gradle.
2. Build `app` or run:

```bash
./gradlew assembleDebug
```

## Runtime notes

- Downloads are stored in the app-specific external downloads folder:
  `Android/data/com.aria2.downloader/files/Download/Aria2Downloads`
- Torrent and metalink imports are copied into the app's internal storage before being passed to aria2.
- The bundled aria2 wrapper uses Android CA certificates and async DNS flags suitable for Android command-line binaries.
