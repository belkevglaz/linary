package ru.b3lka.linary.desktop.settings

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Хэш PIN-кода для хранения в настройках: PBKDF2-HMAC-SHA256 с солью.
 * Формат строки: `pbkdf2-sha256$<итерации>$<соль base64>$<хэш base64>`.
 */
internal object PinHash {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val PREFIX = "pbkdf2-sha256"
    private const val ITERATIONS = 210_000
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256
    private const val PARTS = 4

    private val random = SecureRandom()

    fun hash(pin: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val hash = derive(pin, salt, ITERATIONS)
        return listOf(PREFIX, ITERATIONS.toString(), encode(salt), encode(hash)).joinToString("$")
    }

    fun verify(pin: String, stored: String): Boolean {
        val parts = stored.split('$')
        if (parts.size != PARTS || parts[0] != PREFIX) return false
        return runCatching {
            val iterations = parts[1].toInt()
            val salt = Base64.getDecoder().decode(parts[2])
            val expected = Base64.getDecoder().decode(parts[PARTS - 1])
            MessageDigest.isEqual(derive(pin, salt, iterations), expected)
        }.getOrDefault(false)
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, HASH_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}
