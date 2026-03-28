
package dev.secam.simpletag.ui.editor

import dev.secam.simpletag.data.enums.SimpleTagField
import dev.secam.simpletag.data.media.MusicData

/**
 * Result of processing a single file in batch auto-edit
 */
data class BatchFileResult(
    val musicData: MusicData,
    val status: BatchFileStatus,
    val error: String? = null,
    val appliedFields: Map<SimpleTagField, String>? = null
)

/**
 * Status of a batch auto-edit file operation
 */
sealed class BatchFileStatus {
    /** File was successfully matched and metadata was applied */
    object Success : BatchFileStatus()

    /** File processing failed with a specific reason */
    data class Failed(val reason: String) : BatchFileStatus()

    /** File was skipped (e.g., unparseable filename) */
    object Skipped : BatchFileStatus()
}

/**
 * Current processing operation for progress display
 */
enum class ProcessingOperation {
    /** Searching MusicBrainz for metadata */
    Searching,
    /** Applying metadata to files */
    Applying,
    /** Operation complete */
    Done
}

/**
 * Overall state of the batch auto-edit operation
 */
sealed class BatchAutoEditState {
    /** No operation in progress */
    object Idle : BatchAutoEditState()

    /** Currently processing files */
    data class Processing(
        val current: Int,
        val total: Int,
        val currentFileName: String,
        val currentOperation: ProcessingOperation = ProcessingOperation.Searching
    ) : BatchAutoEditState()

    /** Reviewing search results before applying */
    data class Reviewing(
        val results: List<BatchFileResult>,
        val selectedIds: Set<Long> = emptySet()
    ) : BatchAutoEditState()

    /** Processing completed with results */
    data class Completed(
        val results: List<BatchFileResult>,
        val successCount: Int,
        val failureCount: Int,
        val skippedCount: Int
    ) : BatchAutoEditState()

    /** Operation was cancelled by user */
    object Cancelled : BatchAutoEditState()
}

/**
 * Group of files that should be queried together
 * Files are grouped by folder/album to reduce API calls
 */
data class FileQueryGroup(
    val folder: String,
    val album: String,
    val files: List<MusicData>
)
