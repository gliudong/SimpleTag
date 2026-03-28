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
}
