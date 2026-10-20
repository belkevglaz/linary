package ru.b3lka.linary.desktop.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ru.b3lka.linary.core.model.Diary
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.core.model.EntryChange
import ru.b3lka.linary.core.model.TagChange
import ru.b3lka.linary.core.repository.DiaryLoadException
import ru.b3lka.linary.core.repository.DiaryRepository
import ru.b3lka.linary.core.repository.DiarySource
import ru.b3lka.linary.core.repository.DiaryWriteException
import ru.b3lka.linary.desktop.crypto.SyncCrypto
import ru.b3lka.linary.presentation.sync.SyncKeySettings
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardWatchEventKinds
import java.util.concurrent.TimeUnit

/**
 * Папка облачной синхронизации (например `~/Dropbox/Apps/Diarium`).
 *
 * Запись: свежая база перечитывается из папки, изменение применяется к ней, результат
 * шифруется и атомарно заменяет `v4_database`. Dropbox-клиент загружает файл с новым
 * `client_modified`, и телефон при синхронизации скачивает базу (или сливает со своими изменениями).
 * Перед каждой записью делается резервная копия, после — проверка; при ошибке копия возвращается.
 */
class SyncFolderRepository(
    private val dir: File,
    private val key: () -> String?,
    private val backups: BackupStore = BackupStore(),
) : DiaryRepository {

    override val source = DiarySource(DiarySource.Kind.SyncFolder, dir.absolutePath)

    override val isWritable: Boolean = true

    private val writeLock = Mutex()
    private val mediaStore = SyncMediaStore(dir, ::crypto)

    /**
     * Состояние `v4_database`, которое это приложение уже видело (прочитало или записало само).
     */
    @Volatile
    private var knownStamp: FileStamp? = null

    /**
     * Следит за `v4_database`: Dropbox заменяет файл, когда телефон выгружает правки.
     * Свои записи не сообщаются — их состояние уже в [knownStamp].
     */
    override val externalChanges: Flow<Unit> = flow {
        FileSystems.getDefault().newWatchService().use { watcher ->
            dir.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
            while (currentCoroutineContext().isActive) {
                val key = runInterruptible { watcher.poll(WATCH_POLL_MS, TimeUnit.MILLISECONDS) } ?: continue
                val touched = key.pollEvents().any { (it.context() as? Path)?.toString() == DATABASE_FILE }
                key.reset()
                if (touched) {
                    // Даём Dropbox дописать файл и не реагируем на собственную запись
                    delay(SETTLE_MS)
                    val stamp = runCatching { FileStamp.of(File(dir, DATABASE_FILE)) }.getOrNull()
                    if (stamp != null && stamp != knownStamp) emit(Unit)
                }
            }
        }
    }.flowOn(Dispatchers.IO)
    override suspend fun load(): Diary = withContext(Dispatchers.IO) {
        readingDiary {
            val db = findDatabase()
            val stamp = FileStamp.of(db)
            // Расшифрованная копия нужна только на время чтения
            val tmp = decryptToTempFile(db)
            try {
                SqliteDiaryReader.read(tmp).also { knownStamp = stamp }
            } finally {
                tmp.delete()
            }
        }
    }

    override suspend fun loadMediaBytes(media: DiaryMedia): ByteArray = withContext(Dispatchers.IO) {
        readingDiary { crypto().decrypt(File(dir, "$MEDIA_DIR/${media.id}").readBytes()) }
    }

    override suspend fun saveEntry(change: EntryChange): Long = writeLock.withLock {
        withContext(Dispatchers.IO) {
            writingDiary {
                // Как телефон: сначала файлы вложений и индекс, потом база
                val undoMedia = mediaStore.add(change.addedMedia)
                runCatching {
                    modify(
                        change = { SqliteEntryWriter.apply(it, change) },
                        verify = { file, id -> isSaved(file, id, change) },
                    )
                }.onFailure { undoMedia() }.getOrThrow()
            }
        }
    }

    private fun isSaved(file: File, id: Long, change: EntryChange): Boolean {
        if (!SqliteEntryWriter.contains(file, id, change.html, change.rating)) return false
        val media = SqliteEntryWriter.mediaIds(file, id)
        val order = change.mediaOrder
        val orderOk = order == null || media.filter { it in order } == order.filter { it in media }
        val tagsOk = change.tagIds == null || SqliteEntryWriter.tagIds(file, id) == change.tagIds
        val mediaOk = change.addedMedia.all { it.id in media } && change.removedMediaIds.none { it in media }
        return orderOk && tagsOk && mediaOk
    }

    override suspend fun deleteEntry(entry: DiaryEntry) {
        writeLock.withLock {
            withContext(Dispatchers.IO) {
                writingDiary {
                    modify(
                        change = { SqliteEntryWriter.delete(it, entry) },
                        verify = { file, _ -> !SqliteEntryWriter.exists(file, entry.id) },
                    )
                }
            }
        }
    }

    override suspend fun saveTag(change: TagChange): Long = writeLock.withLock {
        withContext(Dispatchers.IO) {
            writingDiary {
                modify(
                    change = { SqliteTagWriter.apply(it, change) },
                    verify = { file, id -> SqliteTagWriter.contains(file, id, change) },
                )
            }
        }
    }

    /**
     * Общий путь записи: свежая база → [change] → резервная копия → атомарная замена → [verify].
     */
    private fun <T> modify(change: (File) -> T, verify: (File, T) -> Boolean): T {
        val db = File(dir, DATABASE_FILE)
        val stamp = FileStamp.of(db)
        val plain = decryptToTempFile(db)
        try {
            val result = change(plain)
            val encrypted = crypto().encrypt(SyncCrypto.gzip(plain.readBytes()))
            // Файл не должен был поменяться, пока мы его правили (например, докачался Dropbox-ом)
            if (FileStamp.of(db) != stamp) {
                throw DiaryWriteException(DiaryWriteException.Reason.Conflict, DATABASE_FILE)
            }
            val backup = backups.backup(db)
            replaceAtomically(db, encrypted)
            knownStamp = FileStamp.of(db)
            verifyOrRestore(db, backup) { verify(it, result) }
            return result
        } finally {
            plain.delete()
        }
    }

    private fun verifyOrRestore(db: File, backup: File, verify: (File) -> Boolean) {
        val ok = runCatching {
            val check = decryptToTempFile(db)
            try {
                verify(check)
            } finally {
                check.delete()
            }
        }.getOrDefault(false)
        if (!ok) {
            replaceAtomically(db, backup.readBytes())
            knownStamp = FileStamp.of(db)
            throw DiaryWriteException(DiaryWriteException.Reason.Verification, backup.absolutePath)
        }
    }

    /**
     * Пишет во временный файл рядом и переименовывает его поверх [target].
     * Имя `.~…` Dropbox считает временным и не синхронизирует.
     */
    private fun replaceAtomically(target: File, bytes: ByteArray) {
        val tmp = File(target.parentFile, ".~${target.name}.tmp")
        tmp.writeBytes(bytes)
        Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun findDatabase(): File {
        val db = File(dir, DATABASE_FILE)
        if (db.isFile) return db
        val other = dir.listFiles().orEmpty().map { it.name }.filter { otherVersion.matches(it) }
        if (other.isNotEmpty()) {
            throw DiaryLoadException(DiaryLoadException.Reason.UnsupportedSyncVersion, other.joinToString())
        }
        throw DiaryLoadException(DiaryLoadException.Reason.NotSyncFolder, dir.absolutePath)
    }

    /**
     * Шифр с ключом пользователя; без ключа — [DiaryLoadException.Reason.KeyMissing].
     */
    private fun crypto(): SyncCrypto =
        key()?.takeIf { SyncKeySettings.isValid(it) }?.let(::SyncCrypto)
            ?: throw DiaryLoadException(DiaryLoadException.Reason.KeyMissing)

    private fun decryptToTempFile(file: File): File {
        val decrypted = crypto().decrypt(file.readBytes())
        val sqlite = if (SyncCrypto.isGzip(decrypted)) SyncCrypto.gunzip(decrypted) else decrypted
        if (!SyncCrypto.isSqlite(sqlite)) throw DiaryLoadException(DiaryLoadException.Reason.DecryptionFailed)
        return PrivateTemp.file("database-", ".sqlite").apply { writeBytes(sqlite) }
    }

    /**
     * Время изменения и размер файла — чтобы заметить, что его переписали.
     */
    private data class FileStamp(val modified: Long, val size: Long) {
        companion object {
            fun of(file: File) = FileStamp(file.lastModified(), file.length())
        }
    }

    private companion object {
        const val WATCH_POLL_MS = 1000L
        const val SETTLE_MS = 1500L
        const val DATABASE_FILE = "v4_database"
        const val MEDIA_DIR = "v4_Media"
        val otherVersion = Regex("v\\d+_database")
    }
}
