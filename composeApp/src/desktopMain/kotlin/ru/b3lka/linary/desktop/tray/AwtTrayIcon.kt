package ru.b3lka.linary.desktop.tray

import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Запасной значок через AWT (`java.awt.SystemTray`, протокол XEmbed) — для рабочих столов без
 * StatusNotifierItem. Прозрачный фон в XEmbed часто становится чёрным, поэтому картинка своя, без прозрачности
 * (`resources/tray/tray-*.png`, см. `packaging/icons/draw_tray_icon.py`), уменьшенная до точного размера трея.
 */
internal class AwtTrayIcon private constructor(private val icon: TrayIcon) : AutoCloseable {

    override fun close() {
        runCatching { SystemTray.getSystemTray().remove(icon) }
    }

    companion object {
        private val SIZES = listOf(16, 22, 24, 32, 48, 64)

        val isSupported: Boolean get() = runCatching { SystemTray.isSupported() }.getOrDefault(false)

        /**
         * Показывает значок или возвращает null, если трей недоступен.
         */
        fun create(tooltip: String, menu: List<TrayMenuItem>, onActivate: () -> Unit): AwtTrayIcon? = runCatching {
            val tray = SystemTray.getSystemTray()
            val popup = PopupMenu().apply {
                menu.forEach { item ->
                    if (item.label == null) {
                        addSeparator()
                    } else {
                        add(MenuItem(item.label).apply { addActionListener { item.onClick() } })
                    }
                }
            }
            val icon = TrayIcon(trayImage(tray.trayIconSize.height), tooltip, popup).apply {
                isImageAutoSize = true
                addActionListener { onActivate() }
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (e.button == MouseEvent.BUTTON1) onActivate()
                    }
                })
            }
            tray.add(icon)
            AwtTrayIcon(icon)
        }.getOrNull()

        /**
         * Ближайшая не меньшая заготовка, уменьшенная до [size] с хорошим сглаживанием.
         */
        private fun trayImage(size: Int): Image {
            val source = SIZES.firstOrNull { it >= size } ?: SIZES.last()
            val image = TrayImages.read("tray-$source.png")
            if (image.width == size) return image
            return BufferedImage(size, size, BufferedImage.TYPE_INT_RGB).also { scaled ->
                val g = scaled.createGraphics()
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g.drawImage(image, 0, 0, size, size, null)
                g.dispose()
            }
        }
    }
}

/**
 * Картинки значка из ресурсов `tray/`.
 */
internal object TrayImages {
    private val APP_SIZES = listOf(16, 22, 24, 32, 48, 64, 128)

    fun read(name: String): BufferedImage {
        val stream = TrayImages::class.java.getResourceAsStream("/tray/$name") ?: error("Tray image $name is missing")
        return stream.use { ImageIO.read(it) }
    }

    /**
     * Значок с прозрачностью во всех размерах — для StatusNotifierItem.
     */
    fun appPixmaps(): List<IconPixmap> = APP_SIZES.map { size ->
        val image = read("tray-alpha-$size.png")
        IconPixmap(size, image.getRGB(0, 0, size, size, null, 0, size))
    }
}
