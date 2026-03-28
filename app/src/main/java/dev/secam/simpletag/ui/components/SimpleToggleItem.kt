
package dev.secam.simpletag.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SimpleToggleItem(
    modifier: Modifier = Modifier,
    currentState: Boolean,
    headlineContent: String,
    supportingContent: String? = null,
    enabled: Boolean = true,
    onToggle: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(headlineContent,fontWeight = FontWeight.Medium) },
        supportingContent = {
            if (supportingContent != null) {
                Text(supportingContent)
            }
        },
        trailingContent = {
            Switch(
                checked = currentState,
                onCheckedChange = null
            )
        },
        colors = if (enabled) ListItemDefaults.colors(
        ) else ListItemDefaults.colors(
            headlineColor = MaterialTheme.colorScheme.outline,
            supportingColor = MaterialTheme.colorScheme.outlineVariant,
            leadingIconColor = MaterialTheme.colorScheme.outline,
        ),
        modifier = modifier
            .clickable(enabled = enabled, onClick = {
                onToggle(!currentState)
            })
            .padding(horizontal = 0.dp, vertical = 0.dp)
    )
}