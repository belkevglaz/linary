package ru.b3lka.linary.presentation.media

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Хранилище состояния ленты вложений: развёрнута она или свёрнута.
 */
interface PhotoStripPreferences {
    var photosExpanded: Boolean
}

/**
 * Свёрнута ли лента вложений записи — общая для просмотра и редактора и для всех записей,
 * запоминается между запусками.
 */
class PhotoStripState(private val prefs: PhotoStripPreferences) {
    var expanded by mutableStateOf(prefs.photosExpanded)
        private set

    fun toggle() = change(!expanded)

    fun collapse() = change(false)

    private fun change(value: Boolean) {
        if (expanded == value) return
        expanded = value
        prefs.photosExpanded = value
    }
}
