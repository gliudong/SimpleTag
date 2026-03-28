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

package dev.secam.simpletag.data.musicbrainz.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response from Cover Art Archive API for release/release-group
 * JSON structure: https://coverartarchive.org/release/{mbid}/
 */
@JsonClass(generateAdapter = true)
data class CoverArtArchiveResponse(
    val images: List<CoverArtImage>
)

/**
 * Individual cover art image from Cover Art Archive
 */
@JsonClass(generateAdapter = true)
data class CoverArtImage(
    @Json(name = "types")
    val types: List<String>,

    @Json(name = "front")
    val front: Boolean,

    @Json(name = "back")
    val back: Boolean,

    @Json(name = "image")
    val image: String,

    @Json(name = "thumbnails")
    val thumbnails: CoverArtThumbnails?,

    @Json(name = "approved")
    val approved: Boolean,

    @Json(name = "id")
    val id: String
)

/**
 * Thumbnail URLs available for a cover art image
 */
@JsonClass(generateAdapter = true)
data class CoverArtThumbnails(
    @Json(name = "small")
    val small: String?,    // 250px

    @Json(name = "large")
    val large: String?,    // 500px

    @Json(name = "1200")
    val large1200: String? // 1200px
)

/**
 * Cover art availability information from MusicBrainz API
 * This allows pre-checking if cover art exists before calling Cover Art Archive
 *
 * From MusicBrainz Release API response:
 * "cover-art-archive": {
 *     "darkened": false,
 *     "count": 0,
 *     "artwork": false,
 *     "back": false,
 *     "front": false
 * }
 */
data class CoverArtInfo(
    val front: Boolean,    // True if front cover exists
    val back: Boolean,     // True if back cover exists
    val count: Int,        // Total number of cover images
    val artwork: Boolean   // True if any cover art exists
)

/**
 * Quality preference for cover art download
 */
enum class CoverArtQuality {
    MEDIUM,  // 500px (preferred)
    LOW,     // 250px
    ORIGINAL // Full resolution
}

/**
 * Selected cover art with URL and quality information
 */
data class SelectedCoverArt(
    val url: String,
    val quality: CoverArtQuality,
    val type: String // "Front", "Back", "Other", etc.
)

/**
 * Result type for cover art fetching operations
 */
sealed class CoverArtResult {
    /**
     * Successfully fetched cover art (may have used fallback)
     */
    data class Success(val coverArt: SelectedCoverArt) : CoverArtResult()

    /**
     * Partially successful - got cover art but had to use fallback option
     */
    data class PartialSuccess(
        val coverArt: SelectedCoverArt,
        val fallbackMessage: String
    ) : CoverArtResult()

    /**
     * No cover art available for this release
     */
    object NoCoverArt : CoverArtResult()

    /**
     * Error occurred while fetching cover art
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : CoverArtResult()
}
