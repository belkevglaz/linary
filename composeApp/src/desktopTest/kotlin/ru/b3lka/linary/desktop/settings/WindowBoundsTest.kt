package ru.b3lka.linary.desktop.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WindowBoundsTest {

    @Test
    fun roundTrip() {
        val bounds = WindowBounds(-20, 40, 1400, 900)
        assertEquals(bounds, WindowBounds.decode(bounds.encode()))
    }

    @Test
    fun invalidValuesAreIgnored() {
        assertNull(WindowBounds.decode(null))
        assertNull(WindowBounds.decode("1,2,3"))
        assertNull(WindowBounds.decode("a,b,c,d"))
        assertNull(WindowBounds.decode("0,0,100,100")) // слишком маленькое окно
    }
}
