package dev.secam.simpletag.data.musicbrainz

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service for MusicBrainz
 * Documentation: https://musicbrainz.org/doc/MusicBrainz_API
 */
interface MusicBrainzApiService {

    @GET("release/")
    suspend fun searchReleases(
        @Query("query") query: String,
        @Query("limit") limit: Int = 10,
        @Query("fmt") format: String = "xml"
    ): ResponseBody

    @GET("recording/")
    suspend fun searchRecordings(
        @Query("query") query: String,
        @Query("limit") limit: Int = 10,
        @Query("fmt") format: String = "xml"
    ): ResponseBody
}
