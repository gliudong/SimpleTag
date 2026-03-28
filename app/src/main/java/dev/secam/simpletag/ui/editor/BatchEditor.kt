
package dev.secam.simpletag.ui.editor

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.secam.simpletag.data.enums.SimpleTagField
import dev.secam.simpletag.ui.editor.components.EditorArtwork
import dev.secam.simpletag.ui.editor.components.EditorArtworkButtons
import dev.secam.simpletag.ui.editor.components.EditorTextField
import dev.secam.simpletag.ui.editor.components.NoFieldTip
import org.jaudiotagger.tag.images.Artwork

@Composable
fun BatchEditor(
    artwork: Artwork?,
    setArtwork: (Artwork?) -> Unit,
    roundCovers: Boolean?,
    fieldStates: Map<SimpleTagField, EditorFieldState>,
    pickArtwork: ManagedActivityResultLauncher<PickVisualMediaRequest, Uri?>,
    simpleEditor: Boolean,
    removeField: (SimpleTagField) -> Unit,
    deletedFields: Set<SimpleTagField>,
    artworkEnabled: Boolean,
    setArtworkEnabled: (Boolean) -> Unit,
    modifier: Modifier
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        EditorArtwork(
            enabled = artworkEnabled,
            roundCovers = roundCovers ?: true,
            artwork = artwork,
            modifier = Modifier
                .padding(top = 16.dp)
        )
        Row {
            EditorArtworkButtons(
                onAdd = { pickArtwork.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) },
                onDelete = { setArtwork(null) },
                deleteEnabled = artwork != null,
                togglable = true,
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp),
                onToggle = { setArtworkEnabled(it)},
                enabled = artworkEnabled
            )
        }

        //  Field List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 84.dp)
                .animateContentSize()
        ) {
            if(fieldStates.isEmpty()){
                NoFieldTip()
            } else {
                for (field in fieldStates) {
                    val visibleState = remember { MutableTransitionState(true) }
                    if(!visibleState.targetState) {
                        if(!deletedFields.contains(field.key)){
                            visibleState.targetState = true
                        }
                    }
                    AnimatedVisibility(
                        visibleState = visibleState,
                        enter = fadeIn() + expandVertically(tween(150)),
                        exit = fadeOut() + shrinkVertically(tween(150)),
                    ){
                        EditorTextField(
                            state = field.value.textState,
                            label = stringResource(field.key.displayNameRes),
                            hasDelete = !simpleEditor,
                            action = {
                                removeField(field.key)
                                visibleState.targetState = false
                            },
                            togglable = true,
                            onToggle = {
                                field.value.enabledState.value = it
                            },
                            enabled = field.value.enabledState.value,
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}