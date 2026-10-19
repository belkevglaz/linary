package ru.b3lka.linary.desktop.crypto

import ru.b3lka.linary.presentation.sync.SyncKeySettings
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Шифрование облачной синхронизации (формат v4, режим без E2EE): AES-256-CBC + PKCS7,
 * IV — первые 16 байт ключа.
 *
 * Ключ в программу не входит: его задаёт пользователь ([SyncKeySettings]).
 *
 * @param key ключ синхронизации — 32 символа ASCII.
 */
class SyncCrypto(key: String) {
    private val keyBytes = key.toByteArray(Charsets.UTF_8)

    init {
        require(SyncKeySettings.isValid(key)) { "Sync key must be ${SyncKeySettings.LENGTH} ASCII characters" }
    }

    fun decrypt(data: ByteArray): ByteArray = cipher(Cipher.DECRYPT_MODE).doFinal(data)

    fun encrypt(data: ByteArray): ByteArray = cipher(Cipher.ENCRYPT_MODE).doFinal(data)

    /*
     * Cipher не потокобезопасен, поэтому создаётся на каждый вызов.
     */
    private fun cipher(mode: Int): Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding").apply {
        init(mode, SecretKeySpec(keyBytes, "AES"), IvParameterSpec(keyBytes, 0, IV_LENGTH))
    }

    companion object {
        private const val IV_LENGTH = 16
        private const val SQLITE_HEADER = "SQLite format 3"
        private const val GZIP_MAGIC_0: Byte = 0x1f
        private const val GZIP_MAGIC_1: Byte = 0x8b.toByte()
        fun gunzip(data: ByteArray): ByteArray = GZIPInputStream(ByteArrayInputStream(data)).use { it.readBytes() }

        fun gzip(data: ByteArray): ByteArray =
            ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(data) } }.toByteArray()

        fun isSqlite(data: ByteArray): Boolean =
            data.size > SQLITE_HEADER.length &&
                String(data, 0, SQLITE_HEADER.length, Charsets.US_ASCII) == SQLITE_HEADER

        fun isGzip(data: ByteArray): Boolean = data.size >= 2 && data[0] == GZIP_MAGIC_0 && data[1] == GZIP_MAGIC_1
    }
}
