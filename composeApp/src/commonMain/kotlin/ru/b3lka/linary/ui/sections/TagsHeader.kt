package ru.b3lka.linary.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.Diary
import ru.b3lka.linary.core.model.DiaryTag
import ru.b3lka.linary.core.model.TagChange
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.tag_edit_hint
import ru.b3lka.linary.resources.tag_new
import ru.b3lka.linary.resources.tags_none
import ru.b3lka.linary.ui.common.LocalAppLocked
import ru.b3lka.linary.ui.common.Tooltip
import ru.b3lka.linary.ui.common.onSecondaryClick
import ru.b3lka.linary.ui.tags.TagColorDot
import ru.b3lka.linary.ui.tags.TagDialog
import ru.b3lka.linary.ui.tags.contentColorFor
import ru.b3lka.linary.ui.tags.displayColor

/**
 * Что открыто в диалоге тега: новый тег или правка существующего.
 */
private sealed interface TagDialogTarget {
    data object New : TagDialogTarget

    data class Edit(val tag: DiaryTag) : TagDialogTarget
}

/**
 * Шапка раздела «Теги»: все теги чипами в своих цветах и с числом записей; выбранный фильтрует ленту.
 * Если журнал можно менять ([onSaveTag] не null): «Новый тег», а правый клик по тегу — изменить название и цвет.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagsHeader(diary: Diary?, selected: Long?, onSelect: (Long?) -> Unit, onSaveTag: ((TagChange) -> Unit)? = null) {
    val tags = diary?.tags.orEmpty().sortedBy { it.value.lowercase() }
    var dialog by remember { mutableStateOf<TagDialogTarget?>(null) }
    val counts = diary?.tagCounts.orEmpty()
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        if (tags.isEmpty()) {
            Text(
                stringResource(Res.string.tags_none),
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val editHint = if (onSaveTag != null) stringResource(Res.string.tag_edit_hint) else null
            tags.forEach { tag ->
                Tooltip(editHint) {
                    TagFilterChip(
                        tag,
                        count = counts[tag.id] ?: 0,
                        selected = tag.id == selected,
                        onClick = { onSelect(if (tag.id == selected) null else tag.id) },
                        modifier = if (onSaveTag != null) {
                            Modifier.onSecondaryClick { dialog = TagDialogTarget.Edit(tag) }
                        } else {
                            Modifier
                        },
                    )
                }
            }
            if (onSaveTag != null) {
                AssistChip(
                    onClick = { dialog = TagDialogTarget.New },
                    label = { Text(stringResource(Res.string.tag_new)) },
                    leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) },
                )
            }
        }
    }
    val target = dialog
    if (target != null && onSaveTag != null && !LocalAppLocked.current) {
        TagDialog(
            tag = (target as? TagDialogTarget.Edit)?.tag,
            existing = tags,
            onSave = { change ->
                dialog = null
                onSaveTag(change)
            },
            onDismiss = { dialog = null },
        )
    }
}

/**
 * Тег-фильтр: кружок его цвета, название и число записей; выбранный закрашивается цветом тега.
 */
@Composable
private fun TagFilterChip(tag: DiaryTag, count: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val color = tag.displayColor
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text("${tag.value}  $count") },
        leadingIcon = if (color != null && !selected) ({ TagColorDot(color) }) else null,
        colors = if (color != null) {
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = color,
                selectedLabelColor = contentColorFor(color),
                selectedLeadingIconColor = contentColorFor(color),
            )
        } else {
            FilterChipDefaults.filterChipColors()
        },
        modifier = modifier,
    )
}
