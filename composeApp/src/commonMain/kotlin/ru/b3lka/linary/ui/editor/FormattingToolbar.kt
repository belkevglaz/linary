package ru.b3lka.linary.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.editor.EditorController
import ru.b3lka.linary.presentation.editor.TextFormatting
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.format_bold
import ru.b3lka.linary.resources.format_highlight
import ru.b3lka.linary.resources.format_italic
import ru.b3lka.linary.resources.format_link
import ru.b3lka.linary.resources.format_list
import ru.b3lka.linary.resources.format_redo
import ru.b3lka.linary.resources.format_strikethrough
import ru.b3lka.linary.resources.format_text_color
import ru.b3lka.linary.resources.format_underline
import ru.b3lka.linary.resources.format_undo
import ru.b3lka.linary.resources.media_add
import ru.b3lka.linary.ui.common.LocalAppLocked
import ru.b3lka.linary.ui.common.Tooltip
import ru.b3lka.linary.ui.theme.EditorPalette

/**
 * Панель форматирования как на телефоне: список, B, I, U, S, маркер, цвет текста, ссылка, фото,
 * отмена и повтор.
 * Кнопки не забирают фокус: иначе поле ввода теряет фокус и сбрасывает выделение до того,
 * как к нему применится стиль.
 */
@Composable
internal fun FormattingToolbar(controller: EditorController, onAddPhoto: () -> Unit) {
    val state = controller.text
    val history = controller.history
    var linkSelection by remember { mutableStateOf<TextRange?>(null) }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextStyleButtons(state)
            ColorButtons(state)
            FormatButton(Icons.Filled.Link, Res.string.format_link, state.isLink) {
                // Диалог заберёт фокус, и поле сбросит выделение — запоминаем его заранее
                linkSelection = state.selection
            }
            FormatButton(Icons.Outlined.AddPhotoAlternate, Res.string.media_add, active = false) { onAddPhoto() }
            VerticalDivider(Modifier.height(28.dp).padding(horizontal = 8.dp))
            FormatButton(
                Icons.AutoMirrored.Filled.Undo,
                Res.string.format_undo,
                active = false,
                enabled = history.canUndo,
                shortcut = "Ctrl+Z",
            ) { controller.undo() }
            FormatButton(
                Icons.AutoMirrored.Filled.Redo,
                Res.string.format_redo,
                active = false,
                enabled = history.canRedo,
                shortcut = "Ctrl+Shift+Z",
            ) { controller.redo() }
        }
    }

    val locked = LocalAppLocked.current
    linkSelection?.takeUnless { locked }?.let { selection ->
        LinkDialog(state, selection, onDismiss = { linkSelection = null })
    }
}

/**
 * Список, жирный, курсив, подчёркнутый, зачёркнутый.
 */
@Composable
private fun TextStyleButtons(state: RichTextState) {
    val style = state.currentSpanStyle
    FormatButton(Icons.AutoMirrored.Filled.FormatListBulleted, Res.string.format_list, state.isUnorderedList) {
        state.toggleUnorderedList()
    }
    FormatButton(
        Icons.Filled.FormatBold,
        Res.string.format_bold,
        style.fontWeight == FontWeight.Bold,
        shortcut = "Ctrl+B",
    ) {
        TextFormatting.toggleBold(state)
    }
    FormatButton(
        Icons.Filled.FormatItalic,
        Res.string.format_italic,
        style.fontStyle == FontStyle.Italic,
        shortcut = "Ctrl+I",
    ) {
        TextFormatting.toggleItalic(state)
    }
    FormatButton(
        Icons.Filled.FormatUnderlined,
        Res.string.format_underline,
        style.has(TextDecoration.Underline),
        shortcut = "Ctrl+U",
    ) {
        TextFormatting.toggleUnderline(state)
    }
    FormatButton(
        Icons.Filled.FormatStrikethrough,
        Res.string.format_strikethrough,
        style.has(TextDecoration.LineThrough),
    ) {
        TextFormatting.toggleStrikethrough(state)
    }
}

/**
 * Маркер и цвет текста — кнопки с выпадающей палитрой.
 */
@Composable
private fun ColorButtons(state: RichTextState) {
    var palette by remember { mutableStateOf<PaletteKind?>(null) }
    val style = state.currentSpanStyle
    Box {
        FormatButton(Icons.Filled.FormatColorFill, Res.string.format_highlight, style.background.isSet()) {
            palette = PaletteKind.Highlight
        }
        ColorPaletteMenu(
            expanded = palette == PaletteKind.Highlight,
            colors = EditorPalette.highlight,
            onPick = { applyBackground(state, it) },
            onDismiss = { palette = null },
        )
    }
    Box {
        FormatButton(Icons.Filled.FormatColorText, Res.string.format_text_color, style.color.isSet()) {
            palette = PaletteKind.Text
        }
        ColorPaletteMenu(
            expanded = palette == PaletteKind.Text,
            colors = EditorPalette.text,
            onPick = { applyColor(state, it) },
            onDismiss = { palette = null },
        )
    }
}

private enum class PaletteKind { Highlight, Text }

@Composable
private fun FormatButton(
    icon: ImageVector,
    label: StringResource,
    active: Boolean,
    enabled: Boolean = true,
    shortcut: String? = null,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val text = stringResource(label)
    Tooltip(if (shortcut != null) "$text ($shortcut)" else text) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .focusProperties { canFocus = false }
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (active) colors.secondaryContainer else Color.Transparent)
                .width(44.dp),
        ) {
            Icon(icon, contentDescription = text)
        }
    }
}

private fun SpanStyle.has(decoration: TextDecoration): Boolean = textDecoration?.contains(decoration) == true

private fun Color.isSet(): Boolean = this != Color.Unspecified && this != Color.Transparent

private fun applyColor(state: RichTextState, color: Color?) {
    val current = state.currentSpanStyle.color
    if (current.isSet()) state.removeSpanStyle(SpanStyle(color = current))
    if (color != null) state.addSpanStyle(SpanStyle(color = color))
}

private fun applyBackground(state: RichTextState, color: Color?) {
    val current = state.currentSpanStyle.background
    if (current.isSet()) state.removeSpanStyle(SpanStyle(background = current))
    if (color != null) state.addSpanStyle(SpanStyle(background = color))
}
