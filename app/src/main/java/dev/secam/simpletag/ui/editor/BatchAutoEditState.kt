/*
 * Copyright (C) 2025-2026 Sergio Camacho
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
