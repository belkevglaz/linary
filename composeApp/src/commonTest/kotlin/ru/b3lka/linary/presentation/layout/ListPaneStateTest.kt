package ru.b3lka.linary.presentation.layout

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListPaneStateTest {

    private class MemoryPreferences(override var listPaneVisible: Boolean) : ListPanePreferences

    @Test
    fun toggleIsSaved() {
        val prefs = MemoryPreferences(listPaneVisible = true)
        val pane = ListPaneState(prefs)
        assertTrue(pane.visible)
        pane.toggle()
        assertFalse(pane.visible)
        assertFalse(prefs.listPaneVisible)
        pane.toggle()
        assertTrue(pane.visible)
        assertTrue(prefs.listPaneVisible)
    }
}
