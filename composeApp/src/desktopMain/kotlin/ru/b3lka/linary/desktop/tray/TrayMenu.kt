package ru.b3lka.linary.desktop.tray

import ru.b3lka.linary.desktop.tray.dbus.DBusConnection
import ru.b3lka.linary.desktop.tray.dbus.DBusMessage
import ru.b3lka.linary.desktop.tray.dbus.DBusWriter

/**
 * Пункт меню значка.
 *
 * @param label текст; null — разделитель.
 */
class TrayMenuItem(val label: String?, val onClick: () -> Unit = {})

/**
 * Меню значка по протоколу `com.canonical.dbusmenu` — его показывает сама панель (KDE, GNOME с AppIndicator),
 * в своём оформлении. Меню неизменное: корень с id 0 и пункты с id 1, 2, …
 */
internal class TrayMenu(private val items: List<TrayMenuItem>) {

    /**
     * Обрабатывает вызов, адресованный меню. false — вызов не к меню.
     */
    fun handle(connection: DBusConnection, call: DBusMessage): Boolean {
        if (call.path != PATH) return false
        when (call.member) {
            "GetLayout" -> connection.reply(call, "u(ia{sv}av)") {
                val (parent, depth) = call.args()
                uint32(REVISION)
                writeLayout(parent as Int, depth as Int)
            }
            "GetGroupProperties" -> connection.reply(call, "a(ia{sv})") {
                @Suppress("UNCHECKED_CAST")
                val ids = (call.args()[0] as List<Int>).ifEmpty { allIds() }
                array(DBusWriter.STRUCT_ALIGN) {
                    ids.filter { it in allIds() }.forEach { id ->
                        struct {
                            int32(id)
                            writeProperties(id)
                        }
                    }
                }
            }
            "GetProperty" -> replyProperty(connection, call)
            "Event" -> {
                val (id, event) = call.args()
                if (event == "clicked") items.getOrNull((id as Int) - 1)?.onClick?.invoke()
                connection.reply(call)
            }
            "EventGroup" -> {
                @Suppress("UNCHECKED_CAST")
                (call.args()[0] as List<List<Any?>>).forEach { (id, event) ->
                    if (event == "clicked") items.getOrNull((id as Int) - 1)?.onClick?.invoke()
                }
                connection.reply(call, "ai") { array(DBusWriter.INT_ALIGN) {} }
            }
            "AboutToShow" -> connection.reply(call, "b") { boolean(false) }
            "AboutToShowGroup" -> connection.reply(call, "aiai") {
                array(DBusWriter.INT_ALIGN) {}
                array(DBusWriter.INT_ALIGN) {}
            }
            else -> return false
        }
        return true
    }

    /**
     * Свойства интерфейса меню для `org.freedesktop.DBus.Properties`.
     */
    val properties: Map<String, Property> = mapOf(
        "Version" to Property("u") { uint32(DBUSMENU_VERSION) },
        "TextDirection" to Property("s") { string("ltr") },
        "Status" to Property("s") { string("normal") },
        "IconThemePath" to Property("as") { array(DBusWriter.INT_ALIGN) {} },
    )

    private fun allIds(): List<Int> = (0..items.size).toList()

    private fun DBusWriter.writeLayout(id: Int, depth: Int) {
        struct {
            int32(id)
            writeProperties(id)
            val children = if (id == ROOT && depth != 0) items.indices.map { it + 1 } else emptyList()
            array(1) {
                children.forEach { child -> variant("(ia{sv}av)") { writeLayout(child, 0) } }
            }
        }
    }

    private fun DBusWriter.writeProperties(id: Int) {
        array(DBusWriter.STRUCT_ALIGN) {
            itemProperties(id).forEach { (name, property) ->
                struct {
                    string(name)
                    variant(property.signature, property.write)
                }
            }
        }
    }

    private fun itemProperties(id: Int): Map<String, Property> {
        if (id == ROOT) return mapOf("children-display" to Property("s") { string("submenu") })
        val label = items.getOrNull(id - 1)?.label
        return if (label == null) {
            mapOf("type" to Property("s") { string("separator") })
        } else {
            mapOf("label" to Property("s") { string(label) }, "enabled" to Property("b") { boolean(true) })
        }
    }

    private fun replyProperty(connection: DBusConnection, call: DBusMessage) {
        val (id, name) = call.args()
        val property = itemProperties(id as Int)[name]
        if (property == null) {
            connection.replyError(call, "org.freedesktop.DBus.Error.InvalidArgs", "No property $name")
        } else {
            connection.reply(call, "v") { variant(property.signature, property.write) }
        }
    }

    companion object {
        const val PATH = "/MenuBar"
        const val INTERFACE = "com.canonical.dbusmenu"
        private const val ROOT = 0
        private const val REVISION = 1L
        private const val DBUSMENU_VERSION = 3L
    }
}

/**
 * Значение свойства D-Bus: подпись и как его записать.
 */
class Property(val signature: String, val write: DBusWriter.() -> Unit)
