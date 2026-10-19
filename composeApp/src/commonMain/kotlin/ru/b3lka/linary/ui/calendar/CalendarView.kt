package ru.b3lka.linary.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import ru.b3lka.linary.core.model.Diary
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.time.YearMonth
import ru.b3lka.linary.presentation.calendar.CalendarGeometry
import ru.b3lka.linary.presentation.calendar.DayActions
import ru.b3lka.linary.ui.common.LocalDateFormatter

private val CellGap = 2.dp

/**
 * Календарь как в Diarium: месяцы идут сплошной лентой сверху вниз, плитки дней вплотную.
 *
 * Список создаётся заново при каждом показе и при загрузке журнала и сразу стоит
 * на месяце выбранного дня. Если выбранный день меняется извне (кнопка «Сегодня»)
 * и его неделю не видно, календарь прокручивается так, чтобы она оказалась посередине.
 */
@Composable
fun CalendarView(
    diary: Diary?,
    months: List<YearMonth>,
    selected: LocalDate,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    onPickEntry: (DiaryEntry) -> Unit = {},
    onNewEntry: ((LocalDate) -> Unit)? = null,
) {
    val listState = remember(months.size) {
        LazyListState(firstVisibleItemIndex = months.indexOf(YearMonth.of(selected)).coerceAtLeast(0))
    }
    LaunchedEffect(listState, selected) {
        val index = months.indexOf(YearMonth.of(selected))
        if (index < 0) return@LaunchedEffect
        // Дождаться раскладки именно этого списка (а не устаревших данных прошлого показа)
        snapshotFlow { listState.layoutInfo }
            .first { it.totalItemsCount == months.size && it.visibleItemsInfo.isNotEmpty() }
        if (!CalendarGeometry.isWeekVisible(listState, index, months[index], selected)) {
            centerWeek(listState, index, months[index], selected)
        }
    }

    Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
        WeekdayHeader()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(months, key = { it.year * 100 + it.month }) { month ->
                MonthBlock(month, diary, selected, today, DayActions(onSelect, onPickEntry, onNewEntry))
            }
        }
    }
}

private suspend fun centerWeek(listState: LazyListState, index: Int, month: YearMonth, date: LocalDate) {
    listState.scrollToItem(index)
    val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return
    val viewport = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
    val target = info.size * CalendarGeometry.weekFraction(month, date) - viewport / 2f
    if (target > 0) listState.scrollBy(target)
}

@Composable
private fun WeekdayHeader() {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().background(colors.surfaceContainerHighest).padding(vertical = 8.dp)) {
        LocalDateFormatter.current.weekdayHeaders.forEach { day ->
            Text(
                day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface,
            )
        }
    }
}

@Composable
private fun MonthBlock(
    month: YearMonth,
    diary: Diary?,
    selected: LocalDate,
    today: LocalDate,
    actions: DayActions,
) {
    Column {
        Text(
            LocalDateFormatter.current.monthYear(month),
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 14.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val offset = CalendarGeometry.leadingBlanks(month)
        Column(verticalArrangement = Arrangement.spacedBy(CellGap)) {
            for (row in 0 until CalendarGeometry.rows(month)) {
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    for (column in 0 until CalendarGeometry.DAYS_IN_WEEK) {
                        val day = row * CalendarGeometry.DAYS_IN_WEEK + column - offset + 1
                        Box(Modifier.weight(1f).aspectRatio(1f)) {
                            if (day in 1..month.length) {
                                val date = LocalDate(month.year, month.month, day)
                                DayCell(
                                    date = date,
                                    entries = diary?.entriesOn(date).orEmpty(),
                                    isSelected = date == selected,
                                    isToday = date == today,
                                    onClick = { actions.onSelect(date) },
                                    onPickEntry = actions.onPickEntry,
                                    onNewEntry = actions.onNewEntry?.let { { it(date) } },
                                )
                            } else {
                                EmptyCell()
                            }
                        }
                    }
                }
            }
        }
    }
}
