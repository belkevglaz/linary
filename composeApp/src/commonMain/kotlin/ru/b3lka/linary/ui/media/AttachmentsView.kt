package ru.b3lka.linary.ui.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.Diary
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.model.MediaType
import ru.b3lka.linary.core.time.YearMonth
import ru.b3lka.linary.presentation.state.ViewerState
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.attachments_empty
import ru.b3lka.linary.ui.common.LocalDateFormatter
import ru.b3lka.linary.ui.common.LocalMediaActions
import ru.b3lka.linary.ui.common.onSecondaryClick

/**
 * Раздел «Вложения»: сетка всех фото и видео, новые сверху, с заголовками месяцев.
 */
@Composable
fun AttachmentsView(diary: Diary?, onOpenPhoto: (ViewerState) -> Unit, modifier: Modifier = Modifier) {
    val formatter = LocalDateFormatter.current
    val allImages = remember(diary) { diary?.entries.orEmpty().flatMap { it.images } }
    val groups: List<Pair<YearMonth, List<DiaryMedia>>> = remember(diary) {
        diary?.entries.orEmpty()
            .filter { it.media.isNotEmpty() }
            .groupBy { YearMonth.of(it.date) }
            .map { (month, entries) -> month to entries.flatMap { it.media } }
    }
    if (groups.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.attachments_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 2.dp, end = 2.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        groups.forEach { (month, media) ->
            item(key = "h${month.year}-${month.month}", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    formatter.monthYear(month),
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(media, key = { it.id }) { m -> AttachmentTile(m, allImages, onOpenPhoto) }
        }
    }
}

@Composable
private fun AttachmentTile(media: DiaryMedia, allImages: List<DiaryMedia>, onOpenPhoto: (ViewerState) -> Unit) {
    val actions = LocalMediaActions.current
    var menu by remember { mutableStateOf(false) }
    // Правый клик — меню: скачать, сделать обложкой дня
    val tile = Modifier.fillMaxWidth().aspectRatio(1f).onSecondaryClick { menu = actions != null }
    Box {
        if (media.type == MediaType.Image) {
            MediaImage(
                media,
                maxSize = 360,
                modifier = tile.clickable {
                    onOpenPhoto(ViewerState(allImages, allImages.indexOf(media).coerceAtLeast(0)))
                },
            )
        } else {
            MediaFileTile(media, modifier = tile)
        }
        MediaMenu(
            expanded = menu,
            onDismiss = { menu = false },
            onSetCover = actions?.takeIf { it.canSetCover && media.type == MediaType.Image && !it.isCover(media) }
                ?.let { a -> { a.setCover(media) } },
            onDownload = actions?.let { a -> { a.download(media) } },
        )
    }
}
