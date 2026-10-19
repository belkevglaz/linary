package ru.b3lka.linary.core.model

/**
 * Новое вложение, готовое к сохранению: картинка уже уменьшена и сжата в JPEG, как это делает телефон.
 *
 * @property id GUID — имя файла в `v4_Media` и `Media.DiaryMediaId`.
 * @property name имя без расширения (`Media.Name`), например имя исходного файла.
 * @property ext расширение без точки (`Media.FileEnding` хранится с точкой).
 */
class NewMedia(
    val id: String,
    val name: String,
    val ext: String,
    val bytes: ByteArray,
)
