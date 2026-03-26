package dev.secam.simpletag.ui.editor

import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRelease

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
