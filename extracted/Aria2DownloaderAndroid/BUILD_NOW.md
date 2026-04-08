# Build Aria2 Downloader APK — Final Instructions

## Environment Requirement
- macOS or Linux with Android Studio installed
- OR: Java 17+ installed with Android SDK command-line tools

## Option 1: Build in Android Studio (Easiest)

1. **Extract the project:**
```bash
tar -xzf Aria2DownloaderAndroid.tar.gz
cd Aria2DownloaderAndroid
```

2. **Open in Android Studio:**
   - Launch Android Studio
   - File → Open → Select the `Aria2DownloaderAndroid` folder
   - Wait for Gradle sync (30-60 seconds)

3. **Build the APK:**
   - Click **Build** → **Build Bundle(s) / APK(s)** → **Build APK**
   - Or use keyboard shortcut: **Ctrl+Shift+A** (Mac: **Cmd+Shift+A**)
   - APK generates in: `app/build/outputs/apk/debug/app-debug.apk`

4. **Install on device:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Option 2: Command Line Build (No Android Studio)

**Requirement:** Android SDK installed via Android Studio or `sdkmanager`

1. **Extract the project:**
```bash
tar -xzf Aria2DownloaderAndroid.tar.gz
cd Aria2DownloaderAndroid
```

2. **Add Gradle wrapper (one-time):**
```bash
gradle wrapper --gradle-version 8.2
```

3. **Build APK:**
```bash
./gradlew assembleDebug
```

4. **APK location:**
```
app/build/outputs/apk/debug/app-debug.apk
```

5. **Install:**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Troubleshooting

**"JAVA_HOME not set"**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
# Or your JDK path:
export JAVA_HOME="$(brew --cellar java)/openjdk-17.0.x/libexec/openjdk.jdk/Contents/Home"
```

**"Android SDK not found"**
- Android Studio → Settings → Appearance & Behavior → System Settings → Android SDK
- Verify SDK Location is set
- Ensure Build Tools 34.0.0+ is installed

**"Gradle sync failed"**
- File → Invalidate Caches → Invalidate and Restart
- Delete `.gradle/` and `build/` folders
- Sync again

---

## Project Details

**What builds:**
- Complete Aria2 Downloader app
- Multi-connection download engine
- Material 3 UI with dark/light modes
- Download history persistence
- Foreground service for background downloads

**Build time:** ~2-5 minutes (first build with dependency download)

**APK size:** ~5-8 MB (debug build)

**Min SDK:** 26 (Android 8.0+)
**Target SDK:** 34 (Android 14)

---

## Next Steps After APK Build

1. **Test on emulator:**
   - Open Android Virtual Device (AVD)
   - Drag APK onto emulator, or:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **Test on physical device:**
   - Enable Developer Mode (Settings → About → tap Build Number 7x)
   - Enable USB Debugging
   - Connect via USB
   - Run: `adb install app/build/outputs/apk/debug/app-debug.apk`

3. **Release build (for Play Store):**
   - Generate keystore:
   ```bash
   keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias aria2
   ```
   - In Android Studio: Build → Generate Signed Bundle / APK
   - Select release.jks and sign
   - APK ready for Play Store submission

