package ru.b3lka.linary.core.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import ru.b3lka.linary.core.model.Diary
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.model.EntryChange

/**
 * Источник данных дневника (папка синхронизации, бэкап `.diary`, ...).
 */
interface DiaryRepository {

    val source: DiarySource

    /**
     * Загружает журнал.
     *
     * @throws DiaryLoadException если источник не удалось прочитать.
     */
    suspend fun load(): Diary

    /**
     * Возвращает расшифрованное содержимое вложения.
     *
     * @throws DiaryLoadException если вложение не удалось прочитать.
     */
    suspend fun loadMediaBytes(media: DiaryMedia): ByteArray

    /**
     * Можно ли сохранять изменения в этот источник.
     */
    val isWritable: Boolean get() = false

    /**
     * Сигналы о том, что источник изменился на диске не через это приложение
     * (например, Dropbox докачал правки с телефона). По умолчанию источник не отслеживается.
     */
    val externalChanges: Flow<Unit> get() = emptyFlow()

    /**
     * Сохраняет запись (новую или изменённую) и возвращает её идентификатор.
     *
     * @throws DiaryWriteException если сохранить не удалось; источник при этом не меняется.
     */
    suspend fun saveEntry(change: EntryChange): Long =
        throw DiaryWriteException(DiaryWriteException.Reason.ReadOnly)

    /**
     * Удаляет запись. [entry] — запись в том виде, в каком её видел пользователь:
     * если её успели изменить на другом устройстве, удаление отменяется (Conflict).
     *
     * @throws DiaryWriteException если удалить не удалось; источник при этом не меняется.
     */
    suspend fun deleteEntry(entry: DiaryEntry): Unit =
        throw DiaryWriteException(DiaryWriteException.Reason.ReadOnly)
}
