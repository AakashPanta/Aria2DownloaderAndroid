# Aria2 Downloader Android App

A production-grade Android downloader app built with Kotlin, Jetpack Compose, and Material 3.

## Features

- **Multi-connection Segmented Downloads**: Parallel segment downloads using HTTP Range requests
- **Adaptive Downloading**: Automatic fallback to single-connection for servers without Range support
- **Real-time Progress Tracking**: Live speed, ETA, and progress updates
- **Download Control**: Pause, Resume, Cancel, and Retry functionality
- **Download History**: Persistent storage of completed and failed downloads
- **URL Validation**: Pre-download validation with server capability detection
- **Material 3 UI**: Premium, modern interface with dark/light theme support
- **Foreground Service**: Background downloading with persistent notifications
- **Room Database**: Robust local persistence layer
- **Responsive Design**: Works seamlessly on all Android devices

## Tech Stack

- **Language**: Kotlin 1.9.x
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture separation
- **Networking**: OkHttp 4.12.0
- **Persistence**: Room 2.6.1
- **Dependency Injection**: Hilt
- **Async**: Coroutines + Flow
- **Navigation**: Navigation Compose

## Project Structure

```
app/src/main/
├── java/com/aria2/downloader/
│   ├── Aria2DownloaderApp.kt          # Application entry point
│   ├── MainActivity.kt                 # Main activity
│   ├── data/
│   │   ├── local/                     # Room database
│   │   │   ├── AppDatabase.kt
│   │   │   ├── DownloadDao.kt
│   │   │   └── DownloadEntity.kt
│   │   └── repository/
│   │       └── DownloadRepository.kt
│   ├── di/
│   │   └── AppModule.kt               # Hilt DI configuration
│   ├── domain/
│   │   ├── model/                     # Domain models
│   │   │   ├── DownloadInfo.kt
│   │   │   ├── DownloadStatus.kt
│   │   │   └── DownloadProgress.kt
│   │   └── engine/                    # Download engine
│   │       ├── DownloadEngine.kt
│   │       ├── SegmentedDownloader.kt
│   │       ├── BandwidthTracker.kt
│   │       └── URLValidator.kt
│   ├── service/
│   │   └── DownloadService.kt         # Foreground download service
│   └── ui/
│       ├── navigation/
│       │   └── AppNavigation.kt
│       ├── theme/
│       │   ├── Color.kt
│       │   ├── Theme.kt
│       │   └── Type.kt
│       ├── screens/
│       │   ├── home/
│       │   ├── newdownload/
│       │   ├── detail/
│       │   ├── history/
│       │   └── settings/
│       └── components/
│           ├── DownloadCard.kt
│           ├── ProgressIndicator.kt
│           └── SpeedDisplay.kt
└── res/
    ├── drawable/
    ├── values/
    └── xml/
```

## Build & Requirements

- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34
- **Java**: 11
- **Android Studio**: Latest version

## Building the App

### Prerequisites
1. Install Android Studio
2. Install Android SDK (API 34)
3. Configure ANDROID_HOME environment variable

### Build Steps

```bash
# Clone or navigate to project directory
cd Aria2DownloaderAndroid

# Build the APK
./gradlew assembleDebug

# Build release APK (requires signing configuration)
./gradlew assembleRelease

# Run tests
./gradlew test
```

### Output
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/app-release.apk`

## Key Implementation Details

### Segmented Downloading
The app uses HTTP Range headers to download large files in parallel segments:
- Supports servers with `Accept-Ranges: bytes`
- Automatic fallback to single-connection for unsupported servers
- Configurable number of parallel connections (1-8)
- Smart bandwidth tracking and ETA calculation

### Download Engine
- **DownloadEngine**: Orchestrates the download process
- **SegmentedDownloader**: Handles multi-threaded downloads
- **URLValidator**: Pre-validates URLs and fetches server capabilities
- **BandwidthTracker**: Tracks real-time speed and estimates time remaining

### UI Architecture
- Screens organized by navigation destination
- ViewModels manage state using Kotlin Flow
- Composables bind to StateFlow for reactive updates
- MVVM pattern ensures clean separation of concerns

### Persistence
- Room database stores download metadata
- SharedPreferences for user settings
- File-based storage for downloaded content

### Notifications
- Foreground service ensures downloads survive app backgrounding
- Material 3 styled notification cards
- Download progress shown in notification bar

## Permissions

Required permissions (declared in AndroidManifest.xml):
- `INTERNET` - Download files
- `READ_EXTERNAL_STORAGE` - Access files (Android 12 and below)
- `WRITE_EXTERNAL_STORAGE` - Save downloads (Android 12 and below)
- `MANAGE_EXTERNAL_STORAGE` - Broad file access (Android 11+)
- `FOREGROUND_SERVICE` - Background downloads
- `POST_NOTIFICATIONS` - Download notifications

## Configuration

### Max Connections
Configure max parallel connections in Settings (1-8 connections)

### Theme
- Automatic dark mode based on system preference
- Manual override in Settings
- Material 3 dynamic color support on Android 12+

### Notifications
Enable/disable download notifications in Settings

## ProGuard Rules

Production builds use ProGuard minification with configured rules for:
- AndroidX and Compose
- Hilt DI
- Room ORM
- OkHttp
- Serialization
- Domain models and data entities

## Known Limitations

1. Downloads resume from last byte (some servers may not support partial resume)
2. Very large files (>2GB) may require device restart for cleanup
3. No bandwidth throttling - uses all available bandwidth

## Future Enhancements

- Batch downloading
- Download scheduling
- Network type restrictions
- Proxy support
- Torrent support
- Plugin system for custom handlers

## License

Proprietary - Aria2 Downloader

## Support

For issues or feature requests, contact development team.
