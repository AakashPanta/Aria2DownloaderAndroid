# Build Instructions - Aria2 Downloader Android

## System Requirements

### Minimum Requirements
- **OS**: Linux, macOS, or Windows
- **RAM**: 8GB (16GB recommended for comfortable builds)
- **Disk Space**: 10GB (for SDK and build artifacts)
- **JDK**: Java 11 or higher (OpenJDK 11+ or OracleJDK)

### Android SDK Requirements
- **Compile SDK**: 34
- **Target SDK**: 34
- **Min SDK**: 26 (Android 8.0)
- **Build Tools**: 34.0.0 or later

## Installation & Setup

### 1. Install Android Studio
- Download from [developer.android.com](https://developer.android.com/studio)
- Follow the installation wizard
- Complete the SDK setup during initial launch

### 2. Install Android SDK Components
In Android Studio:
1. Go to **Tools > SDK Manager**
2. Install the following:
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0+
   - Android Emulator (optional)
   - Android SDK Platform-Tools
   - Google Play services

### 3. Configure Environment Variables

#### Linux/macOS:
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/tools/bin:$ANDROID_HOME/platform-tools
```

#### Windows:
```cmd
set ANDROID_HOME=C:\Users\<YourUsername>\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools
```

Add these to your shell profile (`.bashrc`, `.zshrc`, etc.) or system environment variables for persistence.

### 4. Verify Installation
```bash
java -version          # Should show Java 11+
adb version           # Should show ADB version
```

## Building the App

### Navigate to Project Directory
```bash
cd /home/node/.openclaw/workspace/Aria2DownloaderAndroid
```

### Build Debug APK
```bash
# Using Gradle wrapper (recommended)
./gradlew assembleDebug

# Or with verbose output
./gradlew assembleDebug --info
```

**Output**: `app/build/outputs/apk/debug/app-debug.apk`

### Build Release APK
```bash
# Build unsigned release APK
./gradlew assembleRelease

# With signing (requires keystore configuration)
./gradlew assembleRelease
```

**Output**: `app/build/outputs/apk/release/app-release.apk`

### Build Options

#### Clean Build
```bash
./gradlew clean assembleDebug
```

#### Skip Tests
```bash
./gradlew assembleDebug -x test
```

#### Build Specific Module
```bash
./gradlew app:assembleDebug
```

#### Parallel Build (Faster)
```bash
./gradlew assembleDebug --parallel
```

#### With ProGuard Minification
```bash
./gradlew assembleRelease  # Release builds use minification by default
```

## Installing & Running

### Install on Connected Device/Emulator
```bash
./gradlew installDebug
```

### Run on Emulator
```bash
# First start emulator
emulator -avd <emulator_name>

# Then run
./gradlew installDebug

# Or in one command
./gradlew installDebugAndRun
```

### Manual Installation
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Troubleshooting

### "ANDROID_HOME not set"
```bash
# Add to current session
export ANDROID_HOME=$HOME/Android/Sdk

# Or check Android Studio settings
# File > Settings > Appearance & Behavior > System Settings > Android SDK
```

### "Build fails with 'Failed to resolve: com.google.android...'"
- Update Android SDK in SDK Manager
- Delete `.gradle/` directory and rebuild:
  ```bash
  rm -rf ~/.gradle
  ./gradlew clean assembleDebug
  ```

### "Execution failed for task ':app:preDexDebug'"
- Increase Gradle heap size in `gradle.properties`:
  ```properties
  org.gradle.jvmargs=-Xmx2048m
  ```

### "No connected devices found"
- Ensure device is connected: `adb devices`
- Enable USB debugging on Android device
- Install ADB drivers if on Windows

### Build Times Too Long
- Use parallel builds: `./gradlew assembleDebug --parallel`
- Use daemon: `./gradlew --daemon assembleDebug`
- Increase heap: `org.gradle.jvmargs=-Xmx4096m`

### Kotlin Compilation Issues
- Update Kotlin version in `gradle/libs.versions.toml`
- Clear gradle cache: `./gradlew cleanBuildCache`
- Recompile: `./gradlew assemble`

## IDE Setup (Android Studio)

### Import Project
1. Open Android Studio
2. File > Open
3. Select `Aria2DownloaderAndroid` directory
4. Wait for Gradle sync to complete

### Configure Gradle
Android Studio will auto-detect and configure Gradle. To verify:
- File > Settings > Build, Execution, Deployment > Gradle
- Ensure "Use Gradle from specified location" is selected
- Use embedded Gradle is fine for most cases

### Run Configurations
1. Select "Run" from top menu
2. Click device or emulator dropdown
3. Click "Run 'app'" or press Shift+F10

## CI/CD Build

### GitHub Actions Example
```yaml
name: Build APK
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: 11
      - run: chmod +x ./gradlew
      - run: ./gradlew assembleDebug
```

### GitLab CI Example
```yaml
build:
  image: gradle:7.4-jdk11
  script:
    - cd /workspace/Aria2DownloaderAndroid
    - ./gradlew assembleDebug
  artifacts:
    paths:
      - app/build/outputs/apk/debug/app-debug.apk
```

## Keystore Setup (Release Builds)

### Create Keystore
```bash
keytool -genkey -v -keystore aria2.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias aria2key
```

### Configure Signing
Edit `app/build.gradle.kts`:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file("../aria2.keystore")
        storePassword = "your_store_password"
        keyAlias = "aria2key"
        keyPassword = "your_key_password"
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
    }
}
```

## APK Details

### Debug APK
- **Size**: ~5-8 MB
- **Signed**: Debug key only
- **Optimized**: No
- **ProGuard**: Disabled
- **Use**: Development and testing

### Release APK
- **Size**: ~2-3 MB (with minification)
- **Signed**: None (unsigned)
- **Optimized**: Yes
- **ProGuard**: Enabled
- **Use**: Production distribution

## Build Variants

### Available Variants
```
├── debug
│   └── debugApk
├── release
    └── releaseApk
```

Switch variants in Android Studio:
- Build > Select Build Variant > Choose debug or release

## Gradle Tasks

### Common Tasks
```bash
./gradlew tasks                    # List all tasks
./gradlew assembleDebug            # Build debug APK
./gradlew assembleRelease          # Build release APK
./gradlew test                     # Run unit tests
./gradlew clean                    # Clean build artifacts
./gradlew build                    # Full build (debug + release)
./gradlew installDebug             # Build and install debug
./gradlew --version                # Show Gradle version
```

## Performance Optimization

### Build Cache
```bash
# Enable build cache
./gradlew assembleDebug --build-cache

# View cache stats
./gradlew assembleDebug --build-cache --info | grep "Cache"
```

### Incremental Build
Changes to only one file trigger incremental compilation, saving time.

### Parallel Compilation
```bash
org.gradle.parallel=true
org.gradle.workers.max=4
```
Add to `gradle.properties` for parallel task execution.

## Kotlin Version & Compatibility

- **Kotlin**: 1.9.23
- **Compose**: 2024.02.00 BOM
- **Java Target**: 11

Updates available via `gradle/libs.versions.toml`.

## Build Artifacts

### Generated Files
```
app/build/
├── intermediates/      # Intermediate build files
├── outputs/            # Final outputs
│   └── apk/
│       ├── debug/
│       │   └── app-debug.apk
│       └── release/
│           └── app-release.apk
└── generated/          # Generated code (BuildConfig, R, etc.)
```

## Cleanup

### Clear Build Cache
```bash
rm -rf ~/.gradle/caches
```

### Clean Project
```bash
./gradlew clean
```

### Deep Clean (Removes all build files)
```bash
./gradlew clean && rm -rf build app/build
```

## Next Steps

1. ✅ Build the APK: `./gradlew assembleDebug`
2. 📦 Install on device: `adb install app/build/outputs/apk/debug/app-debug.apk`
3. 🚀 Run and test the app
4. 📝 Review AndroidManifest.xml for required permissions
5. 🎨 Customize theme in `ui/theme/Color.kt`
6. ⚙️ Adjust settings in SettingsViewModel

## Support

For build issues, check:
- Android Studio's Build output log
- Stack trace in console
- Gradle sync errors in the IDE
- System ANDROID_HOME configuration
