package ru.b3lka.linary.desktop.platform

import java.awt.Frame
import java.awt.Window

/**
 * Поднимает окно поверх остальных и передаёт ему фокус.
 *
 * Оконные менеджеры не дают программам перехватывать фокус; краткое «поверх всех» обычно помогает
 * поднять окно. Если менеджер всё равно откажет, он хотя бы подсветит окно на панели задач.
 */
fun bringToFront(window: Window) {
    if (window is Frame && window.extendedState and Frame.ICONIFIED != 0) {
        window.extendedState = window.extendedState and Frame.ICONIFIED.inv()
    }
    window.isAlwaysOnTop = true
    window.toFront()
    window.requestFocus()
    window.isAlwaysOnTop = false
}
