package ru.b3lka.linary.desktop.media

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals

class ExifOrientationTest {

    /**
     * Минимальный JPEG-заголовок: SOI, APP1 с EXIF (один тег Orientation), SOS.
     */
    private fun jpegWithOrientation(orientation: Int, bigEndian: Boolean): ByteArray {
        fun u16(v: Int) = if (bigEndian) listOf(v shr 8, v and 0xFF) else listOf(v and 0xFF, v shr 8)
        fun u32(v: Int) = if (bigEndian) u16(v shr 16) + u16(v and 0xFFFF) else u16(v and 0xFFFF) + u16(v shr 16)
        val tiff = (if (bigEndian) listOf('M'.code, 'M'.code) else listOf('I'.code, 'I'.code)) +
            u16(42) + u32(8) + // заголовок TIFF, IFD сразу за ним
            u16(1) + // один тег
            u16(0x0112) + u16(3) + u32(1) + u16(orientation) + u16(0) +
            u32(0)
        val exif = "Exif".map { it.code } + listOf(0, 0) + tiff
        val app1 = listOf(0xFF, 0xE1, (exif.size + 2) shr 8, (exif.size + 2) and 0xFF) + exif
        val bytes = listOf(0xFF, 0xD8) + app1 + listOf(0xFF, 0xDA, 0, 2)
        return bytes.map { it.toByte() }.toByteArray()
    }

    @Test
    fun readsOrientationInBothByteOrders() {
        assertEquals(6, ExifOrientation.read(jpegWithOrientation(6, bigEndian = true)))
        assertEquals(8, ExifOrientation.read(jpegWithOrientation(8, bigEndian = false)))
    }

    @Test
    fun noExifMeansNormal() {
        assertEquals(1, ExifOrientation.read(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xDA.toByte())))
        assertEquals(1, ExifOrientation.read("not a jpeg".toByteArray()))
    }

    @Test
    fun rotationMovesPixelsAndSwapsSize() {
        // 3×2, красный пиксель в левом верхнем углу
        val image = BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB)
        image.setRGB(0, 0, RED)

        val cw = ExifOrientation.apply(image, 6)
        assertEquals(2 to 3, cw.width to cw.height)
        assertEquals(RED, cw.getRGB(1, 0)) // по часовой: левый верхний → правый верхний

        val ccw = ExifOrientation.apply(image, 8)
        assertEquals(RED, ccw.getRGB(0, 2)) // против часовой: → левый нижний

        val flipped = ExifOrientation.apply(image, 3)
        assertEquals(3 to 2, flipped.width to flipped.height)
        assertEquals(RED, flipped.getRGB(2, 1))

        assertEquals(image, ExifOrientation.apply(image, 1))
    }

    private companion object {
        const val RED = 0xFFFF0000.toInt()
    }
}
