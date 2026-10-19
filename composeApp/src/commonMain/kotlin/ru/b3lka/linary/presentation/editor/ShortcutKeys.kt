package ru.b3lka.linary.presentation.editor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.utf16CodePoint

/**
 * Горячие клавиши, не зависящие от раскладки.
 *
 * В Linux при русской раскладке Compose не узнаёт в Ctrl+Я сочетание Ctrl+Z: у события нет
 * латинского кода клавиши, а поле ввода печатает «я». Поэтому букву клавиши определяем
 * по символу через раскладку ЙЦУКЕН, а набор букв с зажатым Ctrl подавляем.
 */
object ShortcutKeys {

    private val latinKeys: Map<Key, Char> = listOf(
        Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G, Key.H, Key.I, Key.J, Key.K, Key.L, Key.M,
        Key.N, Key.O, Key.P, Key.Q, Key.R, Key.S, Key.T, Key.U, Key.V, Key.W, Key.X, Key.Y, Key.Z,
    ).zip('A'..'Z').toMap()

    /**
     * Буквы ЙЦУКЕН → латинские буквы на тех же клавишах QWERTY.
     */
    private val cyrillicToLatin: Map<Char, Char> =
        "йцукенгшщзфывапролдячсмить".zip("QWERTYUIOPASDFGHJKLZXCVBNM").toMap()

    /**
     * Латинская буква клавиши (`'A'..'Z'`) при Ctrl без Alt, иначе null.
     * Работает и в латинской, и в русской раскладке.
     */
    fun ctrlLetter(event: KeyEvent): Char? =
        if (event.isCtrlPressed && !event.isAltPressed) letter(event.key, event.utf16CodePoint) else null

    /**
     * Команда редактора для события или null.
     */
    fun command(event: KeyEvent): EditorCommand? {
        val nonLatin = nonLatinLetter(event.utf16CodePoint) != null
        return command(ctrlLetter(event), shift = event.isShiftPressed, nonLatin = nonLatin)
    }

    /**
     * Ctrl + буква нелатинской раскладки: такое событие нельзя пропускать в поле, иначе напечатается буква.
     */
    fun isSuppressedTyping(event: KeyEvent): Boolean =
        event.isCtrlPressed && !event.isAltPressed && nonLatinLetter(event.utf16CodePoint) != null

    /**
     * Латинская буква клавиши по коду клавиши, а если он не латинский — по напечатанному символу.
     */
    fun letter(key: Key, codePoint: Int): Char? = latinKeys[key] ?: nonLatinLetter(codePoint)

    /**
     * Команда по букве. Copy/Cut/SelectAll — только для нелатинской раскладки: с латинской
     * их обрабатывает само поле ввода. Paste — всегда: в буфере может быть картинка.
     */
    fun command(letter: Char?, shift: Boolean, nonLatin: Boolean): EditorCommand? = when (letter) {
        'S' -> EditorCommand.Save
        'T' -> EditorCommand.InsertTime
        'Z' -> if (shift) EditorCommand.Redo else EditorCommand.Undo
        'Y' -> EditorCommand.Redo
        'B' -> EditorCommand.Bold
        'I' -> EditorCommand.Italic
        'U' -> EditorCommand.Underline
        'V' -> EditorCommand.Paste
        'C' -> EditorCommand.Copy.takeIf { nonLatin }
        'X' -> EditorCommand.Cut.takeIf { nonLatin }
        'A' -> EditorCommand.SelectAll.takeIf { nonLatin }
        else -> null
    }

    private fun nonLatinLetter(codePoint: Int): Char? {
        if (codePoint <= 0 || codePoint > Char.MAX_VALUE.code) return null
        return cyrillicToLatin[codePoint.toChar().lowercaseChar()]
    }
}
