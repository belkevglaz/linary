package ru.b3lka.linary.presentation.lock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Блокировка экрана по бездействию.
 *
 * Если она включена, приложение запускается заблокированным и блокируется снова, когда пользователь
 * ничего не делает [timeoutMinutes] минут. Снять блокировку можно только PIN-кодом. После
 * [MAX_ATTEMPTS] неверных попыток следующая возможна только через [COOLDOWN].
 *
 * Это защита от чужих глаз, а не шифрование: файлы дневника на диске она не защищает.
 */
class LockController(
    private val prefs: LockPreferences,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    var enabled by mutableStateOf(prefs.lockEnabled && prefs.hasPin)
        private set

    var timeoutMinutes by mutableStateOf(prefs.lockTimeoutMinutes.takeIf { it in TIMEOUT_OPTIONS } ?: DEFAULT_TIMEOUT)
        private set

    var locked by mutableStateOf(enabled)
        private set

    var style by mutableStateOf(LockStyle.entries.firstOrNull { it.name == prefs.lockStyle } ?: LockStyle.Solid)
        private set

    /**
     * Сила размытия для [LockStyle.Blurred], в dp.
     */
    var blurRadius by mutableStateOf(prefs.lockBlurRadius.coerceIn(MIN_BLUR, MAX_BLUR))
        private set

    /**
     * Сворачивать ленту вложений и закрывать просмотр фото, когда приложение блокируется.
     */
    var collapsePhotos by mutableStateOf(prefs.lockCollapsePhotos)
        private set

    /**
     * Блокировать сразу при закрытии окна, не дожидаясь бездействия.
     */
    var lockOnClose by mutableStateOf(prefs.lockOnClose)
        private set

    /**
     * Сколько секунд ждать до следующей попытки ввода PIN (0 — можно вводить).
     */
    var retryAfterSeconds by mutableStateOf(0L)
        private set

    private var lastActivity: TimeMark = timeSource.markNow()
    private var failures = 0
    private var blockedUntil: TimeMark? = null

    /**
     * Пользователь что-то сделал: двинул мышью, нажал клавишу.
     */
    fun onActivity() {
        if (!locked) lastActivity = timeSource.markNow()
    }

    /**
     * Вызывается раз в секунду: блокирует по истечении времени и обновляет обратный отсчёт.
     */
    fun tick() {
        if (enabled && !locked && lastActivity.elapsedNow() >= timeoutMinutes.minutes) locked = true
        val remainingMs = blockedUntil?.let { -it.elapsedNow().inWholeMilliseconds } ?: 0
        retryAfterSeconds = ((remainingMs + MS_PER_SECOND - 1) / MS_PER_SECOND).coerceAtLeast(0)
    }

    fun lockNow() {
        if (enabled) locked = true
    }

    /**
     * Окно закрыли, а приложение осталось работать (в трее или свёрнутым): блокирует, если так настроено.
     */
    fun onWindowClosed() {
        if (lockOnClose) lockNow()
    }

    fun unlock(pin: String): UnlockResult {
        if (blockedUntil?.hasNotPassedNow() == true) return UnlockResult.Blocked
        if (prefs.checkPin(pin)) {
            locked = false
            failures = 0
            blockedUntil = null
            retryAfterSeconds = 0
            lastActivity = timeSource.markNow()
            return UnlockResult.Ok
        }
        failures++
        if (failures >= MAX_ATTEMPTS) {
            blockedUntil = timeSource.markNow() + COOLDOWN
            retryAfterSeconds = COOLDOWN.inWholeSeconds
        }
        return UnlockResult.Wrong
    }

    /**
     * Включает блокировку с новым PIN.
     */
    fun enable(pin: String) {
        require(isValidPin(pin)) { "PIN must be $MIN_PIN_LENGTH..$MAX_PIN_LENGTH digits" }
        prefs.setPin(pin)
        prefs.lockEnabled = true
        prefs.lockTimeoutMinutes = timeoutMinutes
        enabled = true
        lastActivity = timeSource.markNow()
    }

    /**
     * Выключает блокировку и забывает PIN. false — неверный текущий PIN.
     */
    fun disable(currentPin: String): Boolean {
        if (!prefs.checkPin(currentPin)) return false
        prefs.lockEnabled = false
        prefs.clearPin()
        enabled = false
        return true
    }

    /**
     * Меняет PIN. false — неверный текущий PIN.
     */
    fun changePin(currentPin: String, newPin: String): Boolean {
        require(isValidPin(newPin)) { "PIN must be $MIN_PIN_LENGTH..$MAX_PIN_LENGTH digits" }
        if (!prefs.checkPin(currentPin)) return false
        prefs.setPin(newPin)
        return true
    }

    fun changeTimeout(minutes: Int) {
        if (minutes !in TIMEOUT_OPTIONS) return
        timeoutMinutes = minutes
        prefs.lockTimeoutMinutes = minutes
        lastActivity = timeSource.markNow()
    }

    fun changeStyle(value: LockStyle) {
        style = value
        prefs.lockStyle = value.name
    }

    fun changeBlurRadius(radius: Int) {
        blurRadius = radius.coerceIn(MIN_BLUR, MAX_BLUR)
        prefs.lockBlurRadius = blurRadius
    }

    fun changeLockOnClose(value: Boolean) {
        lockOnClose = value
        prefs.lockOnClose = value
    }

    fun changeCollapsePhotos(value: Boolean) {
        collapsePhotos = value
        prefs.lockCollapsePhotos = value
    }

    enum class UnlockResult { Ok, Wrong, Blocked }

    companion object {
        /**
         * Варианты времени бездействия в минутах.
         */
        val TIMEOUT_OPTIONS = listOf(1, 5, 10, 15, 30, 60)
        const val DEFAULT_TIMEOUT = 5

        /**
         * Пределы размытия в dp: слабее текст ещё читается, сильнее — просто пятна цвета.
         */
        const val MIN_BLUR = 8
        const val MAX_BLUR = 48
        const val DEFAULT_BLUR = 24
        const val BLUR_STEP = 4
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 8
        const val MAX_ATTEMPTS = 5
        val COOLDOWN = 30.seconds
        private const val MS_PER_SECOND = 1000L

        fun isValidPin(pin: String): Boolean =
            pin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH && pin.all { it in '0'..'9' }
    }
}
