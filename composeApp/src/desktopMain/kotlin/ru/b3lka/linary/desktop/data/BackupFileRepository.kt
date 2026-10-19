package ru.b3lka.linary.desktop.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.b3lka.linary.core.model.Diary
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.repository.DiaryLoadException
import ru.b3lka.linary.core.repository.DiaryRepository
import ru.b3lka.linary.core.repository.DiarySource
import ru.b3lka.linary.desktop.crypto.SyncCrypto
import java.io.File
import java.sql.Connection

/**
 * Файл резервной копии `.diary`. Обычно это SQLite; на всякий случай поддерживаются
 * сжатый и зашифрованный (ключом синхронизации, если он задан) варианты.
 */
class BackupFileRepository(
    private val file: File,
    private val key: () -> String? = { null },
) : DiaryRepository {

    override val source = DiarySource(DiarySource.Kind.Backup, file.absolutePath)

    private var connection: Connection? = null

    /**
     * Расшифрованная копия, если бэкап не был открытым SQLite; удаляется при перечитывании.
     */
    private var plainCopy: File? = null
    private val lock = Any()

    override suspend fun load(): Diary = withContext(Dispatchers.IO) {
        readingDiary {
            val target = plainDatabaseFile()
            synchronized(lock) {
                connection?.close()
                plainCopy?.takeIf { it != target }?.delete()
                plainCopy = target.takeIf { it != file }
                val opened = SqliteDiaryReader.open(target)
                connection = opened
                SqliteDiaryReader.read(opened)
            }
        }
    }

    override suspend fun loadMediaBytes(media: DiaryMedia): ByteArray = withContext(Dispatchers.IO) {
        readingDiary {
            synchronized(lock) {
                val c = connection ?: throw DiaryLoadException(DiaryLoadException.Reason.Io, "not loaded")
                c.prepareStatement("select Data from Media where DiaryMediaId = ?").use { st ->
                    st.setString(1, media.id)
                    st.executeQuery().use { rs -> if (rs.next()) rs.getBytes(1) else null }
                } ?: throw DiaryLoadException(DiaryLoadException.Reason.Io, media.id)
            }
        }
    }

    /**
     * Файл с базой в открытом виде: сам бэкап или расшифрованная временная копия.
     */
    private fun plainDatabaseFile(): File {
        val raw = file.readBytes()
        if (SyncCrypto.isSqlite(raw)) return file
        val plain = when {
            SyncCrypto.isGzip(raw) -> SyncCrypto.gunzip(raw)
            else -> key()?.let { k -> runCatching { SyncCrypto(k).decrypt(raw) }.getOrNull() }
                ?.let { if (SyncCrypto.isGzip(it)) SyncCrypto.gunzip(it) else it }
        }
        if (plain == null || !SyncCrypto.isSqlite(plain)) {
            throw DiaryLoadException(DiaryLoadException.Reason.UnknownFormat, file.absolutePath)
        }
        return PrivateTemp.file("backup-", ".sqlite").apply { writeBytes(plain) }
    }
}
