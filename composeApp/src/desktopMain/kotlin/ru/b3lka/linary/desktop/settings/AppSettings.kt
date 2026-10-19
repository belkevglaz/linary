package ru.b3lka.linary.desktop.settings

import ru.b3lka.linary.desktop.APP_ID
import ru.b3lka.linary.presentation.layout.ListPanePreferences
import ru.b3lka.linary.presentation.lock.LockController
import ru.b3lka.linary.presentation.lock.LockPreferences
import ru.b3lka.linary.presentation.lock.LockStyle
import ru.b3lka.linary.presentation.media.PhotoStripPreferences
import ru.b3lka.linary.presentation.sync.SyncKeyPreferences
import ru.b3lka.linary.presentation.window.WindowPreferences
import java.util.prefs.Preferences

/**
 * Настройки приложения в `java.util.prefs` (на Linux — `~/.java/.userPrefs/linary/prefs.xml`).
 * PIN блокировки хранится только хэшем ([PinHash]).
 */
class AppSettings :
    LockPreferences,
    PhotoStripPreferences,
    WindowPreferences,
    SyncKeyPreferences,
    ListPanePreferences {
    private val prefs: Preferences = Preferences.userRoot().node(APP_ID)

    var source: String?
        get() = prefs.get(KEY_SOURCE, null)
        set(value) = if (value == null) prefs.remove(KEY_SOURCE) else prefs.put(KEY_SOURCE, value)

    var dark: Boolean
        get() = prefs.getBoolean(KEY_DARK, true)
        set(value) = prefs.putBoolean(KEY_DARK, value)

    /**
     * Положение и размер окна, когда оно не развёрнуто.
     */
    var windowBounds: WindowBounds?
        get() = WindowBounds.decode(prefs.get(KEY_WINDOW_BOUNDS, null))
        set(value) {
            if (value == null) prefs.remove(KEY_WINDOW_BOUNDS) else prefs.put(KEY_WINDOW_BOUNDS, value.encode())
        }

    var windowMaximized: Boolean
        get() = prefs.getBoolean(KEY_WINDOW_MAXIMIZED, false)
        set(value) = prefs.putBoolean(KEY_WINDOW_MAXIMIZED, value)

    override var lockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        set(value) {
            prefs.putBoolean(KEY_LOCK_ENABLED, value)
            flush()
        }

    override var lockTimeoutMinutes: Int
        get() = prefs.getInt(KEY_LOCK_TIMEOUT, LockController.DEFAULT_TIMEOUT)
        set(value) {
            prefs.putInt(KEY_LOCK_TIMEOUT, value)
            flush()
        }

    override var lockStyle: String
        get() = prefs.get(KEY_LOCK_STYLE, LockStyle.Solid.name)
        set(value) {
            prefs.put(KEY_LOCK_STYLE, value)
            flush()
        }

    override var lockBlurRadius: Int
        get() = prefs.getInt(KEY_LOCK_BLUR, LockController.DEFAULT_BLUR)
        set(value) {
            prefs.putInt(KEY_LOCK_BLUR, value)
            flush()
        }

    override var lockCollapsePhotos: Boolean
        get() = prefs.getBoolean(KEY_LOCK_COLLAPSE_PHOTOS, true)
        set(value) {
            prefs.putBoolean(KEY_LOCK_COLLAPSE_PHOTOS, value)
            flush()
        }

    override var photosExpanded: Boolean
        get() = prefs.getBoolean(KEY_PHOTOS_EXPANDED, true)
        set(value) {
            prefs.putBoolean(KEY_PHOTOS_EXPANDED, value)
            flush()
        }

    override var listPaneVisible: Boolean
        get() = prefs.getBoolean(KEY_LIST_PANE, true)
        set(value) {
            prefs.putBoolean(KEY_LIST_PANE, value)
            flush()
        }

    override var trayIcon: Boolean
        get() = prefs.getBoolean(KEY_TRAY_ICON, true)
        set(value) {
            prefs.putBoolean(KEY_TRAY_ICON, value)
            flush()
        }

    override var closeKeepsRunning: Boolean
        get() = prefs.getBoolean(KEY_CLOSE_KEEPS_RUNNING, true)
        set(value) {
            prefs.putBoolean(KEY_CLOSE_KEEPS_RUNNING, value)
            flush()
        }

    /**
     * Ключ синхронизации, который ввёл пользователь (в программу он не входит).
     */
    override var syncKey: String?
        get() = prefs.get(KEY_SYNC_KEY, null)
        set(value) {
            if (value == null) prefs.remove(KEY_SYNC_KEY) else prefs.put(KEY_SYNC_KEY, value)
            flush()
        }

    override val hasPin: Boolean get() = prefs.get(KEY_LOCK_PIN, null) != null

    override fun setPin(pin: String) {
        prefs.put(KEY_LOCK_PIN, PinHash.hash(pin))
        flush()
    }

    override fun checkPin(pin: String): Boolean =
        prefs.get(KEY_LOCK_PIN, null)?.let { PinHash.verify(pin, it) } ?: false

    override fun clearPin() {
        prefs.remove(KEY_LOCK_PIN)
        flush()
    }

    /**
     * Записывает настройки на диск сразу, не дожидаясь выхода из JVM.
     */
    fun flush() {
        runCatching { prefs.flush() }
    }

    private companion object {
        const val KEY_SOURCE = "source"
        const val KEY_DARK = "dark"
        const val KEY_WINDOW_BOUNDS = "window.bounds"
        const val KEY_WINDOW_MAXIMIZED = "window.maximized"
        const val KEY_LOCK_ENABLED = "lock.enabled"
        const val KEY_LOCK_TIMEOUT = "lock.timeoutMinutes"
        const val KEY_LOCK_PIN = "lock.pin"
        const val KEY_LOCK_STYLE = "lock.style"
        const val KEY_LOCK_BLUR = "lock.blurRadius"
        const val KEY_LOCK_COLLAPSE_PHOTOS = "lock.collapsePhotos"
        const val KEY_PHOTOS_EXPANDED = "media.expanded"
        const val KEY_TRAY_ICON = "window.trayIcon"
        const val KEY_LIST_PANE = "layout.listPane"
        const val KEY_SYNC_KEY = "sync.key"
        const val KEY_CLOSE_KEEPS_RUNNING = "window.closeKeepsRunning"
    }
}
