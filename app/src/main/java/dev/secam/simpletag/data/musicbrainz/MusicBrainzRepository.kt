package dev.secam.simpletag.data.musicbrainz

import android.util.Xml
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzException
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRelease
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzResult
import dev.secam.simpletag.data.musicbrainz.models.ErrorType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.ResponseBody
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.TimeUnit
import kotlin.math.min

/**
 * Repository for MusicBrainz API operations
 * Features:
 * - Rate limiting (1 request/second per MusicBrainz policy)
 * - In-memory caching with LruCache (max 50 entries)
 * - XML parsing for API responses
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
     * @param title Track title (required)
     * @param artist Artist name (optional)
     * @param album Album name (optional)
     * @param track Track number (optional)
     * @return MusicBrainzResult with list of releases or error
     */
    suspend fun searchReleases(
        title: String,
        artist: String? = null,
        album: String? = null,
        track: Int? = null
    ): MusicBrainzResult<List<MusicBrainzRelease>> {
        // Build query
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
                format = "xml"
            )

            val releases = parseReleasesResponse(response.string())

            if (releases.isEmpty()) {
                MusicBrainzResult.NoResults
            } else {
                // Cache the results
                cache.put(cacheKey, releases)
                MusicBrainzResult.Success(releases)
            }
        } catch (e: Exception) {
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
                e.message?.contains("XML", ignoreCase = true) == true -> {
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
     * Build MusicBrainz query string from parameters
     */
    private fun buildQueryString(
        title: String,
        artist: String? = null,
        album: String? = null,
        track: Int? = null
    ): String {
        val parts = mutableListOf<String>()

        // Add artist if available
        artist?.let {
            parts.add("artist:\"$it\"")
        }

        // Add album if available
        album?.let {
            parts.add("release:\"$it\"")
        }

        // Add title (required)
        parts.add("recording:\"$title\"")

        // Add track number if available
        track?.let {
            parts.add("number:$it")
        }

        return parts.joinToString(" AND ")
    }

    /**
     * Generate cache key from query string
     */
    private fun getCacheKey(query: String): String {
        return query.lowercase().trim()
    }

    /**
     * Parse MusicBrainz XML response for releases
     */
    private fun parseReleasesResponse(xmlString: String): List<MusicBrainzRelease> {
        val releases = mutableListOf<MusicBrainzRelease>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xmlString))

        var currentRelease: MutableMap<String, String>? = null
        var currentArtist: MutableMap<String, String>? = null
        var currentText: String? = null
        var inReleaseList = false
        var inRelease = false
        var inArtistCredit = false
        var inNameCredit = false
        var inArtist = false
        var inMediumList = false
        var inMedium = false
        var inTrackList = false
        var inTrack = false
        var currentTrack: MutableMap<String, String>? = null
        val tracks = mutableListOf<MutableMap<String, String>>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "release-list" -> inReleaseList = true
                        "release" -> {
                            inRelease = true
                            currentRelease = mutableMapOf()
                            // Get MBID from id attribute
                            parser.getAttributeValue(null, "id")?.let {
                                currentRelease?.set("id", it)
                            }
                        }
                        "artist-credit" -> inArtistCredit = true
                        "name-credit" -> inNameCredit = true
                        "artist" -> {
                            if (inRelease) {
                                inArtist = true
                                currentArtist = mutableMapOf()
                                parser.getAttributeValue(null, "id")?.let {
                                    currentArtist?.set("id", it)
                                }
                            }
                        }
                        "medium-list" -> inMediumList = true
                        "medium" -> inMedium = true
                        "track-list" -> inTrackList = true
                        "track" -> {
                            if (inMedium) {
                                inTrack = true
                                currentTrack = mutableMapOf()
                                parser.getAttributeValue(null, "id")?.let {
                                    currentTrack?.set("id", it)
                                }
                            }
                        }
                        "number", "position", "length", "title", "name",
                        "date", "country", "label", "catalog-number",
                        "barcode", "asin", "status", "type" -> {
                            // These tags contain text content
                            currentText = null
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "release-list" -> inReleaseList = false
                        "release" -> {
                            inRelease = false
                            currentRelease?.let { releaseMap ->
                                val release = MusicBrainzRelease(
                                    id = releaseMap["id"] ?: "",
                                    title = releaseMap["title"] ?: "",
                                    artist = releaseMap["artist"] ?: "",
                                    artistId = releaseMap["artistId"],
                                    album = releaseMap["title"] ?: "",
                                    date = releaseMap["date"],
                                    year = releaseMap["date"]?.substringBefore("-"),
                                    country = releaseMap["country"],
                                    label = releaseMap["label"],
                                    catalogNumber = releaseMap["catalog-number"],
                                    trackCount = tracks.size,
                                    tracks = tracks.map { trackMap ->
                                        dev.secam.simpletag.data.musicbrainz.models.MusicBrainzTrack(
                                            id = trackMap["id"] ?: "",
                                            title = trackMap["title"] ?: "",
                                            number = trackMap["number"]?.toIntOrNull() ?: 0,
                                            duration = trackMap["length"]?.toIntOrNull()
                                        )
                                    },
                                    coverArtUrl = releaseMap["id"]?.let { mbid ->
                                        "https://coverartarchive.org/release/$mbid/front"
                                    },
                                    barcode = releaseMap["barcode"],
                                    asin = releaseMap["asin"],
                                    releaseStatus = releaseMap["status"],
                                    releaseType = releaseMap["type"]
                                )
                                releases.add(release)
                            }
                            currentRelease = null
                            tracks.clear()
                        }
                        "artist-credit" -> inArtistCredit = false
                        "name-credit" -> inNameCredit = false
                        "artist" -> {
                            if (inRelease) {
                                inArtist = false
                                currentArtist = null
                            }
                        }
                        "medium-list" -> inMediumList = false
                        "medium" -> inMedium = false
                        "track-list" -> inTrackList = false
                        "track" -> {
                            if (inMedium) {
                                inTrack = false
                                currentTrack?.let { tracks.add(it) }
                                currentTrack = null
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    currentText = parser.text
                }
            }
            eventType = parser.next()
        }

        return releases
    }

    /**
     * Clear the cache
     */
    fun clearCache() {
        cache.evictAll()
    }
}
