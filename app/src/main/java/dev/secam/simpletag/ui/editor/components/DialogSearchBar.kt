
package dev.secam.simpletag.ui.editor.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.secam.simpletag.R

@Composable
fun DialogSearchBar (textFieldState: TextFieldState) {
    TextField(
        state = textFieldState,
        colors = TextFieldDefaults.colors(
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
        ),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_search_24px),
                contentDescription = stringResource(R.string.cd_search_button),
                modifier = Modifier
            )
        },
        trailingIcon = if (textFieldState.text.isEmpty()) null else {
            {
                IconButton(
                    onClick = { textFieldState.clearText() }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close_24px),
                        contentDescription = stringResource(R.string.cd_clear_button)
                    )
                }
            }
        },
        placeholder = { Text(stringResource(R.string.search_fields)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    )
}