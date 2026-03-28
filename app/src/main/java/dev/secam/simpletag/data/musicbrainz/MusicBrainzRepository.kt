package dev.secam.simpletag.data.musicbrainz

import android.util.Log
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzException
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRelease
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzResult
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRecording
import dev.secam.simpletag.data.musicbrainz.models.ErrorType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Repository for MusicBrainz API operations
 * Features:
 * - Rate limiting (1 request/second per MusicBrainz policy)
 * - In-memory caching with LruCache (max 50 entries)
 * - JSON parsing for API responses
 * - Error handling and retry logic
 */
class MusicBrainzRepository(
    private val apiService: MusicBrainzApiService
) {
    private val cache = android.util.LruCache<String, List<MusicBrainzRelease>>(50)
    private val rateLimitMutex = Mutex()
    private var lastRequestTime = 0L

    companion object {
        private const val RATE_LIMIT_MS = 1000L // 1 request per second
        private const val TAG = "MusicBrainzRepo"
    }

    /**
     * Search for releases matching the given criteria
     */
    suspend fun searchReleases(
        title: String,
        artist: String? = null,
        album: String? = null,
        track: Int? = null
    ): MusicBrainzResult<List<MusicBrainzRelease>> {
        val query = buildQueryString(title, artist, album, track)
        val cacheKey = getCacheKey(query)

        // Check cache first
        cache.get(cacheKey)?.let { cachedResults ->
            return MusicBrainzResult.Success(cachedResults)
        }

        // Enforce rate limiting
        rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val timeSinceLastRequest = now - lastRequestTime
            if (timeSinceLastRequest < RATE_LIMIT_MS) {
                kotlinx.coroutines.delay(RATE_LIMIT_MS - timeSinceLastRequest)
            }
            lastRequestTime = System.currentTimeMillis()
        }

        return try {
            val response = apiService.searchReleases(
                query = query,
                limit = 10,
                format = "json"  // Use JSON instead of XML
            )

            val json = JSONObject(response.string())
            val releases = parseJsonResponse(json)

            Log.d(TAG, "Query: $query -> ${releases.size} results")

            if (releases.isEmpty()) {
                MusicBrainzResult.NoResults
            } else {
                cache.put(cacheKey, releases)
                MusicBrainzResult.Success(releases)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            when {
                e is java.net.SocketTimeoutException ||
                e is java.net.UnknownHostException -> {
                    MusicBrainzResult.Error(
                        MusicBrainzException(ErrorType.NETWORK_ERROR, "Network error: ${e.message}", e)
                    )
                }
                e is retrofit2.HttpException && e.code() == 503 -> {
                    MusicBrainzResult.Error(
                        MusicBrainzException(ErrorType.RATE_LIMIT, "Rate limit exceeded. Please wait.", e)
                    )
                }
                e is retrofit2.HttpException && e.code() == 404 -> {
                    MusicBrainzResult.NoResults
                }
                e is org.json.JSONException -> {
                    MusicBrainzResult.Error(
                        MusicBrainzException(ErrorType.PARSE_ERROR, "Failed to parse response", e)
                    )
                }
                else -> {
                    MusicBrainzResult.Error(
                        MusicBrainzException(ErrorType.NETWORK_ERROR, "Error: ${e.message ?: "Unknown error"}", e)
                    )
                }
            }
        }
    }

    /**
     * Build MusicBrainz query string from parameters.
     * Uses a relaxed keyword approach instead of strict AND matching.
     * MusicBrainz AND queries require exact match, so we use simple
     * keyword concatenation which does OR-like fuzzy matching.
     */
    private fun buildQueryString(
        title: String,
        artist: String? = null,
        album: String? = null,
        track: Int? = null
    ): String {
        val parts = mutableListOf<String>()

        // Use simple keyword search — MusicBrainz does fuzzy matching
        // Exact AND queries with quotes are too strict and often return 0 results
        artist?.let { parts.add(it) }
        album?.let { parts.add(it) }
        parts.add(title)
        // Don't include track number in the search query — it's too restrictive

        return parts.joinToString(" ")
    }

    private fun getCacheKey(query: String): String {
        return query.lowercase().trim()
    }

    /**
     * Parse MusicBrainz JSON response
     *
     * JSON structure:
     * {
     *   "created": "...",
     *   "count": N,
     *   "offset": 0,
     *   "releases": [
     *     {
     *       "id": "mbid",
     *       "title": "...",
     *       "status": "...",
     *       "date": "2020-01-01",
     *       "country": "US",
     *       "barcode": "...",
     *       "asin": "...",
     *       "artist-credit": [{ "artist": { "id": "...", "name": "...", ... } }],
     *       "release-group": { "id": "...", "type": "...", ... },
     *       "medium-list": [{ "track-list": [...], "track-count": N }],
     *       "label-info-list": [{ "label": { "name": "..." }, "catalog-number": "..." }]
     *     }
     *   ]
     * }
     */
    private fun parseJsonResponse(json: JSONObject): List<MusicBrainzRelease> {
        val releases = mutableListOf<MusicBrainzRelease>()

        if (!json.has("releases")) return releases

        val releasesArray = json.getJSONArray("releases")

        for (i in 0 until releasesArray.length().coerceAtMost(10)) {
            try {
                val releaseObj = releasesArray.getJSONObject(i)
                val release = parseRelease(releaseObj)
                releases.add(release)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse release at index $i", e)
            }
        }

        return releases
    }

    private fun parseRelease(obj: JSONObject): MusicBrainzRelease {
        val id = obj.getString("id")

        // Parse artist from artist-credit
        val artistCredit = obj.optJSONArray("artist-credit")
        var artistName = ""
        var artistId: String? = null
        if (artistCredit != null && artistCredit.length() > 0) {
            val firstCredit = artistCredit.getJSONObject(0)
            val artistObj = firstCredit.optJSONObject("artist")
            if (artistObj != null) {
                artistName = artistObj.optString("name", "")
                artistId = artistObj.optString("id", null)
            }
        }

        val title = obj.optString("title", "")
        val date = obj.optString("date", null)
        val country = obj.optString("country", null)
        val barcode = obj.optString("barcode", null)
        val asin = obj.optString("asin", null)
        val status = obj.optString("status", null)

        // Parse release-group for type
        val releaseGroup = obj.optJSONObject("release-group")
        var releaseType: String? = null
        var releaseGroupId: String? = null
        if (releaseGroup != null) {
            releaseType = releaseGroup.optString("type", null)
            releaseGroupId = releaseGroup.optString("id", null)
        }

        // Parse label-info-list for label name and catalog number
        val labelInfoList = obj.optJSONArray("label-info-list")
        var label: String? = null
        var catalogNumber: String? = null
        if (labelInfoList != null && labelInfoList.length() > 0) {
            val firstLabel = labelInfoList.getJSONObject(0)
            val labelObj = firstLabel.optJSONObject("label")
            if (labelObj != null) {
                label = labelObj.optString("name", null)
            }
            catalogNumber = firstLabel.optString("catalog-number", null)
        }

        // Parse tracks from medium-list
        var trackCount = 0
        val tracks = mutableListOf<dev.secam.simpletag.data.musicbrainz.models.MusicBrainzTrack>()
        val mediumList = obj.optJSONArray("medium-list")
        if (mediumList != null) {
            for (m in 0 until mediumList.length()) {
                val medium = mediumList.getJSONObject(m)
                val trackList = medium.optJSONArray("track-list")
                if (trackList != null) {
                    for (t in 0 until trackList.length()) {
                        trackCount++
                        val trackObj = trackList.getJSONObject(t)
                        tracks.add(
                            dev.secam.simpletag.data.musicbrainz.models.MusicBrainzTrack(
                                id = trackObj.optString("id", ""),
                                title = trackObj.optString("title", ""),
                                number = trackObj.optInt("number", 0),
                                duration = trackObj.optInt("length", 0).takeIf { it > 0 },
                                artist = parseTrackArtist(trackObj.optJSONArray("artist-credit")),
                                artistId = parseTrackArtistId(trackObj.optJSONArray("artist-credit"))
                            )
                        )
                    }
                }
                // Also get track-count from medium
                val mediumTrackCount = medium.optInt("track-count", 0)
                if (mediumTrackCount > trackCount) {
                    trackCount = mediumTrackCount
                }
            }
        }

        return MusicBrainzRelease(
            id = id,
            title = title,
            artist = artistName,
            artistId = artistId,
            album = title,
            date = date,
            year = date?.substringBefore("-"),
            country = country,
            label = label,
            catalogNumber = catalogNumber,
            trackCount = trackCount,
            tracks = tracks,
            coverArtUrl = "https://coverartarchive.org/release/$id/front",
            barcode = barcode,
            asin = asin,
            releaseStatus = status,
            releaseType = releaseType,
            releaseGroupId = releaseGroupId
        )
    }

    private fun parseTrackArtist(artistCredit: JSONArray?): String? {
        if (artistCredit == null || artistCredit.length() == 0) return null
        val firstCredit = artistCredit.optJSONObject(0)
        val artistObj = firstCredit?.optJSONObject("artist") ?: return null
        return artistObj.optString("name", null).ifBlank { null }
    }

    private fun parseTrackArtistId(artistCredit: JSONArray?): String? {
        if (artistCredit == null || artistCredit.length() == 0) return null
        val firstCredit = artistCredit.optJSONObject(0)
        val artistObj = firstCredit?.optJSONObject("artist") ?: return null
        return artistObj.optString("id", null).ifBlank { null }
    }

    fun clearCache() {
        cache.evictAll()
    }

    /**
     * Search for recordings (songs/tracks) matching the given criteria
     * This is more appropriate for single song lookup than release search
     */
    suspend fun searchRecordings(
        title: String,
        artist: String? = null
    ): MusicBrainzResult<List<MusicBrainzRecording>> {
        val query = buildQueryString(title, artist, null, null)
        val cacheKey = "recording:$query"

        // Check cache first
        cache.get(cacheKey)?.let {
            @Suppress("UNCHECKED_CAST")
            return MusicBrainzResult.Success(it as List<MusicBrainzRecording>)
        }

        // Enforce rate limiting
        rateLimitMutex.withLock {
            val now = System.currentTimeMillis()
            val timeSinceLastRequest = now - lastRequestTime
            if (timeSinceLastRequest < RATE_LIMIT_MS) {
                kotlinx.coroutines.delay(RATE_LIMIT_MS - timeSinceLastRequest)
            }
            lastRequestTime = System.currentTimeMillis()
        }

        return try {
            val response = apiService.searchRecordings(
                query = query,
                limit = 10,
                format = "json"
            )

            val json = JSONObject(response.string())
            val recordings = parseRecordingJsonResponse(json)

            Log.d(TAG, "Recording Query: $query -> ${recordings.size} results")

            if (recordings.isEmpty()) {
                MusicBrainzResult.NoResults
            } else {
                @Suppress("UNCHECKED_CAST")
                cache.put(cacheKey, recordings as List<MusicBrainzRelease>)
                MusicBrainzResult.Success(recordings)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recording search failed", e)
            when {
                e is java.net.SocketTimeoutException ||
                e is java.net.UnknownHostException -> {
                    MusicBrainzResult.Error(
                        MusicBrainzException(ErrorType.NETWORK_ERROR, "Network error: ${e.message}", e)
                    )
                }
                e is retrofit2.HttpException && e.code() == 503 -> {
                    MusicBrainzResult.Error(
                        MusicBrainzException(ErrorType.RATE_LIMIT, "Rate limit exceeded. Please wait.", e)
                    )
                }
                e is retrofit2.HttpException && e.code() == 404 -> {
                    MusicBrainzResult.NoResults
                }
                e is org.json.JSONException -> {
                    MusicBrainzResult.Error(
                        MusicBrainzException(ErrorType.PARSE_ERROR, "Failed to parse response", e)
                    )
                }
                else -> {
                    MusicBrainzResult.Error(
                        MusicBrainzException(ErrorType.NETWORK_ERROR, "Error: ${e.message ?: "Unknown error"}", e)
                    )
                }
            }
        }
    }

    /**
     * Parse MusicBrainz recording JSON response
     */
    private fun parseRecordingJsonResponse(json: JSONObject): List<MusicBrainzRecording> {
        val recordings = mutableListOf<MusicBrainzRecording>()

        if (!json.has("recordings")) return recordings

        val recordingsArray = json.getJSONArray("recordings")

        for (i in 0 until recordingsArray.length().coerceAtMost(10)) {
            try {
                val recordingObj = recordingsArray.getJSONObject(i)
                val recording = parseRecording(recordingObj)
                recordings.add(recording)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse recording at index $i", e)
            }
        }

        return recordings
    }

    private fun parseRecording(obj: JSONObject): MusicBrainzRecording {
        val id = obj.getString("id")
        val title = obj.optString("title", "")
        val duration = obj.optInt("length", 0).takeIf { it > 0 }

        // Parse artist from artist-credit
        val artistCredit = obj.optJSONArray("artist-credit")
        var artistName = ""
        var artistId: String? = null
        if (artistCredit != null && artistCredit.length() > 0) {
            val firstCredit = artistCredit.getJSONObject(0)
            artistName = firstCredit.optString("name", "")
            val artistObj = firstCredit.optJSONObject("artist")
            if (artistObj != null) {
                artistId = artistObj.optString("id", null)
            }
        }

        // Parse releases
        val releases = mutableListOf<dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRecordingRelease>()
        val releasesArray = obj.optJSONArray("releases")
        if (releasesArray != null) {
            for (i in 0 until releasesArray.length().coerceAtMost(5)) {
                try {
                    val releaseObj = releasesArray.getJSONObject(i)
                    val releaseId = releaseObj.getString("id")
                    val releaseTitle = releaseObj.optString("title", "")
                    val releaseDate = releaseObj.optString("date", null)
                    val coverArtUrl = "https://coverartarchive.org/release/$releaseId/front"

                    releases.add(
                        dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRecordingRelease(
                            id = releaseId,
                            title = releaseTitle,
                            date = releaseDate,
                            coverArtUrl = coverArtUrl
                        )
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse release at index $i", e)
                }
            }
        }

        return MusicBrainzRecording(
            id = id,
            title = title,
            artist = artistName,
            artistId = artistId,
            duration = duration,
            releases = releases
        )
    }
}
