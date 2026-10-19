package ru.b3lka.linary.core.repository

/**
 * Ошибка чтения дневника. Текст для пользователя подбирается в UI по [reason].
 *
 * @property details подробности (версия формата, путь, сообщение системы) — подставляются в текст.
 */
class DiaryLoadException(
    val reason: Reason,
    val details: String? = null,
    cause: Throwable? = null,
) : Exception("$reason${details?.let { ": $it" }.orEmpty()}", cause) {

    enum class Reason {
        NotSyncFolder,
        UnsupportedSyncVersion,
        DecryptionFailed,

        /**
         * Ключ синхронизации не задан — пользователь должен ввести его сам.
         */
        KeyMissing,
        NotDiaryDatabase,
        UnknownFormat,
        Io,

        /**
         * Непредвиденная ошибка (ошибка в программе); подробности — в [details].
         */
        Unexpected,
    }
}
