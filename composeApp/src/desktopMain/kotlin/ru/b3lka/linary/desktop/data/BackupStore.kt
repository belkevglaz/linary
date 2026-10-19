package ru.b3lka.linary.desktop.data

import ru.b3lka.linary.desktop.APP_ID
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Резервные копии `v4_database` перед каждой записью.
 *
 * Лежат в `$XDG_DATA_HOME/linary/backups` (обычно `~/.local/share/linary/backups`),
 * то есть вне папки Dropbox. Хранятся последние [keep] копий.
 */
class BackupStore(
    private val dir: File = defaultDir(),
    private val keep: Int = DEFAULT_KEEP,
) {
    private val stampFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")

    /**
     * Копирует [file] и возвращает путь копии.
     */
    fun backup(file: File): File {
        dir.mkdirs()
        val target = File(dir, "${file.name}.${LocalDateTime.now().format(stampFormat)}")
        Files.copy(file.toPath(), target.toPath(), StandardCopyOption.COPY_ATTRIBUTES)
        prune(file.name)
        return target
    }

    private fun prune(prefix: String) {
        dir.listFiles { f -> f.name.startsWith("$prefix.") }
            .orEmpty()
            .sortedByDescending { it.name }
            .drop(keep)
            .forEach { it.delete() }
    }

    companion object {
        private const val DEFAULT_KEEP = 20

        fun defaultDir(): File = File(dataHome(), "$APP_ID/backups")

        /**
         * `$XDG_DATA_HOME`, по умолчанию `~/.local/share`.
         */
        fun dataHome(): File = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }?.let(::File)
            ?: File(System.getProperty("user.home"), ".local/share")
    }
}
