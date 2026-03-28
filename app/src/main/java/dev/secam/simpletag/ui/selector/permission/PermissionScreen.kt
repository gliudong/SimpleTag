
package dev.secam.simpletag.ui.selector.permission

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberPermissionState
import dev.secam.simpletag.R
import dev.secam.simpletag.ui.theme.SimpleTagTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    modifier: Modifier = Modifier,
    mediaPermissionState: PermissionState,
) {
    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(bottom = 80.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music_note_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(48.dp)

        )
        Text(
            text = stringResource(R.string.welcome),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier
                .padding(bottom = 6.dp)
        )
        Text(
            text = stringResource(R.string.missing_permission),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = { mediaPermissionState.launchPermissionRequest() }
        ) {
            Text(
                text = stringResource(R.string.grant_permission),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Preview
@Composable
fun PermissionPrev(){
    SimpleTagTheme {
        PermissionScreen(
            mediaPermissionState = rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
        )
    }
}