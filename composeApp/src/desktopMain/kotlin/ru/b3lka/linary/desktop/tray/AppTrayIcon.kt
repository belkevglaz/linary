package ru.b3lka.linary.desktop.tray

import java.awt.EventQueue

/**
 * Значок приложения в системном трее: StatusNotifierItem (D-Bus), а если панель его не поддерживает —
 * AWT. Щелчок по значку и пункты меню вызывают обработчики в потоке интерфейса.
 */
object AppTrayIcon {

    /**
     * Может ли рабочий стол показать значок хоть одним способом.
     */
    fun isSupported(): Boolean = StatusNotifierTray.isAvailable() || AwtTrayIcon.isSupported

    /**
     * Показывает значок; закрытие результата убирает его. null — показать не удалось.
     */
    fun show(
        title: String,
        openLabel: String,
        quitLabel: String,
        onOpen: () -> Unit,
        onQuit: () -> Unit,
    ): AutoCloseable? {
        val open = { EventQueue.invokeLater(onOpen) }
        val quit = { EventQueue.invokeLater(onQuit) }
        val menu = listOf(TrayMenuItem(openLabel, open), TrayMenuItem(null), TrayMenuItem(quitLabel, quit))
        return StatusNotifierTray.create(title, TrayImages.appPixmaps(), menu, open)
            ?: AwtTrayIcon.create(title, menu, open)
    }
}
