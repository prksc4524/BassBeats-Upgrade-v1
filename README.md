# BASS BEATS Android project

Native Java Android offline music-player starter project.

## Build
The GitHub Actions workflow at `.github/workflows/build-apk.yml` builds a debug APK and uploads it as the `BASS-BEATS-APK` artifact.

## Notes
- Package/application ID: `com.bassbeats.player`
- Minimum Android: 8.0 (API 26); target SDK 35
- Local audio is read from Android MediaStore.
- Playback runs in `PlaybackService` with a media notification and lock-screen session.
- Some advanced items (persistent playlists/favorites, real audio-effect attachment, live visualizer, sleep-free playback controls) still need further implementation; the APK build alone does not imply every planned feature is finished.
