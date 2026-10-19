package ru.b3lka.linary.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.time.YearMonth
import ru.b3lka.linary.presentation.state.ViewerState
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_delete
import ru.b3lka.linary.resources.action_edit
import ru.b3lka.linary.resources.action_write
import ru.b3lka.linary.resources.entry_none_for_day
import ru.b3lka.linary.ui.common.LocalDateFormatter
import ru.b3lka.linary.ui.common.Tooltip
import ru.b3lka.linary.ui.theme.LinaryColors
import kotlin.math.round

private const val MAX_RATING = 5
private const val COORDINATE_SCALE = 100_000.0

/**
 * Открытая запись: дата, вложения, текст, теги, геометка.
 */
@Composable
fun EntryDetail(
    entry: DiaryEntry?,
    date: LocalDate,
    onOpenPhoto: (ViewerState) -> Unit,
    modifier: Modifier = Modifier,
    onEdit: ((DiaryEntry) -> Unit)? = null,
    onCreate: (() -> Unit)? = null,
    dayEntries: List<DiaryEntry> = emptyList(),
    onSelectEntry: (DiaryEntry) -> Unit = {},
    onDelete: ((DiaryEntry) -> Unit)? = null,
) {
    if (entry == null) {
        NoEntry(date, modifier, onCreate)
        return
    }
    val scroll = rememberScrollState()
    LaunchedEffect(entry.id) { scroll.scrollTo(0) }

    // Шапка с датой на месте, прокручивается только содержимое записи
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 12.dp)) {
            if (dayEntries.size > 1) {
                DayEntriesSwitcher(dayEntries, entry, onSelectEntry)
                Spacer(Modifier.height(12.dp))
            }
            EntryHeader(entry, onEdit, onDelete)
        }
        // Разделитель виден, когда текст ушёл под шапку; место под него есть всегда, чтобы ничего не прыгало
        HorizontalDivider(
            color = if (scroll.value > 0) MaterialTheme.colorScheme.outlineVariant else Color.Transparent,
        )
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 32.dp),
        ) {
            if (entry.media.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                EntryGallery(entry, onOpenPhoto)
            }
            if (entry.heading.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                SelectionContainer {
                    Text(
                        entry.heading,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            EntryText(entry.html)
            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                EntryTags(entry.tags)
            }
            if (entry.hasLocation) {
                Spacer(Modifier.height(16.dp))
                EntryLocation(entry)
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun NoEntry(date: LocalDate, modifier: Modifier, onCreate: (() -> Unit)?) {
    val formatter = LocalDateFormatter.current
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${formatter.weekdayFull(date.dayOfWeek).capitalized()}, ${formatter.longDate(date)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(stringResource(Res.string.entry_none_for_day), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (onCreate != null) {
                Spacer(Modifier.height(16.dp))
                FilledTonalButton(onClick = onCreate) {
                    Icon(Icons.Filled.Edit, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(Res.string.action_write))
                }
            }
        }
    }
}

/**
 * Переключатель между записями одного дня (по времени создания).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayEntriesSwitcher(entries: List<DiaryEntry>, current: DiaryEntry, onSelect: (DiaryEntry) -> Unit) {
    val formatter = LocalDateFormatter.current
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { e ->
            FilterChip(
                selected = e.id == current.id,
                onClick = { onSelect(e) },
                label = { Text(formatter.time(e.dateTime)) },
            )
        }
    }
}

@Composable
private fun EntryHeader(entry: DiaryEntry, onEdit: ((DiaryEntry) -> Unit)?, onDelete: ((DiaryEntry) -> Unit)?) {
    val formatter = LocalDateFormatter.current
    val date = entry.date
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            date.dayOfMonth.toString(),
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(14.dp))
        Column {
            Text(
                formatter.monthYear(YearMonth.of(date)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "${formatter.weekdayFull(date.dayOfWeek).capitalized()}, ${formatter.time(entry.dateTime)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        if (entry.rating > 0) Rating(entry.rating)
        if (onEdit != null) {
            Spacer(Modifier.size(12.dp))
            val edit = stringResource(Res.string.action_edit)
            Tooltip(edit) {
                FilledTonalIconButton(onClick = { onEdit(entry) }) { Icon(Icons.Filled.Edit, edit) }
            }
        }
        if (onDelete != null) {
            Spacer(Modifier.size(4.dp))
            val delete = stringResource(Res.string.action_delete)
            Tooltip(delete) {
                IconButton(onClick = { onDelete(entry) }) { Icon(Icons.Outlined.Delete, delete) }
            }
        }
    }
}

@Composable
private fun Rating(rating: Int) {
    Row {
        for (i in 1..MAX_RATING) {
            Icon(
                if (i <= rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                contentDescription = null,
                tint = LinaryColors.Star,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun EntryLocation(entry: DiaryEntry) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(6.dp))
        Text(
            "${roundCoordinate(entry.latitude)}, ${roundCoordinate(entry.longitude)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun String.capitalized(): String = replaceFirstChar { it.uppercase() }

private fun roundCoordinate(value: Double): String = (round(value * COORDINATE_SCALE) / COORDINATE_SCALE).toString()
