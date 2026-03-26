package dev.secam.simpletag.data.musicbrainz

import dev.secam.simpletag.data.enums.SimpleTagField
import dev.secam.simpletag.data.musicbrainz.models.MusicBrainzRelease

/**
 * Maps MusicBrainz API responses to SimpleTagField format
 * This mapper converts MusicBrainz release data to a map of SimpleTagField to String values
 * that can be applied to the editor
 */
object MusicBrainzMapper {

    /**
     * Map a MusicBrainz release to a map of SimpleTagField values
     * @param release The MusicBrainz release to map
     * @return Map of SimpleTagField to String values
     */
    fun mapToFieldStates(release: MusicBrainzRelease): Map<SimpleTagField, String> {
        val fieldMap = mutableMapOf<SimpleTagField, String>()

        // Basic fields
        fieldMap[SimpleTagField.Title] = release.title
        fieldMap[SimpleTagField.Artist] = release.artist
        fieldMap[SimpleTagField.Album] = release.album

        release.date?.let { date ->
            fieldMap[SimpleTagField.Year] = date.substringBefore("-")
        }

        release.year?.let { year ->
            fieldMap[SimpleTagField.Year] = year
        }

        release.country?.let { country ->
            fieldMap[SimpleTagField.Country] = country
        }

        release.label?.let { label ->
            fieldMap[SimpleTagField.Label] = label
        }

        release.catalogNumber?.let { catalogNumber ->
            fieldMap[SimpleTagField.CatalogNumber] = catalogNumber
        }

        release.barcode?.let { barcode ->
            fieldMap[SimpleTagField.Barcode] = barcode
        }

        release.asin?.let { asin ->
            fieldMap[SimpleTagField.ASIN] = asin
        }

        release.releaseStatus?.let { status ->
            fieldMap[SimpleTagField.ReleaseStatus] = status
        }

        release.releaseType?.let { type ->
            fieldMap[SimpleTagField.ReleaseType] = type
        }

        // MusicBrainz IDs
        release.artistId?.let { artistId ->
            fieldMap[SimpleTagField.MusicBrainzArtistId] = artistId
        }

        fieldMap[SimpleTagField.MusicBrainzReleaseId] = release.id

        release.releaseGroupId?.let { releaseGroupId ->
            fieldMap[SimpleTagField.MusicBrainzReleaseGroupId] = releaseGroupId
        }

        // Map track information (use first track as reference)
        if (release.tracks.isNotEmpty()) {
            val firstTrack = release.tracks.first()
            fieldMap[SimpleTagField.Title] = firstTrack.title
            fieldMap[SimpleTagField.Track] = firstTrack.number.toString()

            firstTrack.artist?.let { trackArtist ->
                if (trackArtist.isNotBlank()) {
                    fieldMap[SimpleTagField.Artist] = trackArtist
                }
            }

            firstTrack.artistId?.let { trackArtistId ->
                fieldMap[SimpleTagField.MusicBrainzArtistId] = trackArtistId
            }
        }

        return fieldMap
    }

    /**
     * Get the primary artist name from a release
     */
    fun getArtistName(release: MusicBrainzRelease): String {
        return release.artist
    }

    /**
     * Get the album title from a release
     */
    fun getAlbumTitle(release: MusicBrainzRelease): String {
        return release.album
    }

    /**
     * Get the release year from a release
     */
    fun getReleaseYear(release: MusicBrainzRelease): String? {
        return release.year ?: release.date?.substringBefore("-")
    }
}
