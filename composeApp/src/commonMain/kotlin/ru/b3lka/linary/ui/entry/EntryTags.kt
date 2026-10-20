package ru.b3lka.linary.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import ru.b3lka.linary.core.model.EntryTag
import ru.b3lka.linary.ui.tags.TagChip

/**
 * Теги записи плашками в цвет тега. У тегов-трекеров рядом показывается значение.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryTags(tags: List<EntryTag>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { t ->
            val value = t.trackingValue?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
            TagChip(t.tag, "#${t.tag.value}$value")
        }
    }
}
