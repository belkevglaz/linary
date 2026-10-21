package ru.b3lka.linary.presentation.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Тема оформления: как в системе, всегда светлая или всегда тёмная.
 */
enum class ThemeMode { System, Light, Dark }

/**
 * Хранилище выбранной темы.
 */
interface ThemePreferences {
    var themeMode: ThemeMode
}

/**
 * Выбранная тема. Для [ThemeMode.System] тёмная ли она, решает тема системы; если система её не сообщает,
 * используется тёмная — прежняя тема приложения по умолчанию.
 */
class ThemeSettings(private val prefs: ThemePreferences) {
    var mode by mutableStateOf(prefs.themeMode)
        private set

    fun change(value: ThemeMode) {
        if (mode == value) return
        mode = value
        prefs.themeMode = value
    }

    /**
     * Тёмная ли тема сейчас; [systemDark] — тема системы (null — неизвестна).
     */
    fun isDark(systemDark: Boolean?): Boolean = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> systemDark ?: true
    }
}
