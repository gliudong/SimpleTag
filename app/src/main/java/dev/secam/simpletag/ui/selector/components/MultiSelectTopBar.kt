
package dev.secam.simpletag.ui.selector.components

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.secam.simpletag.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MultiSelectTopBar(
    numSelected: Int,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    onBatchAutoEdit: () -> Unit = {}
) {
    BackHandler {
        onBack()
    }
    TopAppBar(
        title = { Text("$numSelected " + stringResource(R.string.multiselect_title)) },
        navigationIcon = {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close_24px),
                    contentDescription = stringResource(R.string.cd_exit_mulitselect)
                )
            }
        },
        actions = {
            IconButton(
                onClick = onBatchAutoEdit
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_auto_edit_24px),
                    contentDescription = stringResource(R.string.cd_batch_auto_edit)
                )
            }
            IconButton(
                onClick = {
                    onEdit()
                    onBack()
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit_24px),
                    contentDescription = stringResource(R.string.cd_edit_multiselect)
                )
            }
        },
//        colors = ,
        scrollBehavior = scrollBehavior
    )
}