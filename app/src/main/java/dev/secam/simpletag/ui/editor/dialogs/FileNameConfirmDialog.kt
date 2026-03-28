
package dev.secam.simpletag.ui.editor.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.secam.simpletag.R
import dev.secam.simpletag.ui.components.SimpleDialog
import dev.secam.simpletag.ui.components.SimpleDialogOptions

/**
 * Dialog for confirming filename-parsed artist and title before searching MusicBrainz
 *
 * @param artist Pre-filled artist name (can be null/empty)
 * @param title Pre-filled title
 * @param onDismiss Callback when dialog is dismissed
 * @param onSearch Callback with (artist, title) when user clicks Search
 */
@Composable
fun FileNameConfirmDialog(
    artist: String?,
    title: String,
    onDismiss: () -> Unit,
    onSearch: (artist: String?, title: String) -> Unit
) {
    val artistState = rememberTextFieldState(artist ?: "")
    val titleState = rememberTextFieldState(title)

    SimpleDialog(
        title = stringResource(R.string.auto_edit),
        onDismiss = onDismiss
    ) {
        OutlinedTextField(
            state = artistState,
            label = { Text(stringResource(R.string.artist_field)) },
            placeholder = { Text(stringResource(R.string.artist_field)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        OutlinedTextField(
            state = titleState,
            label = { Text(stringResource(R.string.title_field)) },
            placeholder = { Text(stringResource(R.string.title_field)) },
            lineLimits = TextFieldLineLimits.SingleLine,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        SimpleDialogOptions(
            option1 = stringResource(R.string.dialog_cancel),
            option2 = stringResource(R.string.auto_edit_search),
            action1 = onDismiss,
            action2 = {
                val searchArtist = artistState.text.toString().trim().ifBlank { null }
                val searchTitle = titleState.text.toString().trim()
                if (searchTitle.isNotBlank()) {
                    onSearch(searchArtist, searchTitle)
                }
            }
        )
    }
}
