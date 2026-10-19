package ru.b3lka.linary.presentation.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.model.NewMedia
import ru.b3lka.linary.presentation.platform.ImageSource

/**
 * Правка одной записи. Сам текст живёт в состоянии редактора (UI) и передаётся при сохранении.
 *
 * @property original запись до правки; null — новая запись.
 */
class EditorSession(
    original: DiaryEntry?,
    val dateTime: LocalDateTime,
) {
    var original: DiaryEntry? by mutableStateOf(original)
        private set

    var rating by mutableStateOf(original?.rating ?: 0)

    /**
     * Были ли изменения в тексте (выставляет UI) или в оценке.
     */
    var textChanged by mutableStateOf(false)

    /**
     * Вложения в том порядке, в каком они будут сохранены. Первое фото — обложка записи
     * (его показывает календарь).
     */
    val attachments = mutableStateListOf<Attachment>().apply {
        addAll(original?.media.orEmpty().sortedBy { it.index }.map { Attachment.Saved(it) })
    }

    /**
     * Сохранённые вложения, которые пользователь убрал.
     */
    private val removedIds = mutableSetOf<String>()

    /**
     * Сколько картинок ещё готовится (уменьшение и сжатие).
     */
    var preparingMedia by mutableStateOf(0)
        private set

    /**
     * Сколько выбранных файлов не удалось прочитать как картинки.
     */
    var failedMedia by mutableStateOf(0)

    val isNew: Boolean get() = original == null

    val isDirty: Boolean
        get() = textChanged || rating != (original?.rating ?: 0) || mediaChanged

    val initialHtml: String get() = original?.html.orEmpty()

    val addedMedia: List<NewMedia> get() = attachments.filterIsInstance<Attachment.Added>().map { it.media }

    val removedMediaIds: Set<String> get() = removedIds.toSet()

    /**
     * Итоговый порядок вложений.
     */
    val mediaOrder: List<String> get() = attachments.map { it.id }

    private val mediaChanged: Boolean
        get() = mediaOrder != original?.media.orEmpty().sortedBy { it.index }.map { it.id }

    /**
     * Готовит картинки в фоне и добавляет их в конец по мере готовности.
     */
    suspend fun attach(sources: List<ImageSource>) {
        preparingMedia += sources.size
        sources.forEach { source ->
            val media = try {
                withContext(Dispatchers.Default) { runCatching { source.prepare() }.getOrNull() }
            } finally {
                preparingMedia--
            }
            if (media != null) attachments += Attachment.Added(media) else failedMedia++
        }
    }

    fun remove(item: Attachment) {
        attachments.remove(item)
        if (item is Attachment.Saved) removedIds += item.id
    }

    /**
     * Сдвигает вложение на [delta] позиций (−1 — влево, +1 — вправо).
     */
    fun move(item: Attachment, delta: Int) {
        val from = attachments.indexOf(item)
        if (from < 0) return
        val to = (from + delta).coerceIn(0, attachments.lastIndex)
        if (to == from) return
        attachments.removeAt(from)
        attachments.add(to, item)
    }

    /**
     * Делает вложение обложкой — переносит его на первое место.
     */
    fun makeCover(item: Attachment) = move(item, -attachments.indexOf(item))

    /**
     * После конфликта: правка продолжается поверх свежей версии записи [fresh]
     * (null — запись удалили на другом устройстве, она сохранится как новая).
     * Порядок вложений пользователя сохраняется; вложения, добавленные на другом устройстве, — в конце.
     */
    fun rebase(fresh: DiaryEntry?) {
        original = fresh
        val freshMedia = fresh?.media.orEmpty().sortedBy { it.index }
        val freshIds = freshMedia.map { it.id }.toSet()
        attachments.removeAll { it is Attachment.Saved && it.id !in freshIds }
        val present = attachments.map { it.id }.toSet()
        val added = freshMedia.filter { it.id !in present && it.id !in removedIds }
        attachments.addAll(added.map { Attachment.Saved(it) })
        removedIds.retainAll(freshIds)
    }
}
