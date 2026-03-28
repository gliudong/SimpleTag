
package dev.secam.simpletag.ui.editor.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DialogListItem(text: String, onClick: (() -> Unit)? = null){
    ListItem(
        headlineContent = {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = Modifier
            .height(48.dp)
            .clickable(
                enabled = true
            ) { onClick?.invoke() }
    )
}