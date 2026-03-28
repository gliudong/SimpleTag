package dev.secam.simpletag.data.musicbrainz.models

/**
 * Data models for MusicBrainz API responses
 * These represent the parsed XML/JSON responses from MusicBrainz
 */

data class MusicBrainzRelease(
    val id: String, // MBID (MusicBrainz ID)
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val album: String,
    val date: String? = null, // Release date (YYYY-MM-DD)
    val year: String? = null,
    val country: String? = null,
    val label: String? = null,
    val catalogNumber: String? = null,
    val trackCount: Int = 0,
    val tracks: List<MusicBrainzTrack> = emptyList(),
    val coverArtUrl: String? = null, // Derived from MBID for Cover Art Archive
    val barcode: String? = null,
    val asin: String? = null,
    val releaseStatus: String? = null,
    val releaseType: String? = null,
    val releaseGroupId: String? = null
)

data class MusicBrainzTrack(
    val id: String,
    val title: String,
    val number: Int,
    val duration: Int? = null, // In milliseconds
    val artist: String? = null,
    val artistId: String? = null
)

data class MusicBrainzArtist(
    val id: String,
    val name: String,
    val sortName: String? = null,
    val disambiguation: String? = null,
    val type: String? = null,
    val country: String? = null,
    val beginDate: String? = null,
    val endDate: String? = null
)

/**
 * Represents a release that contains a recording
 */
data class MusicBrainzRecordingRelease(
    val id: String,
    val title: String,
    val date: String? = null,
    val coverArtUrl: String? = null
)

/**
 * Represents a MusicBrainz recording (song/track)
 * Returned from the recording search API
 */
data class MusicBrainzRecording(
    val id: String,
    val title: String,
    val artist: String,
    val artistId: String? = null,
    val duration: Int? = null, // In milliseconds
    val releases: List<MusicBrainzRecordingRelease> = emptyList()
)

data class MusicBrainzSearchResponse(
    val releases: List<MusicBrainzRelease>,
    val count: Int,
    val offset: Int
)

sealed class MusicBrainzResult<out T> {
    data class Success<T>(val data: T) : MusicBrainzResult<T>()
    data class Error(val exception: MusicBrainzException) : MusicBrainzResult<Nothing>()
    object NoResults : MusicBrainzResult<Nothing>()
}

class MusicBrainzException(
    val type: ErrorType,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

enum class ErrorType {
    NETWORK_ERROR,
    TIMEOUT,
    RATE_LIMIT,
    PARSE_ERROR,
    NOT_FOUND
}
