package ru.b3lka.linary.ui.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.MediaType
import ru.b3lka.linary.core.model.NewMedia
import ru.b3lka.linary.presentation.editor.Attachment
import ru.b3lka.linary.presentation.editor.EditorSession
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_close
import ru.b3lka.linary.resources.media_actions
import ru.b3lka.linary.resources.media_attach_failed
import ru.b3lka.linary.resources.media_cover
import ru.b3lka.linary.resources.media_remove
import ru.b3lka.linary.ui.common.LocalMediaActions
import ru.b3lka.linary.ui.common.LocalPlatform
import ru.b3lka.linary.ui.common.Tooltip
import ru.b3lka.linary.ui.common.onSecondaryClick
import ru.b3lka.linary.ui.media.CollapsibleMedia
import ru.b3lka.linary.ui.media.MediaFileTile
import ru.b3lka.linary.ui.media.MediaImage
import ru.b3lka.linary.ui.media.MediaMenu

private const val THUMBNAIL_PX = 320

/**
 * Вложения записи в редакторе: имеющиеся, только что добавленные и ещё готовящиеся.
 * Первое фото — обложка дня. Клик или правый клик по плитке — меню: сделать обложкой,
 * переместить, скачать, убрать. Пустая лента не показывается; непустую можно свернуть.
 */
@Composable
internal fun EditorAttachments(session: EditorSession) {
    val items = session.attachments
    if (items.isEmpty() && session.preparingMedia == 0 && session.failedMedia == 0) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp)) {
        CollapsibleMedia(items.size + session.preparingMedia) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items.forEachIndexed { index, item ->
                    AttachmentTile(session, item, index, isLast = index == items.lastIndex)
                }
                repeat(session.preparingMedia) {
                    Box(tileModifier().background(MaterialTheme.colorScheme.surfaceVariant), Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                }
            }
        }
        if (session.failedMedia > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.media_attach_failed, session.failedMedia),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = { session.failedMedia = 0 }) { Text(stringResource(Res.string.action_close)) }
            }
        }
    }
}

private fun tileModifier(): Modifier = Modifier.height(96.dp).aspectRatio(4f / 3f).clip(RoundedCornerShape(10.dp))

@Composable
private fun AttachmentTile(session: EditorSession, item: Attachment, index: Int, isLast: Boolean) {
    var menu by remember { mutableStateOf(false) }
    val actions = LocalMediaActions.current
    Box {
        val tile = tileModifier().clickable { menu = true }.onSecondaryClick { menu = true }
        when (item) {
            is Attachment.Saved -> if (item.media.type == MediaType.Image) {
                MediaImage(item.media, maxSize = THUMBNAIL_PX, modifier = tile)
            } else {
                MediaFileTile(item.media, tile)
            }
            is Attachment.Added -> NewMediaImage(item.media, tile)
        }
        if (index == 0 && item.isImage) CoverBadge(Modifier.align(Alignment.BottomStart))
        Row(Modifier.align(Alignment.TopEnd).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TileButton(Icons.Filled.MoreVert, stringResource(Res.string.media_actions)) { menu = true }
            TileButton(Icons.Filled.Close, stringResource(Res.string.media_remove)) { session.remove(item) }
        }
        MediaMenu(
            expanded = menu,
            onDismiss = { menu = false },
            onSetCover = if (index > 0 && item.isImage) ({ session.makeCover(item) }) else null,
            onMoveLeft = if (index > 0) ({ session.move(item, -1) }) else null,
            onMoveRight = if (!isLast) ({ session.move(item, 1) }) else null,
            onDownload = actions?.let { a ->
                {
                    when (item) {
                        is Attachment.Saved -> a.download(item.media)
                        is Attachment.Added -> a.download(item.media)
                    }
                }
            },
            onRemove = { session.remove(item) },
        )
    }
}

@Composable
private fun CoverBadge(modifier: Modifier) {
    Text(
        stringResource(Res.string.media_cover),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun TileButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    // Не IconButton: тот растягивается до минимальной области нажатия 48dp и кружки налезают друг на друга
    Tooltip(label) {
        Box(
            Modifier
                .focusProperties { canFocus = false }
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onClick)
                .pointerHoverIcon(PointerIcon.Hand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, tint = Color.White, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun NewMediaImage(media: NewMedia, modifier: Modifier) {
    val platform = LocalPlatform.current
    val bitmap: ImageBitmap? by produceState<ImageBitmap?>(null, media) {
        value = withContext(Dispatchers.Default) { platform.decodeImage(media.bytes, THUMBNAIL_PX) }
    }
    val current = bitmap
    if (current != null) {
        Image(current, contentDescription = media.name, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
