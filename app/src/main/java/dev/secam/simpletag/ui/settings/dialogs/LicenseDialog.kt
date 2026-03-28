
package dev.secam.simpletag.ui.settings.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.secam.simpletag.R
import dev.secam.simpletag.ui.components.SimpleDialog
import dev.secam.simpletag.ui.components.SimpleDialogOptions

@Composable
fun LicenseDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit
) {
    SimpleDialog(
        title = stringResource(R.string.license),
        onDismiss = onDismissRequest
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .size(300.dp)
                .padding(end = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.gplv3_fulltext),
                modifier = modifier
                    .verticalScroll(rememberScrollState())
            )
        }
        SimpleDialogOptions(
            option = stringResource(R.string.dialog_close)
        ) { onDismissRequest() }
    }
}

@Preview
@Composable
fun LicPrev(){
    LicenseDialog {  }
}