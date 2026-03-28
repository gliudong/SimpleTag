
package dev.secam.simpletag.ui.editor.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.secam.simpletag.R
import dev.secam.simpletag.ui.editor.BatchAutoEditState
import dev.secam.simpletag.ui.editor.BatchFileStatus
import dev.secam.simpletag.ui.editor.ProcessingOperation

/**
 * Dialog for batch auto-edit operations
 * Shows progress during processing and summary when complete
 */
@Composable
fun BatchAutoEditDialog(
    state: BatchAutoEditState,
    onCancel: () -> Unit,
    onApply: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = { /* Cannot dismiss by clicking outside */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state) {
                    is BatchAutoEditState.Processing -> {
                        ProcessingContent(
                            current = state.current,
                            total = state.total,
                            currentFileName = state.currentFileName,
                            currentOperation = state.currentOperation,
                            onCancel = onCancel
                        )
                    }
                    is BatchAutoEditState.Completed -> {
                        CompletedContent(
                            successCount = state.successCount,
                            failureCount = state.failureCount,
                            skippedCount = state.skippedCount,
                            onApply = onApply ?: {},
                            onCancel = onCancel
                        )
                    }
                    else -> {
                        // Idle, Reviewing, or Cancelled - don't show dialog
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessingContent(
    current: Int,
    total: Int,
    currentFileName: String,
    currentOperation: ProcessingOperation = ProcessingOperation.Searching,
    onCancel: () -> Unit
) {
    Text(
        text = stringResource(R.string.batch_auto_edit_processing, current, total),
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(modifier = Modifier.height(16.dp))

    LinearProgressIndicator(
        modifier = Modifier.fillMaxWidth(),
        progress = { current.toFloat() / total.toFloat() }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Current operation indicator
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val (operationText, showSpinner) = when (currentOperation) {
            ProcessingOperation.Searching -> {
                stringResource(R.string.batch_operation_searching) to true
            }
            ProcessingOperation.Applying -> {
                stringResource(R.string.batch_operation_applying) to true
            }
            ProcessingOperation.Done -> {
                stringResource(R.string.batch_operation_done) to false
            }
        }

        if (showSpinner) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Text(
            text = operationText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = currentFileName,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onCancel,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.dialog_cancel))
    }
}

@Composable
private fun CompletedContent(
    successCount: Int,
    failureCount: Int,
    skippedCount: Int,
    onApply: () -> Unit,
    onCancel: () -> Unit
) {
    Text(
        text = stringResource(R.string.batch_auto_edit_complete),
        style = MaterialTheme.typography.titleMedium
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Summary counts
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SummaryItem(
            label = stringResource(R.string.batch_auto_edit_success),
            count = successCount,
            color = MaterialTheme.colorScheme.primary
        )

        SummaryItem(
            label = stringResource(R.string.batch_auto_edit_failed),
            count = failureCount,
            color = MaterialTheme.colorScheme.error
        )

        SummaryItem(
            label = stringResource(R.string.batch_auto_edit_skipped),
            count = skippedCount,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Button(onClick = onCancel) {
            Text(stringResource(R.string.dialog_cancel))
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onApply,
            enabled = successCount > 0
        ) {
            Text(stringResource(R.string.batch_auto_edit_apply))
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    count: Int,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
