package ru.b3lka.linary.presentation.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.model.NewMedia
import ru.b3lka.linary.presentation.platform.PlatformServices
import ru.b3lka.linary.presentation.state.AppState

/**
 * Действия с вложениями из просмотра записи, галереи и редактора: скачать и сделать обложкой дня.
 *
 * @param saveTitle заголовок диалога сохранения.
 */
class MediaActions(
    private val app: AppState,
    private val cache: MediaCache,
    private val platform: PlatformServices,
    private val scope: CoroutineScope,
    private val saveTitle: String,
) {
    /**
     * Можно ли менять обложку (источник доступен для записи).
     */
    val canSetCover: Boolean get() = app.canEdit

    /**
     * Стоит ли фото на первом месте в своей записи — то есть показывается ли оно в календаре.
     */
    fun isCover(media: DiaryMedia): Boolean =
        app.diary?.entries?.firstOrNull { it.id == media.entryId }?.media?.minByOrNull { it.index }?.id == media.id

    /**
     * Переносит фото на первое место в записи и сразу сохраняет.
     */
    fun setCover(media: DiaryMedia) {
        scope.launch { app.editing.setCover(media) }
    }

    fun download(media: DiaryMedia) {
        scope.launch {
            runCatching { platform.saveFile(saveTitle, fileName(media.name, media.ext), cache.bytes(media)) }
        }
    }

    fun download(media: NewMedia) {
        scope.launch { runCatching { platform.saveFile(saveTitle, fileName(media.name, media.ext), media.bytes) } }
    }

    private fun fileName(name: String, ext: String): String = "${name.ifBlank { "photo" }}.$ext"
}
