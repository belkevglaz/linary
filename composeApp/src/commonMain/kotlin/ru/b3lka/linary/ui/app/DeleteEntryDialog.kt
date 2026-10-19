package ru.b3lka.linary.ui.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_cancel
import ru.b3lka.linary.resources.action_delete
import ru.b3lka.linary.resources.delete_text
import ru.b3lka.linary.resources.delete_title
import ru.b3lka.linary.ui.common.LocalDateFormatter

/**
 * Подтверждение удаления записи.
 */
@Composable
internal fun DeleteEntryDialog(entry: DiaryEntry, onDelete: () -> Unit, onCancel: () -> Unit) {
    val formatter = LocalDateFormatter.current
    val danger = MaterialTheme.colorScheme.error
    val title = "${formatter.longDate(entry.date)}, ${formatter.time(entry.dateTime)}"
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Outlined.Delete, null, tint = danger) },
        title = { Text(stringResource(Res.string.delete_title)) },
        text = { Text(stringResource(Res.string.delete_text, title)) },
        confirmButton = {
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = danger)) {
                Text(stringResource(Res.string.action_delete))
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
