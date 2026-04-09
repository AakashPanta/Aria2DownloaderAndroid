# Build Instructions

## Android Studio
1. Open the project root.
2. Let Gradle sync.
3. Build the debug APK with **Build > Build APK(s)**.

## Command line
```bash
./gradlew clean assembleDebug
```

## Runtime requirements
- arm64 Android device for the bundled aria2 binary
- Notification permission on Android 13+ for foreground progress notifications
- Optional custom output folder is chosen through the system folder picker in Settings
