package ru.b3lka.linary.ui.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.Diary
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.tags_none

/**
 * Шапка раздела «Теги»: все теги чипами с числом записей; выбранный фильтрует ленту.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TagsHeader(diary: Diary?, selected: Long?, onSelect: (Long?) -> Unit) {
    val tags = diary?.tags.orEmpty().sortedBy { it.value.lowercase() }
    if (tags.isEmpty()) {
        Text(
            stringResource(Res.string.tags_none),
            modifier = Modifier.padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val counts = diary?.tagCounts.orEmpty()
    FlowRow(
        Modifier.fillMaxWidth().padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tags.forEach { tag ->
            FilterChip(
                selected = tag.id == selected,
                onClick = { onSelect(if (tag.id == selected) null else tag.id) },
                label = { Text("${tag.value}  ${counts[tag.id] ?: 0}") },
            )
        }
    }
}
