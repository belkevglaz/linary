package ru.b3lka.linary.presentation.window

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Хранилище настроек окна.
 */
interface WindowPreferences {
    var trayIcon: Boolean
    var closeKeepsRunning: Boolean
}

/**
 * Поведение окна приложения: значок в системном трее и что делает закрытие окна.
 *
 * Если [closeKeepsRunning] включено, закрытие окна (крестик, `Alt+F4`) не завершает приложение:
 * со значком в трее окно прячется в трей, без него — сворачивается. Выход — [quit].
 *
 * @param traySupported рабочий стол показывает значки в трее; если нет, [trayIcon] всегда выключен.
 * @param quit завершить приложение.
 */
class WindowBehavior(
    private val prefs: WindowPreferences,
    val traySupported: Boolean,
    val quit: () -> Unit,
) {
    var trayIcon by mutableStateOf(traySupported && prefs.trayIcon)
        private set

    var closeKeepsRunning by mutableStateOf(prefs.closeKeepsRunning)
        private set

    fun changeTrayIcon(value: Boolean) {
        if (!traySupported) return
        trayIcon = value
        prefs.trayIcon = value
    }

    fun changeCloseKeepsRunning(value: Boolean) {
        closeKeepsRunning = value
        prefs.closeKeepsRunning = value
    }

    /**
     * Что сделать, когда пользователь закрывает окно.
     */
    fun onCloseAction(): CloseAction = when {
        !closeKeepsRunning -> CloseAction.Quit
        trayIcon -> CloseAction.HideToTray
        else -> CloseAction.Minimize
    }

    enum class CloseAction { Quit, HideToTray, Minimize }
}
