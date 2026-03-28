
package dev.secam.simpletag.util

/**
 * Result of parsing a filename to extract artist and title information
 */
data class FileNameParseResult(
    val artist: String?,
    val title: String
)

/**
 * Parser for extracting artist and title from audio file filenames
 * Supports common naming patterns like "Artist - Title.mp3", "01 - Artist - Title.flac", etc.
 */
object FileNameParser {

    /**
     * Parse a filename to extract artist and title information
     * @param fileName The filename (with or without extension) to parse
     * @return FileNameParseResult containing the extracted artist and title
     */
    fun parse(fileName: String): FileNameParseResult {
        // Remove file extension if present
        val nameWithoutExt = fileName.substringBeforeLast(".", "")

        // Pattern 1: Track number + Artist + Title (e.g., "01. Artist - Title" or "01-Artist_Title")
        val trackPattern = """^\s*(\d{1,3})\s*[.\-_]\s*(.+?)\s*[.\-_]\s*(.+)\s*$""".toRegex()
        val trackMatch = trackPattern.find(nameWithoutExt)
        if (trackMatch != null) {
            val artist = trackMatch.groupValues[2].trim()
            val title = trackMatch.groupValues[3].trim()
            return FileNameParseResult(
                artist = artist.ifBlank { null },
                title = title
            )
        }

        // Pattern 2: Split on " - " (with spaces, first occurrence only)
        val dashWithSpaceSplit = nameWithoutExt.split(" - ", limit = 2)
        if (dashWithSpaceSplit.size == 2) {
            val artist = dashWithSpaceSplit[0].trim()
            val title = dashWithSpaceSplit[1].trim()
            return FileNameParseResult(
                artist = artist.ifBlank { null },
                title = title
            )
        }

        // Pattern 3: Split on "-" (without spaces, first occurrence only)
        val dashSplit = nameWithoutExt.split("-", limit = 2)
        if (dashSplit.size == 2) {
            val artist = dashSplit[0].trim()
            val title = dashSplit[1].trim()
            return FileNameParseResult(
                artist = artist.ifBlank { null },
                title = title
            )
        }

        // Pattern 4: Split on "_" (if only one underscore)
        val underscoreSplit = nameWithoutExt.split("_", limit = 2)
        if (underscoreSplit.size == 2) {
            val artist = underscoreSplit[0].trim()
            val title = underscoreSplit[1].trim()
            return FileNameParseResult(
                artist = artist.ifBlank { null },
                title = title
            )
        }

        // Fallback: Entire filename as title, artist is null
        return FileNameParseResult(
            artist = null,
            title = nameWithoutExt.trim()
        )
    }

    /**
     * Extract track number from filename
     * @param fileName The filename to parse
     * @return Track number as Int, or null if not found
     */
    fun extractTrackNumber(fileName: String): Int? {
        // Remove file extension if present
        val nameWithoutExt = fileName.substringBeforeLast(".", "")

        // Pattern: Track number at the start (e.g., "01. Artist - Title" or "01-Artist_Title")
        val trackPattern = """^\s*(\d{1,3})\s*[.\-_]\s*""".toRegex()
        val trackMatch = trackPattern.find(nameWithoutExt)
        return trackMatch?.groupValues?.get(1)?.toIntOrNull()
    }
}
