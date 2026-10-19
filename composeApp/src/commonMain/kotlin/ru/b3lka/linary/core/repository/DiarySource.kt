package ru.b3lka.linary.core.repository

/**
 * Описание источника данных для показа в интерфейсе.
 */
data class DiarySource(
    val kind: Kind,
    val path: String,
) {
    enum class Kind {
        SyncFolder,
        Backup,
    }
}
