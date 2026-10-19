package ru.b3lka.linary.presentation.editor

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Отметка времени для пункта «Вставить время», как в Diarium:
 * `— HH:MM —` в записи сегодняшнего дня и `— DD.MM.YYYY HH:MM —` в записи другого дня.
 */
object TimeStamp {

    fun format(entryDate: LocalDate, now: LocalDateTime): String {
        val time = "${pad(now.hour)}:${pad(now.minute)}"
        return if (now.date == entryDate) {
            "— $time —"
        } else {
            "— ${pad(now.dayOfMonth)}.${pad(now.monthNumber)}.${now.year} $time —"
        }
    }

    private fun pad(value: Int): String = value.toString().padStart(2, '0')
}
