package ru.b3lka.linary.core.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.b3lka.linary.core.html.HtmlParser

/**
 * Вложение записи (фото, видео, аудио).
 *
 * @property ext расширение файла без точки: `jpg`, `mp4`, ...
 */
data class DiaryMedia(
    val id: String,
    val type: MediaType,
    val name: String,
    val ext: String,
    val index: Int,
    val entryId: Long,
)

/**
 * Тег журнала.
 */
data class DiaryTag(
    val id: Long,
    val value: String,
    val type: Int,
    val color: String?,
    val icon: String?,
)

/**
 * Тег, привязанный к записи. У тегов-трекеров есть значение.
 */
data class EntryTag(
    val tag: DiaryTag,
    val trackingValue: String?,
)

/**
 * Запись дневника.
 *
 * @property id .NET DateTime ticks — одновременно идентификатор и время записи.
 * @property html текст записи в HTML (каждая строка — отдельный `<p>`).
 */
data class DiaryEntry(
    val id: Long,
    val dateTime: LocalDateTime,
    val heading: String,
    val html: String,
    val rating: Int,
    val latitude: Double,
    val longitude: Double,
    val tags: List<EntryTag>,
    val media: List<DiaryMedia>,
) {
    val date: LocalDate get() = dateTime.date

    val hasLocation: Boolean get() = latitude != 0.0 || longitude != 0.0

    val images: List<DiaryMedia> by lazy { media.filter { it.type == MediaType.Image } }

    val plainText: String by lazy { HtmlParser.toPlainText(html) }
}
