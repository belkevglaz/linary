package ru.b3lka.linary.ui.app

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.repository.DiaryWriteException
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_cancel
import ru.b3lka.linary.resources.action_save_anyway
import ru.b3lka.linary.resources.save_error_title
import ru.b3lka.linary.ui.common.writeErrorText

/**
 * Сообщение о неудачном сохранении или удалении. Редактор при этом остаётся открытым с текстом.
 * При конфликте ([onSaveAnyway] != null) можно сохранить свою версию поверх изменённой.
 */
@Composable
internal fun SaveErrorDialog(error: DiaryWriteException, onDismiss: () -> Unit, onSaveAnyway: (() -> Unit)?) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.save_error_title)) },
        text = { Text(writeErrorText(error)) },
        confirmButton = {
            if (onSaveAnyway != null) {
                TextButton(onClick = onSaveAnyway) { Text(stringResource(Res.string.action_save_anyway)) }
            } else {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        },
        dismissButton = if (onSaveAnyway != null) {
            { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } }
        } else {
            null
        },
    )
}
