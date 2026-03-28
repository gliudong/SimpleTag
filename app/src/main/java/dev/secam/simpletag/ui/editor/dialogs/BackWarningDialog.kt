
package dev.secam.simpletag.ui.editor.dialogs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.secam.simpletag.R
import dev.secam.simpletag.ui.components.SimpleDialog
import dev.secam.simpletag.ui.components.SimpleDialogOptions

@Composable
fun BackWarningDialog(onCancel: () -> Unit, onLeave: () -> Unit) {
    SimpleDialog(
        title = stringResource(R.string.unsaved_changes),
        onDismiss = onCancel
    ) {
        Text(
            text = stringResource(R.string.unsaved_changes_long)
        )
        SimpleDialogOptions(
            option1 = stringResource(R.string.dialog_cancel),
            option2 = stringResource(R.string.dialog_leave),
            action1 = onCancel,
            action2 = onLeave
        )
    }
}

@Preview
@Composable
fun BackWarnPrev(){
    BackWarningDialog({}) { }
}