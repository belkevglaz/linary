package ru.b3lka.linary.core.time

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YearMonthTest {
    @Test
    fun length() {
        assertEquals(29, YearMonth(2024, 2).length)
        assertEquals(31, YearMonth(2026, 10).length)
    }

    @Test
    fun plusMonthsAcrossYear() {
        assertEquals(YearMonth(2027, 1), YearMonth(2026, 12).plusMonths(1))
        assertEquals(YearMonth(2025, 12), YearMonth(2026, 1).plusMonths(-1))
    }

    @Test
    fun rangeAndContains() {
        assertEquals(3, YearMonth.range(YearMonth(2025, 11), YearMonth(2026, 1)).size)
        assertTrue(LocalDate(2026, 10, 16) in YearMonth(2026, 10))
    }
}
