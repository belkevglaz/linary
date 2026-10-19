package ru.b3lka.linary.core.html

import androidx.compose.ui.text.AnnotatedString

/**
 * Блок текста записи. Diarium хранит каждую строку отдельным `<p>`, пустая строка — это `<p></p>`.
 */
sealed interface HtmlBlock {
    val text: AnnotatedString

    companion object {
        /**
         * Тег строковой аннотации с адресом ссылки (`<a href>`).
         */
        const val URL_ANNOTATION = "url"
    }

    data class Paragraph(
        override val text: AnnotatedString,
    ) : HtmlBlock

    data class Heading(
        override val text: AnnotatedString,
        val level: Int,
    ) : HtmlBlock

    data class ListItem(
        override val text: AnnotatedString,
        val marker: String,
        val level: Int,
    ) : HtmlBlock
}
