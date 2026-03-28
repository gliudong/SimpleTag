
package dev.secam.simpletag.ui.settings.dialogs

import android.os.Build
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.secam.simpletag.R
import dev.secam.simpletag.data.enums.AppColorScheme
import dev.secam.simpletag.ui.components.SimpleDialog

@Composable
fun ColorSchemeDialog(
    colorScheme: AppColorScheme,
    setColorScheme: (AppColorScheme) -> Unit,
    onDismissRequest: () -> Unit
) {
    SimpleDialog(
        title = stringResource(R.string.color_scheme),
        onDismiss = onDismissRequest
    ) {
        val radioOptions =
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) AppColorScheme.entries.toList()
            else AppColorScheme.entries.toList().subList(1,AppColorScheme.entries.size)
        val (selectedOption, onOptionSelected) = remember { mutableStateOf(colorScheme ) }
        radioOptions.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .selectable(
                        selected = (option == selectedOption),
                        onClick = {
                            onOptionSelected(option)
                            setColorScheme(option)
                            onDismissRequest()
                        },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (option == selectedOption),
                    onClick = null
                )
                Text(
                    text = stringResource(option.displayNameRes),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}