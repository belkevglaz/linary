package ru.b3lka.linary.core.html

import androidx.compose.ui.graphics.Color

/**
 * Небольшой разборщик HTML, которого хватает для записей Diarium:
 * p, div, br, hr, h1–h6, ul/ol/li, b/strong, i/em, u, s/strike/del, a, sup/sub, code,
 * span/font с цветом текста и фона.
 */
object HtmlParser {
    private const val GROUP_SLASH = 1
    private const val GROUP_TAG = 2
    private const val GROUP_ATTRS = 3

    private val token = Regex("<!--.*?-->|<(/?)([a-zA-Z][a-zA-Z0-9]*)([^>]*)>", RegexOption.DOT_MATCHES_ALL)

    /**
     * Цвет ссылок по умолчанию (если UI не передал свой).
     */
    val DefaultLinkColor = Color(0xFF1E88E5)

    fun parse(html: String, linkColor: Color = DefaultLinkColor): List<HtmlBlock> {
        val builder = HtmlDocumentBuilder(linkColor)
        var pos = 0
        for (m in token.findAll(html)) {
            builder.text(html.substring(pos, m.range.first))
            pos = m.range.last + 1
            if (m.value.startsWith("<!--")) continue
            val tag = m.groupValues[GROUP_TAG].lowercase()
            if (m.groupValues[GROUP_SLASH] == "/") {
                builder.closeTag(tag)
            } else {
                builder.openTag(tag, m.groupValues[GROUP_ATTRS])
            }
        }
        builder.text(html.substring(pos))
        return builder.finish()
    }

    fun toPlainText(html: String): String = parse(html).joinToString("\n") { it.text.text }.trim()
}
