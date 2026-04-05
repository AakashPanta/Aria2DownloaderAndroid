# Build APK on GitHub Codespaces (Free)

## Why Codespaces?
- **Free tier:** 120 hours/month of Codespaces for free
- **Instant setup:** Java 25 + Android SDK + Gradle pre-installed
- **No local setup:** Build APK directly in browser
- **Download APK:** Download from Codespaces, install on your device

---

## Step-by-Step Instructions

### 1. Fork or Push This Project to GitHub

**Option A: Fork (Easiest)**
- Go to: https://github.com/your-username/Aria2DownloaderAndroid
- (You need to have this repo on GitHub first)

**Option B: Create GitHub Repo**
```bash
cd Aria2DownloaderAndroid
git init
git add .
git commit -m "Initial commit: Aria2 Downloader Android"
git remote add origin https://github.com/YOUR_USERNAME/Aria2DownloaderAndroid.git
git branch -M main
git push -u origin main
```

### 2. Open in GitHub Codespaces

In your GitHub repo:
1. Click the green **"Code"** button
2. Click **"Codespaces"** tab
3. Click **"Create codespace on main"**
4. Wait ~60 seconds for environment to load (shows Java + Android SDK installing)

### 3. Open Terminal in Codespaces

Once Codespaces loads:
- Ctrl+` (backtick) or View → Terminal → New Terminal

### 4. Build the APK

```bash
# Navigate to project
cd Aria2DownloaderAndroid

# Build APK
./gradlew assembleDebug
```

**First build:** 2-5 minutes (downloads dependencies)
**Subsequent builds:** 30-60 seconds

### 5. Find & Download the APK

APK location:
```
app/build/outputs/apk/debug/app-debug.apk
```

**In Codespaces:**
1. Open Explorer (left sidebar)
2. Navigate to `app/build/outputs/apk/debug/`
3. Right-click `app-debug.apk`
4. Select **"Download"**

Or from terminal:
```bash
# Show file size
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

---

## Install APK on Your Device

### Android Device (via ADB)
```bash
# On your Mac (with ADB installed)
adb install ~/Downloads/app-debug.apk
```

### Android Emulator
```bash
# In Android Studio on your Mac
adb install ~/Downloads/app-debug.apk
```

### Direct Installation (No ADB)
1. Transfer `app-debug.apk` to your Android phone
2. Open Files app → navigate to Downloads
3. Tap `app-debug.apk`
4. Tap **"Install"**
5. Grant permission if prompted

---

## Troubleshooting in Codespaces

**"Gradle not found"**
```bash
which gradle
# If not found, restart terminal (Ctrl+Shift+`)
```

**"Android SDK not found"**
```bash
# Codespaces should have pre-installed, but if not:
sdkmanager --list  # Shows installed SDKs
```

**"Build failed - Out of memory"**
- Codespaces has 4GB RAM, usually enough
- If it fails, try restarting Codespaces (green button top-left → Stop codespace)

**"Gradle daemon killed"**
```bash
# Clear Gradle cache
./gradlew clean
./gradlew assembleDebug
```

---

## What You're Building

✅ **Complete Aria2 Downloader APK**
- Multi-connection download engine
- Material 3 UI (dark/light theme)
- Real-time progress tracking
- Download history
- Pause/Resume/Cancel/Retry
- Works on Android 8.0+ (minSdk 26)

**APK Size:** ~5-8 MB (debug)

---

## Next: Build Release APK (Optional)

For Play Store or distribution:

```bash
./gradlew assembleRelease
```

You'll need a signing keystore — see `BUILD_NOW.md` for details.

---

## Free Codespaces Quota

- **120 hours/month** free (2-core, 4GB RAM)
- Each codespace costs ~1 hour per hour running
- Stop when not in use (top-left green button → Stop codespace)
- Deletes automatically after 30 days of inactivity

---

## Need Help?

- Codespaces docs: https://docs.github.com/en/codespaces
- Android Gradle: https://developer.android.com/build
- This project: See `BUILD_NOW.md` and `README.md`

