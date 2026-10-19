package ru.b3lka.linary.presentation.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Хранилище ключа синхронизации.
 */
interface SyncKeyPreferences {
    var syncKey: String?
}

/**
 * Ключ, которым зашифрована папка синхронизации (режим без E2EE). В программу он не входит —
 * пользователь задаёт его сам, один раз; без ключа папку синхронизации не открыть.
 */
class SyncKeySettings(private val prefs: SyncKeyPreferences) {
    var isSet by mutableStateOf(prefs.syncKey?.let { isValid(it) } == true)
        private set

    /**
     * Сохранённый ключ, если он годится по форме.
     */
    val key: String? get() = prefs.syncKey?.takeIf { isValid(it) }

    /**
     * Сохраняет ключ. false — ключ не годится по форме (см. [isValid]); подходит ли он к папке,
     * покажет загрузка дневника.
     */
    fun save(key: String): Boolean {
        val trimmed = key.trim()
        if (!isValid(trimmed)) return false
        prefs.syncKey = trimmed
        isSet = true
        return true
    }

    fun clear() {
        prefs.syncKey = null
        isSet = false
    }

    companion object {
        const val LENGTH = 32
        private const val PRINTABLE_FIRST = ' '
        private const val PRINTABLE_LAST = '~'

        /**
         * 32 печатных символа ASCII — так выглядит ключ AES-256 этого формата.
         */
        fun isValid(key: String): Boolean = key.length == LENGTH && key.all { it in PRINTABLE_FIRST..PRINTABLE_LAST }
    }
}
