package ru.b3lka.linary.ui.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.ui.common.LocalDateFormatter
import ru.b3lka.linary.ui.media.MediaImage

private const val PREVIEW_CHARS = 400

/**
 * Карточка записи в ленте: число и день недели, превью текста, миниатюра первого фото.
 */
@Composable
fun EntryCard(entry: DiaryEntry, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val formatter = LocalDateFormatter.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) colors.secondaryContainer else colors.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                entry.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.primary,
            )
            Text(
                formatter.weekdayShort(entry.date.dayOfWeek),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            if (entry.heading.isNotBlank()) {
                Text(
                    entry.heading,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                entry.plainText.replace('\n', ' ').take(PREVIEW_CHARS),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (entry.heading.isNotBlank()) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatter.time(entry.dateTime),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        entry.images.firstOrNull()?.let { thumb ->
            Spacer(Modifier.width(10.dp))
            MediaImage(thumb, maxSize = 220, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)))
        }
    }
}
