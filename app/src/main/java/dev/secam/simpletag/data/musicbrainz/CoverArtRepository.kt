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

import android.util.Log
import dev.secam.simpletag.data.musicbrainz.models.CoverArtArchiveResponse
import dev.secam.simpletag.data.musicbrainz.models.CoverArtImage
import dev.secam.simpletag.data.musicbrainz.models.CoverArtInfo
import dev.secam.simpletag.data.musicbrainz.models.CoverArtQuality
import dev.secam.simpletag.data.musicbrainz.models.CoverArtResult
import dev.secam.simpletag.data.musicbrainz.models.SelectedCoverArt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException

/**
 * Repository for Cover Art Archive API operations
 * Features:
 * - Smart quality selection (500px > 250px > original)
 * - Pre-check optimization using MusicBrainz cover-art-archive info
 * - Multi-level fallback (release -> release group)
 * - LRU cache for cover art metadata (max 30 entries)
 * - Coordinated rate limiting with MusicBrainzRepository
 */
class CoverArtRepository(
    private val apiService: CoverArtArchiveApiService
) {
    private val cache = android.util.LruCache<String, CoverArtArchiveResponse>(30)

    companion object {
        private const val TAG = "CoverArtRepo"
    }

    /**
     * Pre-check: Determine if we should fetch cover art based on MusicBrainz info
     * @return true if front cover is available, false otherwise
     */
    fun shouldFetchCoverArt(coverArtInfo: CoverArtInfo?): Boolean {
        return coverArtInfo?.front == true
    }

    /**
     * Get cover art with intelligent fallback strategy
     *
     * Strategy:
     * 0. Pre-check using coverArtInfo (skip if no front cover)
     * 1. Try release cover art (with quality priority: 500px > 250px > original)
     * 2. Fallback to release group cover art
     *
     * @param releaseId MusicBrainz Release ID
     * @param releaseGroupId MusicBrainz Release Group ID (for fallback)
     * @param coverArtInfo Cover art availability info from MusicBrainz (for pre-check)
     * @return CoverArtResult with selected cover art or error
     */
    suspend fun getReleaseCoverArtWithFallback(
        releaseId: String,
        releaseGroupId: String? = null,
        coverArtInfo: CoverArtInfo? = null
    ): CoverArtResult {
        // 0. Pre-check: Skip API call if no front cover available
        if (!shouldFetchCoverArt(coverArtInfo)) {
            Log.d(TAG, "No front cover available (cover-art-archive.front=false), skipping release fetch")
            // Directly try release group fallback
            if (!releaseGroupId.isNullOrBlank()) {
                return getReleaseGroupCoverArt(releaseGroupId)
            }
            return CoverArtResult.NoCoverArt
        }

        // 1. Try release cover art
        Log.d(TAG, "Fetching release cover art for release=$releaseId")
        val releaseResult = getReleaseCoverArt(releaseId)
        when (releaseResult) {
            is CoverArtResult.Success -> {
                Log.d(TAG, "Successfully fetched release cover art: ${releaseResult.coverArt.url}")
                return releaseResult
            }
            is CoverArtResult.PartialSuccess -> {
                Log.d(TAG, "Partially fetched release cover art: ${releaseResult.coverArt.url}")
                return releaseResult
            }
            is CoverArtResult.NoCoverArt -> {
                Log.d(TAG, "No cover art found for release, trying fallback")
            }
            is CoverArtResult.Error -> {
                Log.w(TAG, "Error fetching release cover art: ${releaseResult.message}, trying fallback")
            }
        }

        // 2. Fallback to release group
        if (!releaseGroupId.isNullOrBlank()) {
            Log.d(TAG, "Fetching release group cover art for release-group=$releaseGroupId")
            return getReleaseGroupCoverArt(releaseGroupId)
        }

        return releaseResult
    }

    /**
     * Get cover art for a specific release
     */
    suspend fun getReleaseCoverArt(releaseId: String): CoverArtResult {
        val cacheKey = "release:$releaseId"

        // Check cache first
        cache.get(cacheKey)?.let { cachedResponse ->
            Log.d(TAG, "Cache hit for release=$releaseId")
            return selectBestCoverArt(cachedResponse)
        }

        return try {
            val response = apiService.getReleaseCoverArt(releaseId)
            cache.put(cacheKey, response)
            selectBestCoverArt(response)
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error fetching release cover art", e)
            when (e.code()) {
                404 -> CoverArtResult.NoCoverArt
                503 -> CoverArtResult.Error("Service unavailable", e)
                else -> CoverArtResult.Error("HTTP ${e.code()}: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching release cover art", e)
            CoverArtResult.Error("Network error: ${e.message}", e)
        }
    }

    /**
     * Get cover art for a release group (fallback)
     */
    suspend fun getReleaseGroupCoverArt(releaseGroupId: String): CoverArtResult {
        val cacheKey = "release-group:$releaseGroupId"

        // Check cache first
        cache.get(cacheKey)?.let { cachedResponse ->
            Log.d(TAG, "Cache hit for release-group=$releaseGroupId")
            val result = selectBestCoverArt(cachedResponse)
            // Add fallback message if successful
            return when (result) {
                is CoverArtResult.Success -> CoverArtResult.PartialSuccess(
                    result.coverArt,
                    "Using release group cover art"
                )
                else -> result
            }
        }

        return try {
            val response = apiService.getReleaseGroupCoverArt(releaseGroupId)
            cache.put(cacheKey, response)
            val result = selectBestCoverArt(response)
            // Add fallback message if successful
            when (result) {
                is CoverArtResult.Success -> CoverArtResult.PartialSuccess(
                    result.coverArt,
                    "Using release group cover art"
                )
                else -> result
            }
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP error fetching release group cover art", e)
            when (e.code()) {
                404 -> CoverArtResult.NoCoverArt
                503 -> CoverArtResult.Error("Service unavailable", e)
                else -> CoverArtResult.Error("HTTP ${e.code()}: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching release group cover art", e)
            CoverArtResult.Error("Network error: ${e.message}", e)
        }
    }

    /**
     * Select the best cover art from the response using smart quality selection
     *
     * Priority:
     * 1. Front image with 500px thumbnail (large)
     * 2. Front image with 250px thumbnail (small)
     * 3. Front image (original)
     * 4. Approved image with 500px thumbnail
     * 5. Approved image with 250px thumbnail
     * 6. Approved image (original)
     *
     * @return CoverArtResult with the best available cover art
     */
    private fun selectBestCoverArt(response: CoverArtArchiveResponse): CoverArtResult {
        if (response.images.isEmpty()) {
            return CoverArtResult.NoCoverArt
        }

        // Group images by type and approval status
        val frontImages = response.images.filter { it.front }
        val approvedImages = response.images.filter { it.approved }

        // Try to select from front images first
        if (frontImages.isNotEmpty()) {
            val selection = selectBestImageFromList(frontImages)
            if (selection != null) {
                return CoverArtResult.Success(selection)
            }
        }

        // Fallback to approved images
        if (approvedImages.isNotEmpty()) {
            val selection = selectBestImageFromList(approvedImages)
            if (selection != null) {
                return CoverArtResult.PartialSuccess(
                    selection,
                    "No front cover available, using side/back cover"
                )
            }
        }

        // Last resort: use any image
        val selection = selectBestImageFromList(response.images)
        return if (selection != null) {
            CoverArtResult.PartialSuccess(
                selection,
                "Using unapproved cover art"
            )
        } else {
            CoverArtResult.NoCoverArt
        }
    }

    /**
     * Select the best image from a list based on quality priority
     * Priority: large (500px) > small (250px) > 1200px > original
     */
    private fun selectBestImageFromList(images: List<CoverArtImage>): SelectedCoverArt? {
        if (images.isEmpty()) return null

        // Use the first image in the list (Cover Art Archive returns them in preferred order)
        val image = images.first()
        val type = image.types.firstOrNull() ?: "Cover"

        // Priority: large (500px) > small (250px) > 1200px > original
        return when {
            image.thumbnails?.large != null -> SelectedCoverArt(
                url = image.thumbnails.large,
                quality = CoverArtQuality.MEDIUM,
                type = type
            )
            image.thumbnails?.small != null -> SelectedCoverArt(
                url = image.thumbnails.small,
                quality = CoverArtQuality.LOW,
                type = type
            )
            image.thumbnails?.large1200 != null -> SelectedCoverArt(
                url = image.thumbnails.large1200,
                quality = CoverArtQuality.ORIGINAL,
                type = type
            )
            image.image.isNotEmpty() -> SelectedCoverArt(
                url = image.image,
                quality = CoverArtQuality.ORIGINAL,
                type = type
            )
            else -> null
        }
    }

    /**
     * Clear the cover art cache
     */
    fun clearCache() {
        cache.evictAll()
    }
}
