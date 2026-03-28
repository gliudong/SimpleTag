
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
