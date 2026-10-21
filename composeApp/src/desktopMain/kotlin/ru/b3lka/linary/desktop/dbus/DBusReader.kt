package ru.b3lka.linary.desktop.dbus

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Значение-вариант D-Bus: подпись и разобранное значение.
 */
data class Variant(val signature: String, val value: Any?)

/**
 * Разбор значений D-Bus по подписи.
 *
 * Типы в Kotlin: `y`, `n`, `q`, `i`, `h` — Int; `u`, `x`, `t` — Long; `b` — Boolean; `d` — Double;
 * `s`, `o`, `g` — String; `v` — [Variant]; `a{..}` — Map; прочие массивы и структуры — List.
 *
 * @param base смещение [data] от начала сообщения — от него считается выравнивание.
 */
class DBusReader(data: ByteArray, order: ByteOrder, private val base: Int = 0) {
    private val buffer: ByteBuffer = ByteBuffer.wrap(data).order(order)

    /**
     * Читает подряд все значения из [signature].
     */
    fun readAll(signature: String): List<Any?> {
        val values = mutableListOf<Any?>()
        var at = 0
        while (at < signature.length) {
            val end = typeEnd(signature, at)
            values += read(signature.substring(at, end))
            at = end
        }
        return values
    }

    /**
     * Читает одно значение полного типа [type].
     */
    @Suppress("CyclomaticComplexMethod")
    fun read(type: String): Any? = when (type[0]) {
        'y' -> buffer.get().toInt() and BYTE_MASK
        'b' -> readInt() != 0
        'n' -> align(2).let { buffer.getShort().toInt() }
        'q' -> align(2).let { buffer.getShort().toInt() and SHORT_MASK }
        'i', 'h' -> readInt()
        'u' -> readInt().toLong() and UINT_MASK
        'x', 't' -> align(LONG_ALIGN).let { buffer.getLong() }
        'd' -> align(LONG_ALIGN).let { buffer.getDouble() }
        's', 'o' -> readString()
        'g' -> readSignature()
        'v' -> readSignature().let { Variant(it, read(it)) }
        'a' -> readArray(type.substring(1))
        '(' -> readStruct(type.substring(1, type.length - 1))
        else -> throw DBusException("org.freedesktop.DBus.Error.InvalidSignature", "Unsupported type $type")
    }

    private fun readArray(element: String): Any {
        val length = readInt()
        align(alignment(element[0]))
        val end = buffer.position() + length
        if (element[0] == '{') {
            val inner = element.substring(1, element.length - 1)
            val keyEnd = typeEnd(inner, 0)
            val map = LinkedHashMap<Any?, Any?>()
            while (buffer.position() < end) {
                align(DBusWriter.STRUCT_ALIGN)
                map[read(inner.substring(0, keyEnd))] = read(inner.substring(keyEnd))
            }
            return map
        }
        val list = mutableListOf<Any?>()
        while (buffer.position() < end) list += read(element)
        return list
    }

    private fun readStruct(fields: String): List<Any?> {
        align(DBusWriter.STRUCT_ALIGN)
        return readAll(fields)
    }

    private fun readInt(): Int {
        align(DBusWriter.INT_ALIGN)
        return buffer.getInt()
    }

    private fun readString(): String {
        val length = readInt()
        val bytes = ByteArray(length).also { buffer.get(it) }
        buffer.get()
        return String(bytes, Charsets.UTF_8)
    }

    private fun readSignature(): String {
        val length = buffer.get().toInt() and BYTE_MASK
        val bytes = ByteArray(length).also { buffer.get(it) }
        buffer.get()
        return String(bytes, Charsets.US_ASCII)
    }

    private fun align(alignment: Int) {
        val misaligned = (base + buffer.position()) % alignment
        if (misaligned != 0) buffer.position(buffer.position() + alignment - misaligned)
    }

    companion object {
        private const val BYTE_MASK = 0xFF
        private const val SHORT_MASK = 0xFFFF
        private const val UINT_MASK = 0xFFFF_FFFFL
        private const val LONG_ALIGN = 8

        fun alignment(type: Char): Int = when (type) {
            'n', 'q' -> 2
            'b', 'i', 'u', 'h', 's', 'o', 'a' -> DBusWriter.INT_ALIGN
            'x', 't', 'd', '(', '{' -> LONG_ALIGN
            else -> 1
        }

        /**
         * Конец полного типа, начинающегося в [signature] с позиции [start].
         */
        fun typeEnd(signature: String, start: Int): Int = when (signature[start]) {
            'a' -> typeEnd(signature, start + 1)
            '(', '{' -> {
                var depth = 0
                var at = start
                do {
                    when (signature[at]) {
                        '(', '{' -> depth++
                        ')', '}' -> depth--
                    }
                    at++
                } while (depth > 0)
                at
            }
            else -> start + 1
        }
    }
}
