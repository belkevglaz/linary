package ru.b3lka.linary.core.model

/**
 * Новый тег или изменение существующего (название и цвет).
 *
 * @property id тег, который меняется; null — новый тег.
 * @property color цвет в формате Diarium `#AARRGGBB` (см. [TagColor]); null — без цвета.
 */
data class TagChange(
    val id: Long?,
    val value: String,
    val color: String?,
)

/**
 * Цвет тега в формате Diarium: строка `#AARRGGBB`, например `#FFE53935`.
 */
object TagColor {
    private const val RGB_MASK = 0xFFFFFF
    private const val RGB_DIGITS = 6
    private const val HEX = 16

    /**
     * Непрозрачный цвет из ARGB-числа (как у `Color.toArgb()`).
     */
    fun format(argb: Int): String = "#FF" + (argb and RGB_MASK).toString(HEX).uppercase().padStart(RGB_DIGITS, '0')
}
