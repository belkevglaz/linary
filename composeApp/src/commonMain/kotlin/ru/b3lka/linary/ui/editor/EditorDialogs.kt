package ru.b3lka.linary.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.richeditor.model.RichTextState
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_apply
import ru.b3lka.linary.resources.action_cancel
import ru.b3lka.linary.resources.action_discard
import ru.b3lka.linary.resources.action_keep_editing
import ru.b3lka.linary.resources.editor_discard_text
import ru.b3lka.linary.resources.editor_discard_title
import ru.b3lka.linary.resources.link_remove
import ru.b3lka.linary.resources.link_text
import ru.b3lka.linary.resources.link_title
import ru.b3lka.linary.resources.link_url

/**
 * Добавление или правка ссылки: на выделенный текст, новым текстом или у существующей ссылки.
 * [selection] — выделение в момент открытия: пока открыт диалог, поле ввода без фокуса его сбрасывает.
 */
@Composable
internal fun LinkDialog(state: RichTextState, selection: TextRange, onDismiss: () -> Unit) {
    val editing = state.isLink
    val hasSelection = !selection.collapsed
    var text by remember { mutableStateOf(state.selectedLinkText.orEmpty()) }
    var url by remember { mutableStateOf(state.selectedLinkUrl ?: "https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.link_title)) },
        text = {
            Column {
                if (!editing && !hasSelection) {
                    OutlinedTextField(text, { text = it }, label = { Text(stringResource(Res.string.link_text)) })
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(url, { url = it }, label = { Text(stringResource(Res.string.link_url)) })
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.isNotBlank() && (editing || hasSelection || text.isNotBlank()),
                onClick = {
                    state.selection = selection
                    when {
                        editing -> state.updateLink(url)
                        hasSelection -> state.addLinkToSelection(url)
                        else -> state.addLink(text = text, url = url)
                    }
                    onDismiss()
                },
            ) { Text(stringResource(Res.string.action_apply)) }
        },
        dismissButton = {
            if (editing) {
                TextButton(onClick = {
                    state.selection = selection
                    state.removeLink()
                    onDismiss()
                }) { Text(stringResource(Res.string.link_remove)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
            }
        },
    )
}

/**
 * Подтверждение выхода без сохранения.
 */
@Composable
internal fun DiscardDialog(onDiscard: () -> Unit, onKeepEditing: () -> Unit) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text(stringResource(Res.string.editor_discard_title)) },
        text = { Text(stringResource(Res.string.editor_discard_text)) },
        confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(Res.string.action_discard)) } },
        dismissButton = {
            TextButton(onClick = onKeepEditing) { Text(stringResource(Res.string.action_keep_editing)) }
        },
    )
}
