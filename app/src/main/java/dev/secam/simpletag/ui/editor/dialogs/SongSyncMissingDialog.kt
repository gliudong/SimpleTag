

package dev.secam.simpletag.ui.editor.dialogs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.secam.simpletag.R
import dev.secam.simpletag.ui.components.SimpleDialog
import dev.secam.simpletag.ui.components.SimpleDialogOptions

@Composable
fun SongSyncMissingDialog(
    onDismiss: () -> Unit
){
    SimpleDialog(
        title = stringResource(R.string.song_sync_missing),
        manualPadding = false,
        onDismiss = onDismiss
    ) {
        Text(
            text = stringResource(R.string.song_sync_long)
        )
        SimpleDialogOptions(
            option = stringResource(R.string.dialog_ok),
            action = onDismiss,
        )
    }
}