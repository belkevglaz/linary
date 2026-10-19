package ru.b3lka.linary.core.time

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

/**
 * Преобразование .NET DateTime ticks (интервалы по 100 нс с 01.01.0001) в локальную дату-время.
 */
object Ticks {
    private const val TICKS_PER_SECOND = 10_000_000L
    private const val TICKS_PER_DAY = 864_000_000_000L
    private const val NANOS_PER_TICK = 100L
    private const val SECONDS_PER_HOUR = 3600
    private const val SECONDS_PER_MINUTE = 60

    /**
     * Число дней между 0001-01-01 и 1970-01-01.
     */
    private const val DAYS_TO_UNIX_EPOCH = 719_162L

    fun toLocalDateTime(ticks: Long): LocalDateTime {
        val days = ticks / TICKS_PER_DAY - DAYS_TO_UNIX_EPOCH
        val rem = ticks % TICKS_PER_DAY
        val secondOfDay = (rem / TICKS_PER_SECOND).toInt()
        val nanos = ((rem % TICKS_PER_SECOND) * NANOS_PER_TICK).toInt()
        val time = LocalTime(
            hour = secondOfDay / SECONDS_PER_HOUR,
            minute = (secondOfDay / SECONDS_PER_MINUTE) % SECONDS_PER_MINUTE,
            second = secondOfDay % SECONDS_PER_MINUTE,
            nanosecond = nanos,
        )
        return LocalDateTime(LocalDate.fromEpochDays(days.toInt()), time)
    }

    fun fromLocalDateTime(dateTime: LocalDateTime): Long {
        val days = dateTime.date.toEpochDays() + DAYS_TO_UNIX_EPOCH
        val seconds = dateTime.hour.toLong() * SECONDS_PER_HOUR +
            dateTime.minute.toLong() * SECONDS_PER_MINUTE +
            dateTime.second
        return days * TICKS_PER_DAY + seconds * TICKS_PER_SECOND + dateTime.nanosecond / NANOS_PER_TICK
    }
}
