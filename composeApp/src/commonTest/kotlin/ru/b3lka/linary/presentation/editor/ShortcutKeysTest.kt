package ru.b3lka.linary.presentation.editor

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShortcutKeysTest {

    @Test
    fun latinKeyIsRecognisedByKeyCode() {
        assertEquals('Z', ShortcutKeys.letter(Key.Z, 0))
        assertEquals('S', ShortcutKeys.letter(Key.S, 's'.code))
    }

    @Test
    fun russianLayoutIsMappedToQwertyKeys() {
        assertEquals('Z', ShortcutKeys.letter(Key.Unknown, 'я'.code))
        assertEquals('Z', ShortcutKeys.letter(Key.Unknown, 'Я'.code))
        assertEquals('S', ShortcutKeys.letter(Key.Unknown, 'ы'.code))
        assertEquals('N', ShortcutKeys.letter(Key.Unknown, 'т'.code))
        assertNull(ShortcutKeys.letter(Key.Unknown, '1'.code))
    }

    @Test
    fun commands() {
        assertEquals(EditorCommand.Undo, ShortcutKeys.command('Z', shift = false, nonLatin = false))
        assertEquals(EditorCommand.Redo, ShortcutKeys.command('Z', shift = true, nonLatin = false))
        assertEquals(EditorCommand.Redo, ShortcutKeys.command('Y', shift = false, nonLatin = false))
        assertEquals(EditorCommand.Paste, ShortcutKeys.command('V', shift = false, nonLatin = false))
        assertNull(ShortcutKeys.command('Q', shift = false, nonLatin = false))
    }

    @Test
    fun clipboardCommandsAreOursOnlyForNonLatinLayout() {
        assertNull(ShortcutKeys.command('C', shift = false, nonLatin = false))
        assertEquals(EditorCommand.Copy, ShortcutKeys.command('C', shift = false, nonLatin = true))
        assertEquals(EditorCommand.SelectAll, ShortcutKeys.command('A', shift = false, nonLatin = true))
    }
}
