# Adless Streaming

An original Android music player by Tanishq. Search public catalogs or play local audio, with an ad-free interface and no account requirement for listeners.

> Public alpha. The current GitHub APK is debug-signed for testing, not a production-signed release. Online sources are subject to their own terms and per-track licenses.

## What is here now

- Original Compose interface and a documented design system.
- A pluggable `MusicSource` interface and independent source adapters for local audio, Jamendo, Audius, Internet Archive, and Wikimedia Commons.
- Search across enabled adapters, track attribution/license details, source controls, a Media3 playback service, seek/repeat/shuffle controls, and an editable queue.
- On-device likes, recent listening history, user playlists, and saved source/theme settings.
- No ads, trackers, analytics, or listener accounts are included.

Offline downloads, synced lyrics, sleep timer, equalizer, playback speed, adaptive artwork colors, and automated UI/accessibility verification are planned follow-up work. Jamendo offline downloads are intentionally disabled under its API terms. Online playback is not available for tracks with unclear rights.

## Sources and rights

See [the source review](docs/source-review.md) for access methods, terms, and exclusions. Supported sources are pluggable and can be turned off. Search results retain source attribution and license metadata. Never assume that publicly reachable audio is free to reuse.

The following sources are excluded: YouTube scraping/extraction, FMA raw-file deep links, Free To Use without an app license, and private-account stream extraction from subscription services.

See [the privacy statement](docs/privacy.md) for data flows and on-device storage behavior.

## Setup

Requirements: JDK 17 or later and Android SDK Platform 36. The Gradle wrapper pins Gradle 8.14.6. Open the project folder in Android Studio or run tasks from the project root:

```sh
./gradlew spotlessApply
./gradlew spotlessCheck lintDebug testDebugUnitTest assembleDebug
```

## Production signing

Release builds use R8 and resource shrinking and require a signing keystore. Provide `ADLESS_RELEASE_KEYSTORE_FILE`, `ADLESS_RELEASE_STORE_PASSWORD`, `ADLESS_RELEASE_KEY_ALIAS`, and `ADLESS_RELEASE_KEY_PASSWORD` in the build environment, then run `./gradlew :app:assembleRelease`. Release builds fail closed when these values are missing. Never commit the keystore or passwords. For GitHub Actions, add repository secrets named `ADLESS_RELEASE_KEYSTORE_BASE64`, `ADLESS_RELEASE_STORE_PASSWORD`, `ADLESS_RELEASE_KEY_ALIAS`, and `ADLESS_RELEASE_KEY_PASSWORD`, then run the **Signed Android APK** workflow. It publishes the APK as a workflow artifact and does not create a GitHub release. Back up the keystore securely; losing it prevents signed in-place updates. The currently published alpha APK is debug-signed; do not treat it as a production update key.

To enable Jamendo, create a developer application and place the client ID in ignored `local.properties`:

```properties
jamendoClientId=YOUR_CLIENT_ID
```

Alternatively, export `JAMENDO_CLIENT_ID` before building. `.env.example` documents the setting; no credential is committed. Local audio uses Android's media permission and is not uploaded.

## Architecture

`domain` defines source and track contracts plus queue behavior. `data/source` contains provider-specific adapters, `SourceRegistry` isolates provider failures, `MusicLibraryStore` saves user data in SQLite, `LocalSettingsStore` saves source/theme choices, `playback` owns the Media3 background session, and Compose UI consumes a ViewModel. See [architecture notes](docs/architecture.md).

## Design

The original palette, spacing, typography, component, motion, and adaptive layout rules are documented in [docs/design-system.md](docs/design-system.md). The visual identity is designed independently of Echo Music.

## Credits

**Inspired by Echo Music:** [EchoMusicApp/Echo-Music](https://github.com/EchoMusicApp/Echo-Music). Echo Music's authors and contributors are credited by their upstream project. Adless Streaming is an independent, clean-room implementation; no Echo Music code or assets were copied.

## Screenshots

Screenshots from a physical-device validation pass are still pending.

## Download page

The standalone page is [docs/download.html](docs/download.html). It links to the GitHub Releases page when hosted at `USERNAME.github.io`.

## License

Original Adless Streaming code is licensed under the [MIT License](LICENSE), copyright (c) 2026 Tanishq. Third-party libraries and music files retain their own licenses.

