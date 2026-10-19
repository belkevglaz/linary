package ru.b3lka.linary.core.time

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class TicksTest {
    @Test
    fun unixEpoch() {
        // DateTime(1970, 1, 1).Ticks в .NET
        assertEquals(LocalDateTime(1970, 1, 1, 0, 0, 0, 0), Ticks.toLocalDateTime(621_355_968_000_000_000L))
    }

    @Test
    fun withTime() {
        // 2026-10-14 14:40:42.3865840
        val dateTime = Ticks.toLocalDateTime(639_275_856_423_865_840L)
        assertEquals(LocalDateTime(2026, 10, 14, 14, 40, 42, 386_584_000), dateTime)
    }

    @Test
    fun roundTrip() {
        val ticks = 639_275_856_423_865_840L
        assertEquals(ticks, Ticks.fromLocalDateTime(Ticks.toLocalDateTime(ticks)))
    }
}
