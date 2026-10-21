package ru.b3lka.linary.desktop.dbus

/**
 * Запись значений D-Bus (little-endian) с выравниванием по правилам протокола.
 *
 * Выравнивание считается от начала буфера, поэтому тело сообщения пишется отдельным писателем:
 * в сообщении оно всё равно начинается с границы 8 байт.
 */
class DBusWriter {
    private var buffer = ByteArray(INITIAL_SIZE)

    var size = 0
        private set

    fun toByteArray(): ByteArray = buffer.copyOf(size)

    fun byte(value: Int) {
        ensure(1)
        buffer[size++] = value.toByte()
    }

    fun boolean(value: Boolean) = uint32(if (value) 1 else 0)

    fun int32(value: Int) {
        pad(INT_ALIGN)
        putInt(value)
    }

    fun uint32(value: Long) = int32(value.toInt())

    fun uint32(value: Int) = int32(value)

    fun string(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        int32(bytes.size)
        raw(bytes)
        byte(0)
    }

    fun objectPath(value: String) = string(value)

    fun signature(value: String) {
        val bytes = value.toByteArray(Charsets.US_ASCII)
        byte(bytes.size)
        raw(bytes)
        byte(0)
    }

    /**
     * Вариант: подпись [signature] и значение, которое пишет [value].
     */
    fun variant(signature: String, value: DBusWriter.() -> Unit) {
        signature(signature)
        value()
    }

    /**
     * Массив: длина в байтах, выравнивание первого элемента по [elementAlign], элементы из [elements].
     */
    fun array(elementAlign: Int, elements: DBusWriter.() -> Unit) {
        pad(INT_ALIGN)
        val lengthAt = size
        putInt(0)
        pad(elementAlign)
        val start = size
        elements()
        patchInt(lengthAt, size - start)
    }

    /**
     * Структура или элемент словаря: выравнивание по 8 байт.
     */
    fun struct(fields: DBusWriter.() -> Unit) {
        pad(STRUCT_ALIGN)
        fields()
    }

    fun bytes(value: ByteArray) = array(1) { raw(value) }

    fun pad(alignment: Int) {
        while (size % alignment != 0) byte(0)
    }

    private fun raw(bytes: ByteArray) {
        ensure(bytes.size)
        bytes.copyInto(buffer, size)
        size += bytes.size
    }

    private fun putInt(value: Int) {
        ensure(INT_ALIGN)
        patchInt(size, value)
        size += INT_ALIGN
    }

    private fun patchInt(at: Int, value: Int) {
        for (i in 0 until INT_ALIGN) buffer[at + i] = (value ushr (BITS_PER_BYTE * i)).toByte()
    }

    private fun ensure(extra: Int) {
        if (size + extra > buffer.size) buffer = buffer.copyOf(maxOf(buffer.size * 2, size + extra))
    }

    companion object {
        const val INT_ALIGN = 4
        const val STRUCT_ALIGN = 8
        private const val BITS_PER_BYTE = 8
        private const val INITIAL_SIZE = 256
    }
}
