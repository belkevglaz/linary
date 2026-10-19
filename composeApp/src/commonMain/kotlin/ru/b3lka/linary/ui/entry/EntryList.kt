package ru.b3lka.linary.ui.entry

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.time.YearMonth
import ru.b3lka.linary.ui.common.LocalDateFormatter

private sealed interface ListRow {
    data class Header(val month: YearMonth) : ListRow

    data class Item(val entry: DiaryEntry) : ListRow
}

/**
 * Лента записей, сгруппированная по месяцам (заголовки как в календаре Diarium).
 */
@Composable
fun EntryList(
    entries: List<DiaryEntry>,
    selectedId: Long?,
    onSelect: (DiaryEntry) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.TopCenter) {
            Text(
                emptyText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
        return
    }
    val rows = remember(entries) { groupByMonth(entries) }
    // Список сразу стоит на выбранной записи (с заголовком её месяца над ней)
    val listState = remember(rows) { LazyListState(firstVisibleItemIndex = startIndex(rows, selectedId)) }
    LaunchedEffect(listState, selectedId) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }.first { it.isNotEmpty() }
        val i = rows.indexOfFirst { it is ListRow.Item && it.entry.id == selectedId }
        if (i >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.index == i }) {
            listState.animateScrollToItem(startIndex(rows, selectedId))
        }
    }
    val formatter = LocalDateFormatter.current
    LazyColumn(modifier, state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
        items(rows.size, key = { i -> rowKey(rows[i]) }) { i ->
            when (val row = rows[i]) {
                is ListRow.Header -> Text(
                    formatter.monthYear(row.month),
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is ListRow.Item -> EntryCard(
                    row.entry,
                    selected = row.entry.id == selectedId,
                    onClick = { onSelect(row.entry) },
                )
            }
        }
    }
}

private fun groupByMonth(entries: List<DiaryEntry>): List<ListRow> = buildList {
    var current: YearMonth? = null
    for (e in entries) {
        val month = YearMonth.of(e.date)
        if (month != current) {
            add(ListRow.Header(month))
            current = month
        }
        add(ListRow.Item(e))
    }
}

private fun startIndex(rows: List<ListRow>, selectedId: Long?): Int =
    (rows.indexOfFirst { it is ListRow.Item && it.entry.id == selectedId } - 1).coerceAtLeast(0)

private fun rowKey(row: ListRow): Any = when (row) {
    is ListRow.Header -> "h${row.month.year}-${row.month.month}"
    is ListRow.Item -> row.entry.id
}
