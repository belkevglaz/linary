package ru.b3lka.linary.core.html

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/**
 * Собирает блоки [HtmlBlock] из потока тегов и текста, который выдаёт [HtmlParser].
 */
internal class HtmlDocumentBuilder(private val linkColor: Color) {

    private enum class Kind { Paragraph, Heading, ListItem }

    private class ListState(val ordered: Boolean) {
        var counter = 0
    }

    private class InlineTag(val tag: String, val style: SpanStyle?, val href: String?)

    private val blocks = mutableListOf<HtmlBlock>()
    private val inlineStyles = mutableListOf<InlineTag>()
    private val lists = mutableListOf<ListState>()

    /**
     * Цвет и подчёркивание ссылки задаются стилем тега `<a>`; при наведении подчёркивание остаётся.
     */
    private val linkStyles = TextLinkStyles(hoveredStyle = SpanStyle(textDecoration = TextDecoration.Underline))

    private var builder = AnnotatedString.Builder()
    private var kind = Kind.Paragraph
    private var headingLevel = 1
    private var marker = BULLET
    private var listLevel = 0
    private var explicitBlock = false

    fun text(raw: String) {
        if (raw.isEmpty()) return
        val collapsed = raw.replace(whitespace, " ")
        // Пробелы и переводы строк между блоками — это не текст
        if (collapsed.isBlank() && (!explicitBlock || builder.length == 0)) return
        val decoded = HtmlEntities.decode(collapsed)
        val start = builder.length
        val style = currentStyle()
        if (style != null) builder.withStyle(style) { append(decoded) } else builder.append(decoded)
        inlineStyles.lastOrNull { it.href != null }?.href?.let {
            // Строковая аннотация — для записи HTML обратно, ссылка — чтобы по ней можно было кликнуть
            builder.addStringAnnotation(HtmlBlock.URL_ANNOTATION, it, start, builder.length)
            builder.addLink(LinkAnnotation.Url(it, linkStyles), start, builder.length)
        }
    }

    fun openTag(tag: String, attrs: String) {
        when {
            tag == "br" -> builder.append("\n")
            tag == "hr" -> horizontalRule()
            tag == "ul" || tag == "ol" -> openList(ordered = tag == "ol")
            tag in blockTags -> openBlock(tag)
            !attrs.trimEnd().endsWith("/") -> inlineStyles.add(
                InlineTag(tag, CssStyles.styleFor(tag, attrs, linkColor), if (tag == "a") hrefOf(attrs) else null),
            )
        }
    }

    fun closeTag(tag: String) {
        when {
            tag == "ul" || tag == "ol" -> lists.removeLastOrNull()
            tag in blockTags -> if (explicitBlock || builder.length > 0) emit()
            else -> {
                val i = inlineStyles.indexOfLast { it.tag == tag }
                if (i >= 0) inlineStyles.removeAt(i)
            }
        }
    }

    fun finish(): List<HtmlBlock> {
        if (builder.length > 0 || explicitBlock) emit()
        return blocks.toList()
    }

    private fun horizontalRule() {
        if (builder.length > 0 || explicitBlock) emit()
        builder.append(HORIZONTAL_RULE)
        emit()
    }

    private fun openList(ordered: Boolean) {
        if (builder.length > 0) emit()
        lists.add(ListState(ordered))
    }

    private fun openBlock(tag: String) {
        if (builder.length > 0) emit()
        explicitBlock = true
        when {
            tag == "li" -> {
                kind = Kind.ListItem
                listLevel = (lists.size - 1).coerceAtLeast(0)
                val list = lists.lastOrNull()
                marker = if (list != null && list.ordered) "${++list.counter}." else BULLET
            }
            tag.length == 2 && tag[0] == 'h' -> {
                kind = Kind.Heading
                headingLevel = tag[1].digitToInt()
            }
            else -> kind = Kind.Paragraph
        }
    }

    private fun hrefOf(attrs: String): String? =
        hrefAttr.find(attrs)?.groupValues?.get(2)?.let { HtmlEntities.decode(it) }

    private fun currentStyle(): SpanStyle? =
        inlineStyles.mapNotNull { it.style }.fold(null as SpanStyle?) { acc, s -> acc?.merge(s) ?: s }

    private fun emit() {
        val text = builder.toAnnotatedString()
        blocks += when (kind) {
            Kind.Paragraph -> HtmlBlock.Paragraph(text)
            Kind.Heading -> HtmlBlock.Heading(text, headingLevel)
            Kind.ListItem -> HtmlBlock.ListItem(text, marker, listLevel)
        }
        builder = AnnotatedString.Builder()
        kind = Kind.Paragraph
        explicitBlock = false
    }

    private companion object {
        const val BULLET = "•"
        const val HORIZONTAL_RULE = "———"
        val whitespace = Regex("[ \\t\\r\\n\\f]+")
        val hrefAttr = Regex("href\\s*=\\s*([\"'])(.*?)\\1", RegexOption.IGNORE_CASE)
        val blockTags = setOf("p", "div", "li", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote")
    }
}
