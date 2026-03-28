# Phase 2: Batch Auto Edit - Implementation Plan

## Context

Phase 1 (single-file auto-edit from filename) is fully implemented. This plan adds **batch matching functionality** that allows users to automatically match multiple selected files using their filenames and apply MusicBrainz metadata without per-file confirmation.

## Design Decisions

1. **Entry Point**: Add batch auto-edit button to `MultiSelectTopBar` alongside the existing Edit button
2. **Workflow**: Triggered from selector screen, not inside batch editor (alternative workflow)
3. **Progress UI**: Modal dialog with linear progress indicator and real-time status updates
4. **Error Handling**: Continue on failure with summary at end (success/fail/skipped counts)
5. **Track Selection**: Auto-select best match (no per-file confirmation in batch mode)
6. **Query Deduplication**: Group files by folder/album to reduce API calls

## Key Constraints

- **Rate Limiting**: 1 request/second enforced by `MusicBrainzRepository` (Mutex-based)
- **No Batch API**: MusicBrainz doesn't support batch queries
- **Cache**: LRU with 50 entries helps with duplicate queries

---

## Implementation Plan

### Phase 1: Data Models & ViewModel (Core Logic)

#### 1. Create Batch Auto Edit State Models
**New File**: `app/src/main/java/dev/secam/simpletag/ui/editor/BatchAutoEditState.kt`

```kotlin
data class BatchFileResult(
    val musicData: MusicData,
    val status: BatchFileStatus,
    val error: String? = null,
    val appliedFields: Map<SimpleTagField, String>? = null
)

sealed class BatchFileStatus {
    object Success : BatchFileStatus()
    data class Failed(val reason: String) : BatchFileStatus()
    object Skipped : BatchFileStatus()
}

sealed class BatchAutoEditState {
    object Idle : BatchAutoEditState()
    data class Processing(
        val current: Int,
        val total: Int,
        val currentFileName: String
    ) : BatchAutoEditState()
    data class Completed(
        val results: List<BatchFileResult>,
        val successCount: Int,
        val failureCount: Int,
        val skippedCount: Int
    ) : BatchAutoEditState()
    object Cancelled : BatchAutoEditState()
}

data class FileQueryGroup(
    val folder: String,
    val album: String,
    val files: List<MusicData>
)
```

#### 2. Create Batch Auto Edit ViewModel
**New File**: `app/src/main/java/dev/secam/simpletag/ui/editor/BatchAutoEditViewModel.kt`

Key methods:
- `startBatchAutoEdit(files: List<MusicData>)` - Main entry point
- `groupFilesByAlbum(files)` - Group for query deduplication
- `processGroup(group)` - Process a folder/album group
- `mapFileToBestTrack(file, releases)` - Auto-select best matching track
- `processIndividualFile(file)` - Fallback for individual file queries
- `cancelBatch()` - Cancel in-progress operation
- `reset()` - Clear state

Dependencies (inject via Hilt):
- `MusicBrainzRepository`
- `CoverArtRepository`
- `MediaRepo`

#### 3. Add Navigation Route
**Modify**: `app/src/main/java/dev/secam/simpletag/ui/Screens.kt`

Add:
```kotlin
@Serializable
data class BatchAutoEdit(
    val musicList: String
)
```

---

### Phase 2: UI Components

#### 4. Create Batch Auto Edit Dialog
**New File**: `app/src/main/java/dev/secam/simpletag/ui/editor/dialogs/BatchAutoEditDialog.kt`

States:
- **Processing**: LinearProgressIndicator + "Processing X of Y files..." + current filename + Cancel button
- **Completed**: Summary (success/fail/skipped counts) + Cancel/Apply buttons

#### 5. Create Batch Auto Edit Screen
**New File**: `app/src/main/java/dev/secam/simpletag/ui/editor/BatchAutoEditScreen.kt`

- Uses `BatchAutoEditViewModel`
- Shows `SimpleTopBar` with title "Batch Auto Edit"
- Renders `BatchAutoEditDialog` based on state
- `LaunchedEffect` starts processing on screen load
- Handle back navigation with confirmation if processing

---

### Phase 3: Integration

#### 6. Modify MultiSelectTopBar
**Modify**: `app/src/main/java/dev/secam/simpletag/ui/selector/components/MultiSelectTopBar.kt`

Add parameter: `onBatchAutoEdit: () -> Unit`

Add action button:
```kotlin
IconButton(onClick = onBatchAutoEdit) {
    Icon(painterResource(R.drawable.ic_auto_edit_24px), ...)
}
```

#### 7. Modify SelectorScreen
**Modify**: `app/src/main/java/dev/secam/simpletag/ui/selector/SelectorScreen.kt`

Add navigation callback parameter: `onNavigateToBatchAutoEdit: (List<MusicData>) -> Unit`

Pass to `MultiSelectTopBar`:
```kotlin
onBatchAutoEdit = { onNavigateToBatchAutoEdit(selectedItems.toList()) }
```

#### 8. Modify SimpleTagApp Navigation
**Modify**: `app/src/main/java/dev/secam/simpletag/ui/SimpleTagApp.kt`

Add composable route for `BatchAutoEdit`:
```kotlin
composable<BatchAutoEdit>(...) { backStackEntry ->
    val route: BatchAutoEdit = backStackEntry.toRoute()
    val musicList = Json.decodeFromString<List<MusicData>>(route.musicList)
    BatchAutoEditScreen(musicList, onNavigateBack = { navController.navigateUp() })
}
```

Update `SelectorScreen` call:
```kotlin
SelectorScreen(
    onNavigateToSettings = { navController.navigate(Settings) },
    onNavigateToEditor = { musicList -> navController.navigate(Editor(Json.encodeToString(musicList))) },
    onNavigateToBatchAutoEdit = { musicList -> navController.navigate(BatchAutoEdit(Json.encodeToString(musicList))) }
)
```

---

### Phase 4: Utilities & Wiring

#### 9. Enhance FileNameParser
**Modify**: `app/src/main/java/dev/secam/simpletag/util/FileNameParser.kt`

Add function to extract track number (already in regex but not exposed):
```kotlin
fun extractTrackNumber(fileName: String): Int? {
    val nameWithoutExt = fileName.substringBeforeLast(".", "")
    val trackPattern = """^\s*(\d{1,3})\s*[.\-_]\s*""".toRegex()
    return trackPattern.find(nameWithoutExt)?.groupValues?.get(1)?.toIntOrNull()
}
```

#### 10. Add Result Application Function
**Modify**: `app/src/main/java/dev/secam/simpletag/ui/editor/EditorViewModel.kt` OR create utility

Function to apply batch results to actual files:
```kotlin
suspend fun applyBatchResults(
    context: Context,
    results: List<BatchFileResult>,
    onComplete: (success: Int, failed: Int) -> Unit
)
```

#### 11. Add String Resources
**Modify**: `app/src/main/res/values/strings.xml`

```xml
<string name="batch_auto_edit_title">Batch Auto Edit</string>
<string name="cd_batch_auto_edit">Auto edit selected files</string>
<string name="batch_auto_edit_processing">Processing %1$d of %2$d files…</string>
<string name="batch_auto_edit_complete">Batch Auto Edit Complete</string>
<string name="batch_auto_edit_success">Successfully matched</string>
<string name="batch_auto_edit_failed">Failed</string>
<string name="batch_auto_edit_skipped">Skipped</string>
<string name="batch_auto_edit_apply">Apply Changes</string>
<string name="batch_auto_edit_cancel_confirm">Cancel batch auto-edit?</string>
<string name="batch_auto_edit_cancel_message">Changes will not be saved.</string>
```

---

### Phase 5: Dependency Injection

#### 12. Update AppModule
**Modify**: `app/src/main/java/dev/secam/simpletag/di/AppModule.kt`

Add ViewModel provider:
```kotlin
@Provides
@Singleton
fun provideBatchAutoEditViewModel(
    musicBrainzRepo: MusicBrainzRepository,
    coverArtRepo: CoverArtRepository,
    mediaRepo: MediaRepo
): BatchAutoEditViewModel = BatchAutoEditViewModel(musicBrainzRepo, coverArtRepo, mediaRepo)
```

---

## Verification

### Test Scenarios

1. **Success Case**: Select 5 files from same album → 1 query, all matched
2. **Mixed Albums**: 5 files from album A, 5 from album B → 2 queries
3. **Parse Failure**: File with unparseable name (e.g., "abc123.mp3") → skipped
4. **No Results**: File with no MusicBrainz matches → failed
5. **Network Error**: Offline during query → failed with error message
6. **Cancellation**: Cancel during processing → stops immediately
7. **Large Batch**: 20+ files → progress shows 1-20, takes ~20 seconds

### Manual Testing Steps

1. Select multiple files in SelectorScreen
2. Click batch auto-edit button (ic_auto_edit_24px)
3. Verify progress dialog shows correct file count
4. Wait for completion
5. Verify summary shows correct counts
6. Click "Apply Changes"
7. Verify files were updated correctly
8. Run `./gradlew assembleDebug` to confirm compilation

---

## Critical Files Reference

| File | Operation |
|------|-----------|
| `ui/editor/BatchAutoEditState.kt` | **New** — Data models |
| `ui/editor/BatchAutoEditViewModel.kt` | **New** — Core logic |
| `ui/editor/dialogs/BatchAutoEditDialog.kt` | **New** — Progress/results UI |
| `ui/editor/BatchAutoEditScreen.kt` | **New** — Main screen |
| `ui/selector/components/MultiSelectTopBar.kt` | **Modify** — Add button |
| `ui/selector/SelectorScreen.kt` | **Modify** — Wire navigation |
| `ui/Screens.kt` | **Modify** — Add route |
| `ui/SimpleTagApp.kt` | **Modify** — Add composable |
| `util/FileNameParser.kt` | **Modify** — Add extractTrackNumber |
| `di/AppModule.kt` | **Modify** — Add ViewModel provider |
| `res/values/strings.xml` | **Modify** — Add strings |

## Existing Components to Reuse

- `FileNameParser.parse()` — Filename parsing logic
- `MusicBrainzMapper.mapTrackToFieldStates()` — Map track+release to fields
- `MusicBrainzRepository.searchReleases()` — Query with rate limiting
- `CoverArtRepository` — Cover art downloading
- `SimpleDialog` — For confirmation dialogs
- `ic_auto_edit_24px.xml` — Icon already exists
- `SelectorUiState.selectedItems` — Selected files set
