package ru.b3lka.linary.desktop.media

import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import ru.b3lka.linary.core.model.NewMedia
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam

/**
 * Подготовка картинки к сохранению так же, как это делает телефон: JPEG, большая сторона не больше
 * [MAX_SIZE] пикселей, поворот по EXIF применяется к пикселям.
 */
internal object ImagePreparer {
    private const val MAX_SIZE = 1600
    private const val JPEG_QUALITY = 0.85f

    fun fromFile(file: File): NewMedia? {
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        val image = decode(bytes) ?: return null
        return toMedia(ExifOrientation.apply(image, ExifOrientation.read(bytes)), file.nameWithoutExtension)
    }

    fun fromImage(image: java.awt.Image, name: String): NewMedia? {
        val width = image.getWidth(null)
        val height = image.getHeight(null)
        if (width <= 0 || height <= 0) return null
        val buffered = image as? BufferedImage ?: BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also {
            val g = it.createGraphics()
            g.drawImage(image, 0, 0, null)
            g.dispose()
        }
        return toMedia(buffered, name)
    }

    private fun decode(bytes: ByteArray): BufferedImage? =
        runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull()
            // WebP и другие форматы, которых нет в ImageIO, умеет Skia
            ?: runCatching { Image.makeFromEncoded(bytes).toComposeImageBitmap().toAwtImage() }.getOrNull()

    private fun toMedia(image: BufferedImage, name: String): NewMedia =
        NewMedia(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "image" },
            ext = "jpg",
            bytes = encodeJpeg(scaleToRgb(image)),
        )

    /**
     * Уменьшает картинку и убирает прозрачность (JPEG её не поддерживает): фон белый.
     */
    private fun scaleToRgb(source: BufferedImage): BufferedImage {
        val k = minOf(1.0, MAX_SIZE.toDouble() / maxOf(source.width, source.height))
        val width = (source.width * k).toInt().coerceAtLeast(1)
        val height = (source.height * k).toInt().coerceAtLeast(1)
        val out = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, width, height)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g.drawImage(source, AffineTransform.getScaleInstance(k, k), null)
        g.dispose()
        return out
    }

    private fun encodeJpeg(image: BufferedImage): ByteArray {
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        val out = ByteArrayOutputStream()
        ImageIO.createImageOutputStream(out).use { stream ->
            writer.output = stream
            val param = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = JPEG_QUALITY
            }
            writer.write(null, IIOImage(image, null, null), param)
            writer.dispose()
        }
        return out.toByteArray()
    }
}
