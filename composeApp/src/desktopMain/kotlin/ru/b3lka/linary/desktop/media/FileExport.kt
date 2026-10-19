package ru.b3lka.linary.desktop.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import ru.b3lka.linary.desktop.platform.NativeFileDialog
import java.awt.Component
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.JFileChooser
import javax.swing.JOptionPane

/**
 * Сохранение вложения в файл: системный диалог (или Swing), по умолчанию — папка «Изображения».
 */
internal object FileExport {
    private const val XDG_TIMEOUT_SECONDS = 2L

    private var lastDirectory: File? = null

    /**
     * Спрашивает, куда сохранить, и записывает [bytes]. false — пользователь отменил.
     */
    suspend fun save(parent: Component?, title: String, fileName: String, bytes: ByteArray): Boolean {
        val suggested = File(lastDirectory ?: picturesDirectory(), fileName)
        val native = NativeFileDialog.saveFile(title, suggested)
        val target = if (native != null) {
            native.firstOrNull()
        } else {
            withContext(Dispatchers.Swing) { saveWithSwing(parent, title, suggested) }
        }
        target ?: return false
        withContext(Dispatchers.IO) { target.writeBytes(bytes) }
        lastDirectory = target.parentFile
        return true
    }

    private fun saveWithSwing(parent: Component?, title: String, suggested: File): File? {
        val chooser = JFileChooser(suggested.parentFile).apply {
            dialogTitle = title
            selectedFile = suggested
        }
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return null
        val file = chooser.selectedFile ?: return null
        val overwrite = !file.exists() || JOptionPane.showConfirmDialog(
            parent,
            file.name,
            title,
            JOptionPane.OK_CANCEL_OPTION,
        ) == JOptionPane.OK_OPTION
        return file.takeIf { overwrite }
    }

    /**
     * Папка «Изображения» пользователя (`xdg-user-dir PICTURES`), иначе домашняя.
     */
    private fun picturesDirectory(): File {
        val home = File(System.getProperty("user.home"))
        val xdg = runCatching {
            val process = ProcessBuilder("xdg-user-dir", "PICTURES").redirectErrorStream(true).start()
            val path = process.inputStream.bufferedReader().readText().trim()
            process.waitFor(XDG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            File(path)
        }.getOrNull()
        return listOfNotNull(xdg, File(home, "Pictures")).firstOrNull { it.isDirectory && it != home } ?: home
    }
}
