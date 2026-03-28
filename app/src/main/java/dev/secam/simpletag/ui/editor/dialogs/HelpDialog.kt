
package dev.secam.simpletag.ui.editor.dialogs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.secam.simpletag.R
import dev.secam.simpletag.ui.components.SimpleDialog
import dev.secam.simpletag.ui.components.SimpleDialogOptions

@Composable
fun HelpDialog(onCancel: () -> Unit) {
    val padding = 12.dp
    SimpleDialog(
        title = stringResource(R.string.help_dialog_title),
        onDismiss = onCancel
    ) {
        Text(
            text = stringResource(R.string.batch_help_header),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier
                .padding(bottom = padding)
        )
        Text(
            text = stringResource(R.string.batch_help_one),
            modifier = Modifier
                .padding(bottom = padding)
        )
        Text(
            text = stringResource(R.string.batch_help_two),
            modifier = Modifier
                .padding(bottom = padding)
        )
        Text(
            text = stringResource(R.string.batch_help_three),
            modifier = Modifier
                .padding(bottom = padding)
        )
        Text(
            text = stringResource(R.string.batch_help_four)
        )

        SimpleDialogOptions(
            option = stringResource(R.string.dialog_ok),
            action = onCancel,
        )
    }
}

@Preview
@Composable
fun HelpPrev(){
    HelpDialog {}
}