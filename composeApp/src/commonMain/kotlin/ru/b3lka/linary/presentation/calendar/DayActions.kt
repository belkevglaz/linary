package ru.b3lka.linary.presentation.calendar

import kotlinx.datetime.LocalDate
import ru.b3lka.linary.core.model.DiaryEntry

/**
 * Действия с днём календаря.
 *
 * @property onPickEntry выбор конкретной записи дня (из меню дня).
 * @property onNewEntry новая запись на день; null — источник только для чтения.
 */
class DayActions(
    val onSelect: (LocalDate) -> Unit,
    val onPickEntry: (DiaryEntry) -> Unit,
    val onNewEntry: ((LocalDate) -> Unit)?,
)
