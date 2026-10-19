package ru.b3lka.linary.desktop.media

import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage

/**
 * Ориентация из EXIF (тег 0x0112) у JPEG с камеры. ImageIO её не применяет, поэтому
 * без поворота снимки, сделанные «вертикально», легли бы на бок.
 */
internal object ExifOrientation {
    private const val NORMAL = 1
    private const val FLIP_HORIZONTAL = 2
    private const val ROTATE_180 = 3
    private const val FLIP_VERTICAL = 4
    private const val TRANSPOSE = 5
    private const val ROTATE_90 = 6
    private const val TRANSVERSE = 7
    private const val ROTATE_270 = 8
    private const val ORIENTATION_TAG = 0x0112
    private const val MARKER_SOI = 0xD8
    private const val MARKER_APP1 = 0xE1
    private const val MARKER_SOS = 0xDA
    private const val BYTE_MASK = 0xFF
    private const val SEGMENT_HEADER = 4
    private const val EXIF_ID_LENGTH = 4
    private const val TIFF_HEADER_OFFSET = 6
    private const val IFD_OFFSET_POSITION = 4
    private const val IFD_ENTRY_SIZE = 12
    private const val IFD_VALUE_POSITION = 8
    private const val BYTE_BITS = 8
    private const val SHORT_BITS = 16

    /**
     * Значение ориентации 1..8; 1 — без поворота (и если EXIF нет или он не читается).
     */
    fun read(jpeg: ByteArray): Int = runCatching { find(jpeg) }.getOrNull() ?: NORMAL

    fun apply(image: BufferedImage, orientation: Int): BufferedImage {
        if (orientation !in FLIP_HORIZONTAL..ROTATE_270) return image
        val w = image.width.toDouble()
        val h = image.height.toDouble()
        val t = AffineTransform()
        when (orientation) {
            FLIP_HORIZONTAL -> t.apply { translate(w, 0.0) }.scale(-1.0, 1.0)
            ROTATE_180 -> t.apply { translate(w, h) }.rotate(Math.PI)
            FLIP_VERTICAL -> t.apply { translate(0.0, h) }.scale(1.0, -1.0)
            TRANSPOSE -> t.apply { rotate(-Math.PI / 2) }.scale(-1.0, 1.0)
            ROTATE_90 -> t.apply { translate(h, 0.0) }.rotate(Math.PI / 2)
            TRANSVERSE -> t.apply { scale(-1.0, 1.0) }.apply { translate(-h, w) }.rotate(-Math.PI / 2)
            else -> t.apply { translate(0.0, w) }.rotate(-Math.PI / 2)
        }
        val swaps = orientation >= TRANSPOSE
        val out = BufferedImage(
            if (swaps) image.height else image.width,
            if (swaps) image.width else image.height,
            BufferedImage.TYPE_INT_ARGB,
        )
        val g = out.createGraphics()
        g.drawImage(image, AffineTransformOp(t, AffineTransformOp.TYPE_NEAREST_NEIGHBOR), 0, 0)
        g.dispose()
        return out
    }

    private fun find(d: ByteArray): Int? {
        if (d.u8(0) != BYTE_MASK || d.u8(1) != MARKER_SOI) return null
        var i = 2
        while (i + SEGMENT_HEADER <= d.size && d.u8(i) == BYTE_MASK) {
            val marker = d.u8(i + 1)
            val length = d.u16(i + 2, bigEndian = true)
            if (marker == MARKER_SOS) return null
            if (marker == MARKER_APP1 && String(d, i + SEGMENT_HEADER, EXIF_ID_LENGTH, Charsets.US_ASCII) == "Exif") {
                return orientationFromTiff(d, i + SEGMENT_HEADER + TIFF_HEADER_OFFSET)
            }
            i += 2 + length
        }
        return null
    }

    private fun orientationFromTiff(d: ByteArray, tiff: Int): Int? {
        val bigEndian = d.u8(tiff) == 'M'.code
        val ifd = tiff + d.u32(tiff + IFD_OFFSET_POSITION, bigEndian)
        val count = d.u16(ifd, bigEndian)
        for (n in 0 until count) {
            val entry = ifd + 2 + n * IFD_ENTRY_SIZE
            if (d.u16(entry, bigEndian) == ORIENTATION_TAG) return d.u16(entry + IFD_VALUE_POSITION, bigEndian)
        }
        return null
    }

    private fun ByteArray.u8(i: Int): Int = this[i].toInt() and BYTE_MASK

    private fun ByteArray.u16(i: Int, bigEndian: Boolean): Int =
        if (bigEndian) (u8(i) shl BYTE_BITS) or u8(i + 1) else (u8(i + 1) shl BYTE_BITS) or u8(i)

    private fun ByteArray.u32(i: Int, bigEndian: Boolean): Int =
        if (bigEndian) {
            (u16(i, true) shl SHORT_BITS) or u16(i + 2, true)
        } else {
            (u16(i + 2, false) shl SHORT_BITS) or u16(i, false)
        }
}
