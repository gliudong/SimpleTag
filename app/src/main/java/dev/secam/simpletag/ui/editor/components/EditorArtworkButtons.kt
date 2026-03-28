
package dev.secam.simpletag.ui.editor.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.secam.simpletag.R

@Composable
fun EditorArtworkButtons(
    deleteEnabled: Boolean,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    togglable: Boolean = false,
    onToggle: (Boolean) -> Unit = {},
    enabled: Boolean = true
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Button(
            onClick = onAdd,
            enabled = enabled,
            modifier = Modifier
                .weight(.5f, false)
                .fillMaxWidth(.98f)
        ) {
            Icon(
                painter = painterResource((R.drawable.ic_photo_24px)),
                contentDescription = stringResource(R.string.cd_add_cover_icon),
                modifier = Modifier
                    .padding(end = 4.dp)
            )
            Text(
                text = stringResource(R.string.add_cover),
                fontWeight = FontWeight.Bold
            )
        }
        Button(
            onClick = onDelete,
            enabled = deleteEnabled && enabled,
            modifier = Modifier
                .weight(.5f, false)
                .fillMaxWidth(.98f)

        ) {
            Icon(
                painter = painterResource((R.drawable.ic_delete_24px)),
                contentDescription = stringResource(R.string.cd_delete_cover_icon),
                modifier = Modifier
                    .padding(end = 4.dp)
            )
            Text(
                text = stringResource(R.string.delete_cover),
                fontWeight = FontWeight.Bold
            )
        }
        if(togglable){
            Checkbox(
                checked = enabled,
                onCheckedChange = onToggle,
            )
        }
    }
}