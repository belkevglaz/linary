package ru.b3lka.linary.presentation.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EditHistoryTest {

    @Test
    fun undoAndRedo() {
        val history = EditHistory()
        history.reset("a")
        history.push("ab")
        history.push("abc")

        assertEquals("ab", history.undo())
        history.push("ab") // снимок после отмены
        assertEquals("a", history.undo())
        history.push("a")
        assertNull(history.undo())
        assertTrue(history.canRedo)

        assertEquals("ab", history.redo())
        history.push("ab")
        assertEquals("abc", history.redo())
        history.push("abc")
        assertFalse(history.canRedo)
    }

    @Test
    fun newEditClearsRedo() {
        val history = EditHistory()
        history.reset("a")
        history.push("ab")
        history.undo()
        history.push("a")
        history.push("ax")

        assertFalse(history.canRedo)
        assertEquals("a", history.undo())
    }

    @Test
    fun snapshotAfterUndoReplacesRestoredState() {
        val history = EditHistory()
        history.reset("<p>a</p>")
        history.push("<p>ab</p>")
        history.undo()
        // Редактор записал восстановленный текст немного иначе — это не новая правка
        history.push("<p>a</p>\n")

        assertTrue(history.canRedo)
        assertEquals("<p>ab</p>", history.redo())
    }

    @Test
    fun limitDropsOldestSnapshots() {
        val history = EditHistory(limit = 3)
        history.reset("0")
        listOf("1", "2", "3", "4").forEach(history::push)

        assertEquals("3", history.undo())
        history.push("3")
        assertEquals("2", history.undo())
        history.push("2")
        assertNull(history.undo())
    }
}
