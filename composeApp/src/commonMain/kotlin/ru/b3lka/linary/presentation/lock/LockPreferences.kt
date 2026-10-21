package ru.b3lka.linary.presentation.lock

/**
 * Хранилище настроек блокировки. PIN хранится только в виде хэша — какого, решает платформа.
 */
interface LockPreferences {
    var lockEnabled: Boolean
    var lockTimeoutMinutes: Int

    /**
     * Вид экрана блокировки — имя из [LockStyle].
     */
    var lockStyle: String
    var lockBlurRadius: Int

    /**
     * Сворачивать ленту вложений и закрывать просмотр фото при блокировке.
     */
    var lockCollapsePhotos: Boolean

    /**
     * Блокировать сразу, когда окно закрывают крестиком (а приложение остаётся работать).
     */
    var lockOnClose: Boolean
    val hasPin: Boolean

    fun setPin(pin: String)

    fun checkPin(pin: String): Boolean

    fun clearPin()
}
