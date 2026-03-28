package dev.secam.simpletag.ui.editor

import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRelease
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzTrack
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRecording
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRecordingRelease

/**
 * Sealed class representing the state of auto-edit operations
 */
sealed class AutoEditState {
    /** Initial state, no operation in progress */
    object Idle : AutoEditState()

    /** Currently fetching data from MusicBrainz */
    object Loading : AutoEditState()

    /** Successfully fetched releases from MusicBrainz */
    data class Success(val results: List<MusicBrainzRelease>) : AutoEditState()

    /** Error occurred during fetch */
    data class Error(val message: String) : AutoEditState()

    /** No results found for the query */
    object NoResults : AutoEditState()
}

/**
 * Query parameters for MusicBrainz search
 */
data class AutoEditQueryParams(
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val track: Int? = null
)

/**
 * Represents a track from a release for auto-edit selection
 */
data class AutoEditTrackItem(
    val release: MusicBrainzRelease,
    val track: MusicBrainzTrack
)

/**
 * Represents a recording (song) for auto-edit selection
 * Used when searching for individual songs instead of releases
 */
data class AutoEditRecordingItem(
    val recording: MusicBrainzRecording,
    val release: MusicBrainzRecordingRelease? = null // Preferred release (if available)
)
