package ru.b3lka.linary.core.model

/**
 * Шаблон записи — заготовка, которую Diarium вставляет в запись (таблица `Templates`).
 *
 * @property html текст в формате записи (каждая строка — `<p>`).
 * @property tags теги, которые шаблон добавляет к записи: названия через запятую (может быть пусто).
 */
data class DiaryTemplate(
    val id: Long,
    val name: String,
    val heading: String,
    val html: String,
    val tags: String,
) {
    /**
     * Названия тегов шаблона.
     */
    val tagNames: List<String> get() = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    /**
     * Идентификаторы тегов шаблона среди [all] (по имени, без учёта регистра); неизвестные имена пропускаются.
     */
    fun tagIds(all: List<DiaryTag>): List<Long> =
        tagNames.mapNotNull { name -> all.firstOrNull { it.value.equals(name, ignoreCase = true) }?.id }.distinct()
}
