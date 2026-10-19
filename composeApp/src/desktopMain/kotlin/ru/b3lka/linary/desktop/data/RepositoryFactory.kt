package ru.b3lka.linary.desktop.data

import ru.b3lka.linary.core.repository.DiaryLoadException
import ru.b3lka.linary.core.repository.DiaryRepository
import ru.b3lka.linary.core.repository.DiaryWriteException
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.sql.SQLException

/**
 * Создаёт репозиторий по пути: папка — синхронизация, файл — резервная копия.
 *
 * @param key ключ синхронизации пользователя (читается при каждом обращении — его могут задать позже).
 */
fun createRepository(path: File, key: () -> String?): DiaryRepository? = when {
    path.isDirectory -> SyncFolderRepository(path, key)
    path.isFile -> BackupFileRepository(path, key)
    else -> null
}

/**
 * Выполняет чтение дневника, переводя системные ошибки в [DiaryLoadException].
 */
internal inline fun <T> readingDiary(block: () -> T): T = runCatching(block).getOrElse { throw toLoadException(it) }

@PublishedApi
internal fun toLoadException(error: Throwable): Throwable = when (error) {
    is DiaryLoadException -> error
    is IOException -> DiaryLoadException(DiaryLoadException.Reason.Io, error.message, error)
    is SQLException -> DiaryLoadException(DiaryLoadException.Reason.NotDiaryDatabase, error.message, error)
    is GeneralSecurityException -> DiaryLoadException(DiaryLoadException.Reason.DecryptionFailed, error.message, error)
    else -> error
}

/**
 * Выполняет запись, переводя любые ошибки чтения и ввода-вывода в [DiaryWriteException].
 */
internal inline fun <T> writingDiary(block: () -> T): T = runCatching(block).getOrElse { throw toWriteException(it) }

@PublishedApi
internal fun toWriteException(error: Throwable): Throwable = when (error) {
    is DiaryWriteException -> error
    is DiaryLoadException -> DiaryWriteException(DiaryWriteException.Reason.Io, error.message, error)
    is IOException, is SQLException, is GeneralSecurityException ->
        DiaryWriteException(DiaryWriteException.Reason.Io, error.message, error)
    else -> error
}
