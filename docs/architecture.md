# Architecture

## Layers

- **UI:** Jetpack Compose screens and controls render immutable state and send user actions to `MainViewModel`.
- **Domain:** `MusicTrack`, `MusicSource`, `SourceSearch`, and `QueueState` define stable app-side concepts independent of provider protocols.
- **Data:** one adapter per source translates catalog APIs into `MusicTrack`. `SourceRegistry` runs selected adapters concurrently and contains each provider's failure so one outage does not fail the whole search.
- **Playback:** a Media3 `MediaSessionService` owns ExoPlayer. `PlaybackController` connects the foreground UI to the session, which supplies background and OS media controls.
- **Composition:** `AppContainer` provides the source registry and playback controller. This small manual composition root is dependency injection without a framework dependency.

## Source contract

Each `MusicSource` exposes an id, user-facing name, short rights note, search operation, and stream resolver. A source can be enabled or disabled without screen changes. Track records carry the source page, license name/URL, and resolved URI. No provider should silently override missing rights metadata with a claim of free use.

## Data and privacy

Likes, listening history, playlists, source selection, and theme preferences are stored locally in SQLite and SharedPreferences. The app uses the device MediaStore only after the user grants audio permission and does not send local-library data to a server. Search text is sent only to selected remote providers. Queue restoration and configurable cache are follow-up work.

## Playback policy

Remote sources stream only. Downloads and durable audio caching remain off until the specific source and track license permit them. Jamendo's API terms prohibit app-designed offline access, so that adapter does not support downloads. Every resolver must preserve source links and any required attribution.

