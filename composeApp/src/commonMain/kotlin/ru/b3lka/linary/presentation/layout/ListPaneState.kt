package ru.b3lka.linary.presentation.layout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Хранилище: показана ли левая панель (календарь или список записей).
 */
interface ListPanePreferences {
    var listPaneVisible: Boolean
}

/**
 * Левая панель — календарь или список записей — сворачивается, как выдвижное меню, и запись занимает
 * всё окно. Состояние общее для всех разделов и запоминается между запусками.
 */
class ListPaneState(private val prefs: ListPanePreferences) {
    var visible by mutableStateOf(prefs.listPaneVisible)
        private set

    fun toggle() {
        visible = !visible
        prefs.listPaneVisible = visible
    }
}
