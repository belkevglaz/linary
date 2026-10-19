package ru.b3lka.linary.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_delete
import ru.b3lka.linary.resources.action_insert_time
import ru.b3lka.linary.resources.editor_more
import ru.b3lka.linary.ui.common.Tooltip

/**
 * Меню «⋮» редактора: «Вставить время» и, для уже сохранённой записи, «Удалить».
 */
@Composable
internal fun EditorMenu(onInsertTime: () -> Unit, onDelete: (() -> Unit)?) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        val more = stringResource(Res.string.editor_more)
        Tooltip(more) {
            IconButton(onClick = { expanded = true }) { Icon(Icons.Filled.MoreVert, more) }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_insert_time)) },
                leadingIcon = { Icon(Icons.Outlined.Schedule, null) },
                trailingIcon = { Text("Ctrl+T") },
                onClick = {
                    expanded = false
                    onInsertTime()
                },
            )
            if (onDelete != null) {
                HorizontalDivider()
                val danger = MaterialTheme.colorScheme.error
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.action_delete), color = danger) },
                    leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = danger) },
                    onClick = {
                        expanded = false
                        onDelete()
                    },
                )
            }
        }
    }
}
