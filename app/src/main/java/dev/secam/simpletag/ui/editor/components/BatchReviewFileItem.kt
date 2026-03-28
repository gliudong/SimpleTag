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

package dev.secam.simpletag.ui.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.secam.simpletag.R
import dev.secam.simpletag.data.enums.SimpleTagField
import dev.secam.simpletag.data.media.MusicData
import dev.secam.simpletag.ui.editor.BatchFileResult
import dev.secam.simpletag.ui.editor.BatchFileStatus
import dev.secam.simpletag.ui.selector.components.SimpleAlbumArtwork

/**
 * File item for batch review screen
 * Shows file info, artwork, changed fields preview, and selection checkbox
 */
@Composable
fun BatchReviewFileItem(
    result: BatchFileResult,
    isSelected: Boolean,
    isSelectable: Boolean,
    onSelectionToggle: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected && isSelectable) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            when (result.status) {
                is BatchFileStatus.Success -> MaterialTheme.colorScheme.surface
                is BatchFileStatus.Failed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                is BatchFileStatus.Skipped -> MaterialTheme.colorScheme.surfaceContainer
            }
        },
        animationSpec = tween(150),
        label = "containerColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelectable) {
                    Modifier.clickable(onClick = onSelectionToggle)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Main row with artwork, info, and checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (result.status) {
                        is BatchFileStatus.Success -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_check_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        is BatchFileStatus.Failed -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_close_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        is BatchFileStatus.Skipped -> {
                            Icon(
                                painter = painterResource(R.drawable.ic_info_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // File info column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = result.musicData.getFileName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = result.musicData.artist ?: "Unknown Artist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Show error message if failed
                    if (result.status is BatchFileStatus.Failed) {
                        Text(
                            text = result.status.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else if (result.status is BatchFileStatus.Skipped) {
                        Text(
                            text = result.error ?: "Skipped",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Checkbox for selectable items
                if (isSelectable) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSelectionToggle() }
                    )
                } else if (result.status is BatchFileStatus.Success && result.appliedFields != null) {
                    // View details button for successful items
                    IconButton(onClick = onViewDetails) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_upward_24px),
                            contentDescription = "View details"
                        )
                    }
                }
            }

            // Field preview for successful items
            if (result.status is BatchFileStatus.Success && result.appliedFields != null) {
                var expanded by remember { mutableStateOf(false) }

                if (!expanded) {
                    // Show preview of key fields
                    Spacer(modifier = Modifier.padding(4.dp))
                    FieldPreviewRow(
                        appliedFields = result.appliedFields,
                        originalData = result.musicData
                    )
                }

                // Expandable details
                if (expanded) {
                    Spacer(modifier = Modifier.padding(8.dp))
                    FieldChangesDetails(
                        appliedFields = result.appliedFields,
                        originalData = result.musicData
                    )
                }

                // Expand/collapse button
                if (result.appliedFields.size > 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (expanded) "Show less" else "View all ${result.appliedFields.size} fields",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { expanded = !expanded }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview row showing 3 key fields
 */
@Composable
private fun FieldPreviewRow(
    appliedFields: Map<SimpleTagField, String>,
    originalData: MusicData
) {
    val keyFields = listOf(
        SimpleTagField.Title,
        SimpleTagField.Artist,
        SimpleTagField.Album
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keyFields.forEach { field ->
            appliedFields[field]?.let { newValue ->
                val oldValue = when (field) {
                    SimpleTagField.Title -> originalData.title
                    SimpleTagField.Artist -> originalData.artist
                    SimpleTagField.Album -> originalData.album
                    else -> null
                }

                val hasChanged = oldValue != newValue && oldValue?.isNotBlank() == true

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${field.name}: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = newValue,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (hasChanged) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (hasChanged) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

/**
 * Full field changes display
 */
@Composable
private fun FieldChangesDetails(
    appliedFields: Map<SimpleTagField, String>,
    originalData: MusicData
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Field Changes",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            appliedFields.forEach { (field, newValue) ->
                val oldValue = when (field) {
                    SimpleTagField.Title -> originalData.title
                    SimpleTagField.Artist -> originalData.artist
                    SimpleTagField.Album -> originalData.album
                    SimpleTagField.Track -> originalData.track?.toString()
                    // These fields are not in MusicData - would need to read from file
                    SimpleTagField.AlbumArtist,
                    SimpleTagField.Year,
                    SimpleTagField.Genre,
                    SimpleTagField.Composer,
                    SimpleTagField.DiscNumber,
                    SimpleTagField.Comment -> null
                    else -> null
                }

                val hasChanged = oldValue != newValue && oldValue?.isNotBlank() == true

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = field.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )

                    if (hasChanged && oldValue?.isNotBlank() == true) {
                        // Show before -> after
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(2f)
                        ) {
                            Text(
                                text = oldValue,
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
                        // New value only
                        Text(
                            text = newValue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(2f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get filename from path
 */
private fun MusicData.getFileName(): String {
    return path.substringAfterLast("/")
}
