
package dev.secam.simpletag.ui.editor.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.secam.simpletag.R

@Composable
fun NoFieldTip(){
    Card(
        modifier = Modifier
            .padding(bottom = 8.dp, top = 8.dp)
    ){
        Text(
            text = stringResource(R.string.add_field_tip),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(12.dp)
                .padding(vertical = 4.dp)
                .fillMaxWidth()
        )
    }
}