package ru.b3lka.linary.desktop.tray

import ru.b3lka.linary.desktop.APP_ID
import ru.b3lka.linary.desktop.tray.dbus.DBusConnection
import ru.b3lka.linary.desktop.tray.dbus.DBusMessage
import ru.b3lka.linary.desktop.tray.dbus.DBusWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Картинка значка: размер и пиксели ARGB (как у `BufferedImage.getRGB`).
 */
class IconPixmap(val size: Int, val argb: IntArray)

/**
 * Значок в трее по протоколу StatusNotifierItem (D-Bus) — так значки показывают KDE Plasma, а также
 * GNOME с расширением AppIndicator, XFCE, Cinnamon и др. Панель сама рисует картинку нужного размера
 * с прозрачностью и сама показывает меню ([TrayMenu]).
 *
 * Щелчок по значку (`Activate`) вызывает [onActivate]. Если панель перезапустится, значок
 * зарегистрируется заново.
 */
internal class StatusNotifierTray private constructor(
    private val connection: DBusConnection,
    private val title: String,
    private val icons: List<IconPixmap>,
    private val menu: TrayMenu,
    private val onActivate: () -> Unit,
) : AutoCloseable {
    private val busName = "org.kde.StatusNotifierItem-${ProcessHandle.current().pid()}-1"

    private val properties: Map<String, Property> = mapOf(
        "Category" to Property("s") { string("ApplicationStatus") },
        "Id" to Property("s") { string(ID) },
        "Title" to Property("s") { string(title) },
        "Status" to Property("s") { string("Active") },
        "WindowId" to Property("i") { int32(0) },
        "IconName" to Property("s") { string("") },
        "IconPixmap" to Property("a(iiay)") { writePixmaps() },
        "OverlayIconName" to Property("s") { string("") },
        "OverlayIconPixmap" to Property("a(iiay)") { array(DBusWriter.STRUCT_ALIGN) {} },
        "AttentionIconName" to Property("s") { string("") },
        "AttentionIconPixmap" to Property("a(iiay)") { array(DBusWriter.STRUCT_ALIGN) {} },
        "AttentionMovieName" to Property("s") { string("") },
        "ToolTip" to Property("(sa(iiay)ss)") {
            struct {
                string("")
                array(DBusWriter.STRUCT_ALIGN) {}
                string(title)
                string("")
            }
        },
        "ItemIsMenu" to Property("b") { boolean(false) },
        "Menu" to Property("o") { objectPath(TrayMenu.PATH) },
    )

    private fun start() {
        connection.onMethodCall = ::handle
        connection.onSignal = ::onSignal
        connection.callBus("RequestName", "su") {
            string(busName)
            uint32(DO_NOT_QUEUE)
        }
        connection.addMatch(
            "type='signal',sender='${DBusConnection.BUS_NAME}',interface='${DBusConnection.BUS_INTERFACE}'," +
                "member='NameOwnerChanged',arg0='$WATCHER'",
        )
        register()
    }

    private fun register() {
        connection.call(WATCHER, WATCHER_PATH, WATCHER, "RegisterStatusNotifierItem", "s") { string(busName) }
    }

    /**
     * Панель перезапустилась — регистрируемся у нового наблюдателя. Не в потоке чтения: там нельзя ждать ответа.
     */
    private fun onSignal(signal: DBusMessage) {
        if (signal.member != "NameOwnerChanged") return
        val (name, _, newOwner) = signal.args()
        if (name == WATCHER && (newOwner as String).isNotEmpty()) {
            Thread({ runCatching { register() } }, "tray-register").apply { isDaemon = true }.start()
        }
    }

    private fun handle(call: DBusMessage) {
        if (menu.handle(connection, call)) return
        when (call.member) {
            "Activate", "SecondaryActivate" -> {
                connection.reply(call)
                onActivate()
            }
            "ContextMenu", "Scroll", "ProvideXdgActivationToken" -> connection.reply(call)
            "Get" -> replyGet(call)
            "GetAll" -> connection.reply(call, "a{sv}") { writeAll(propertiesOf(call.path)) }
            "Introspect" -> connection.reply(call, "s") { string(introspection(call.path)) }
            "Ping" -> connection.reply(call)
            else -> connection.replyError(call, DBusConnection.UNKNOWN_METHOD, "No such method ${call.member}")
        }
    }

    private fun propertiesOf(path: String?): Map<String, Property> =
        if (path == TrayMenu.PATH) menu.properties else properties

    private fun replyGet(call: DBusMessage) {
        val (_, name) = call.args()
        val property = propertiesOf(call.path)[name]
        if (property == null) {
            connection.replyError(call, "org.freedesktop.DBus.Error.UnknownProperty", "No property $name")
        } else {
            connection.reply(call, "v") { variant(property.signature, property.write) }
        }
    }

    private fun DBusWriter.writeAll(values: Map<String, Property>) {
        array(DBusWriter.STRUCT_ALIGN) {
            values.forEach { (name, property) ->
                struct {
                    string(name)
                    variant(property.signature, property.write)
                }
            }
        }
    }

    /**
     * Пиксели в порядке байт A, R, G, B (network byte order), как требует протокол.
     */
    private fun DBusWriter.writePixmaps() {
        array(DBusWriter.STRUCT_ALIGN) {
            icons.forEach { icon ->
                struct {
                    int32(icon.size)
                    int32(icon.size)
                    bytes(networkOrder(icon.argb))
                }
            }
        }
    }

    private fun networkOrder(argb: IntArray): ByteArray {
        val bytes = ByteArray(argb.size * ARGB_BYTES)
        ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).asIntBuffer().put(argb)
        return bytes
    }

    private fun introspection(path: String?): String {
        val iface = if (path == TrayMenu.PATH) TrayMenu.INTERFACE else ITEM_INTERFACE
        return """<!DOCTYPE node PUBLIC "-//freedesktop//DTD D-BUS Object Introspection 1.0//EN"
            |"http://www.freedesktop.org/standards/dbus/1.0/introspect.dtd">
            |<node><interface name="$iface"/><interface name="org.freedesktop.DBus.Properties"/></node>
        """.trimMargin()
    }

    override fun close() = connection.close()

    companion object {
        private const val ID = APP_ID
        private const val WATCHER = "org.kde.StatusNotifierWatcher"
        private const val WATCHER_PATH = "/StatusNotifierWatcher"
        private const val ITEM_INTERFACE = "org.kde.StatusNotifierItem"
        private const val DO_NOT_QUEUE = 4
        private const val ARGB_BYTES = 4

        /**
         * Есть ли на рабочем столе панель, принимающая такие значки.
         */
        fun isAvailable(): Boolean = DBusConnection.session()?.use { hasWatcher(it) } ?: false

        /**
         * Показывает значок или возвращает null, если панели нет или шина недоступна.
         */
        fun create(
            title: String,
            icons: List<IconPixmap>,
            menu: List<TrayMenuItem>,
            onActivate: () -> Unit,
        ): StatusNotifierTray? {
            val connection = DBusConnection.session() ?: return null
            return runCatching {
                check(hasWatcher(connection))
                StatusNotifierTray(connection, title, icons, TrayMenu(menu), onActivate).also { it.start() }
            }.onFailure { connection.close() }.getOrNull()
        }

        private fun hasWatcher(connection: DBusConnection): Boolean = runCatching {
            connection.callBus("NameHasOwner", "s") { string(WATCHER) }.args().first() as Boolean
        }.getOrDefault(false)
    }
}
