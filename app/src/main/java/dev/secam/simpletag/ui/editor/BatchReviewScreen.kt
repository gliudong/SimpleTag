
package dev.secam.simpletag.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.util.Log
import dev.secam.simpletag.R
import dev.secam.simpletag.ui.editor.components.BatchReviewFileItem

/**
 * Review screen for batch auto-edit results
 * Shows all files with their matched metadata and allows selective application
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchReviewScreen(
    results: List<BatchFileResult>,
    selectedIds: Set<Long>,
    onSelectionToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onApplySelected: () -> Unit,
    onApplyAll: () -> Unit,
    onCancel: () -> Unit,
    onRetryFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val failedFiles = results.filter { it.status is BatchFileStatus.Failed }
    val skippedFiles = results.filter { it.status is BatchFileStatus.Skipped }
    val successfulFiles = results.filter { it.status is BatchFileStatus.Success }
    val selectedCount = selectedIds.size

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.batch_review_title),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                R.string.batch_review_subtitle,
                                selectedCount,
                                successfulFiles.size
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_24px),
                            contentDescription = stringResource(R.string.cd_back_button)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp
            ) {
                ActionBottomBarContent(
                    selectedCount = selectedCount,
                    successCount = successfulFiles.size,
                    onApplySelected = onApplySelected,
                    onApplyAll = onApplyAll
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Selection controls
            SelectionControlBar(
                selectedCount = selectedCount,
                totalCount = successfulFiles.size,
                onSelectAll = onSelectAll,
                onDeselectAll = onDeselectAll,
                modifier = Modifier.fillMaxWidth()
            )

            // File list
            if (results.isEmpty()) {
                EmptyResultsState(onCancel = onCancel)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Failed files section (if any)
                    if (failedFiles.isNotEmpty()) {
                        item {
                            FailedSectionHeader(
                                count = failedFiles.size,
                                onRetryFailed = onRetryFailed
                            )
                        }
                        items(
                            items = failedFiles,
                            key = { it.musicData.id }
                        ) { result ->
                            BatchReviewFileItem(
                                result = result,
                                isSelected = false,
                                isSelectable = false,
                                onSelectionToggle = { },
                                onViewDetails = { }
                            )
                        }
                    }

                    // Skipped files section (if any)
                    if (skippedFiles.isNotEmpty()) {
                        item {
                            SkippedSectionHeader(count = skippedFiles.size)
                        }
                        items(
                            items = skippedFiles,
                            key = { it.musicData.id }
                        ) { result ->
                            BatchReviewFileItem(
                                result = result,
                                isSelected = false,
                                isSelectable = false,
                                onSelectionToggle = { },
                                onViewDetails = { }
                            )
                        }
                    }

                    // Successful files section
                    if (successfulFiles.isNotEmpty()) {
                        item {
                            SuccessfulSectionHeader(count = successfulFiles.size)
                        }
                        items(
                            items = successfulFiles,
                            key = { it.musicData.id }
                        ) { result ->
                            BatchReviewFileItem(
                                result = result,
                                isSelected = result.musicData.id in selectedIds,
                                isSelectable = true,
                                onSelectionToggle = { onSelectionToggle(result.musicData.id) },
                                onViewDetails = { }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionControlBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$selectedCount of $totalCount selected",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onSelectAll, enabled = selectedCount < totalCount) {
                    Text(stringResource(R.string.batch_review_select_all))
                }
                Button(
                    onClick = onDeselectAll,
                    enabled = selectedCount > 0
                ) {
                    Text(stringResource(R.string.batch_review_deselect_all))
                }
            }
        }
    }
}

@Composable
private fun FailedSectionHeader(
    count: Int,
    onRetryFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = stringResource(R.string.batch_review_failed_files, count),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(onClick = onRetryFailed) {
                Text(stringResource(R.string.batch_review_retry_failed))
            }
        }
    }
}

@Composable
private fun SkippedSectionHeader(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.batch_auto_edit_skipped, count),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SuccessfulSectionHeader(
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_check_24px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.batch_review_successful_files, count),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ActionBottomBarContent(
    selectedCount: Int,
    successCount: Int,
    onApplySelected: () -> Unit,
    onApplyAll: () -> Unit
) {
    Log.d("BatchReviewScreen", "ActionBottomBarContent: selectedCount=$selectedCount, successCount=$successCount")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (successCount > 1) {
            Log.d("BatchReviewScreen", "Showing two buttons (successCount=$successCount)")
            Button(
                onClick = {
                    Log.d("BatchReviewScreen", "Apply All clicked!")
                    onApplyAll()
                },
                enabled = successCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.batch_review_apply_all, successCount))
            }

            VerticalDivider(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    Log.d("BatchReviewScreen", "Apply Selected clicked!")
                    onApplySelected()
                },
                enabled = selectedCount > 0,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.batch_review_apply_selected, selectedCount))
            }
        } else {
            Log.d("BatchReviewScreen", "Showing single button (successCount=$successCount)")
            // Single file or all selected
            Button(
                onClick = {
                    Log.d("BatchReviewScreen", "Apply button clicked! selectedCount=$selectedCount, successCount=$successCount")
                    if (selectedCount == successCount) {
                        Log.d("BatchReviewScreen", "Calling onApplyAll")
                        onApplyAll()
                    } else {
                        Log.d("BatchReviewScreen", "Calling onApplySelected")
                        onApplySelected()
                    }
                },
                enabled = selectedCount > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.batch_review_apply_selected, selectedCount))
            }
        }
    }
}

@Composable
private fun EmptyResultsState(
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.batch_review_no_successful_files),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(onClick = onCancel) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    }
}
