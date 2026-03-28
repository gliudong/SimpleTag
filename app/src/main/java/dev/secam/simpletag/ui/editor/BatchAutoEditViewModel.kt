
package dev.secam.simpletag.ui.editor

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.secam.simpletag.data.enums.SimpleTagField
import dev.secam.simpletag.data.media.MediaRepo
import dev.secam.simpletag.data.media.MusicData
import dev.secam.simpletag.data.musicbrainz.CoverArtRepository
import dev.secam.simpletag.data.musicbrainz.MusicBrainzMapper
import dev.secam.simpletag.data.musicbrainz.MusicBrainzRepository
import dev.secam.simpletag.data.musicbrainz.models.CoverArtResult
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRecording
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRelease
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzResult
import dev.secam.simpletag.util.FileNameParser
import dev.secam.simpletag.util.tag.oggFileWriter
import dev.secam.simpletag.util.tag.simpleFileReader
import dev.secam.simpletag.util.tag.simpleFileWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jaudiotagger.tag.FieldKey
import javax.inject.Inject

/**
 * ViewModel for batch auto-edit operations
 * Handles querying MusicBrainz for multiple files and applying metadata automatically
 */
@HiltViewModel
class BatchAutoEditViewModel @Inject constructor(
    private val musicBrainzRepo: MusicBrainzRepository,
    private val coverArtRepo: CoverArtRepository,
    private val mediaRepo: MediaRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow<BatchAutoEditState>(BatchAutoEditState.Idle)
    val uiState: StateFlow<BatchAutoEditState> = _uiState.asStateFlow()

    private var currentJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val TAG = "BatchAutoEditVM"
    }

    /**
     * Start batch auto-edit operation for the given files
     */
    fun startBatchAutoEdit(files: List<MusicData>) {
        currentJob?.cancel()
        currentJob = viewModelScope.launch {
            try {
                _uiState.update { BatchAutoEditState.Processing(0, files.size, "") }

                val results = mutableListOf<BatchFileResult>()
                var successCount = 0
                var failureCount = 0
                var skippedCount = 0

                // Process each file individually
                for ((index, file) in files.withIndex()) {
                    // Check for cancellation
                    if (_uiState.value is BatchAutoEditState.Cancelled) {
                        break
                    }

                    // Update progress - Searching phase
                    _uiState.update {
                        BatchAutoEditState.Processing(
                            current = index,
                            total = files.size,
                            currentFileName = file.getFileName(),
                            currentOperation = ProcessingOperation.Searching
                        )
                    }

                    // Process this individual file
                    val fileResult = processIndividualFile(file)
                    results.add(fileResult)

                    when (fileResult.status) {
                        is BatchFileStatus.Success -> successCount++
                        is BatchFileStatus.Failed -> failureCount++
                        is BatchFileStatus.Skipped -> skippedCount++
                    }

                    // Update progress after processing
                    _uiState.update {
                        BatchAutoEditState.Processing(
                            current = index + 1,
                            total = files.size,
                            currentFileName = file.getFileName(),
                            currentOperation = ProcessingOperation.Searching
                        )
                    }
                }

                // Finalize - transition to Reviewing state
                if (_uiState.value !is BatchAutoEditState.Cancelled) {
                    enterReviewState(results)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Batch auto-edit failed", e)
                _uiState.update { BatchAutoEditState.Idle }
            }
        }
    }

    /**
     * Get filename from path
     */
    private fun MusicData.getFileName(): String {
        return path.substringAfterLast("/")
    }

    /**
     * Process a single file using searchRecordings (same as single-file auto-edit)
     */
    private suspend fun processIndividualFile(file: MusicData): BatchFileResult {
        // Parse filename to get artist and title
        val parseResult = FileNameParser.parse(file.getFileName())

        // Check if parsing produced meaningful results
        if (parseResult.title.isBlank()) {
            return BatchFileResult(
                musicData = file,
                status = BatchFileStatus.Skipped,
                error = "Could not parse filename"
            )
        }

        // Use searchRecordings like single-file auto-edit
        val searchResult = musicBrainzRepo.searchRecordings(
            title = parseResult.title,
            artist = parseResult.artist.takeIf { !it.isNullOrBlank() }
        )

        return when (searchResult) {
            is MusicBrainzResult.Success -> {
                val recordings = searchResult.data
                if (recordings.isNotEmpty()) {
                    // Use the first (best matching) recording
                    val recording = recordings.first()

                    // Find the best release for this recording
                    val bestRelease = findBestRelease(recording, file)

                    if (bestRelease != null) {
                        // Find matching track in the release
                        val trackNumber = FileNameParser.extractTrackNumber(file.getFileName())
                        val bestTrack = if (trackNumber != null) {
                            bestRelease.tracks.find { it.number == trackNumber }
                        } else {
                            bestRelease.tracks.firstOrNull()
                        }

                        if (bestTrack != null) {
                            val fieldMap = MusicBrainzMapper.mapTrackToFieldStates(bestRelease, bestTrack)
                            BatchFileResult(
                                musicData = file,
                                status = BatchFileStatus.Success,
                                appliedFields = fieldMap
                            )
                        } else {
                            // No matching track, use recording data directly
                            val fieldMap = mapRecordingToFieldStates(recording, bestRelease)
                            BatchFileResult(
                                musicData = file,
                                status = BatchFileStatus.Success,
                                appliedFields = fieldMap
                            )
                        }
                    } else {
                        // No release found, use recording data only
                        val fieldMap = mapRecordingToFieldStates(recording, null)
                        BatchFileResult(
                            musicData = file,
                            status = BatchFileStatus.Success,
                            appliedFields = fieldMap
                        )
                    }
                } else {
                    BatchFileResult(
                        musicData = file,
                        status = BatchFileStatus.Failed("No recordings found"),
                        error = "No recordings found"
                    )
                }
            }
            is MusicBrainzResult.NoResults -> {
                BatchFileResult(
                    musicData = file,
                    status = BatchFileStatus.Failed("No results found"),
                    error = "No results found"
                )
            }
            is MusicBrainzResult.Error -> {
                BatchFileResult(
                    musicData = file,
                    status = BatchFileStatus.Failed(searchResult.exception.message ?: "Unknown error"),
                    error = searchResult.exception.message
                )
            }
        }
    }

    /**
     * Find the best release for a recording
     * Prefers releases with the most tracks and official status
     */
    private fun findBestRelease(
        recording: MusicBrainzRecording,
        file: MusicData
    ): MusicBrainzRelease? {
        // If recording has releases, find the best one
        if (recording.releases.isNotEmpty()) {
            // Prefer releases that match the file's album (if known)
            val fileAlbum = file.album.takeIf { !it.isNullOrBlank() }
            val albumMatch = if (fileAlbum != null) {
                recording.releases.find { it.title.contains(fileAlbum, ignoreCase = true) }
            } else null

            if (albumMatch != null) {
                // Need to fetch full release details
                return null // For now, return null - would need additional API call
            }

            // Otherwise prefer the first release (usually the original)
            return null // Would need to fetch full release
        }

        return null
    }

    /**
     * Map a MusicBrainzRecording to field states
     * Used when we don't have a full release with tracks
     */
    private fun mapRecordingToFieldStates(
        recording: MusicBrainzRecording,
        release: MusicBrainzRelease?
    ): Map<SimpleTagField, String> {
        val fieldMap = mutableMapOf<SimpleTagField, String>()

        // Basic fields from recording
        fieldMap[SimpleTagField.Title] = recording.title
        fieldMap[SimpleTagField.Artist] = recording.artist

        // If we have release info, add those fields
        if (release != null) {
            fieldMap[SimpleTagField.Album] = release.title
            fieldMap[SimpleTagField.AlbumArtist] = release.artist
            release.year?.let { fieldMap[SimpleTagField.Year] = it }
            release.country?.let { fieldMap[SimpleTagField.Country] = it }
            release.label?.let { fieldMap[SimpleTagField.Label] = it }
            release.catalogNumber?.let { fieldMap[SimpleTagField.CatalogNumber] = it }

            // MusicBrainz IDs
            release.artistId?.let { fieldMap[SimpleTagField.MusicBrainzReleaseArtistId] = it }
            fieldMap[SimpleTagField.MusicBrainzReleaseId] = release.id
            release.releaseGroupId?.let { fieldMap[SimpleTagField.MusicBrainzReleaseGroupId] = it }
        }

        recording.artistId?.let { fieldMap[SimpleTagField.MusicBrainzArtistId] = it }
        fieldMap[SimpleTagField.MusicBrainzTrackId] = recording.id

        return fieldMap
    }

    /**
     * Cancel the current batch operation
     */
    fun cancelBatch() {
        _uiState.update { BatchAutoEditState.Cancelled }
        currentJob?.cancel()
    }

    /**
     * Reset the state to Idle
     */
    fun reset() {
        _uiState.update { BatchAutoEditState.Idle }
        currentJob = null
    }

    /**
     * Enter review state with search results
     * All successful results are pre-selected
     */
    fun enterReviewState(results: List<BatchFileResult>) {
        val successIds = results
            .filter { it.status is BatchFileStatus.Success }
            .map { it.musicData.id }
            .toSet()
        Log.d(TAG, "enterReviewState: ${results.size} results, ${successIds.size} successful")
        _uiState.update { BatchAutoEditState.Reviewing(results, successIds) }
    }

    /**
     * Toggle selection state for a single file
     */
    fun toggleSelection(fileId: Long) {
        val currentState = _uiState.value
        if (currentState is BatchAutoEditState.Reviewing) {
            val newSelected = if (currentState.selectedIds.contains(fileId)) {
                currentState.selectedIds - fileId
            } else {
                currentState.selectedIds + fileId
            }
            Log.d(TAG, "toggleSelection: fileId=$fileId, newSelectedCount=${newSelected.size}")
            _uiState.update { currentState.copy(selectedIds = newSelected) }
        }
    }

    /**
     * Select all successful files
     */
    fun selectAll() {
        val currentState = _uiState.value
        if (currentState is BatchAutoEditState.Reviewing) {
            val successIds = currentState.results
                .filter { it.status is BatchFileStatus.Success }
                .map { it.musicData.id }
                .toSet()
            Log.d(TAG, "selectAll: selected ${successIds.size} files")
            _uiState.update { currentState.copy(selectedIds = successIds) }
        }
    }

    /**
     * Deselect all files
     */
    fun deselectAll() {
        val currentState = _uiState.value
        if (currentState is BatchAutoEditState.Reviewing) {
            Log.d(TAG, "deselectAll: cleared selection")
            _uiState.update { currentState.copy(selectedIds = emptySet()) }
        }
    }

    /**
     * Retry failed files
     * Re-processes only the files that failed previously
     */
    fun retryFailed() {
        val currentState = _uiState.value
        if (currentState is BatchAutoEditState.Reviewing) {
            viewModelScope.launch {
                val failedResults = currentState.results.filter { it.status is BatchFileStatus.Failed }

                if (failedResults.isEmpty()) {
                    return@launch
                }

                val newResults = currentState.results.toMutableList()

                for (failed in failedResults) {
                    // Re-process this file
                    val newResult = processIndividualFile(failed.musicData)
                    val index = newResults.indexOfFirst { it.musicData.id == failed.musicData.id }
                    if (index != -1) {
                        newResults[index] = newResult
                    }
                }

                // Recalculate selected IDs (only successful files)
                val successIds = newResults
                    .filter { it.status is BatchFileStatus.Success }
                    .map { it.musicData.id }
                    .toSet()

                _uiState.update { BatchAutoEditState.Reviewing(newResults, successIds) }
            }
        }
    }

    /**
     * Apply the batch results to the actual files
     */
    suspend fun applyBatchResults(
        context: Context,
        results: List<BatchFileResult>,
        onComplete: (success: Int, failed: Int) -> Unit
    ) {
        var successCount = 0
        var failedCount = 0
        val filesToRefresh = mutableListOf<MusicData>()

        for (result in results) {
            if (result.status is BatchFileStatus.Success && result.appliedFields != null) {
                try {
                    applyFieldsToFile(context, result.musicData, result.appliedFields)
                    filesToRefresh.add(result.musicData)
                    successCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to apply fields to ${result.musicData.getFileName()}", e)
                    failedCount++
                }
            }
        }

        // Refresh MediaStore to reflect changes
        if (filesToRefresh.isNotEmpty()) {
            try {
                refreshMediaStore(filesToRefresh)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh MediaStore", e)
            }
        }

        onComplete(successCount, failedCount)
    }

    /**
     * Apply field states to an actual audio file
     */
    private suspend fun applyFieldsToFile(
        context: Context,
        musicData: MusicData,
        fieldMap: Map<SimpleTagField, String>
    ) {
        Log.d(TAG, "Applying fields to ${musicData.getFileName()}")
        Log.d(TAG, "Field map: $fieldMap")

        val audioFile = simpleFileReader(musicData.path)
            ?: throw Exception("Could not read audio file")

        // Apply each field
        var fieldsWritten = 0
        fieldMap.forEach { (field, value) ->
            if (value.isNotBlank()) {
                val fieldKey = when (field) {
                    SimpleTagField.Title -> FieldKey.TITLE
                    SimpleTagField.Artist -> FieldKey.ARTIST
                    SimpleTagField.Album -> FieldKey.ALBUM
                    SimpleTagField.AlbumArtist -> FieldKey.ALBUM_ARTIST
                    SimpleTagField.Year -> FieldKey.YEAR
                    SimpleTagField.Track -> FieldKey.TRACK
                    SimpleTagField.Genre -> FieldKey.GENRE
                    SimpleTagField.Composer -> FieldKey.COMPOSER
                    SimpleTagField.DiscNumber -> FieldKey.DISC_NO
                    SimpleTagField.Comment -> FieldKey.COMMENT
                    SimpleTagField.MusicBrainzArtistId -> FieldKey.MUSICBRAINZ_ARTISTID
                    SimpleTagField.MusicBrainzReleaseId -> FieldKey.MUSICBRAINZ_RELEASEID
                    SimpleTagField.MusicBrainzReleaseGroupId -> FieldKey.MUSICBRAINZ_RELEASE_GROUP_ID
                    SimpleTagField.MusicBrainzTrackId -> FieldKey.MUSICBRAINZ_TRACK_ID
                    SimpleTagField.MusicBrainzDiscId -> FieldKey.MUSICBRAINZ_DISC_ID
                    SimpleTagField.MusicBrainzReleaseArtistId -> FieldKey.MUSICBRAINZ_ARTISTID
                    SimpleTagField.Country -> FieldKey.COUNTRY
                    SimpleTagField.Label -> FieldKey.RECORD_LABEL
                    SimpleTagField.CatalogNumber -> FieldKey.CATALOG_NO
                    SimpleTagField.Barcode -> FieldKey.BARCODE
                    SimpleTagField.ASIN -> FieldKey.AMAZON_ID
                    SimpleTagField.ReleaseStatus -> FieldKey.MUSICBRAINZ_RELEASE_STATUS
                    SimpleTagField.ReleaseType -> FieldKey.MUSICBRAINZ_RELEASE_TYPE
                    else -> null
                }

                fieldKey?.let {
                    audioFile.tag.setField(it, value)
                    fieldsWritten++
                    Log.d(TAG, "Wrote field: $field = $value")
                }
            } else {
                Log.d(TAG, "Skipping blank field: $field")
            }
        }

        Log.d(TAG, "Wrote $fieldsWritten fields to ${musicData.getFileName()}")

        // Write the file
        when {
            musicData.path.endsWith("ogg", true) ||
                    musicData.path.endsWith("opus", true) -> {
                oggFileWriter(audioFile, context)
            }
            else -> {
                simpleFileWriter(audioFile)
            }
        }

        Log.d(TAG, "File written successfully: ${musicData.getFileName()}")

        // Update MediaStore
        val resolver = context.contentResolver
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.getContentUri("external"),
            musicData.id
        )
        val values = android.content.ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, fieldMap[SimpleTagField.Title] ?: musicData.title)
            put(MediaStore.Audio.Media.ARTIST, fieldMap[SimpleTagField.Artist] ?: musicData.artist)
            put(MediaStore.Audio.Media.ALBUM, fieldMap[SimpleTagField.Album] ?: musicData.album)
            put(MediaStore.Audio.Media.YEAR, (fieldMap[SimpleTagField.Year] ?: "").toIntOrNull() ?: 0)
        }
        resolver.update(uri, values, null, null)

        Log.d(TAG, "MediaStore updated: ${musicData.getFileName()}")
    }

    /**
     * Refresh MediaStore to reflect changes
     */
    private suspend fun refreshMediaStore(files: List<MusicData>) {
        mediaRepo.refreshMediaStore(files)
        Log.d(TAG, "MediaStore refreshed for ${files.size} files")
    }
}
