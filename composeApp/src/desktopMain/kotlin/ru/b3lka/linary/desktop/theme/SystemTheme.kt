package ru.b3lka.linary.desktop.theme

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import ru.b3lka.linary.desktop.dbus.DBusConnection
import ru.b3lka.linary.desktop.dbus.DBusException
import ru.b3lka.linary.desktop.dbus.DBusMessage
import ru.b3lka.linary.desktop.dbus.Variant

/**
 * Тема системы из XDG Desktop Portal: настройка `org.freedesktop.appearance` / `color-scheme`.
 * Её понимают KDE Plasma, GNOME и другие окружения с xdg-desktop-portal; при смене темы в системе
 * портал присылает сигнал `SettingChanged`.
 */
object SystemTheme {
    /**
     * Тёмная ли тема системы: сначала текущее значение, затем каждое изменение.
     * null — окружение не сообщает тему (нет портала). Без D-Bus поток пустой.
     */
    fun darkFlow(): Flow<Boolean?> = callbackFlow {
        val connection = DBusConnection.session()
        if (connection == null) {
            close()
            return@callbackFlow
        }
        connection.onSignal = { signal -> changedScheme(signal)?.let { trySend(isDark(it)) } }
        runCatching {
            connection.addMatch(
                "type='signal',interface='$SETTINGS',member='SettingChanged',arg0='$NAMESPACE',arg1='$KEY'",
            )
        }
        trySend(readScheme(connection)?.let(::isDark))
        awaitClose { connection.close() }
    }.distinctUntilChanged().flowOn(Dispatchers.IO)

    /**
     * Текущее значение `color-scheme`; null — портала нет. Старые порталы знают только `Read`.
     */
    private fun readScheme(connection: DBusConnection): Long? {
        val reply = try {
            connection.call(PORTAL, PORTAL_PATH, SETTINGS, "ReadOne", "ss") {
                string(NAMESPACE)
                string(KEY)
            }
        } catch (_: DBusException) {
            runCatching {
                connection.call(PORTAL, PORTAL_PATH, SETTINGS, "Read", "ss") {
                    string(NAMESPACE)
                    string(KEY)
                }
            }.getOrNull()
        }
        return reply?.args()?.firstOrNull()?.let(::unwrap) as? Long
    }

    private fun changedScheme(signal: DBusMessage): Long? {
        if (signal.member != "SettingChanged") return null
        val args = signal.args()
        if (args.getOrNull(0) != NAMESPACE || args.getOrNull(1) != KEY) return null
        return args.getOrNull(2)?.let(::unwrap) as? Long
    }

    /**
     * `Read` возвращает значение, завёрнутое в несколько вариантов.
     */
    private fun unwrap(value: Any?): Any? = if (value is Variant) unwrap(value.value) else value

    /**
     * 1 — тёмная, 2 — светлая, 0 — без предпочтения. KDE Plasma для светлой схемы присылает 0,
     * поэтому всё, кроме 1, считается светлой темой.
     */
    private fun isDark(scheme: Long): Boolean = scheme == PREFER_DARK

    private const val PORTAL = "org.freedesktop.portal.Desktop"
    private const val PORTAL_PATH = "/org/freedesktop/portal/desktop"
    private const val SETTINGS = "org.freedesktop.portal.Settings"
    private const val NAMESPACE = "org.freedesktop.appearance"
    private const val KEY = "color-scheme"
    private const val PREFER_DARK = 1L
}
