package ru.b3lka.linary.ui.media

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.ui.common.LocalMediaCache

/**
 * Картинка-вложение: расшифровывается и декодируется лениво.
 *
 * @param maxSize ограничение большей стороны в пикселях; null — исходный размер.
 * @param loading что показать, пока картинка грузится.
 */
@Composable
fun MediaImage(
    media: DiaryMedia,
    maxSize: Int?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: Color = MaterialTheme.colorScheme.surfaceVariant,
    loading: (@Composable () -> Unit)? = null,
) {
    val cache = LocalMediaCache.current
    // key(): при смене вложения состояние создаётся заново, иначе produceState
    // сохраняет старую картинку (его remember не зависит от ключей).
    key(media.id, maxSize) {
        val bitmap: ImageBitmap? by produceState(cache.peek(media, maxSize), media.id, maxSize) {
            if (value == null) value = cache.image(media, maxSize)
        }
        val current = bitmap
        if (current != null) {
            Image(current, contentDescription = media.name, modifier = modifier, contentScale = contentScale)
        } else {
            Box(modifier.background(placeholder), contentAlignment = Alignment.Center) { loading?.invoke() }
        }
    }
}
