package ru.b3lka.linary.presentation.window

import ru.b3lka.linary.presentation.window.WindowBehavior.CloseAction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WindowBehaviorTest {

    private class MemoryPreferences : WindowPreferences {
        override var trayIcon = true
        override var closeKeepsRunning = true
    }

    private val prefs = MemoryPreferences()

    @Test
    fun closeHidesToTrayOrMinimizesOrQuits() {
        val window = WindowBehavior(prefs, traySupported = true, quit = {})
        assertEquals(CloseAction.HideToTray, window.onCloseAction())

        window.changeTrayIcon(false)
        assertEquals(CloseAction.Minimize, window.onCloseAction())
        assertFalse(prefs.trayIcon)

        window.changeCloseKeepsRunning(false)
        assertEquals(CloseAction.Quit, window.onCloseAction())
        assertFalse(prefs.closeKeepsRunning)
    }

    @Test
    fun noTrayWhenDesktopDoesNotSupportIt() {
        val window = WindowBehavior(prefs, traySupported = false, quit = {})
        assertFalse(window.trayIcon)
        window.changeTrayIcon(true)
        assertFalse(window.trayIcon)
        assertTrue(prefs.trayIcon)
        assertEquals(CloseAction.Minimize, window.onCloseAction())
    }
}
