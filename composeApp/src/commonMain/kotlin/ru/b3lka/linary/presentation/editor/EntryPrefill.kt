package ru.b3lka.linary.presentation.editor

import ru.b3lka.linary.core.model.DiaryTag
import ru.b3lka.linary.core.model.DiaryTemplate

/**
 * Начальное содержимое новой записи (из шаблона по умолчанию).
 */
data class EntryPrefill(val heading: String, val html: String, val tagIds: List<Long>) {
    companion object {
        /**
         * Содержимое шаблона [template]; теги ищутся среди [tags] журнала.
         */
        fun of(template: DiaryTemplate, tags: List<DiaryTag>) =
            EntryPrefill(template.heading, template.html, template.tagIds(tags))
    }
}
