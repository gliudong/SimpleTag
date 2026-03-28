
package dev.secam.simpletag.ui.selector.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.secam.simpletag.R
import dev.secam.simpletag.ui.components.SimpleDialog
import dev.secam.simpletag.ui.components.SimpleDialogOptions
import dev.secam.simpletag.ui.components.ToggleDialogItem

@Composable
fun FilterDialog(
    hasTag: Boolean,
    onConfirm: (Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var toggleState by remember { mutableStateOf(hasTag) }
    SimpleDialog(
        title = stringResource(R.string.filter),
        onDismiss = onCancel,
        manualPadding = true
    ) {
        ToggleDialogItem (
            currentState = toggleState,
            headlineContent = stringResource(R.string.untagged_files_filter),
        ) {
            toggleState = !toggleState
        }
        SimpleDialogOptions(
            option1 = stringResource(R.string.dialog_cancel),
            option2 = stringResource(R.string.dialog_ok),
            action1 = { onCancel() },
            action2 = {
                onCancel()
                onConfirm(toggleState)
            },
            manualPadding = true
        )
    }
}

@Preview
@Composable
fun FilterPrev(){
    FilterDialog(
        hasTag = false,
        onConfirm = { }
    ) { }
}