package ru.b3lka.linary.presentation.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Хранилище шаблона новой записи. В Diarium это локальная настройка телефона, в базе её нет.
 */
interface TemplatePreferences {
    var defaultTemplateId: Long?
}

/**
 * Шаблон, которым заполняется новая запись; null — пустая запись.
 */
class DefaultTemplateState(private val prefs: TemplatePreferences) {
    var id by mutableStateOf(prefs.defaultTemplateId)
        private set

    fun change(value: Long?) {
        if (id == value) return
        id = value
        prefs.defaultTemplateId = value
    }
}
