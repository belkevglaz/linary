package ru.b3lka.linary.presentation.calendar

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import ru.b3lka.linary.core.time.YearMonth

/**
 * Расчёты сетки месяца (неделя начинается с понедельника) и видимости недели в списке.
 */
object CalendarGeometry {
    const val DAYS_IN_WEEK = 7

    /**
     * Примерная доля высоты блока месяца, которую занимает его заголовок.
     */
    private const val TITLE_FRACTION = 0.12f
    private const val VISIBILITY_MARGIN_PX = 40

    /**
     * Середина строки недели (в долях строки).
     */
    private const val ROW_CENTER = 0.5f

    fun leadingBlanks(month: YearMonth): Int = month.firstDay.dayOfWeek.isoDayNumber - 1

    fun rows(month: YearMonth): Int = (leadingBlanks(month) + month.length + DAYS_IN_WEEK - 1) / DAYS_IN_WEEK

    /**
     * Доля высоты блока месяца, на которую приходится середина недели с [date].
     */
    fun weekFraction(month: YearMonth, date: LocalDate): Float {
        val row = (leadingBlanks(month) + date.dayOfMonth - 1) / DAYS_IN_WEEK
        return TITLE_FRACTION + (1f - TITLE_FRACTION) * (row + ROW_CENTER) / rows(month)
    }

    fun isWeekVisible(state: LazyListState, index: Int, month: YearMonth, date: LocalDate): Boolean {
        val info = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == index } ?: return false
        val y = info.offset + info.size * weekFraction(month, date)
        return y > state.layoutInfo.viewportStartOffset + VISIBILITY_MARGIN_PX &&
            y < state.layoutInfo.viewportEndOffset - VISIBILITY_MARGIN_PX
    }
}
