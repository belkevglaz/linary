package ru.b3lka.linary.presentation.editor

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeStampTest {
    private val now = LocalDateTime(2026, 10, 16, 9, 5)

    @Test
    fun todayEntryGetsTimeOnly() {
        assertEquals("— 09:05 —", TimeStamp.format(LocalDate(2026, 10, 16), now))
    }

    @Test
    fun otherDayEntryGetsDateAndTime() {
        assertEquals("— 16.10.2026 09:05 —", TimeStamp.format(LocalDate(2026, 10, 14), now))
    }
}
