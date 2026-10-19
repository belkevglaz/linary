package ru.b3lka.linary.desktop.data

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import ru.b3lka.linary.core.model.EntryChange
import ru.b3lka.linary.core.model.NewMedia
import ru.b3lka.linary.core.repository.DiaryLoadException
import ru.b3lka.linary.core.repository.DiaryWriteException
import ru.b3lka.linary.desktop.crypto.SyncCrypto
import ru.b3lka.linary.desktop.crypto.TEST_SYNC_KEY
import ru.b3lka.linary.presentation.sync.SyncKeySettings
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SyncFolderRepositoryTest {

    private fun syncFolder(): File {
        val dir = Files.createTempDirectory("linary-sync").toFile()
        val sqlite = File.createTempFile("linary-test", ".sqlite")
        DriverManager.getConnection("jdbc:sqlite:${sqlite.absolutePath}").use { c ->
            c.createStatement().use {
                it.executeUpdate(
                    "CREATE TABLE Entries(DiaryEntryId bigint primary key not null, Heading varchar not null, " +
                        "Text varchar not null, Rating integer not null, Latitude float not null, " +
                        "Longitude float not null)",
                )
                it.executeUpdate("insert into Entries values (639275856423865840, '', '<p>old</p>', 0, 0, 0)")
                it.executeUpdate(
                    "CREATE TABLE Media(DiaryMediaId varchar(36) primary key not null, Type integer not null, " +
                        "Data blob, Name varchar not null, FileEnding varchar not null, \"Index\" integer not null, " +
                        "DiaryEntryId bigint not null)",
                )
            }
        }
        File(dir, "v4_database").writeBytes(SyncCrypto(TEST_SYNC_KEY).encrypt(SyncCrypto.gzip(sqlite.readBytes())))
        sqlite.delete()
        return dir
    }

    private fun repository(dir: File, key: String? = TEST_SYNC_KEY) =
        SyncFolderRepository(dir, { key }, BackupStore(File(dir, "backups-test"), keep = 2))

    @Test
    fun withoutKeyLoadAsksForIt() = runBlocking<Unit> {
        val error = assertFailsWith<DiaryLoadException> { repository(syncFolder(), key = null).load() }
        assertEquals(DiaryLoadException.Reason.KeyMissing, error.reason)
    }

    @Test
    fun wrongKeyIsReportedAsDecryptionFailure() = runBlocking<Unit> {
        val wrong = "x".repeat(SyncKeySettings.LENGTH)
        val error = assertFailsWith<DiaryLoadException> { repository(syncFolder(), key = wrong).load() }
        assertEquals(DiaryLoadException.Reason.DecryptionFailed, error.reason)
    }

    @Test
    fun updateAndInsert() = runBlocking<Unit> {
        val dir = syncFolder()
        val repo = repository(dir)
        val entry = repo.load().entries.single()

        repo.saveEntry(EntryChange(entry, entry.dateTime, "<p>new</p>", 4))
        val newId = repo.saveEntry(EntryChange(null, LocalDateTime(2026, 10, 16, 12, 0), "<p>second</p>", 0))

        val diary = repo.load()
        assertEquals(2, diary.entries.size)
        assertEquals("<p>new</p>", diary.entries.first { it.id == entry.id }.html)
        assertEquals(4, diary.entries.first { it.id == entry.id }.rating)
        assertEquals("<p>second</p>", diary.entries.first { it.id == newId }.html)
        assertTrue(File(dir, "backups-test").listFiles().orEmpty().size in 1..2)
        assertTrue(File(dir, ".~v4_database.tmp").exists().not())
    }

    @Test
    fun addAndRemoveMedia() = runBlocking<Unit> {
        val dir = syncFolder()
        val repo = repository(dir)
        val entry = repo.load().entries.single()
        val first = NewMedia("00000000-0000-0000-0000-000000000002", "shot", "jpg", byteArrayOf(1, 2, 3))
        val second = NewMedia("00000000-0000-0000-0000-000000000001", "photo", "jpg", byteArrayOf(4, 5))

        repo.saveEntry(EntryChange(entry, entry.dateTime, entry.html, 0, addedMedia = listOf(first, second)))

        val saved = repo.load().entries.single()
        assertEquals(listOf(first.id to 0, second.id to 1), saved.media.map { it.id to it.index })
        assertEquals("jpg", saved.media.first().ext)
        assertContentEquals(first.bytes, repo.loadMediaBytes(saved.media.first()))
        val index = SyncCrypto.gunzip(File(dir, "v4_mediaindex").readBytes()).decodeToString()
        assertEquals("[\"${second.id}\",\"${first.id}\"]", index)

        repo.saveEntry(EntryChange(saved, saved.dateTime, saved.html, 0, removedMediaIds = setOf(first.id)))
        assertEquals(listOf(second.id), repo.load().entries.single().media.map { it.id })
    }

    @Test
    fun reorderMediaMakesCover() = runBlocking<Unit> {
        val dir = syncFolder()
        val repo = repository(dir)
        val entry = repo.load().entries.single()
        val photos = listOf("a", "b", "c").map {
            NewMedia("00000000-0000-0000-0000-00000000000$it", it, "jpg", byteArrayOf(1))
        }
        repo.saveEntry(EntryChange(entry, entry.dateTime, entry.html, 0, addedMedia = photos))
        val saved = repo.load().entries.single()

        val order = listOf(photos[2].id, photos[0].id, photos[1].id)
        repo.saveEntry(EntryChange(saved, saved.dateTime, saved.html, 0, mediaOrder = order))

        val media = repo.load().entries.single().media
        assertEquals(order, media.map { it.id })
        assertEquals(listOf(0, 1, 2), media.map { it.index })
    }

    @Test
    fun deleteEntry() = runBlocking<Unit> {
        val dir = syncFolder()
        val repo = repository(dir)
        val entry = repo.load().entries.single()
        val second = repo.saveEntry(EntryChange(null, LocalDateTime(2026, 10, 16, 12, 0), "<p>second</p>", 0))

        repo.deleteEntry(entry)

        val diary = repo.load()
        // В тестовой базе нет EntryTags/Events/Media — отсутствующие таблицы пропускаются
        assertEquals(listOf(second), diary.entries.map { it.id })
    }

    @Test
    fun deleteConflictWhenEntryChangedElsewhere() = runBlocking<Unit> {
        val dir = syncFolder()
        val repo = repository(dir)
        val entry = repo.load().entries.single()
        repo.saveEntry(EntryChange(entry, entry.dateTime, "<p>from phone</p>", 0))

        val error = assertFailsWith<DiaryWriteException> { repo.deleteEntry(entry) }
        assertEquals(DiaryWriteException.Reason.Conflict, error.reason)
        assertEquals(1, repo.load().entries.size)
    }

    @Test
    fun conflictWhenEntryChangedElsewhere() = runBlocking<Unit> {
        val dir = syncFolder()
        val repo = repository(dir)
        val entry = repo.load().entries.single()
        repo.saveEntry(EntryChange(entry, entry.dateTime, "<p>from phone</p>", 0))

        val error = assertFailsWith<DiaryWriteException> {
            repo.saveEntry(EntryChange(entry, entry.dateTime, "<p>from linux</p>", 0))
        }
        assertEquals(DiaryWriteException.Reason.Conflict, error.reason)
        assertEquals("<p>from phone</p>", repo.load().entries.single().html)
    }
}
