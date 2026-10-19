package ru.b3lka.linary.presentation.format

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.isoDayNumber
import ru.b3lka.linary.core.time.YearMonth

/**
 * Форматирование дат по строкам из ресурсов текущего языка.
 *
 * Шаблоны используют подстановки `{day}`, `{month}`, `{year}`.
 */
class DateFormatter(
    private val monthsNominative: List<String>,
    private val monthsGenitive: List<String>,
    private val weekdaysShort: List<String>,
    private val weekdaysFull: List<String>,
    private val patterns: Patterns,
) {
    /**
     * Шаблоны дат.
     *
     * @property monthYear «сентябрь 2026» / «September 2026».
     * @property dayMonth «14 октября» / «October 14».
     * @property longDate «14 октября 2026» / «October 14, 2026».
     */
    data class Patterns(
        val monthYear: String,
        val dayMonth: String,
        val longDate: String,
    )

    /**
     * Сокращённые названия дней недели, начиная с понедельника.
     */
    val weekdayHeaders: List<String> get() = weekdaysShort

    fun monthYear(month: YearMonth): String =
        fill(patterns.monthYear, month = monthsNominative[month.month - 1], year = month.year)

    fun dayMonth(date: LocalDate): String =
        fill(patterns.dayMonth, day = date.dayOfMonth, month = monthsGenitive[date.monthNumber - 1])

    fun longDate(date: LocalDate): String = fill(
        patterns.longDate,
        day = date.dayOfMonth,
        month = monthsGenitive[date.monthNumber - 1],
        year = date.year,
    )

    fun weekdayShort(day: DayOfWeek): String = weekdaysShort[day.isoDayNumber - 1]

    fun weekdayFull(day: DayOfWeek): String = weekdaysFull[day.isoDayNumber - 1]

    fun time(dateTime: LocalDateTime): String =
        "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"

    private fun fill(pattern: String, month: String, day: Int? = null, year: Int? = null): String =
        pattern
            .replace("{month}", month)
            .replace("{day}", day?.toString().orEmpty())
            .replace("{year}", year?.toString().orEmpty())
}
