package ru.b3lka.linary.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.editor.EditorSession
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_cancel
import ru.b3lka.linary.resources.action_save
import ru.b3lka.linary.resources.editor_rating
import ru.b3lka.linary.resources.editor_saving
import ru.b3lka.linary.ui.common.LocalDateFormatter
import ru.b3lka.linary.ui.common.Tooltip
import ru.b3lka.linary.ui.theme.LinaryColors

private const val MAX_RATING = 5

/**
 * Шапка редактора как на телефоне: «назад», дата и время, теги, оценка и кнопка сохранения.
 */
@Composable
internal fun EditorHeader(
    session: EditorSession,
    saving: Boolean,
    onSave: () -> Unit,
    onClose: () -> Unit,
    onInsertTime: () -> Unit,
    onDelete: (() -> Unit)?,
    tags: EditorTagSource?,
) {
    val formatter = LocalDateFormatter.current
    val date = session.dateTime.date
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val back = stringResource(Res.string.action_cancel)
            Tooltip("$back (Esc)") {
                IconButton(onClick = onClose, enabled = !saving) { Icon(Icons.AutoMirrored.Filled.ArrowBack, back) }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${formatter.weekdayShort(date.dayOfWeek)}, ${formatter.longDate(date)}",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    formatter.time(session.dateTime),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (tags != null) TagPickerButton(session, tags)
            RatingBar(session.rating, onChange = { session.rating = it })
            Spacer(Modifier.width(16.dp))
            SaveButton(saving, onSave)
            EditorMenu(onInsertTime, onDelete)
        }
    }
}

@Composable
private fun SaveButton(saving: Boolean, onSave: () -> Unit) {
    Button(onClick = onSave, enabled = !saving) {
        if (saving) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.editor_saving))
        } else {
            Icon(Icons.Filled.Check, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.action_save))
        }
    }
}

/**
 * Оценка звёздами; повторный клик по выбранной звезде убирает оценку.
 */
@Composable
private fun RatingBar(rating: Int, onChange: (Int) -> Unit) {
    val description = stringResource(Res.string.editor_rating)
    Row {
        for (i in 1..MAX_RATING) {
            Tooltip("$description $i") {
                IconButton(onClick = { onChange(if (i == rating) 0 else i) }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "$description $i",
                        tint = if (i <= rating) LinaryColors.Star else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}
