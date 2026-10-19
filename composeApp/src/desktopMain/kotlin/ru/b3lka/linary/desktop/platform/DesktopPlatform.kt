package ru.b3lka.linary.desktop.platform

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.desktop.data.PrivateTemp
import ru.b3lka.linary.desktop.media.FileExport
import ru.b3lka.linary.desktop.media.ImageInput
import ru.b3lka.linary.presentation.platform.ImageSource
import ru.b3lka.linary.presentation.platform.PlatformServices
import java.awt.Component
import java.awt.Desktop
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URI
import javax.imageio.ImageIO

/**
 * Реализация платформенных сервисов для JVM-десктопа.
 */
class DesktopPlatform : PlatformServices {

    /**
     * Окно приложения — родитель для диалогов.
     */
    var owner: Component? = null

    /**
     * Лаунчер jpackage передаёт версию пакета в системном свойстве.
     */
    override val appVersion: String? = System.getProperty("jpackage.app-version")

    override fun decodeImage(bytes: ByteArray, maxSize: Int?): ImageBitmap? {
        if (maxSize == null) return decodeWithSkia(bytes) ?: decodeWithImageIo(bytes)?.toComposeImageBitmap()
        val source = decodeWithImageIo(bytes) ?: return decodeWithSkia(bytes)
        return scale(source, maxSize).toComposeImageBitmap()
    }

    override fun openExternally(media: DiaryMedia, bytes: ByteArray) {
        val file = File(PrivateTemp.filesDir, "${media.id}.${media.ext}")
        if (!file.exists() || file.length() != bytes.size.toLong()) file.writeBytes(bytes)
        val opened = isLinux() && runCatching { ProcessBuilder("xdg-open", file.absolutePath).start() }.isSuccess
        if (!opened && Desktop.isDesktopSupported()) Desktop.getDesktop().open(file)
    }

    override fun openUrl(url: String) {
        val opened = isLinux() && runCatching { ProcessBuilder("xdg-open", url).start() }.isSuccess
        if (!opened && Desktop.isDesktopSupported()) runCatching { Desktop.getDesktop().browse(URI(url)) }
    }

    override suspend fun chooseImages(title: String, filterName: String): List<ImageSource> =
        ImageInput.choose(owner, title, filterName)

    override fun clipboardImages(): List<ImageSource> = ImageInput.fromClipboard()

    override suspend fun saveFile(title: String, fileName: String, bytes: ByteArray): Boolean =
        FileExport.save(owner, title, fileName, bytes)

    @OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
    override fun imageDropTarget(onActive: (Boolean) -> Unit, onDrop: (List<ImageSource>) -> Unit): Modifier =
        Modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { true },
            target = object : DragAndDropTarget {
                override fun onEntered(event: DragAndDropEvent) = onActive(true)

                override fun onExited(event: DragAndDropEvent) = onActive(false)

                override fun onEnded(event: DragAndDropEvent) = onActive(false)

                override fun onDrop(event: DragAndDropEvent): Boolean {
                    onActive(false)
                    val images = ImageInput.fromTransferable(event.awtTransferable)
                    if (images.isNotEmpty()) onDrop(images)
                    return images.isNotEmpty()
                }
            },
        )

    private fun decodeWithSkia(bytes: ByteArray): ImageBitmap? =
        runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap() }.getOrNull()

    private fun decodeWithImageIo(bytes: ByteArray): BufferedImage? =
        runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull()

    private fun scale(source: BufferedImage, maxSize: Int): BufferedImage {
        val longest = maxOf(source.width, source.height)
        if (longest <= maxSize) return source
        val k = maxSize.toDouble() / longest
        val width = (source.width * k).toInt().coerceAtLeast(1)
        val height = (source.height * k).toInt().coerceAtLeast(1)
        val out = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(source, 0, 0, width, height, null)
        g.dispose()
        return out
    }

    private fun isLinux(): Boolean = System.getProperty("os.name").lowercase().contains("linux")
}
