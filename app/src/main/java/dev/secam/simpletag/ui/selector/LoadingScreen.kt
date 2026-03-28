
package dev.secam.simpletag.ui.selector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    text: String? = null,
    subtext: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 80.dp)
    ) {
        CircularProgressIndicator()
        text?.let {
            Text(
                text = it,//stringResource(R.string.loading_music),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
            )
        }
        subtext?.let {
            Text(
                text = it,// stringResource(R.string.loading_music_long),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview
@Composable
fun LoadingPrev() {
    LoadingScreen()
}