package ru.b3lka.linary.core.html

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration

/**
 * Стили текста для inline-тегов и простого CSS из атрибута `style`.
 */
object CssStyles {
    private const val BOLD_WEIGHT_THRESHOLD = 600
    private const val NORMAL_WEIGHT = 400
    private const val OPAQUE = 0xFF000000
    private const val HEX_RADIX = 16
    private const val SHORT_HEX_LENGTH = 3
    private const val RGB_HEX_LENGTH = 6
    private const val ARGB_HEX_LENGTH = 8

    private val styleAttr = Regex("style\\s*=\\s*([\"'])(.*?)\\1", RegexOption.IGNORE_CASE)
    private val colorAttr = Regex("color\\s*=\\s*([\"'])(.*?)\\1", RegexOption.IGNORE_CASE)
    private val rgbFunction = Regex("rgba?\\((\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)")

    private val namedColors = mapOf(
        "black" to Color.Black,
        "white" to Color.White,
        "red" to Color.Red,
        "green" to Color(0xFF008000),
        "blue" to Color.Blue,
        "yellow" to Color.Yellow,
        "gray" to Color.Gray,
        "grey" to Color.Gray,
    )

    internal fun styleFor(tag: String, attrs: String, linkColor: Color): SpanStyle? = when (tag) {
        "b", "strong" -> SpanStyle(fontWeight = FontWeight.Bold)
        "i", "em" -> SpanStyle(fontStyle = FontStyle.Italic)
        "u", "ins" -> SpanStyle(textDecoration = TextDecoration.Underline)
        "s", "strike", "del" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
        "a" -> linkColor.takeIf { it != Color.Unspecified }?.let {
            SpanStyle(color = it, textDecoration = TextDecoration.Underline)
        }
        "sup" -> SpanStyle(baselineShift = BaselineShift.Superscript)
        "sub" -> SpanStyle(baselineShift = BaselineShift.Subscript)
        "code", "tt" -> SpanStyle(fontFamily = FontFamily.Monospace)
        else -> fromAttributes(attrs)
    }

    /**
     * Разбирает цвет в форматах `#rgb`, `#rrggbb`, `#aarrggbb`, `rgb(r, g, b)` и простые имена.
     */
    fun parseColor(value: String): Color? {
        val v = value.trim().lowercase()
        return when {
            v.startsWith("#") -> parseHex(v.substring(1))
            rgbFunction.containsMatchIn(v) -> rgbFunction.find(v)?.destructured?.let { (r, g, b) ->
                Color(r.toInt(), g.toInt(), b.toInt())
            }
            else -> namedColors[v]
        }
    }

    private fun parseHex(hex: String): Color? {
        val full = when (hex.length) {
            SHORT_HEX_LENGTH -> hex.map { "$it$it" }.joinToString("")
            RGB_HEX_LENGTH -> hex
            ARGB_HEX_LENGTH -> hex.substring(2)
            else -> return null
        }
        return full.toLongOrNull(HEX_RADIX)?.let { Color(OPAQUE or it) }
    }

    private fun fromAttributes(attrs: String): SpanStyle? {
        val css = CssDeclarations()
        styleAttr.find(attrs)?.groupValues?.get(2)?.split(';')?.forEach { css.apply(it) }
        if (css.color == null) colorAttr.find(attrs)?.let { css.color = parseColor(it.groupValues[2]) }
        return css.toSpanStyle()
    }

    private class CssDeclarations {
        var color: Color? = null
        var background: Color? = null
        var bold = false
        var italic = false
        var decoration: TextDecoration? = null

        fun apply(declaration: String) {
            val parts = declaration.split(':', limit = 2)
            if (parts.size != 2) return
            val value = parts[1].trim().lowercase()
            when (parts[0].trim().lowercase()) {
                "color" -> color = parseColor(value)
                "background-color", "background" -> background = parseColor(value)
                "font-weight" -> bold = isBold(value)
                "font-style" -> italic = value == "italic"
                "text-decoration", "text-decoration-line" -> decoration = decorationOf(value)
            }
        }

        fun toSpanStyle(): SpanStyle? {
            if (isEmpty()) return null
            return SpanStyle(
                color = color ?: Color.Unspecified,
                background = background ?: Color.Unspecified,
                fontWeight = if (bold) FontWeight.Bold else null,
                fontStyle = if (italic) FontStyle.Italic else null,
                textDecoration = decoration,
            )
        }

        private fun isEmpty(): Boolean {
            val hasColors = color != null || background != null
            val hasFont = bold || italic || decoration != null
            return !hasColors && !hasFont
        }

        private fun isBold(value: String): Boolean =
            value == "bold" || (value.toIntOrNull() ?: NORMAL_WEIGHT) >= BOLD_WEIGHT_THRESHOLD

        private fun decorationOf(value: String): TextDecoration? = when {
            "underline" in value -> TextDecoration.Underline
            "line-through" in value -> TextDecoration.LineThrough
            else -> null
        }
    }
}
