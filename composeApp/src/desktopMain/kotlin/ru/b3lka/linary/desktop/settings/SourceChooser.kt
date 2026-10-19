package ru.b3lka.linary.desktop.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import ru.b3lka.linary.desktop.platform.NativeFileDialog
import java.awt.Component
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Выбор источника дневника: папки синхронизации или файла `.diary`.
 */
object SourceChooser {
    private const val DATABASE_FILE = "v4_database"
    private val defaultLocations = listOf("Dropbox/Apps/Diarium", "Dropbox/Приложения/Diarium")

    /**
     * `~/Dropbox/Apps/Diarium`, если такая папка есть.
     */
    fun defaultSource(): String? {
        val home = System.getProperty("user.home")
        return defaultLocations
            .map { File(home, it) }
            .firstOrNull { File(it, DATABASE_FILE).isFile }
            ?.absolutePath
    }

    /**
     * Системный диалог (если есть) или диалог Swing. Папку синхронизации выбирают через её файл
     * `v4_database`: системные диалоги не умеют выбирать «файл или папку» одновременно.
     */
    suspend fun choose(parent: Component, current: String?, title: String, filterName: String): String? {
        val start = current?.let(::File)?.let { if (it.isDirectory) it else it.parentFile }
            ?: File(System.getProperty("user.home"))
        val filter = NativeFileDialog.Filter(filterName, listOf(DATABASE_FILE, "*.diary"))
        val native = NativeFileDialog.openFiles(title, filter, multiple = false, start = start)
        val file = if (native != null) {
            native.firstOrNull()
        } else {
            withContext(Dispatchers.Swing) { chooseWithSwing(parent, start, title, filterName) }
        }
        return file?.let { if (it.name == DATABASE_FILE) it.parentFile else it }?.absolutePath
    }

    private fun chooseWithSwing(parent: Component, start: File, title: String, backupFilter: String): File? {
        val chooser = JFileChooser(start).apply {
            dialogTitle = title
            fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
            isAcceptAllFileFilterUsed = true
            addChoosableFileFilter(FileNameExtensionFilter(backupFilter, "diary"))
            isFileHidingEnabled = false
        }
        return if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }
}
