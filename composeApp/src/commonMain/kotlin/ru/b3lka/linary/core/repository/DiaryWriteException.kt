package ru.b3lka.linary.core.repository

/**
 * Ошибка сохранения. Текст для пользователя подбирается в UI по [reason].
 */
class DiaryWriteException(
    val reason: Reason,
    val details: String? = null,
    cause: Throwable? = null,
) : Exception("$reason${details?.let { ": $it" }.orEmpty()}", cause) {

    enum class Reason {
        /**
         * Источник только для чтения (например, файл резервной копии).
         */
        ReadOnly,

        /**
         * Запись или база изменились на другом устройстве, пока шла правка.
         */
        Conflict,

        /**
         * Записанный файл не прошёл проверку; восстановлена предыдущая версия.
         */
        Verification,
        Io,

        /**
         * Непредвиденная ошибка (ошибка в программе); подробности — в [details].
         */
        Unexpected,
    }
}
