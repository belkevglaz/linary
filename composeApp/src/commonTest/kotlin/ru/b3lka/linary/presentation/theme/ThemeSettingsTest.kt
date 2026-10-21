package ru.b3lka.linary.presentation.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeSettingsTest {
    private class MemoryPreferences : ThemePreferences {
        override var themeMode = ThemeMode.System
    }

    @Test
    fun systemModeFollowsSystemAndFallsBackToDark() {
        val prefs = MemoryPreferences()
        val theme = ThemeSettings(prefs)
        assertFalse(theme.isDark(systemDark = false))
        assertTrue(theme.isDark(systemDark = true))
        assertTrue(theme.isDark(systemDark = null))

        theme.change(ThemeMode.Light)
        assertEquals(ThemeMode.Light, prefs.themeMode)
        assertFalse(theme.isDark(systemDark = true))
    }
}
