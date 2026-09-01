<div align="center">

# AnilibrixPlus

### Native Android client for streaming, torrent management, and catalog browsing

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-13%2B%20(API%2033%2B)-3DDC84.svg?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-E53935.svg?style=flat-square&logo=google&logoColor=white)](https://developer.android.com/media/media3)
[![Room](https://img.shields.io/badge/Room-Database%20v6-2E7D32.svg?style=flat-square)](https://developer.android.com/training/data-storage/room)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

<p align="center">
  <a href="#features">Features</a> •
  <a href="#media-streaming--voiceover-sources">Media Sources</a> •
  <a href="#torrent-engine">Torrent Engine</a> •
  <a href="#architecture">Architecture</a> •
  <a href="#building-from-source">Building</a> •
  <a href="#api-integrations">APIs</a>
</p>

</div>

---

## Overview

**AnilibrixPlus** is a high-performance, native Android application engineered for browsing anime catalogs, streaming episodes from multiple providers, and managing torrent downloads directly within the app. Built on modern Android architecture principles using Jetpack Compose, Kotlin Coroutines/Flow, Media3 (ExoPlayer), and Clean Architecture.

---

## Features

### UI and UX
- **Material Design 3**: Strict compliance with Material You guidelines, including dynamic theming, smooth transitions, and refined elevation hierarchies.
- **Catalog Navigation**: Comprehensive search and multi-parameter filtering across genres, release types, seasons, and release years.
- **Release Schedule**: Weekly calendar tracking ongoing anime simulcasts and release days.

### Media Streaming and Voiceover Routing
- **Multi-Provider Playback**:
  - **AniLibria**: Official primary stream source with HLS multi-bitrate delivery (480p, 720p, 1080p).
  - **Kodik API**: Integrated secondary provider indexing external Russian dubbing studios and localized subtitle tracks.
  - **Consumet API (Gogoanime)**: International fallback and stream provider for English Dub and Subtitle releases.
- **Voiceover Preferences**:
  - Global preference fallback configurable in application settings.
  - Granular per-title voiceover selection persisted in local database.
  - Dynamic stream resolution and episode list updates upon switching providers.

### Torrent Engine and Offline Storage
- **Native Torrent Service**: Standalone in-app background download manager implemented via `ForegroundService` with notification channel progress tracking.
- **Smart Release Parser (`TorrentNameParser`)**:
  - Parses unstructured torrent naming schemas from **Nyaa.si** and **AniLibria**.
  - Extracts release groups (e.g., *SubsPlease*, *Erai-raws*, *Judas*, *EMBER*, *AniLibria*), video codecs (*HEVC x265 10-bit*, *AV1*, *x264*), resolutions (*1080p*, *720p*, *4K*), and audio configurations (*Dual-Audio*, *Multi-Sub*).
- **Filtering & Episode Range Selection**:
  - In-memory real-time keyword search.
  - Horizontal episode chips enabling one-touch filtering for specific episodes or full season batch packs.
  - BEncode metadata inspection for selective episode file extraction.

### Video Player (Media3 / ExoPlayer)
- **AniSkip Integration**: Automated and interactive skipping for anime openings and endings based on crowd-sourced timestamp APIs.
- **Audio & Subtitle Management**: Embedded track selection alongside external `.srt` / `.vtt` file parser integration.
- **Gesture Controls**: Touch-based brightness control, volume adjustment, and double-tap seeking.
- **Background Playback & PiP**: Full Picture-in-Picture support and `MediaSessionService` system integration with lock screen transport controls.

### Library and Cloud Synchronization
- **Shikimori**: Full OAuth 2.0 authentication, profile synchronization, status tracking (*Watching*, *Completed*, *Planned*), and rating sync.
- **MyAnimeList / Jikan**: Secondary metadata lookup for character lists, voice cast, screenshots, and relations.

---

## Architecture

The project adheres to Clean Architecture layers and the MVI (Model-View-Intent) pattern with unidirectional data flow (UDF):

```
app/src/main/java/com/anilibrix/plus/
├── app/
│   ├── di/                       # Dagger-Hilt Dependency Injection modules
│   └── MainActivity.kt           # Single-activity container
├── core/
│   ├── database/                 # Room DB v6 (Entities, DAOs, Migrations)
│   ├── datastore/                # DataStore Preferences (Settings, Token storage)
│   ├── download/                 # Media3 DownloadManager integration
│   ├── network/                  # OkHttp interceptors (Auth, Logging, Error handling)
│   ├── notifications/            # Notification channel management
│   ├── playback/                 # MediaSessionService implementation
│   ├── torrent/                  # Torrent engine, BEncode parser, TorrentNameParser
│   └── sync/                     # WorkManager background sync workers
├── data/
│   ├── remote/api/               # Retrofit API definitions (AniLibria, Kodik, Consumet, Shikimori, Jikan)
│   ├── remote/dto/               # Kotlinx.Serialization Data Transfer Objects
│   └── repository/               # Concrete repository implementations
├── domain/
│   ├── model/                    # Pure domain models
│   └── repository/               # Domain repository interfaces
└── ui/
    ├── components/               # Reusable UI primitives
    ├── detail/                   # Title details, episode list, torrents tab
    ├── downloads/                # Download manager and active torrent queue
    ├── player/                   # Video player screen and controllers
    ├── profile/                  # User profile and application settings
    ├── theme/                    # Color schemes, typography, spacing, shapes
    └── navigation/               # NavHost and route transition specs
```

### Technical Stack

| Component | Library / Framework | Purpose |
|---|---|---|
| **Language** | Kotlin 2.0+ | Modern type-safe programming language |
| **UI Framework** | Jetpack Compose / Material 3 | Declarative UI layer with Material You support |
| **Dependency Injection** | Dagger Hilt 2.51+ | Compile-time dependency injection |
| **Concurrency** | Kotlin Coroutines & Flow | Reactive asynchronous operations and state management |
| **Networking** | Retrofit 2 & OkHttp 3 | REST API client and network layer |
| **Serialization** | Kotlinx Serialization | Type-safe JSON serialization/deserialization |
| **Local Database** | Room 2.6+ (Schema v6) | Offline storage for cache, history, and torrent metadata |
| **Preferences** | Jetpack DataStore | DataStore Preferences for reactive settings storage |
| **Media Player** | AndroidX Media3 (ExoPlayer) | Media playback, HLS streaming, caching, and background playback |
| **Image Loading** | Landscapist Glide | Memory-efficient image pipeline with crossfade caching |
| **Background Tasks** | AndroidX WorkManager | Persistent background synchronization |

---

## Building from Source

### Prerequisites
- **Android SDK**: API 35 (Android 15)
- **Minimum Supported OS**: Android 13.0 (API 33)
- **JDK**: OpenJDK 21 (with `jlink` utility available)
- **Gradle**: 8.9+ / Android Gradle Plugin 8.7+

### Build Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/lonxzsy/anilibrix-plus-mobile.git
   cd anilibrix-plus-mobile
   ```

2. **Assemble Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The generated artifact will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

3. **Assemble Release APK:**
   ```bash
   ./gradlew assembleRelease
   ```

4. **Run Unit Tests:**
   ```bash
   ./gradlew test
   ```

---

## API Integrations

- **AniLibria API v3**: Primary catalog indexing, episode metadata, and HLS streaming endpoints.
- **Kodik API**: Multi-studio Russian voiceover tracks, subtitles, and video stream metadata.
- **Consumet API**: International English Dub and Sub anime streams via Gogoanime provider.
- **Shikimori API v2**: User authentication, list tracking, and progress synchronization.
- **Jikan API (Unofficial MyAnimeList)**: Extended title metadata, character profiles, voice actors, and screenshots.
- **AniSkip API**: Timestamp resolution for opening and ending sequences.
- **Nyaa.si**: Torrent index querying and metadata fetching.

---

## License

This project is licensed under the terms of the **MIT License**. See the [LICENSE](LICENSE) file for details.

---

<div align="center">
  <sub>Disclaimer: AnilibrixPlus is an independent, non-commercial client application and is not officially affiliated with AniLibria.</sub>
</div>
