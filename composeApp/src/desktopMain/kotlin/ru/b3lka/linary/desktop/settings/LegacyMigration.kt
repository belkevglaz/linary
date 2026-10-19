package ru.b3lka.linary.desktop.settings

import ru.b3lka.linary.desktop.APP_ID
import ru.b3lka.linary.desktop.data.BackupStore
import java.io.File
import java.util.prefs.Preferences

/**
 * Переход со старого имени `diarium-linux` (версии до 2.0.0): при первом запуске новой версии
 * настройки (`~/.java/.userPrefs/diarium-linux`) и резервные копии (`~/.local/share/diarium-linux`)
 * переносятся под новое имя. Один раз: если под новым именем уже что-то есть, ничего не трогается.
 */
internal object LegacyMigration {
    private const val OLD_ID = "diarium-linux"

    fun run() {
        runCatching { migratePreferences() }
        runCatching { migrateDataDir() }
    }

    private fun migratePreferences() {
        val root = Preferences.userRoot()
        if (!root.nodeExists(OLD_ID)) return
        val target = root.node(APP_ID)
        if (target.keys().isNotEmpty()) return
        val old = root.node(OLD_ID)
        old.keys().forEach { key -> old.get(key, null)?.let { target.put(key, it) } }
        target.flush()
        old.removeNode()
        root.flush()
    }

    private fun migrateDataDir() {
        val old = File(BackupStore.dataHome(), OLD_ID)
        val new = File(BackupStore.dataHome(), APP_ID)
        if (old.isDirectory && !new.exists()) old.renameTo(new)
    }
}
