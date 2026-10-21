package ru.b3lka.linary.desktop.dbus

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Ошибка D-Bus: имя ошибки и сообщение.
 */
class DBusException(val name: String, message: String) : Exception(message)

/**
 * Сообщение D-Bus: заголовок и тело (ещё не разобранное, см. [args]).
 */
class DBusMessage(
    val type: Int,
    val serial: Long,
    val fields: Map<Int, Any?>,
    val body: ByteArray = ByteArray(0),
    val flags: Int = 0,
    private val order: ByteOrder = ByteOrder.LITTLE_ENDIAN,
) {
    val path: String? get() = fields[FIELD_PATH] as String?
    val iface: String? get() = fields[FIELD_INTERFACE] as String?
    val member: String? get() = fields[FIELD_MEMBER] as String?
    val errorName: String? get() = fields[FIELD_ERROR_NAME] as String?
    val replySerial: Long? get() = fields[FIELD_REPLY_SERIAL] as Long?
    val sender: String? get() = fields[FIELD_SENDER] as String?
    val signature: String get() = fields[FIELD_SIGNATURE] as String? ?: ""

    val expectsReply: Boolean get() = type == METHOD_CALL && flags and FLAG_NO_REPLY_EXPECTED == 0

    /**
     * Аргументы из тела по подписи сообщения.
     */
    fun args(): List<Any?> = DBusReader(body, order).readAll(signature)

    /**
     * Сообщение целиком, как оно идёт по сокету (little-endian).
     */
    fun encode(): ByteArray {
        val header = DBusWriter()
        header.byte(LITTLE_ENDIAN.code)
        header.byte(type)
        header.byte(flags)
        header.byte(PROTOCOL_VERSION)
        header.uint32(body.size)
        header.uint32(serial)
        header.array(DBusWriter.STRUCT_ALIGN) {
            fields.forEach { (code, value) ->
                if (value != null) {
                    struct {
                        byte(code)
                        writeField(this, code, value)
                    }
                }
            }
        }
        header.pad(DBusWriter.STRUCT_ALIGN)
        return header.toByteArray() + body
    }

    private fun writeField(writer: DBusWriter, code: Int, value: Any) {
        when (code) {
            FIELD_PATH -> writer.variant("o") { objectPath(value as String) }
            FIELD_REPLY_SERIAL, FIELD_UNIX_FDS -> writer.variant("u") { uint32(value as Long) }
            FIELD_SIGNATURE -> writer.variant("g") { signature(value as String) }
            else -> writer.variant("s") { string(value as String) }
        }
    }

    companion object {
        const val METHOD_CALL = 1
        const val METHOD_RETURN = 2
        const val ERROR = 3
        const val SIGNAL = 4

        const val FLAG_NO_REPLY_EXPECTED = 1

        const val FIELD_PATH = 1
        const val FIELD_INTERFACE = 2
        const val FIELD_MEMBER = 3
        const val FIELD_ERROR_NAME = 4
        const val FIELD_REPLY_SERIAL = 5
        const val FIELD_DESTINATION = 6
        const val FIELD_SENDER = 7
        const val FIELD_SIGNATURE = 8
        const val FIELD_UNIX_FDS = 9

        /**
         * Размер начала заголовка: 12 байт фиксированных полей и длина массива полей.
         */
        const val FIXED_HEADER = 16
        private const val PROTOCOL_VERSION = 1
        private const val TYPE_AT = 1
        private const val FLAGS_AT = 2
        private const val BODY_LENGTH_AT = 4
        private const val SERIAL_AT = 8
        private const val FIELDS_LENGTH_AT = 12
        private const val UINT_MASK = 0xFFFF_FFFFL
        private const val LITTLE_ENDIAN = 'l'
        private const val MAX_MESSAGE = 128 * 1024 * 1024

        /**
         * Полная длина сообщения по первым [FIXED_HEADER] байтам.
         */
        fun totalLength(start: ByteArray): Int {
            val buffer = ByteBuffer.wrap(start).order(orderOf(start[0]))
            val bodyLength = buffer.getInt(BODY_LENGTH_AT)
            val fieldsLength = buffer.getInt(FIELDS_LENGTH_AT)
            val headerLength = alignUp(FIXED_HEADER + fieldsLength, DBusWriter.STRUCT_ALIGN)
            val total = headerLength.toLong() + bodyLength
            if (bodyLength < 0 || fieldsLength < 0 || total > MAX_MESSAGE) {
                throw DBusException("org.freedesktop.DBus.Error.InvalidArgs", "Message too large")
            }
            return total.toInt()
        }

        /**
         * Разбирает сообщение целиком (заголовок и тело).
         */
        fun decode(data: ByteArray): DBusMessage {
            val order = orderOf(data[0])
            val buffer = ByteBuffer.wrap(data).order(order)
            val type = data[TYPE_AT].toInt()
            val flags = data[FLAGS_AT].toInt()
            val serial = buffer.getInt(SERIAL_AT).toLong() and UINT_MASK
            val fieldsLength = buffer.getInt(FIELDS_LENGTH_AT)
            val headerLength = alignUp(FIXED_HEADER + fieldsLength, DBusWriter.STRUCT_ALIGN)
            val header = data.copyOfRange(FIELDS_LENGTH_AT, FIXED_HEADER + fieldsLength)

            @Suppress("UNCHECKED_CAST")
            val raw = DBusReader(header, order, base = FIELDS_LENGTH_AT).read("a(yv)") as List<List<Any?>>
            val fields = raw.associate { (code, variant) -> (code as Int) to (variant as Variant).value }
            return DBusMessage(type, serial, fields, data.copyOfRange(headerLength, data.size), flags, order)
        }

        private fun orderOf(marker: Byte): ByteOrder =
            if (marker.toInt().toChar() == 'B') ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN

        private fun alignUp(value: Int, alignment: Int) = (value + alignment - 1) / alignment * alignment
    }
}
