
package dev.secam.simpletag.ui.editor.dialogs

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.secam.simpletag.R
import dev.secam.simpletag.ui.components.SimpleDialog
import dev.secam.simpletag.ui.components.SimpleDialogOptions

@Composable
fun SaveTagDialog(tagNum: Int = 1, onCancel: () -> Unit, onConfirm: () -> Unit) {
    SimpleDialog(
        title = stringResource(R.string.confirm_changes),
        onDismiss = onCancel
    ) {
        Text(
            text = stringResource(R.string.overwrite).replace("(#)",tagNum.toString())
        )
        SimpleDialogOptions(
            option1 = stringResource(R.string.dialog_cancel),
            option2 = stringResource(R.string.dialog_confirm),
            action1 = onCancel,
            action2 = onConfirm
        )
    }
}

//@Preview
//@Composable
//fun SaveTagPrev(){
//    SaveTagDialog({}) { }
//}