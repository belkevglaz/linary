package ru.b3lka.linary.presentation.editor

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mohamedrejeb.richeditor.model.RichTextState

/**
 * Действия форматирования над текстом редактора — общие для панели и горячих клавиш.
 */
object TextFormatting {

    fun toggleBold(state: RichTextState) = state.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))

    fun toggleItalic(state: RichTextState) = state.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))

    fun toggleUnderline(state: RichTextState) =
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))

    fun toggleStrikethrough(state: RichTextState) =
        state.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))

    fun selectedText(state: RichTextState): String {
        val text = state.annotatedString.text
        val selection = state.selection
        return text.substring(selection.min.coerceIn(0, text.length), selection.max.coerceIn(0, text.length))
    }

    fun selectAll(state: RichTextState) {
        state.selection = TextRange(0, state.annotatedString.text.length)
    }
}
