# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SimpleTag is an Android audio tag editor built with Jetpack Compose and Material You. It uses jaudiotagger (embedded directly in `app/src/main/java/org/jaudiotagger/`) for reading/writing audio metadata across multiple formats (MP3, FLAC, OGG, MP4, WAV, DSF, WMA, Opus, etc.).

**Package:** `dev.secam.simpletag`
**Min SDK:** 26, **Target SDK:** 36
**License:** GPL-3.0

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (outputs to app/build/outputs/apk/release/)
./gradlew assembleRelease

# Install debug build to connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Lint
./gradlew lint
```

Build outputs are named `SimpleTag_{versionName}.apk` for both debug and release variants.

## Architecture

### UI Layer (`ui/`)
- **Navigation:** Type-safe navigation using Kotlinx Serialization. Routes defined in `Screens.kt` as serializable data classes.
- **Three main screens:**
  - `SelectorScreen` - File browser and music selection
  - `EditorScreen` - Tag editing interface
  - `SettingsScreen` - App preferences
- **Components:** Reusable Compose UI components in `ui/components/`
- **Theme:** Material 3 theming with dynamic color support in `ui/theme/`

### Data Layer (`data/`)
- **`MediaRepo`** - Central repository for audio file operations using MediaStore API
  - Manages music loading with folder filtering (include/exclude modes)
  - Uses coroutines for parallel file reading
  - Maintains `musicMapState: MutableStateFlow<Map<Long, MusicData>>` as the source of truth
- **`MusicData`** - Serializable data class representing audio file metadata
- **`PreferencesRepo`** - DataStore-based preferences persistence
- **`coil/`** - Custom Coil image loader for album art extraction from audio files

### Dependency Injection (`di/`)
- Hilt-based DI with singleton-scoped repositories
- `AppModule` provides `MediaRepo` and `PreferencesRepo`

### Utilities (`util/tag/`)
- **`SimpleFileIO.kt`** - Wrapper around jaudiotagger with format-specific readers/writers
  - Special handling for OGG and Opus files (works around temp file permission issues)
  - `simpleFileReader()` - Returns `AudioFile?` (null on read failure)
  - `oggFileWriter()` / `opusFileWriter()` - For OGG/Opus tag writing

### jaudiotagger Integration
The jaudiotagger library is embedded directly in the source tree at `app/src/main/java/org/jaudiotagger/`. This is a Java library for audio metadata handling.

## Key Patterns

### Navigation with Complex Data
Lists of `MusicData` are passed between screens by serializing to JSON strings via `Json.encodeToString(musicList)` and deserializing with `Json.decodeFromString()` in the destination screen. This is necessary because navigation arguments must be serializable.

### File Reading Pattern
Always use `simpleFileReader(path)` from `util/tag/SimpleFileIO.kt` instead of `AudioFileIO.read()` directly. It handles format-specific readers (AAC, Opus) and returns null on failure instead of throwing.

### Tag Writing Pattern
```kotlin
val audioFile = simpleFileReader(path) ?: return
audioFile.tag.setField(FieldKey.TITLE, "New Title")
when {
    path.endsWith("ogg", true) || path.endsWith("opus", true) ->
        oggFileWriter(audioFile, context)
    else ->
        simpleFileWriter(audioFile)
}
```

### Coil Image Loading
Album art is extracted from audio files via the custom `MusicDataKeyer` in `data/coil/`. The image loader is configured at the Application level (`SimpleTag.kt` implements `SingletonImageLoader.Factory`).

### State Management
- UI state flows from repositories via `StateFlow`/`MutableStateFlow`
- `MediaRepo.musicMapState` is the primary source of truth for loaded music
- Preferences flow through `PreferencesRepo.preferencesFlow`

## File Organization

```
app/src/main/java/dev/secam/simpletag/
├── SimpleTag.kt          # Application class with Coil setup
├── ui/
│   ├── MainActivity.kt   # Entry point, sets theme and content
│   ├── SimpleTagApp.kt   # NavHost with screen routing
│   ├── Screens.kt        # Type-safe navigation routes
│   ├── selector/         # File selection UI
│   ├── editor/           # Tag editing UI
│   ├── settings/         # Settings UI
│   ├── components/       # Reusable UI components
│   └── theme/            # Material 3 theming
├── data/
│   ├── media/
│   │   ├── MediaRepo.kt  # Audio file loading and management
│   │   └── MusicData.kt  # Audio file metadata model
│   ├── preferences/      # DataStore preferences
│   ├── enums/            # App-wide enums (SimpleTagField, SortOrder, etc.)
│   └── coil/             # Album art image loading
├── di/
│   └── AppModule.kt      # Hilt dependency injection
└── util/
    └── tag/              # jaudiotagger wrappers (SimpleFileIO.kt, SetArtworkField.kt)
```

## Supported Audio Formats

Defined in `MediaRepo.kt` as `COMPATIBLE_TYPES`:
- MP3, WAV, DSF, AIFF, WMA, OGG, MP4/M4A/M4P, FLAC, AAC, Opus

## Permissions

Storage permissions are handled differently across Android versions:
- API 29 and below: `WRITE_EXTERNAL_STORAGE`
- API 30-32: `READ_EXTERNAL_STORAGE`
- API 33+: `READ_MEDIA_AUDIO`
- Optional (API 31+): `MANAGE_MEDIA` + `ACCESS_MEDIA_LOCATION` for write without prompts
