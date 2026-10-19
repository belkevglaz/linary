package ru.b3lka.linary.core.html

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import kotlin.math.roundToInt

/**
 * Пишет текст записи в том же HTML, что и редактор Diarium (`MyHtml.ToHtml`):
 * `<p>`, `<ul>/<ol>/<li>`, `<strong>`, `<em>`, `<u>`, `<s>`,
 * `<span style="color: #RRGGBB; background-color: #RRGGBB">` и `<a href>`.
 *
 * Каждая строка — отдельный `<p>`, пустая строка — `<p></p>`.
 */
object DiariumHtmlWriter {
    private const val BOLD_WEIGHT = 600
    private const val COLOR_MAX = 255
    private const val HEX_RADIX = 16

    /**
     * Переводит HTML из любого источника (например, из редактора) в формат Diarium.
     */
    fun normalize(html: String): String = write(HtmlParser.parse(html, linkColor = Color.Unspecified))

    fun write(blocks: List<HtmlBlock>): String = buildString {
        var openList: String? = null
        for (block in blocks) {
            val listTag = (block as? HtmlBlock.ListItem)?.let { if (it.marker == "•") "ul" else "ol" }
            if (openList != null && openList != listTag) append("</$openList>")
            if (listTag != null && openList != listTag) append("<$listTag>")
            openList = listTag
            if (listTag != null) {
                append("<li>").append(inline(block.text)).append("</li>")
            } else {
                lines(block.text).forEach { append("<p>").append(inline(it)).append("</p>") }
            }
        }
        if (openList != null) append("</$openList>")
    }

    /**
     * Абзац с переводами строк (`<br>`) становится несколькими абзацами, как у Diarium.
     */
    private fun lines(text: AnnotatedString): List<AnnotatedString> {
        val parts = mutableListOf<AnnotatedString>()
        var start = 0
        while (true) {
            val end = text.text.indexOf('\n', start)
            if (end < 0) break
            parts += text.subSequence(start, end)
            start = end + 1
        }
        // Завершающий перевод строки (<p>текст<br></p>) не порождает лишний пустой абзац
        if (start < text.length || parts.isEmpty()) parts += text.subSequence(start, text.length)
        return parts
    }

    private fun inline(text: AnnotatedString): String {
        val out = StringBuilder()
        val open = mutableListOf<Mark>()
        val bounds = boundaries(text)
        for (i in 0 until bounds.size - 1) {
            val from = bounds[i]
            val to = bounds[i + 1]
            if (from == to) continue
            val wanted = marksAt(text, from)
            val common = open.zip(wanted).takeWhile { (a, b) -> a == b }.size
            for (j in open.lastIndex downTo common) out.append(open.removeAt(j).close)
            for (mark in wanted.drop(common)) {
                out.append(mark.open)
                open += mark
            }
            out.append(escape(text.text.substring(from, to)))
        }
        for (j in open.lastIndex downTo 0) out.append(open[j].close)
        return out.toString()
    }

    private fun boundaries(text: AnnotatedString): List<Int> {
        val points = mutableSetOf(0, text.length)
        text.spanStyles.forEach {
            points += it.start
            points += it.end
        }
        text.getStringAnnotations(HtmlBlock.URL_ANNOTATION, 0, text.length).forEach {
            points += it.start
            points += it.end
        }
        return points.filter { it in 0..text.length }.sorted()
    }

    /**
     * Метки символа в каноническом порядке вложенности: ссылка, цвет, жирный, курсив, подчёркнутый, зачёркнутый.
     */
    private fun marksAt(text: AnnotatedString, index: Int): List<Mark> {
        val styles = text.spanStyles.filter { index >= it.start && index < it.end }.map { it.item }
        val color = styles.lastOrNull { it.color.isVisible() }?.color
        val background = styles.lastOrNull { it.background.isVisible() }?.background
        val decorations = styles.mapNotNull { it.textDecoration }
        val href = text.getStringAnnotations(HtmlBlock.URL_ANNOTATION, index, index + 1).lastOrNull()?.item
        return buildList {
            href?.let { add(Mark.Link(it)) }
            if (color != null || background != null) add(Mark.Colors(color?.toHex(), background?.toHex()))
            if (styles.any { (it.fontWeight?.weight ?: 0) >= BOLD_WEIGHT }) add(Mark.Simple("strong"))
            if (styles.any { it.fontStyle == FontStyle.Italic }) add(Mark.Simple("em"))
            if (decorations.any { TextDecoration.Underline in it }) add(Mark.Simple("u"))
            if (decorations.any { TextDecoration.LineThrough in it }) add(Mark.Simple("s"))
        }
    }

    private sealed interface Mark {
        val open: String
        val close: String

        data class Simple(val tag: String) : Mark {
            override val open get() = "<$tag>"
            override val close get() = "</$tag>"
        }

        data class Colors(val color: String?, val background: String?) : Mark {
            override val open: String
                get() {
                    val css = listOfNotNull(color?.let { "color: $it" }, background?.let { "background-color: $it" })
                    return "<span style=\"${css.joinToString("; ")}\">"
                }
            override val close get() = "</span>"
        }

        data class Link(val href: String) : Mark {
            override val open get() = "<a href=\"${escape(href).replace("\"", "&quot;")}\">"
            override val close get() = "</a>"
        }
    }

    private fun Color.isVisible(): Boolean = this != Color.Unspecified && alpha > 0f

    private fun Color.toHex(): String =
        "#" + listOf(red, green, blue).joinToString("") { channel ->
            (channel * COLOR_MAX).roundToInt().toString(HEX_RADIX).padStart(2, '0').uppercase()
        }

    private fun escape(s: String): String = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
