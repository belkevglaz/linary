package ru.b3lka.linary.ui.entry

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.model.MediaType
import ru.b3lka.linary.presentation.state.ViewerState
import ru.b3lka.linary.ui.common.LocalMediaActions
import ru.b3lka.linary.ui.common.onSecondaryClick
import ru.b3lka.linary.ui.media.CollapsibleMedia
import ru.b3lka.linary.ui.media.MediaFileTile
import ru.b3lka.linary.ui.media.MediaImage
import ru.b3lka.linary.ui.media.MediaMenu

/**
 * Лента вложений записи: плитки одинакового размера при любом их количестве; сворачивается.
 * Правый клик или долгое нажатие — меню: скачать, сделать обложкой дня.
 */
@Composable
fun EntryGallery(entry: DiaryEntry, onOpenPhoto: (ViewerState) -> Unit) {
    val media = entry.media.sortedBy { it.index }
    val images = media.filter { it.type == MediaType.Image }
    CollapsibleMedia(media.size) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            media.forEach { m ->
                GalleryTile(m, isCover = m == media.first()) { onOpenPhoto(ViewerState(images, images.indexOf(m))) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GalleryTile(media: DiaryMedia, isCover: Boolean, onOpen: () -> Unit) {
    val actions = LocalMediaActions.current
    var menu by remember { mutableStateOf(false) }
    val tile = Modifier
        .height(200.dp)
        .aspectRatio(4f / 3f)
        .clip(RoundedCornerShape(10.dp))
        .onSecondaryClick { menu = actions != null }
    Box {
        if (media.type == MediaType.Image) {
            MediaImage(
                media,
                maxSize = 700,
                modifier = tile.combinedClickable(onClick = onOpen, onLongClick = { menu = actions != null }),
            )
        } else {
            MediaFileTile(media, modifier = tile)
        }
        MediaMenu(
            expanded = menu,
            onDismiss = { menu = false },
            onSetCover = actions?.takeIf { it.canSetCover && !isCover && media.type == MediaType.Image }
                ?.let { a -> { a.setCover(media) } },
            onDownload = actions?.let { a -> { a.download(media) } },
        )
    }
}
