package ru.b3lka.linary.desktop.settings

import java.awt.GraphicsEnvironment
import java.awt.Rectangle

/**
 * Положение и размер окна в обычном (не развёрнутом) состоянии, в единицах AWT (= dp окна Compose).
 */
data class WindowBounds(val x: Int, val y: Int, val width: Int, val height: Int) {

    /**
     * Видно ли окно хотя бы частично на одном из подключённых мониторов: после отключения
     * второго монитора сохранённое положение может оказаться за пределами экрана.
     */
    fun isOnScreen(): Boolean = runCatching {
        val window = Rectangle(x, y, width, height)
        GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.any { device ->
            val visible = window.intersection(device.defaultConfiguration.bounds)
            !visible.isEmpty && visible.width >= MIN_VISIBLE && visible.height >= MIN_VISIBLE
        }
    }.getOrDefault(false)

    fun encode(): String = "$x,$y,$width,$height"

    companion object {
        /**
         * Наименьший размер окна (единицы AWT).
         */
        const val MIN_WIDTH = 720
        const val MIN_HEIGHT = 480
        private const val MIN_VISIBLE = 100
        private const val PARTS = 4

        fun decode(value: String?): WindowBounds? {
            val parts = value?.split(',')?.mapNotNull { it.trim().toIntOrNull() } ?: return null
            if (parts.size != PARTS) return null
            val bounds = WindowBounds(x = parts[0], y = parts[1], width = parts[2], height = parts[PARTS - 1])
            return bounds.takeIf { it.width >= MIN_WIDTH && it.height >= MIN_HEIGHT }
        }
    }
}
