# Phase 3: Batch Auto Edit - UX Improvements

## Context

Phase 2 (batch auto-edit from filename) is fully implemented and working. Users can select multiple files, automatically match them using MusicBrainz, and apply metadata. However, the current UX has limitations:

1. **No preview before apply** - Users can't see what will change until after files are modified
2. **Limited error recovery** - Failed files show count but no retry option
3. **Basic progress feedback** - Shows filename but not what operation is happening

This plan adds **UX improvements** to make batch operations more transparent, controllable, and recoverable.

## User Feedback

Current issues reported:
- Users anxious about applying changes without seeing preview
- Failed files require starting entire batch over
- Progress indicator doesn't explain what's happening ("Searching..." vs "Applying...")

---

## Design Decisions

### 1. Preview/Review Screen: Full-Screen Composable
**Decision:** Full-screen composable (not dialog)

**Rationale:**
- Lists can have 50-100 files
- Better UX: Users can see more context, scroll freely
- Pattern consistency: Matches `BatchEditor.kt` pattern
- Dialog limitations: `AutoEditPreviewDialog` uses 400dp height which is insufficient for 50+ items

### 2. Selection Mechanism: Checkbox-Based
**Decision:** All successful results pre-selected by default, checkboxes for multi-select

**Specifications:**
- Successful results: pre-selected, user can deselect
- Failed/skipped files: shown but greyed out, not selectable
- Use `Checkbox` composable with `animateColorAsState` for visual feedback
- Selection state tracked at screen level with `remember { mutableStateOf<Set<Long>>() }`

### 3. State Management: New Reviewing State
**Decision:** Add `Reviewing` state to `BatchAutoEditState`

```kotlin
data class Reviewing(
    val results: List<BatchFileResult>,
    val selectedIds: Set<Long> = emptySet()
) : BatchAutoEditState()
```

**Rationale:**
- Maintains single source of truth in ViewModel
- Allows state restoration on configuration change
- Follows existing pattern of state-driven UI

### 4. Error Display: Inline with Expandable Details
**Approach:**
- Failed files show error message inline (red text)
- "View Details" button shows full error in dialog
- Failed files grouped at top of list with sticky header
- Visual distinction: Red icon + error message

### 5. Field Preview: Key Fields with Expand Option
**Strategy:**
- Default: Show Title, Artist, Album (3 key fields)
- "View More" expands to show all changed fields (15+ possible)
- Highlight changed fields with color (green for new values)

---

## Implementation Plan

### Phase 1: Foundation (P0 - Core Preview)

#### 1. Update State Models
**Modify:** `app/src/main/java/dev/secam/simpletag/ui/editor/BatchAutoEditState.kt`

Add new state and enum:
```kotlin
sealed class BatchAutoEditState {
    object Idle : BatchAutoEditState()
    data class Processing(
        val current: Int,
        val total: Int,
        val currentFileName: String,
        val currentOperation: ProcessingOperation = ProcessingOperation.Searching
    ) : BatchAutoEditState()
    data class Reviewing(
        val results: List<BatchFileResult>,
        val selectedIds: Set<Long> = emptySet()
    ) : BatchAutoEditState()
    data class Completed(
        val results: List<BatchFileResult>,
        val successCount: Int,
        val failureCount: Int,
        val skippedCount: Int
    ) : BatchAutoEditState()
    object Cancelled : BatchAutoEditState()
}

enum class ProcessingOperation {
    Searching,  // "Searching MusicBrainz..."
    Applying,   // "Applying metadata..."
    Done        // "Complete"
}
```

#### 2. Create File Item Component
**New:** `app/src/main/java/dev/secam/simpletag/ui/editor/components/BatchReviewFileItem.kt`

```kotlin
@Composable
fun BatchReviewFileItem(
    result: BatchFileResult,
    isSelected: Boolean,
    isSelectable: Boolean,
    onSelectionToggle: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
)
```

Features:
- Album artwork (48dp) using `SimpleAlbumArtwork` pattern
- File name and current metadata
- Changed fields preview (Title, Artist, Album)
- Checkbox for selection (if selectable)
- Error message inline (if failed)
- "View Details" button for field expansion
- Selection animation with `animateColorAsState` (reuse `SimpleMusicItem` pattern)

#### 3. Create Review Screen
**New:** `app/src/main/java/dev/secam/simpletag/ui/editor/BatchReviewScreen.kt`

```kotlin
@Composable
fun BatchReviewScreen(
    results: List<BatchFileResult>,
    selectedIds: Set<Long>,
    onSelectionToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onApplySelected: () -> Unit,
    onApplyAll: () -> Unit,
    onCancel: () -> Unit,
    onRetryFailed: () -> Unit
)
```

Key components:
- `LazyColumn` with sticky headers for Failed/Success sections
- `BatchReviewFileItem` for each file
- Top bar with action buttons
- Selection counter (e.g., "15 of 20 files selected")

**Reference pattern:** `AutoEditPreviewDialog.kt` lines 98-111 (LazyColumn with items)

#### 4. Update ViewModel
**Modify:** `app/src/main/java/dev/secam/simpletag/ui/editor/BatchAutoEditViewModel.kt`

Add functions:
```kotlin
fun enterReviewState(results: List<BatchFileResult>) {
    val successIds = results
        .filter { it.status is BatchFileStatus.Success }
        .map { it.musicData.id }
        .toSet()
    _uiState.update { BatchAutoEditState.Reviewing(results, successIds) }
}

fun toggleSelection(fileId: Long) {
    val currentState = _uiState.value
    if (currentState is BatchAutoEditState.Reviewing) {
        val newSelected = if (currentState.selectedIds.contains(fileId)) {
            currentState.selectedIds - fileId
        } else {
            currentState.selectedIds + fileId
        }
        _uiState.update { currentState.copy(selectedIds = newSelected) }
    }
}

fun selectAll() {
    val currentState = _uiState.value
    if (currentState is BatchAutoEditState.Reviewing) {
        val successIds = currentState.results
            .filter { it.status is BatchFileStatus.Success }
            .map { it.musicData.id }
            .toSet()
        _uiState.update { currentState.copy(selectedIds = successIds) }
    }
}

fun deselectAll() {
    val currentState = _uiState.value
    if (currentState is BatchAutoEditState.Reviewing) {
        _uiState.update { currentState.copy(selectedIds = emptySet()) }
    }
}
```

#### 5. Update Main Screen
**Modify:** `app/src/main/java/dev/secam/simpletag/ui/editor/BatchAutoEditScreen.kt`

Add handling for `BatchAutoEditState.Reviewing`:
```kotlin
when (val state = uiState) {
    is BatchAutoEditState.Processing -> {
        BatchAutoEditDialog(state = state, ...)
    }
    is BatchAutoEditState.Reviewing -> {
        BatchReviewScreen(
            results = state.results,
            selectedIds = state.selectedIds,
            onSelectionToggle = { viewModel.toggleSelection(it) },
            onSelectAll = { viewModel.selectAll() },
            onDeselectAll = { viewModel.deselectAll() },
            onApplySelected = {
                val selectedResults = state.results.filter { it.musicData.id in state.selectedIds }
                doApply(selectedResults)
            },
            onApplyAll = { doApply(state.results.filter { it.status is BatchFileStatus.Success }) },
            onCancel = { onNavigateBack() },
            onRetryFailed = { viewModel.retryFailed() }
        )
    }
    // ... other states
}
```

Change flow: Processing → Reviewing → Completed (after apply)

#### 6. Add String Resources
**Modify:** `app/src/main/res/values/strings.xml`

```xml
<!-- Batch Review Screen -->
<string name="batch_review_title">Review Changes</string>
<string name="batch_review_subtitle">Review and select files to apply</string>
<string name="batch_review_apply_selected">Apply Selected (%1$d)</string>
<string name="batch_review_apply_all">Apply All (%1$d)</string>
<string name="batch_review_select_all">Select All</string>
<string name="batch_review_deselect_all">Deselect All</string>
<string name="batch_review_no_files_selected">No files selected</string>
<string name="batch_review_failed_files">Failed Files (%1$d)</string>
<string name="batch_review_successful_files">Successfully Matched (%1$d)</string>
<string name="batch_review_view_details">View Details</string>
<string name="batch_review_retry_failed">Retry Failed</string>
<string name="batch_review_no_successful_files">No files to apply</string>
```

---

### Phase 2: Field Preview (P0 - Completion)

#### 7. Create Field Changes Card
**New:** `app/src/main/java/dev/secam/simpletag/ui/editor/components/FieldChangesCard.kt`

```kotlin
@Composable
fun FieldChangesCard(
    appliedFields: Map<SimpleTagField, String>,
    originalData: MusicData,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
)
```

Shows:
- **Collapsed**: 3 key fields (Title, Artist, Album) with before/after
- **Expanded**: All changed fields with color coding
- Use `AnimatedVisibility` for smooth expand/collapse

#### 8. Add Field Preview to File Item
**Modify:** `BatchReviewFileItem.kt`

Add expandable `FieldChangesCard` below file info.

---

### Phase 3: Error Handling (P1)

#### 9. Create Error Detail Dialog
**New:** `app/src/main/java/dev/secam/simpletag/ui/editor/dialogs/BatchErrorDetailDialog.kt`

```kotlin
@Composable
fun BatchErrorDetailDialog(
    fileName: String,
    error: String,
    appliedFields: Map<SimpleTagField, String>?,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
)
```

Shows:
- File name and error message
- Partial results (if any fields were found)
- "Retry" button to re-query MusicBrainz
- "Copy Error" button for debugging

**Reference pattern:** `LogDialog.kt` for scrollable error display

#### 10. Implement Retry Logic
**Modify:** `BatchAutoEditViewModel.kt`

```kotlin
fun retryFailed() {
    val currentState = _uiState.value
    if (currentState is BatchAutoEditState.Reviewing) {
        viewModelScope.launch {
            val failedResults = currentState.results.filter { it.status is BatchFileStatus.Failed }
            val newResults = currentState.results.toMutableList()

            for (failed in failedResults) {
                val newResult = processIndividualFile(failed.musicData)
                val index = newResults.indexOfFirst { it.musicData.id == failed.musicData.id }
                if (index != -1) {
                    newResults[index] = newResult
                }
            }

            val successIds = newResults
                .filter { it.status is BatchFileStatus.Success }
                .map { it.musicData.id }
                .toSet()

            _uiState.update { BatchAutoEditState.Reviewing(newResults, successIds) }
        }
    }
}
```

---

### Phase 4: Better Progress UX (P1)

#### 11. Update Progress Dialog
**Modify:** `app/src/main/java/dev/secam/simpletag/ui/editor/dialogs/BatchAutoEditDialog.kt`

Update `ProcessingContent` to show two-stage progress:
```kotlin
@Composable
private fun ProcessingContent(
    current: Int,
    total: Int,
    currentFileName: String,
    currentOperation: ProcessingOperation,
    onCancel: () -> Unit
) {
    Column {
        // Overall progress
        LinearProgressIndicator(
            progress = { current.toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Processing $current of $total files")

        Spacer(modifier = Modifier.height(8.dp))

        // Current operation
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                when (currentOperation) {
                    ProcessingOperation.Searching -> "Searching MusicBrainz..."
                    ProcessingOperation.Applying -> "Applying metadata..."
                    ProcessingOperation.Done -> "Complete"
                }
            )
        }

        Text(currentFileName, style = MaterialTheme.typography.bodySmall)
    }
}
```

#### 12. Update ViewModel Progress Emission
**Modify:** `BatchAutoEditViewModel.kt`

Emit operation state changes:
```kotlin
// Before search
_uiState.update {
    BatchAutoEditState.Processing(
        current = index,
        total = files.size,
        currentFileName = file.getFileName(),
        currentOperation = ProcessingOperation.Searching
    )
}

// After search completes, before apply
_uiState.update {
    BatchAutoEditState.Processing(
        current = index,
        total = files.size,
        currentFileName = file.getFileName(),
        currentOperation = ProcessingOperation.Applying
    )
}
```

---

### Phase 5: Polish

#### 13. Add Animations
- Smooth transitions between states
- Selection animation (already using `animateColorAsState`)
- Expand/collapse animation for field details

#### 14. Performance Optimization
- LazyColumn with `key` parameter for efficient recomposition
- Limit Coil concurrent image requests
- Test with 100+ files

#### 15. Edge Cases
- Empty results: Show message with "Go Back" option
- All failed: Show error summary, provide "Retry All"
- Single file: Simplify UI, hide selection controls

---

## Implementation Sequence

| Phase | Tasks | Priority |
|-------|-------|----------|
| 1 | Foundation (Review state, file item, review screen) | P0 |
| 2 | Field preview (expandable card) | P0 |
| 3 | Error handling (retry, detail dialog) | P1 |
| 4 | Better progress UX (two-stage progress) | P1 |
| 5 | Polish (animations, optimization) | P2 |

---

## Critical Files Reference

| File | Operation | Reference Pattern |
|------|-----------|-------------------|
| `ui/editor/BatchAutoEditState.kt` | **Modify** — Add Reviewing state, ProcessingOperation enum | Lines 50-71 |
| `ui/editor/BatchAutoEditViewModel.kt` | **Modify** — Add selection/retry functions | Lines 72-135 |
| `ui/editor/BatchAutoEditScreen.kt` | **Modify** — Handle Reviewing state | Lines 57-266 |
| `ui/editor/components/BatchReviewFileItem.kt` | **New** — File item with checkbox | AutoEditPreviewDialog.kt lines 148-246 |
| `ui/editor/components/BatchReviewScreen.kt` | **New** — Review screen with LazyColumn | AutoEditPreviewDialog.kt lines 98-111 |
| `ui/editor/components/FieldChangesCard.kt` | **New** — Expandable field changes | - |
| `ui/editor/dialogs/BatchErrorDetailDialog.kt` | **New** — Error detail with retry | LogDialog.kt |
| `ui/editor/dialogs/BatchAutoEditDialog.kt` | **Modify** — Two-stage progress | Lines 40-80 |
| `res/values/strings.xml` | **Modify** — Add review strings | Lines 254-264 |

## Existing Patterns to Reuse

| Pattern | Source | Usage |
|---------|--------|-------|
| Selection animation | `SimpleMusicItem.kt` lines 54-59 | `animateColorAsState` for checkbox selection |
| Checkmark overlay | `SimpleMusicItem.kt` lines 91-110 | Visual indicator for selected state |
| Scrollable list with radio buttons | `AutoEditPreviewDialog.kt` lines 98-111 | LazyColumn with items pattern |
| Card with clickable selection | `AutoEditPreviewDialog.kt` lines 148-246 | Card color change on selection |
| Error detail dialog | `LogDialog.kt` | Scrollable text display pattern |
| Snackbar with action | `BatchAutoEditScreen.kt` lines 110-115 | Error retry snackbar |

---

## Verification

### Test Scenarios

1. **Happy Path (10 files)**
   - All files match successfully
   - Review screen shows all pre-selected
   - User applies all
   - Files updated correctly

2. **Mixed Results (20 files)**
   - 15 success, 3 failed, 2 skipped
   - Failed files shown at top with error messages
   - User deselects 2 successful files
   - Applies 13 files

3. **Retry Failed (5 files)**
   - 3 failed initially
   - User clicks "Retry Failed"
   - 2 succeed on retry, 1 still fails
   - User applies 2 retried files

4. **Field Preview**
   - User clicks "View Details" on file
   - All 15+ fields shown with before/after
   - Changed fields highlighted

5. **Progress UX**
   - User sees "Searching MusicBrainz..." during fetch
   - User sees "Applying metadata..." during write
   - Overall progress shows 1-20

6. **Edge Cases**
   - All failed: "Retry All" button shown
   - Empty results: "No files matched" message
   - Single file: Simplified UI

### Manual Testing Steps

1. Select 10-20 files in SelectorScreen
2. Click batch auto-edit button
3. Verify progress shows operation (Searching vs Applying)
4. Wait for review screen to appear
5. Verify successful files are pre-selected
6. Deselect a few files
7. Click "View Details" on a file
8. Verify field changes shown correctly
9. Click "Apply Selected"
10. Verify only selected files were updated

---

## Performance Considerations

### LazyColumn Optimization
```kotlin
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    key = { result -> result.musicData.id }  // Efficient recomposition
) {
    stickyHeader(key = "failed") {
        FailedSectionHeader()
    }
    items(results, key = { it.musicData.id }) { result ->
        BatchReviewFileItem(...)
    }
}
```

### Selection State Management
- Use `SnapshotStateList` for efficient updates
- Avoid full list recomposition on selection change
- Implement selection batching for "Select All"

### Image Loading
- Limit concurrent Coil requests to 4
- Use low-quality placeholders during scroll
- Implement memory cache for album art

---

## Success Metrics

### UX Improvements
- Reduced accidental applies (user can review before committing)
- Increased user confidence (visible field changes)
- Better error recovery (retry without starting over)

### Performance
- < 100ms to render 100-item list
- < 500ms to apply selection changes
- Smooth scrolling at 60fps

### Code Quality
- ~600 new lines of code
- Reuses existing patterns
- No new dependencies
