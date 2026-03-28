# Plan: Filename-Based Auto Edit (Phase 1)

## Context

Current Auto Edit reads tag metadata (title/artist/album) from editor fields to query MusicBrainz. For untagged files (empty tags), this feature is useless. This change replaces the Auto Edit button to instead **parse the filename** to extract artist/title, query MusicBrainz, and let the user pick a specific **track** from results to apply.

## Implementation Steps

### Step 1: Create FileNameParser utility

**New file:** `app/src/main/java/dev/secam/simpletag/util/FileNameParser.kt`

- `FileNameParseResult(artist: String?, title: String)` data class
- `FileNameParser.parse(fileName: String): FileNameParseResult`
- Parse priorities:
  1. `^\s*(\d{1,3})\s*[.\-_]\s*(.+?)\s*[.\-_]\s*(.+)\s*$` → track number + artist + title
  2. Split on `" - "` (first occurrence) → artist + title
  3. Split on `_` (if only one) → artist + title
  4. Fallback: entire filename as title, artist = null

### Step 2: Add AutoEditTrackItem data class

**Modify:** `app/src/main/java/dev/secam/simpletag/ui/editor/AutoEditState.kt`

Add at bottom:
```kotlin
data class AutoEditTrackItem(
    val release: MusicBrainzRelease,
    val track: MusicBrainzTrack
)
```

### Step 3: Add mapTrackToFieldStates to MusicBrainzMapper

**Modify:** `app/src/main/java/dev/secam/simpletag/data/musicbrainz/MusicBrainzMapper.kt`

New method `mapTrackToFieldStates(release, track)` — same as `mapToFieldStates(release)` but uses the **specified track** instead of `release.tracks.first()`, and additionally sets `MusicBrainzTrackId`.

### Step 4: Extend EditorUiState and EditorViewModel

**Modify:** `app/src/main/java/dev/secam/simpletag/ui/editor/EditorViewModel.kt`

**EditorUiState** — add fields:
- `showFileNameConfirmDialog: Boolean = false`
- `parsedArtist: String? = null`
- `parsedTitle: String = ""`
- `autoEditTrackItems: List<AutoEditTrackItem> = listOf()`

**New ViewModel methods:**
- `parseAndShowConfirmDialog(filePath)` — extract filename, parse, show confirm dialog
- `fetchAutoEditFromFilename(artist, title)` — query MusicBrainz, build sorted track items list
- `applyAutoEditTrack(trackItem)` — map track+release to fields via `mapTrackToFieldStates()`, download cover art
- `setShowFileNameConfirmDialog(show)`

**Private helper:**
- `buildAutoEditTrackItems(releases, searchTitle)` — flatten all tracks from all releases, sort: exact title match first → contains match → rest

**Modify `resetAutoEditState()`** — also clear `autoEditTrackItems`

### Step 5: Create FileNameConfirmDialog

**New file:** `app/src/main/java/dev/secam/simpletag/ui/editor/dialogs/FileNameConfirmDialog.kt`

- Use `SimpleDialog` as container, title = "Auto Edit"
- Artist `OutlinedTextField` (pre-filled, editable)
- Title `OutlinedTextField` (pre-filled, editable)
- Cancel + Search buttons via `SimpleDialogOptions`
- On Search: read text field values, call `onSearch(artist, title)`

Reuse: `SimpleDialog` (`ui/components/SimpleDialog.kt`), `SimpleDialogOptions`

### Step 6: Create AutoEditTrackSheet

**New file:** `app/src/main/java/dev/secam/simpletag/ui/editor/dialogs/AutoEditTrackSheet.kt`

- `ModalBottomSheet` (follow `LyricsEditorSheet` pattern)
- `LazyColumn` of track items, each showing:
  - Cover thumbnail (48dp, `AsyncImage` from `release.coverArtUrl`)
  - Track number + title
  - Artist (track.artist ?? release.artist)
  - Release album + year
- Click item → `onTrackSelected(trackItem)` → dismiss
- Empty state: "No results found" message
- Reuse `AsyncImage` pattern from `AutoEditPreviewDialog.ReleaseCard`

### Step 7: Modify EditorScreen

**Modify:** `app/src/main/java/dev/secam/simpletag/ui/editor/EditorScreen.kt`

**7a.** Add new state reads from `uiState`:
- `showFileNameConfirmDialog`, `parsedArtist`, `parsedTitle`, `autoEditTrackItems`

**7b.** Replace both Auto Edit button onClick (lines ~244 and ~313):
```kotlin
onClick = { viewModel.parseAndShowConfirmDialog(musicList[0].path) }
```

**7c.** Add `FileNameConfirmDialog` rendering (after existing dialog block, ~line 557)

**7d.** In the `showAutoEditDialog` block:
- Keep `Loading` → `AutoEditLoadingDialog` (unchanged)
- **Change** `Success` → `AutoEditTrackSheet(trackItems, onTrackSelected, onDismiss)`
- Keep `Error` → error `SimpleDialog` (unchanged, update retry to go back to confirm dialog)
- **Change** `NoResults` → `AutoEditTrackSheet(emptyList)` or keep dialog with retry → confirm dialog

### Step 8: Add string resources

**Modify:** `app/src/main/res/values/strings.xml`

Add new strings for the filename auto edit UI.

## File Change Summary

| File | Action |
|------|--------|
| `util/FileNameParser.kt` | **NEW** |
| `ui/editor/AutoEditState.kt` | **MODIFY** — add `AutoEditTrackItem` |
| `data/musicbrainz/MusicBrainzMapper.kt` | **MODIFY** — add `mapTrackToFieldStates()` |
| `ui/editor/EditorViewModel.kt` | **MODIFY** — state fields + 4 new methods |
| `ui/editor/dialogs/FileNameConfirmDialog.kt` | **NEW** |
| `ui/editor/dialogs/AutoEditTrackSheet.kt` | **NEW** |
| `ui/editor/EditorScreen.kt` | **MODIFY** — button + dialog wiring |
| `res/values/strings.xml` | **MODIFY** — new strings |

## Verification

1. `./gradlew assembleDebug` — compile check
2. Manual test with file named `Artist - Title.mp3`:
   - Click Auto Edit → confirm dialog shows Artist/Title
   - Click Search → loading → track sheet appears
   - Matching tracks sorted first
   - Select track → editor fields + cover art populated
3. Test with file named `01 - Artist - Title.flac` (track number pattern)
4. Test with file named `TitleOnly.mp3` (fallback: title only)
