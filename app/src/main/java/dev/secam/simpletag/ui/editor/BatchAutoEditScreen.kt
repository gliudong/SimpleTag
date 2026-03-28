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

package dev.secam.simpletag.ui.editor

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import dev.secam.simpletag.R
import dev.secam.simpletag.data.media.MusicData
import dev.secam.simpletag.ui.components.SimpleDialog
import dev.secam.simpletag.ui.components.SimpleDialogOptions
import dev.secam.simpletag.ui.editor.dialogs.BatchAutoEditDialog
import kotlinx.coroutines.launch

/**
 * Screen for batch auto-edit operations
 * Automatically matches multiple files using their filenames and MusicBrainz
 */
@Composable
fun BatchAutoEditScreen(
    musicList: List<MusicData>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BatchAutoEditViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()
    var showCancelConfirm by remember { mutableStateOf(false) }
    var applyInProgress by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }

    // Start processing when screen loads (only once)
    LaunchedEffect(musicList) {
        if (!hasStarted) {
            hasStarted = true
            viewModel.startBatchAutoEdit(musicList)
        }
    }

    // Handle back button with confirmation
    BackHandler(enabled = !applyInProgress) {
        when (uiState) {
            is BatchAutoEditState.Processing -> {
                showCancelConfirm = true
            }
            else -> {
                onNavigateBack()
            }
        }
    }

    // Show dialog based on state
    when (val state = uiState) {
        is BatchAutoEditState.Processing, is BatchAutoEditState.Completed -> {
            BatchAutoEditDialog(
                state = state,
                onCancel = {
                    when (state) {
                        is BatchAutoEditState.Processing -> {
                            showCancelConfirm = true
                        }
                        else -> {
                            onNavigateBack()
                        }
                    }
                },
                onApply = {
                    if (state is BatchAutoEditState.Completed && !applyInProgress) {
                        applyInProgress = true
                        coroutineScope.launch {
                            viewModel.applyBatchResults(
                                context = context,
                                results = state.results
                            ) { success, failed ->
                                applyInProgress = false
                                coroutineScope.launch {
                                    val message = context.getString(
                                        R.string.tag_written,
                                        "$success " + context.getString(R.string.dialog_ok)
                                    )
                                    snackbarHostState.showSnackbar(
                                        message = message,
                                        duration = SnackbarDuration.Short
                                    )
                                }
                                onNavigateBack()
                            }
                        }
                    }
                }
            )
        }
        is BatchAutoEditState.Cancelled -> {
            // Navigate back when cancelled
            LaunchedEffect(Unit) {
                onNavigateBack()
            }
        }
        is BatchAutoEditState.Idle -> {
            // Don't show anything for Idle state - processing will start soon
        }
    }

    // Cancel confirmation dialog
    if (showCancelConfirm) {
        SimpleDialog(
            title = stringResource(R.string.batch_auto_edit_cancel_confirm),
            onDismiss = { showCancelConfirm = false }
        ) {
            Text(stringResource(R.string.batch_auto_edit_cancel_message))
            SimpleDialogOptions(
                option1 = stringResource(R.string.dialog_cancel),
                option2 = stringResource(R.string.dialog_confirm),
                action1 = { showCancelConfirm = false },
                action2 = {
                    showCancelConfirm = false
                    viewModel.cancelBatch()
                    onNavigateBack()
                }
            )
        }
    }
}
