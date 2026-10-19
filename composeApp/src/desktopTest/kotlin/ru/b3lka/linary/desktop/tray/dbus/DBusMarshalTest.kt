package ru.b3lka.linary.desktop.tray.dbus

import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class DBusMarshalTest {

    @Test
    fun nestedValuesSurviveRoundTrip() {
        val writer = DBusWriter().apply {
            uint32(7L)
            struct {
                int32(-5)
                array(DBusWriter.STRUCT_ALIGN) {
                    struct {
                        string("label")
                        variant("s") { string("Открыть") }
                    }
                    struct {
                        string("enabled")
                        variant("b") { boolean(true) }
                    }
                }
                array(1) {
                    variant("(ia{sv}av)") {
                        struct {
                            int32(1)
                            array(DBusWriter.STRUCT_ALIGN) {}
                            array(1) {}
                        }
                    }
                }
            }
        }
        val (revision, layout) = DBusReader(writer.toByteArray(), ByteOrder.LITTLE_ENDIAN).readAll("u(ia{sv}av)")
        assertEquals(7L, revision)
        val (id, props, children) = layout as List<*>
        assertEquals(-5, id)
        assertEquals(mapOf("label" to Variant("s", "Открыть"), "enabled" to Variant("b", true)), props)
        assertEquals(listOf(Variant("(ia{sv}av)", listOf(1, emptyMap<Any?, Any?>(), emptyList<Any?>()))), children)
    }

    @Test
    fun bytesAndAlignment() {
        val writer = DBusWriter().apply {
            byte(1)
            bytes(byteArrayOf(9, 8, 7))
            int32(42)
        }
        // байт, выравнивание до 4, длина массива, 3 байта, выравнивание до 4, число
        assertEquals(1 + 3 + 4 + 3 + 1 + 4, writer.size)
        val values = DBusReader(writer.toByteArray(), ByteOrder.LITTLE_ENDIAN).readAll("yayi")
        assertEquals(listOf(1, listOf(9, 8, 7), 42), values)
    }

    @Test
    fun messageEncodeDecode() {
        val body = DBusWriter().apply { string("org.kde.StatusNotifierItem-1-1") }.toByteArray()
        val message = DBusMessage(
            type = DBusMessage.METHOD_CALL,
            serial = 3,
            fields = mapOf(
                DBusMessage.FIELD_PATH to "/StatusNotifierWatcher",
                DBusMessage.FIELD_INTERFACE to "org.kde.StatusNotifierWatcher",
                DBusMessage.FIELD_MEMBER to "RegisterStatusNotifierItem",
                DBusMessage.FIELD_SIGNATURE to "s",
            ),
            body = body,
        )
        val encoded = message.encode()
        assertEquals(encoded.size, DBusMessage.totalLength(encoded.copyOf(DBusMessage.FIXED_HEADER)))
        val decoded = DBusMessage.decode(encoded)
        assertEquals(3L, decoded.serial)
        assertEquals("/StatusNotifierWatcher", decoded.path)
        assertEquals("RegisterStatusNotifierItem", decoded.member)
        assertContentEquals(body, decoded.body)
        assertEquals(listOf("org.kde.StatusNotifierItem-1-1"), decoded.args())
    }

    @Test
    fun typeEndHandlesContainers() {
        val signature = "a{sv}(ia{sv}av)u"
        assertEquals(5, DBusReader.typeEnd(signature, 0))
        assertEquals(15, DBusReader.typeEnd(signature, 5))
        assertEquals(16, DBusReader.typeEnd(signature, 15))
    }
}
