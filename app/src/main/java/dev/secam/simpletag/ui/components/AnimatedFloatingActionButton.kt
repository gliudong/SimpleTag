
package dev.secam.simpletag.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun AnimatedFloatingActionButton(visible: Boolean, icon: Painter, iconDescription: String, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(tween(150)) + fadeIn(),
        exit = scaleOut(tween(150)) + fadeOut()
    ) {
        FloatingActionButton(
            onClick = onClick,
        ) {
            Icon(
                painter = icon,
                contentDescription = iconDescription
            )
        }
    }
}