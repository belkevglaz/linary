package ru.b3lka.linary.ui.media

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.media_download
import ru.b3lka.linary.resources.media_move_left
import ru.b3lka.linary.resources.media_move_right
import ru.b3lka.linary.resources.media_remove
import ru.b3lka.linary.resources.media_set_cover

/**
 * Меню фото: сделать обложкой дня, переместить, скачать, убрать. Недоступные пункты (null) не показываются.
 */
@Composable
fun MediaMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSetCover: (() -> Unit)? = null,
    onMoveLeft: (() -> Unit)? = null,
    onMoveRight: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        MenuItem(Icons.Outlined.Star, Res.string.media_set_cover, onSetCover, onDismiss)
        MenuItem(Icons.AutoMirrored.Outlined.ArrowBack, Res.string.media_move_left, onMoveLeft, onDismiss)
        MenuItem(Icons.AutoMirrored.Outlined.ArrowForward, Res.string.media_move_right, onMoveRight, onDismiss)
        MenuItem(Icons.Outlined.Download, Res.string.media_download, onDownload, onDismiss)
        if (onRemove != null) {
            HorizontalDivider()
            MenuItem(Icons.Outlined.Close, Res.string.media_remove, onRemove, onDismiss)
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, label: StringResource, action: (() -> Unit)?, onDismiss: () -> Unit) {
    if (action == null) return
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        leadingIcon = { Icon(icon, null) },
        onClick = {
            onDismiss()
            action()
        },
    )
}
