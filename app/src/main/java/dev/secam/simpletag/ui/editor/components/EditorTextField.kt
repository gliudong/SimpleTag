
package dev.secam.simpletag.ui.editor.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.secam.simpletag.R

@Composable
fun EditorTextField(
    state: TextFieldState,
    label: String,
    modifier: Modifier = Modifier,
    hasDelete: Boolean = false,
    action: (() -> Unit)? = null,
    togglable: Boolean = false,
    onToggle: (Boolean) -> Unit = {},
    enabled: Boolean = true
) {
    Row {
        OutlinedTextField(
            state = if (enabled) state else rememberTextFieldState("<${stringResource(R.string.unchanged)}>"),
            label = {
                Text(
                    text = label
                )
            },
            trailingIcon = if (hasDelete && action != null) {
                {
                    Row {
                        IconButton(onClick =  action, enabled = enabled) {
                            Icon(painterResource(R.drawable.ic_delete_24px), stringResource(R.string.cd_delete))
                        }
                        if (togglable) {
                            Checkbox(
                                checked = enabled,
                                onCheckedChange = onToggle,
                            )
                        }
                    }
                }
            } else if(action != null){
                {
                    if (togglable) {
                        Checkbox(
                            checked = enabled,
                            onCheckedChange = onToggle,
                        )
                    }
                }
            } else null,
            enabled = enabled,
            modifier = modifier
                .fillMaxWidth()
        )

    }
}