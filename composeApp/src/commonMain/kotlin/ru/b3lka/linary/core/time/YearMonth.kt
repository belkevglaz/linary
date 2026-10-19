package ru.b3lka.linary.core.time

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * Месяц конкретного года (1..12).
 */
data class YearMonth(
    val year: Int,
    val month: Int,
) : Comparable<YearMonth> {

    val firstDay: LocalDate get() = LocalDate(year, month, 1)

    val length: Int get() = firstDay.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth

    fun plusMonths(n: Int): YearMonth {
        val total = year * MONTHS_IN_YEAR + (month - 1) + n
        return YearMonth(total / MONTHS_IN_YEAR, total % MONTHS_IN_YEAR + 1)
    }

    operator fun contains(date: LocalDate): Boolean = date.year == year && date.monthNumber == month

    override fun compareTo(other: YearMonth): Int = compareValuesBy(this, other, { it.year }, { it.month })

    companion object {
        private const val MONTHS_IN_YEAR = 12

        fun of(date: LocalDate): YearMonth = YearMonth(date.year, date.monthNumber)

        /**
         * Все месяцы от [from] до [to] включительно, по возрастанию.
         */
        fun range(from: YearMonth, to: YearMonth): List<YearMonth> = buildList {
            var m = from
            while (m <= to) {
                add(m)
                m = m.plusMonths(1)
            }
        }
    }
}
