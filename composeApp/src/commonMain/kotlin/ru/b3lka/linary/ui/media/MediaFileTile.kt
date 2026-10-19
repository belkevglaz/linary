package ru.b3lka.linary.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.model.MediaType
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.media_audio
import ru.b3lka.linary.resources.media_video
import ru.b3lka.linary.ui.common.LocalMediaCache
import ru.b3lka.linary.ui.common.LocalPlatform

/**
 * Плитка видео или аудио: по клику открывается во внешнем плеере.
 */
@Composable
fun MediaFileTile(media: DiaryMedia, modifier: Modifier = Modifier) {
    val cache = LocalMediaCache.current
    val platform = LocalPlatform.current
    val scope = rememberCoroutineScope()
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { scope.launch { runCatching { platform.openExternally(media, cache.bytes(media)) } } },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = when (media.type) {
                    MediaType.Video -> Icons.Filled.PlayCircle
                    MediaType.Audio -> Icons.Filled.Audiotrack
                    else -> Icons.AutoMirrored.Filled.InsertDriveFile
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = when (media.type) {
                    MediaType.Video -> stringResource(Res.string.media_video)
                    MediaType.Audio -> stringResource(Res.string.media_audio)
                    else -> media.ext.uppercase()
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
