package ru.b3lka.linary.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.DiaryTag
import ru.b3lka.linary.core.model.TagChange
import ru.b3lka.linary.presentation.editor.EditorSession
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.editor_tags
import ru.b3lka.linary.resources.tag_new_ellipsis
import ru.b3lka.linary.resources.tag_remove
import ru.b3lka.linary.ui.common.LocalAppLocked
import ru.b3lka.linary.ui.common.Tooltip
import ru.b3lka.linary.ui.tags.TagColorDot
import ru.b3lka.linary.ui.tags.TagDialog
import ru.b3lka.linary.ui.tags.contentColorFor
import ru.b3lka.linary.ui.tags.displayColor

/**
 * Теги для редактора: все теги журнала и создание нового (возвращает его id или null при ошибке).
 */
class EditorTagSource(val all: List<DiaryTag>, val create: suspend (TagChange) -> Long?)

/**
 * Кнопка «Теги» в шапке редактора: меню со всеми тегами (отметить — прикрепить к записи)
 * и пунктом «Новый тег…», который создаёт тег и сразу прикрепляет его.
 */
@Composable
internal fun TagPickerButton(session: EditorSession, source: EditorTagSource) {
    var expanded by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val label = stringResource(Res.string.editor_tags)
    Box {
        Tooltip(label) { IconButton(onClick = { expanded = true }) { Icon(Icons.Outlined.Sell, label) } }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            source.all.sortedBy { it.value.lowercase() }.forEach { tag ->
                val color = tag.displayColor
                DropdownMenuItem(
                    text = { Text(tag.value) },
                    leadingIcon = { Checkbox(checked = tag.id in session.tagIds, onCheckedChange = null) },
                    trailingIcon = if (color != null) ({ TagColorDot(color) }) else null,
                    onClick = { session.toggleTag(tag.id) },
                )
            }
            if (source.all.isNotEmpty()) HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.tag_new_ellipsis)) },
                leadingIcon = { Icon(Icons.Filled.Add, null) },
                onClick = {
                    expanded = false
                    creating = true
                },
            )
        }
    }
    if (creating && !LocalAppLocked.current) {
        TagDialog(
            tag = null,
            existing = source.all,
            onSave = { change ->
                creating = false
                scope.launch {
                    source.create(change)?.let { id -> if (id !in session.tagIds) session.tagIds += id }
                }
            },
            onDismiss = { creating = false },
        )
    }
}

/**
 * Теги записи в редакторе — плашки в цвет тега; щелчок по плашке убирает тег. Пустой ряд не показывается.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditorTagRow(session: EditorSession, all: List<DiaryTag>) {
    val tags = session.tagIds.mapNotNull { id -> all.firstOrNull { it.id == id } }
    if (tags.isEmpty()) return
    val remove = stringResource(Res.string.tag_remove)
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Tooltip(remove) { RemovableTag(tag, onRemove = { session.toggleTag(tag.id) }) }
        }
    }
}

@Composable
private fun RemovableTag(tag: DiaryTag, onRemove: () -> Unit) {
    val color = tag.displayColor
    Surface(
        shape = RoundedCornerShape(50),
        color = color ?: MaterialTheme.colorScheme.secondaryContainer,
        contentColor = color?.let(::contentColorFor) ?: MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onRemove),
    ) {
        Row(
            Modifier.padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("#${tag.value}", style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Filled.Close, null, Modifier.padding(start = 4.dp).size(14.dp))
        }
    }
}
