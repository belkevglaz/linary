package ru.b3lka.linary.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.b3lka.linary.core.html.CssStyles
import ru.b3lka.linary.core.model.EntryTag

/**
 * Теги записи чипами. У тегов-трекеров рядом показывается значение.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EntryTags(tags: List<EntryTag>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { t ->
            val tagColor = t.tag.color?.let { CssStyles.parseColor(it) }
            val value = t.trackingValue?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
            Surface(
                shape = RoundedCornerShape(50),
                color = tagColor?.copy(alpha = 0.18f) ?: MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    "#${t.tag.value}$value",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
