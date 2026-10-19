package ru.b3lka.linary.desktop.data

import ru.b3lka.linary.core.model.NewMedia
import ru.b3lka.linary.desktop.crypto.SyncCrypto
import ru.b3lka.linary.desktop.crypto.TEST_SYNC_KEY
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class SyncMediaStoreTest {
    private val crypto = SyncCrypto(TEST_SYNC_KEY)

    private fun index(dir: File) = SyncCrypto.gunzip(File(dir, "v4_mediaindex").readBytes()).decodeToString()

    @Test
    fun addMergesIndexInOrderAndUndoRestoresIt() {
        val dir = Files.createTempDirectory("linary-media").toFile()
        val before = "[\"bbbbbbbb-0000-0000-0000-000000000000\",\"dddddddd-0000-0000-0000-000000000000\"]"
        File(dir, "v4_mediaindex").writeBytes(SyncCrypto.gzip(before.toByteArray()))
        val media = NewMedia("cccccccc-0000-0000-0000-000000000000", "photo", "jpg", byteArrayOf(1, 2, 3))

        val undo = SyncMediaStore(dir) { crypto }.add(listOf(media))

        assertEquals(
            "[\"bbbbbbbb-0000-0000-0000-000000000000\",\"cccccccc-0000-0000-0000-000000000000\"," +
                "\"dddddddd-0000-0000-0000-000000000000\"]",
            index(dir),
        )
        assertContentEquals(media.bytes, crypto.decrypt(File(dir, "v4_Media/${media.id}").readBytes()))

        undo()
        assertEquals(before, index(dir))
        assertFalse(File(dir, "v4_Media/${media.id}").exists())
    }

    @Test
    fun nothingToAddChangesNothing() {
        val dir = Files.createTempDirectory("linary-media").toFile()
        SyncMediaStore(dir) { crypto }.add(emptyList())()
        assertFalse(File(dir, "v4_mediaindex").exists())
    }
}
