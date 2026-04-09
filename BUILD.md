# Build Instructions

1. Open the project in Android Studio or run the GitHub Actions APK workflow.
2. Build with `./gradlew assembleDebug`.
3. Install the generated debug APK.
4. On first launch, grant notifications, media permissions and All files access if you want direct writes to `/storage/emulated/0/Download`.
5. Test with HTTPS and HTTP sample files, then with a magnet or torrent.

## Debugging daemon startup
- Check `files/aria2/home/aria2.log` inside app storage if startup fails.
- The app now classifies common startup issues: permission denial, invalid flags, storage path failures and RPC port conflicts.
- RPC health checks scan ports `6800..6810` before the app decides startup failed.
