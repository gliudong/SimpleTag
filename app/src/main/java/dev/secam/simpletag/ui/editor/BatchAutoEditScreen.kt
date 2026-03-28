
package dev.secam.simpletag.ui.editor

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import dev.secam.simpletag.util.rememberActivityResult
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
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Wrap everything in a Scaffold to properly show snackbars
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

    val uiState by viewModel.uiState.collectAsState()
    var showCancelConfirm by remember { mutableStateOf(false) }
    var applyInProgress by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }

    // String resources
    val onOkText = stringResource(R.string.dialog_ok)
    val onCancelText = stringResource(R.string.dialog_cancel)
    val onErrorText = stringResource(R.string.tag_written_error)
    val actionText = stringResource(R.string.log)

    // Permission request launcher for Android R+
    val getPermissionResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        rememberActivityResult(
            onResultCanceled = {
                Log.d("BatchAutoEditScreen", "Permission request cancelled")
                applyInProgress = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(onCancelText)
                }
            }
        ) {
            Log.d("BatchAutoEditScreen", "Permission request granted")
            coroutineScope.launch {
                // Permission granted, proceed with applying results
                // Check current state and get results to apply
                val currentState = uiState
                val resultsToApply = when (currentState) {
                    is BatchAutoEditState.Reviewing -> {
                        // In reviewing state, apply selected files
                        currentState.results.filter { it.musicData.id in currentState.selectedIds }
                    }
                    is BatchAutoEditState.Completed -> {
                        // In completed state, apply all successful results
                        currentState.results.filter { it.status is BatchFileStatus.Success }
                    }
                    else -> {
                        Log.e("BatchAutoEditScreen", "Unexpected state after permission granted: $currentState")
                        emptyList()
                    }
                }

                if (resultsToApply.isNotEmpty()) {
                    viewModel.applyBatchResults(
                        context = context,
                        results = resultsToApply
                    ) { success, failed ->
                        applyInProgress = false
                        launch {
                            Log.d("BatchAutoEditScreen", "Apply complete: success=$success, failed=$failed")
                            if (failed > 0) {
                                if (snackbarHostState.showSnackbar(
                                        "$onErrorText ($failed failed)",
                                        actionText
                                    ) == SnackbarResult.ActionPerformed) {
                                    // Could show log dialog here
                                }
                            } else {
                                snackbarHostState.showSnackbar(
                                    context.getString(R.string.tag_written, "$success $onOkText")
                                )
                            }
                            // Delay navigation to allow user to see the snackbar
                            kotlinx.coroutines.delay(1500)
                            onNavigateBack()
                        }
                    }
                } else {
                    Log.e("BatchAutoEditScreen", "No results to apply!")
                    applyInProgress = false
                }
            }
        }
    } else null

    // Function to apply batch results
    fun doApply(results: List<BatchFileResult>) {
        Log.d("BatchAutoEditScreen", "doApply called with ${results.size} results")
        Log.d("BatchAutoEditScreen", "Build.VERSION.SDK_INT = ${Build.VERSION.SDK_INT}")
        Log.d("BatchAutoEditScreen", "activity = $activity")
        Log.d("BatchAutoEditScreen", "getPermissionResult = $getPermissionResult")

        if (results.isEmpty()) {
            Log.e("BatchAutoEditScreen", "No results to apply!")
            coroutineScope.launch {
                snackbarHostState.showSnackbar("No files selected")
            }
            return
        }

        applyInProgress = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity != null) {
            Log.d("BatchAutoEditScreen", "Requesting write permission for Android R+")
            // Request write permission for Android R+
            val uris = ArrayList(results.map { result ->
                ContentUris.withAppendedId(
                    MediaStore.Audio.Media.getContentUri("external"),
                    result.musicData.id
                )
            })

            Log.d("BatchAutoEditScreen", "URIs: $uris")

            val request = androidx.activity.result.IntentSenderRequest.Builder(
                MediaStore.createWriteRequest(
                    context.contentResolver,
                    uris
                )
            ).build()

            Log.d("BatchAutoEditScreen", "Launching permission request")
            getPermissionResult?.launch(request)
            Log.d("BatchAutoEditScreen", "Permission request launched")
        } else {
            Log.d("BatchAutoEditScreen", "Using direct write (Android Q or below, or no activity)")
            // Direct write for Android Q and below
            coroutineScope.launch {
                viewModel.applyBatchResults(
                    context = context,
                    results = results
                ) { success, failed ->
                    applyInProgress = false
                    launch {
                        if (failed > 0) {
                            if (snackbarHostState.showSnackbar(
                                    "$onErrorText ($failed failed)",
                                    actionText
                                ) == SnackbarResult.ActionPerformed) {
                                // Could show log dialog here
                            }
                        } else {
                            snackbarHostState.showSnackbar(
                                context.getString(R.string.tag_written, "$success $onOkText")
                            )
                        }
                        // Delay navigation to allow user to see the snackbar
                        kotlinx.coroutines.delay(1500)
                        onNavigateBack()
                    }
                }
            }
        }
    }

    // Start processing when screen loads
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
        is BatchAutoEditState.Processing -> {
            BatchAutoEditDialog(
                state = state,
                onCancel = {
                    showCancelConfirm = true
                }
            )
        }
        is BatchAutoEditState.Reviewing -> {
            Log.d("BatchAutoEditScreen", "Reviewing state: ${state.results.size} results, ${state.selectedIds.size} selected")
            BatchReviewScreen(
                results = state.results,
                selectedIds = state.selectedIds,
                onSelectionToggle = {
                    Log.d("BatchAutoEditScreen", "Toggle selection for file: $it")
                    viewModel.toggleSelection(it)
                },
                onSelectAll = {
                    Log.d("BatchAutoEditScreen", "Select All clicked")
                    viewModel.selectAll()
                },
                onDeselectAll = {
                    Log.d("BatchAutoEditScreen", "Deselect All clicked")
                    viewModel.deselectAll()
                },
                onApplySelected = {
                    Log.d("BatchAutoEditScreen", "Apply Selected clicked")
                    Log.d("BatchAutoEditScreen", "Selected IDs: ${state.selectedIds}")
                    val selectedResults = state.results.filter { it.musicData.id in state.selectedIds }
                    Log.d("BatchAutoEditScreen", "Selected results count: ${selectedResults.size}")
                    doApply(selectedResults)
                },
                onApplyAll = {
                    Log.d("BatchAutoEditScreen", "Apply All clicked")
                    val successResults = state.results.filter { it.status is BatchFileStatus.Success }
                    Log.d("BatchAutoEditScreen", "Success results count: ${successResults.size}")
                    doApply(successResults)
                },
                onCancel = { onNavigateBack() },
                onRetryFailed = { viewModel.retryFailed() }
            )
        }
        is BatchAutoEditState.Completed -> {
            // Legacy Completed state - show dialog
            BatchAutoEditDialog(
                state = state,
                onCancel = { onNavigateBack() },
                onApply = {
                    if (!applyInProgress) {
                        doApply(state.results)
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
    } // End of Scaffold
}
