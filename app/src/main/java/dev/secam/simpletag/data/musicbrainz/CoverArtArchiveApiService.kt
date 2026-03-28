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

package dev.secam.simpletag.data.musicbrainz

import dev.secam.simpletag.data.musicbrainz.models.CoverArtArchiveResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit API service for Cover Art Archive
 * API documentation: https://coverartarchive.org/
 * Base URL: https://coverartarchive.org/
 */
interface CoverArtArchiveApiService {

    /**
     * Get cover art metadata for a specific release
     * Returns JSON with all available cover images and their thumbnails
     *
     * @param mbid MusicBrainz Release ID
     * @return CoverArtArchiveResponse containing list of cover images
     */
    @GET("release/{mbid}/")
    suspend fun getReleaseCoverArt(@Path("mbid") mbid: String): CoverArtArchiveResponse

    /**
     * Get cover art metadata for a release group
     * Used as fallback when release cover art is not available
     *
     * @param mbid MusicBrainz Release Group ID
     * @return CoverArtArchiveResponse containing list of cover images
     */
    @GET("release-group/{mbid}/")
    suspend fun getReleaseGroupCoverArt(@Path("mbid") mbid: String): CoverArtArchiveResponse
}
