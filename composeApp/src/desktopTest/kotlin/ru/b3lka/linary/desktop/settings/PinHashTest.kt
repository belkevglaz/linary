package ru.b3lka.linary.desktop.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PinHashTest {

    @Test
    fun verifiesOnlyTheSamePin() {
        val stored = PinHash.hash("2468")
        assertTrue(PinHash.verify("2468", stored))
        assertFalse(PinHash.verify("2469", stored))
        assertFalse("2468" in stored)
    }

    @Test
    fun saltMakesEveryHashDifferent() {
        assertNotEquals(PinHash.hash("2468"), PinHash.hash("2468"))
    }

    @Test
    fun brokenValueNeverMatches() {
        assertFalse(PinHash.verify("2468", "garbage"))
        assertFalse(PinHash.verify("2468", "pbkdf2-sha256\$x\$y\$z"))
    }
}
