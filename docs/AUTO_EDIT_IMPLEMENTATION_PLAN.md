# Auto-Edit Feature Implementation Plan

## Context

Implement an "Auto-Edit" feature that fetches missing music metadata from MusicBrainz API and allows users to preview and apply the changes. This addresses the need to automatically populate tag information when editing audio files in the SimpleTag Android app.

**Requirements from autoeditprd.md:**
- Add "Auto Edit" button in the edit tag page
- Query MusicBrainz API to fetch tag data
- Preview fetched tag data
- Confirm and update tags to audio files

---

## Architecture Overview

### New Components
```
data/musicbrainz/
├── MusicBrainzApiService.kt      # Retrofit API interface
├── MusicBrainzRepository.kt      # Repository with caching & rate limiting
├── MusicBrainzMapper.kt          # Maps API responses to SimpleTagField
└── models/
    ├── MusicBrainzRelease.kt
    ├── MusicBrainzTrack.kt
    └── MusicBrainzArtist.kt

ui/editor/dialogs/
├── AutoEditLoadingDialog.kt      # Loading indicator
└── AutoEditPreviewDialog.kt      # Results preview & selection
```

### Technology Stack
- **HTTP Client:** Retrofit 2.x + OkHttp 4.x
- **Serialization:** XML (MusicBrainz native format)
- **Caching:** LruCache (in-memory, max 50 entries)
- **Rate Limiting:** 1 request/second (enforced in repository)

---

## Implementation Phases

### Phase 1: Foundation (Dependencies & API Client)

**Goal:** Set up infrastructure for MusicBrainz API integration

**Tasks:**

1. **Add Dependencies**
   - Update `gradle/libs.versions.toml` with Retrofit and OkHttp versions
   - Modify `app/build.gradle.kts` to include:
     - `com.squareup.retrofit2:retrofit`
     - `com.squareup.retrofit2:converter-scalars`
     - `com.squareup.okhttp3:okhttp`
     - `com.squareup.okhttp3:logging-interceptor`

2. **Add Permissions**
   - Add `<uses-permission android:name="android.permission.INTERNET"/>` to `AndroidManifest.xml`
   - Add `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>`
   - Create `res/xml/network_security_config.xml` for HTTPS-only policy

3. **Create API Service**
   - Create `data/musicbrainz/MusicBrainzApiService.kt` with Retrofit interface:
     ```kotlin
     @GET("release/")
     @Headers("User-Agent: SimpleTag-Android/0.3.1 ( https://github.com/yourusername/SimpleTag )")
     suspend fun searchReleases(
         @Query("query") query: String,
         @Query("limit") limit: Int = 10,
         @Query("fmt") format: String = "xml"
     ): ResponseBody
     ```

4. **Create Repository**
   - Create `data/musicbrainz/MusicBrainzRepository.kt`:
     - Implement rate limiting (1 req/sec)
     - Add LruCache for query results
     - Handle errors (network, timeout, parse errors)
     - Return sealed class: `Success`, `Error`, `NoResults`

5. **Create Data Models**
   - Create `models/MusicBrainzRelease.kt`, `MusicBrainzTrack.kt`, `MusicBrainzArtist.kt`
   - Parse XML responses from MusicBrainz API

6. **Dependency Injection**
   - Update `di/AppModule.kt` to provide:
     - `MusicBrainzApiService` (singleton)
     - `MusicBrainzRepository` (singleton)

**Verification:**
- Unit test repository with mock API
- Verify rate limiting works
- Test error handling

**Files Modified/Created:**
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/network_security_config.xml`
- `app/src/main/java/dev/secam/simpletag/data/musicbrainz/MusicBrainzApiService.kt`
- `app/src/main/java/dev/secam/simpletag/data/musicbrainz/MusicBrainzRepository.kt`
- `app/src/main/java/dev/secam/simpletag/data/musicbrainz/models/*.kt`
- `app/src/main/java/dev/secam/simpletag/di/AppModule.kt`

---

### Phase 2: ViewModel Integration

**Goal:** Connect API to EditorScreen through ViewModel

**Tasks:**

1. **Extend ViewModel State**
   - Modify `EditorUiState` in `EditorViewModel.kt`:
     ```kotlin
     val autoEditState: AutoEditState = AutoEditState.Idle
     val autoEditResults: List<MusicBrainzRelease> = listOf()
     val selectedAutoEditResult: MusicBrainzRelease? = null
     val showAutoEditDialog: Boolean = false
     ```

2. **Create State Sealed Class**
   ```kotlin
   sealed class AutoEditState {
       object Idle : AutoEditState()
       object Loading : AutoEditState()
       data class Success(val results: List<MusicBrainzRelease>) : AutoEditState()
       data class Error(val message: String) : AutoEditState()
       object NoResults : AutoEditState()
   }
   ```

3. **Implement ViewModel Methods**
   - `fetchAutoEditData(queryParams: AutoEditQueryParams)` - Calls repository
   - `selectAutoEditResult(release: MusicBrainzRelease)` - Updates selected result
   - `applyAutoEditData(release: MusicBrainzRelease)` - Applies tags to fieldStates
   - `setShowAutoEditDialog(show: Boolean)` - Controls dialog visibility

4. **Create Mapper**
   - Create `data/musicbrainz/MusicBrainzMapper.kt`:
     - `mapToFieldStates(release: MusicBrainzRelease): Map<SimpleTagField, String>`
     - Map MusicBrainz fields to SimpleTagField enum values
     - Handle all 89 fields (title, artist, album, year, track, genre, etc.)

5. **Query Parameter Builder**
   - Create `AutoEditQueryParams` data class
   - Build query from existing MusicData fields (title, artist, album, track)
   - Use fallback to filename if title is missing

**Verification:**
- Unit test ViewModel state transitions
- Test mapper with various API responses
- Verify error handling in ViewModel

**Files Modified/Created:**
- `app/src/main/java/dev/secam/simpletag/ui/editor/EditorViewModel.kt`
- `app/src/main/java/dev/secam/simpletag/data/musicbrainz/MusicBrainzMapper.kt`

---

### Phase 3: UI Implementation

**Goal:** Create user interface for auto-edit feature

**Tasks:**

1. **Add Auto-Edit Button**
   - Create drawable `ic_auto_edit_24px.xml` (search + edit icon)
   - Modify `EditorScreen.kt` FAB toolbar (advanced mode):
     - Add IconButton between "Lyrics" and "Add Field"
     - Only visible when `musicList.size == 1`
     - Click handler: `viewModel.fetchAutoEditData()`
   - For basic mode: Add as secondary FAB or menu item

2. **Create Loading Dialog**
   - Create `ui/editor/dialogs/AutoEditLoadingDialog.kt`:
     - Circular progress indicator
     - Title: "Fetching metadata from MusicBrainz…"
     - Cancel button
     - Timeout after 30 seconds

3. **Create Preview Dialog**
   - Create `ui/editor/dialogs/AutoEditPreviewDialog.kt`:
     - Title: "Select Matching Release"
     - LazyColumn with search results (max 10)
     - Each result shows:
       - Album artwork (from Cover Art Archive API)
       - Album title
       - Artist name
       - Year
       - Track count
       - Country + Label
     - Radio button for selection
     - Buttons: "Apply", "Cancel", "Search Again"

4. **Handle States in EditorScreen**
   - Show loading dialog when `autoEditState == Loading`
   - Show preview dialog when `autoEditState == Success`
   - Show error dialog when `autoEditState == Error`
   - Show "No results" message when `autoEditState == NoResults`

5. **Add String Resources**
   - Add all UI strings to `res/values/strings.xml`:
     - `auto_edit`, `cd_auto_edit`
     - `auto_edit_loading`, `auto_edit_loading_title`
     - `auto_edit_no_results`, `auto_edit_no_results_message`
     - `auto_edit_error`, `auto_edit_network_error`, `auto_edit_timeout`
     - `auto_edit_select_release`, `auto_edit_apply`, `auto_edit_search_again`

**Verification:**
- Manual testing on emulator/device
- Test button visibility in simple/advanced modes
- Test dialog behaviors
- Test error scenarios (airplane mode, timeout)

**Files Modified/Created:**
- `app/src/main/res/drawable/ic_auto_edit_24px.xml`
- `app/src/main/java/dev/secam/simpletag/ui/editor/EditorScreen.kt`
- `app/src/main/java/dev/secam/simpletag/ui/editor/dialogs/AutoEditLoadingDialog.kt`
- `app/src/main/java/dev/secam/simpletag/ui/editor/dialogs/AutoEditPreviewDialog.kt`
- `app/src/main/res/values/strings.xml`

---

### Phase 4: Cover Art Integration

**Goal:** Download and display album art from Cover Art Archive

**Tasks:**

1. **Cover Art Repository**
   - Create `data/musicbrainz/CoverArtRepository.kt`:
     - Fetch from `https://coverartarchive.org/release/{mbid}/front`
     - Download to app cache directory
     - Return as `Artwork` object

2. **Display in Preview Dialog**
   - Load images using Coil (already configured)
   - Show placeholder if no art available
   - Handle loading/error states

3. **Apply Artwork**
   - When user applies metadata, download cover art
   - Use existing `setArtworkField()` functions in `util/tag/SetArtworkField.kt`
   - Handle different formats (FLAC, MP3, OGG, etc.)

**Verification:**
- Test cover art download with various releases
- Test missing art fallback
- Verify artwork is written correctly to files

**Files Modified/Created:**
- `app/src/main/java/dev/secam/simpletag/data/musicbrainz/CoverArtRepository.kt`
- `app/src/main/java/dev/secam/simpletag/ui/editor/dialogs/AutoEditPreviewDialog.kt`

---

### Phase 5: Testing & Polish

**Goal:** Ensure quality and prepare for release

**Tasks:**

1. **Unit Testing**
   - Repository tests (mock API responses)
   - Mapper tests (various API responses)
   - ViewModel tests (state transitions)
   - Aim for >80% coverage

2. **Integration Testing**
   - Test real API calls (respect rate limits)
   - Test tag writing with fetched data
   - Test cache behavior

3. **UI Testing**
   - Test all dialog states
   - Test screen rotation
   - Test accessibility (TalkBack)

4. **Edge Cases**
   - No internet connection
   - API timeout
   - Empty/missing fields in response
   - Multiple search results
   - Malformed XML

5. **Documentation**
   - Update README.md with auto-edit feature
   - Add inline code comments
   - Create user guide section

**Verification:**
- All tests passing
- No memory leaks (LeakCanary)
- Smooth UI performance
- Documentation complete

---

## Critical Files Reference

**Files to Modify:**
1. `/Users/liudong320/workspace/SimpleTag/app/build.gradle.kts` - Add Retrofit/OkHttp dependencies
2. `/Users/liudong320/workspace/SimpleTag/app/src/main/AndroidManifest.xml` - Add INTERNET permission
3. `/Users/liudong320/workspace/SimpleTag/app/src/main/java/dev/secam/simpletag/ui/editor/EditorViewModel.kt` - Add auto-edit state & methods
4. `/Users/liudong320/workspace/SimpleTag/app/src/main/java/dev/secam/simpletag/ui/editor/EditorScreen.kt` - Add button & dialogs
5. `/Users/liudong320/workspace/SimpleTag/app/src/main/java/dev/secam/simpletag/di/AppModule.kt` - Provide MusicBrainz dependencies

**Files to Create:**
1. `data/musicbrainz/MusicBrainzApiService.kt` - Retrofit API interface
2. `data/musicbrainz/MusicBrainzRepository.kt` - Repository with caching
3. `data/musicbrainz/MusicBrainzMapper.kt` - Maps API to SimpleTagField
4. `data/musicbrainz/models/*.kt` - Data transfer objects
5. `ui/editor/dialogs/AutoEditLoadingDialog.kt` - Loading indicator
6. `ui/editor/dialogs/AutoEditPreviewDialog.kt` - Results preview
7. `res/drawable/ic_auto_edit_24px.xml` - Button icon

---

## Verification Plan

### End-to-End Testing
1. Open SimpleTag app
2. Select a music file with incomplete tags
3. Open EditorScreen (advanced mode)
4. Tap "Auto Edit" button
5. Verify loading dialog appears
6. Verify results appear in preview dialog
7. Select a result
8. Tap "Apply"
9. Verify fields are updated in editor
10. Save changes
11. Verify tags are written to file

### Error Scenario Testing
- Test with airplane mode enabled
- Test with slow network (throttle)
- Test with malformed API response
- Test with no search results
- Test with multiple results

---

## Dependencies

**Gradle (libs.versions.toml):**
```toml
[versions]
retrofit = "2.11.0"
okhttp = "4.12.0"

[libraries]
retrofit-core = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-converter-scalars = { module = "com.squareup.retrofit2:converter-scalars", version.ref = "retrofit" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging-interceptor = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
```

**Manifest:**
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

---

## API Integration Notes

**MusicBrainz API:**
- Base URL: `https://musicbrainz.org/ws/2/`
- Required: User-Agent header with contact info
- Rate limit: 1 request/second
- Format: XML (primary), JSON (fallback)

**Cover Art Archive:**
- Base URL: `https://coverartarchive.org/release/{mbid}/front`
- No API key required
- Public domain images

---

## Timeline Estimate

- Phase 1: 3-5 days (dependencies, API client, repository)
- Phase 2: 3-4 days (ViewModel, mapper)
- Phase 3: 4-5 days (UI components, dialogs)
- Phase 4: 2-3 days (cover art integration)
- Phase 5: 3-4 days (testing, polish)

**Total:** ~15-21 days

---

## Existing Patterns to Reuse

1. **Repository Pattern:** Follow `MediaRepo.kt` singleton pattern
2. **State Management:** Use `StateFlow` like existing ViewModels
3. **Dialog System:** Follow existing dialog patterns in `ui/editor/dialogs/`
4. **Coil Integration:** Use existing `MusicDataKeyer` for image loading
5. **Tag Writing:** Use existing `simpleFileWriter()` and format-specific writers
