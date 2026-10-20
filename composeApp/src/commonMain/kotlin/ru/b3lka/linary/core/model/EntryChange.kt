package ru.b3lka.linary.core.model

import kotlinx.datetime.LocalDateTime

/**
 * Изменение записи для сохранения.
 *
 * @property original запись в том виде, в каком её начали править; null — новая запись.
 *   По ней репозиторий проверяет, что запись не изменилась на другом устройстве.
 * @property html текст в формате Diarium (см. `DiariumHtmlWriter`).
 * @property addedMedia новые вложения; добавляются после уже имеющихся.
 * @property removedMediaIds вложения записи, которые нужно убрать.
 * @property mediaOrder итоговый порядок вложений по id (первое — обложка); null — не менять порядок.
 * @property tagIds теги записи после правки; null — теги не трогать. У оставшихся тегов-трекеров значение сохраняется.
 */
data class EntryChange(
    val original: DiaryEntry?,
    val dateTime: LocalDateTime,
    val html: String,
    val rating: Int,
    val addedMedia: List<NewMedia> = emptyList(),
    val removedMediaIds: Set<String> = emptySet(),
    val mediaOrder: List<String>? = null,
    val tagIds: Set<Long>? = null,
)
