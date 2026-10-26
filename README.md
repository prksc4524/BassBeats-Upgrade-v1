# BASS BEATS

Offline-first Android music player project for Android 16, with best-effort compatibility back to Android 8 (API 26).

## Build an APK from your phone using GitHub Actions
1. Create a GitHub repository named `BassBeats` (or another name).
2. Upload all files and folders from this project ZIP to the repository root. Keep `.github/workflows/build-apk.yml` in place.
3. Open the repository's **Actions** tab and enable workflows if GitHub asks.
4. Select **Build BASS BEATS APK** → **Run workflow**.
5. Open the completed run and download `bass-beats-debug-apk` under Artifacts. Extract it and install `app-debug.apk` on your phone.

The workflow builds a debug APK, not a Play Store release. Android may ask you to allow installs from the browser/file manager you use.

## Current implementation notes
- Local audio library scan and basic song playback UI are implemented.
- Foreground playback service exposes a media notification and Android media-session lock-screen controls.
- The 10-band equalizer screen currently includes UI controls; connecting real device audio effects, real frequency-driven visualizer data, full playlists/favorites/queue persistence, seek/progress synchronization, and robust headset routing remain follow-up implementation work. Do not treat those as completed until tested on-device.
- Exact supplied logo image is included as the app drawable; launcher icon sizing/adaptive icon packaging may need refinement.
