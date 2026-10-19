package ru.b3lka.linary.ui.calendar

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_new_entry
import ru.b3lka.linary.ui.common.LocalDateFormatter

private const val PREVIEW_CHARS = 60

/**
 * Меню дня (долгое нажатие или правый клик): записи дня по времени создания и «Новая запись».
 */
@Composable
internal fun DayMenu(
    expanded: Boolean,
    entries: List<DiaryEntry>,
    onPick: (DiaryEntry) -> Unit,
    onNewEntry: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val formatter = LocalDateFormatter.current
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = Modifier.widthIn(max = 360.dp)) {
        entries.forEach { entry ->
            DropdownMenuItem(
                text = {
                    Text(
                        "${formatter.time(entry.dateTime)}  ${entry.plainText.replace('\n', ' ').take(PREVIEW_CHARS)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                onClick = {
                    onDismiss()
                    onPick(entry)
                },
            )
        }
        if (onNewEntry != null) {
            if (entries.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_new_entry)) },
                leadingIcon = { Icon(Icons.Filled.Add, null) },
                onClick = {
                    onDismiss()
                    onNewEntry()
                },
            )
        }
    }
}
