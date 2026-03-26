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

        // Release-level fields
        fieldMap[SimpleTagField.Album] = release.title
        fieldMap[SimpleTagField.AlbumArtist] = release.artist

        // Year
        release.year?.let { fieldMap[SimpleTagField.Year] = it }

        // Track-level fields override release-level
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
        } else {
            // No tracks: use release title as title, release artist as artist
            fieldMap[SimpleTagField.Title] = release.title
            fieldMap[SimpleTagField.Artist] = release.artist
        }

        // Metadata fields
        release.country?.let { fieldMap[SimpleTagField.Country] = it }
        release.label?.let { fieldMap[SimpleTagField.Label] = it }
        release.catalogNumber?.let { fieldMap[SimpleTagField.CatalogNumber] = it }
        release.barcode?.let { fieldMap[SimpleTagField.Barcode] = it }
        release.asin?.let { fieldMap[SimpleTagField.ASIN] = it }
        release.releaseStatus?.let { fieldMap[SimpleTagField.ReleaseStatus] = it }
        release.releaseType?.let { fieldMap[SimpleTagField.ReleaseType] = it }

        // MusicBrainz IDs
        release.artistId?.let { fieldMap[SimpleTagField.MusicBrainzReleaseArtistId] = it }
        fieldMap[SimpleTagField.MusicBrainzReleaseId] = release.id
        release.releaseGroupId?.let { fieldMap[SimpleTagField.MusicBrainzReleaseGroupId] = it }

        return fieldMap
    }
}
