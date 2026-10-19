package ru.b3lka.linary.presentation.lock

import ru.b3lka.linary.presentation.lock.LockController.UnlockResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class LockControllerTest {

    private class MemoryPreferences : LockPreferences {
        override var lockEnabled = false
        override var lockTimeoutMinutes = LockController.DEFAULT_TIMEOUT
        override var lockStyle = LockStyle.Solid.name
        override var lockBlurRadius = LockController.DEFAULT_BLUR
        override var lockCollapsePhotos = true
        private var pin: String? = null
        override val hasPin: Boolean get() = pin != null

        override fun setPin(pin: String) {
            this.pin = pin
        }

        override fun checkPin(pin: String) = pin == this.pin

        override fun clearPin() {
            pin = null
        }
    }

    private val time = TestTimeSource()
    private val prefs = MemoryPreferences()

    @Test
    fun disabledByDefault() {
        val lock = LockController(prefs, time)
        time += 2.minutes * 60
        lock.tick()
        assertFalse(lock.locked)
    }

    @Test
    fun locksAfterIdleTimeAndActivityResetsTimer() {
        val lock = LockController(prefs, time)
        lock.enable("1234")
        lock.changeTimeout(5)

        time += 4.minutes
        lock.onActivity()
        time += 4.minutes
        lock.tick()
        assertFalse(lock.locked)

        time += 1.minutes
        lock.tick()
        assertTrue(lock.locked)

        assertEquals(UnlockResult.Wrong, lock.unlock("0000"))
        assertEquals(UnlockResult.Ok, lock.unlock("1234"))
        assertFalse(lock.locked)
    }

    @Test
    fun startsLockedWhenEnabled() {
        LockController(prefs, time).enable("1234")
        assertTrue(LockController(prefs, time).locked)
    }

    @Test
    fun tooManyWrongAttemptsBlockForAWhile() {
        val lock = LockController(prefs, time)
        lock.enable("1234")
        lock.lockNow()
        repeat(LockController.MAX_ATTEMPTS) { assertEquals(UnlockResult.Wrong, lock.unlock("0000")) }

        assertEquals(UnlockResult.Blocked, lock.unlock("1234"))
        lock.tick()
        assertEquals(LockController.COOLDOWN.inWholeSeconds, lock.retryAfterSeconds)

        time += LockController.COOLDOWN + 1.seconds
        lock.tick()
        assertEquals(0L, lock.retryAfterSeconds)
        assertEquals(UnlockResult.Ok, lock.unlock("1234"))
    }

    @Test
    fun disableAndChangeNeedCurrentPin() {
        val lock = LockController(prefs, time)
        lock.enable("1234")

        assertFalse(lock.changePin("0000", "5678"))
        assertTrue(lock.changePin("1234", "5678"))
        assertFalse(lock.disable("1234"))
        assertTrue(lock.disable("5678"))
        assertFalse(lock.enabled)
        assertFalse(prefs.hasPin)
    }

    @Test
    fun styleAndBlurAreSavedAndClamped() {
        val lock = LockController(prefs, time)
        lock.changeStyle(LockStyle.Blurred)
        lock.changeBlurRadius(1000)

        val reopened = LockController(prefs, time)
        assertEquals(LockStyle.Blurred, reopened.style)
        assertEquals(LockController.MAX_BLUR, reopened.blurRadius)
    }

    @Test
    fun pinFormat() {
        assertTrue(LockController.isValidPin("1234"))
        assertTrue(LockController.isValidPin("12345678"))
        assertFalse(LockController.isValidPin("123"))
        assertFalse(LockController.isValidPin("123456789"))
        assertFalse(LockController.isValidPin("12a4"))
    }
}
