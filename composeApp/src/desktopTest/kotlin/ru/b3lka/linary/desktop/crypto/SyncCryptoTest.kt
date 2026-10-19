package ru.b3lka.linary.desktop.crypto

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SyncCryptoTest {
    private val crypto = SyncCrypto(TEST_SYNC_KEY)

    @Test
    fun roundTrip() {
        val data = "SQLite format 3\u0000 hello".toByteArray()
        val encrypted = crypto.encrypt(SyncCrypto.gzip(data))
        assertEquals(0, encrypted.size % 16)
        assertContentEquals(data, SyncCrypto.gunzip(crypto.decrypt(encrypted)))
    }

    @Test
    fun keyMustBe32AsciiCharacters() {
        assertFailsWith<IllegalArgumentException> { SyncCrypto("short") }
        assertFailsWith<IllegalArgumentException> { SyncCrypto("я".repeat(32)) }
    }

    /*
     * Проверка на настоящей папке синхронизации с настоящим ключом (в репозиторий он не входит):
     * SYNC_TEST_DIR=~/Dropbox/Apps/Diarium SYNC_TEST_KEY=… ./gradlew desktopTest
     */
    @Test
    fun realFolderIfConfigured() {
        val dir = System.getenv("SYNC_TEST_DIR") ?: return
        val key = System.getenv("SYNC_TEST_KEY") ?: return
        val raw = File(dir, "v4_database").readBytes()
        assertTrue(SyncCrypto.isSqlite(SyncCrypto.gunzip(SyncCrypto(key).decrypt(raw))))
    }
}

/**
 * Ключ только для тестов — не настоящий ключ синхронизации.
 */
const val TEST_SYNC_KEY = "test-key-0123456789-abcdefghijkl"
