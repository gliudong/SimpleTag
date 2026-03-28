
package dev.secam.simpletag.ui.editor.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.secam.simpletag.data.enums.SimpleTagField
import dev.secam.simpletag.data.media.MusicData

/**
 * Card showing field changes with expandable details
 * Shows 3 key fields collapsed, all fields expanded
 */
@Composable
fun FieldChangesCard(
    appliedFields: Map<SimpleTagField, String>,
    originalData: MusicData,
    expanded: Boolean,
    onExpandToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Key fields preview (always visible)
        KeyFieldsPreview(
            appliedFields = appliedFields,
            originalData = originalData
        )

        // Expandable full details
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            AllFieldsDetails(
                appliedFields = appliedFields,
                originalData = originalData,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Expand/collapse button (if more than 3 fields)
        if (appliedFields.size > 3) {
            ExpandToggleRow(
                expanded = expanded,
                fieldCount = appliedFields.size,
                onToggle = onExpandToggle
            )
        }
    }
}

/**
 * Preview of 3 key fields (Title, Artist, Album)
 */
@Composable
private fun KeyFieldsPreview(
    appliedFields: Map<SimpleTagField, String>,
    originalData: MusicData
) {
    val keyFields = listOf(
        SimpleTagField.Title to originalData.title,
        SimpleTagField.Artist to originalData.artist,
        SimpleTagField.Album to originalData.album
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keyFields.forEach { (field, oldValue) ->
            appliedFields[field]?.let { newValue ->
                val hasChanged = oldValue != newValue && oldValue?.isNotBlank() == true

                FieldChangeRow(
                    fieldName = field.name,
                    oldValue = oldValue,
                    newValue = newValue,
                    hasChanged = hasChanged
                )
            }
        }
    }
}

/**
 * All field changes with color coding
 */
@Composable
private fun AllFieldsDetails(
    appliedFields: Map<SimpleTagField, String>,
    originalData: MusicData,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "All Field Changes",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            appliedFields.forEach { (field, newValue) ->
                val oldValue = getOriginalValue(field, originalData)
                val hasChanged = oldValue != newValue && oldValue?.isNotBlank() == true

                FieldChangeRow(
                    fieldName = field.name,
                    oldValue = oldValue,
                    newValue = newValue,
                    hasChanged = hasChanged
                )
            }
        }
    }
}

/**
 * Row showing field name and value(s)
 */
@Composable
private fun FieldChangeRow(
    fieldName: String,
    oldValue: String?,
    newValue: String,
    hasChanged: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$fieldName:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )

        if (hasChanged) {
            // Show before -> after
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(0.6f)
            ) {
                Text(
                    text = oldValue ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "→",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = newValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // New value only (green for additions)
            Text(
                text = newValue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(0.6f)
            )
        }
    }
}

/**
 * Expand/collapse toggle row
 */
@Composable
private fun ExpandToggleRow(
    expanded: Boolean,
    fieldCount: Int,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = if (expanded) "Show less" else "View all $fieldCount fields",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(4.dp)
        )
    }
}

/**
 * Get original value from MusicData for a given field
 * Note: MusicData only contains basic fields (title, artist, album, track)
 * Other fields will return null as they are not loaded from MediaStore
 */
private fun getOriginalValue(field: SimpleTagField, data: MusicData): String? {
    return when (field) {
        SimpleTagField.Title -> data.title
        SimpleTagField.Artist -> data.artist
        SimpleTagField.Album -> data.album
        SimpleTagField.Track -> data.track?.toString()
        // These fields are not in MusicData - would need to read from file
        SimpleTagField.AlbumArtist,
        SimpleTagField.Year,
        SimpleTagField.Genre,
        SimpleTagField.Composer,
        SimpleTagField.DiscNumber,
        SimpleTagField.Comment -> null
        else -> null
    }
}
