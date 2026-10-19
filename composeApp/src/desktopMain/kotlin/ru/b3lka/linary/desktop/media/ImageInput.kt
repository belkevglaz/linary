package ru.b3lka.linary.desktop.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import ru.b3lka.linary.desktop.platform.NativeFileDialog
import ru.b3lka.linary.presentation.platform.ImageSource
import java.awt.Component
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.io.File
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Откуда берутся новые картинки: диалог выбора файлов и буфер обмена.
 */
internal object ImageInput {
    private val extensions = arrayOf("jpg", "jpeg", "png", "gif", "bmp", "webp")

    /**
     * Список файлов в формате `text/uri-list`: так копируют файлы файловые менеджеры Linux.
     */
    private val uriListFlavor = DataFlavor("text/uri-list;class=java.lang.String")

    /**
     * Имя для скриншота — как у скриншотов Android, которые Diarium сохраняет с исходным именем.
     */
    private val screenshotName = DateTimeFormatter.ofPattern("'Screenshot_'yyyy-MM-dd-HH-mm-ss")

    private var lastDirectory: File? = null

    /**
     * Системный диалог выбора картинок, а если его нет — диалог Swing.
     */
    suspend fun choose(parent: Component?, title: String, filterName: String): List<ImageSource> {
        val filter = NativeFileDialog.Filter(filterName, extensions.map { "*.$it" })
        val files = NativeFileDialog.openFiles(title, filter, multiple = true, start = lastDirectory)
            ?: withContext(Dispatchers.Swing) { chooseWithSwing(parent, title, filterName) }
        files.firstOrNull()?.parentFile?.let { lastDirectory = it }
        return files.map { file -> ImageSource { ImagePreparer.fromFile(file) } }
    }

    @Suppress("SpreadOperator") // шесть расширений, копия массива ничего не стоит
    private fun chooseWithSwing(parent: Component?, title: String, filterName: String): List<File> {
        val chooser = JFileChooser(lastDirectory ?: File(System.getProperty("user.home"))).apply {
            dialogTitle = title
            isMultiSelectionEnabled = true
            fileFilter = FileNameExtensionFilter(filterName, *extensions)
        }
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) return emptyList()
        return chooser.selectedFiles.toList()
    }

    fun fromClipboard(): List<ImageSource> {
        val clipboard = runCatching { Toolkit.getDefaultToolkit().systemClipboard }.getOrNull() ?: return emptyList()
        val contents = runCatching { clipboard.getContents(null) }.getOrNull() ?: return emptyList()
        return fromTransferable(contents)
    }

    /**
     * Картинки из буфера обмена или перетаскивания: файлы-картинки или сама картинка (скриншот).
     */
    fun fromTransferable(data: Transferable): List<ImageSource> {
        val files = files(data).filter { it.extension.lowercase() in extensions }
        if (files.isNotEmpty()) return files.map { file -> ImageSource { ImagePreparer.fromFile(file) } }
        val image = read<Image>(data, DataFlavor.imageFlavor) ?: return emptyList()
        val name = LocalDateTime.now().format(screenshotName)
        return listOf(ImageSource { ImagePreparer.fromImage(image, name) })
    }

    private fun files(data: Transferable): List<File> {
        read<List<*>>(data, DataFlavor.javaFileListFlavor)?.let { list ->
            return list.filterIsInstance<File>()
        }
        val uris = read<String>(data, uriListFlavor) ?: return emptyList()
        return uris.lines()
            .map { it.trim() }
            .filter { it.startsWith("file:") }
            .mapNotNull { runCatching { File(URI(it)) }.getOrNull() }
    }

    private inline fun <reified T> read(data: Transferable, flavor: DataFlavor): T? = runCatching {
        if (data.isDataFlavorSupported(flavor)) data.getTransferData(flavor) as? T else null
    }.getOrNull()
}
