package ru.b3lka.linary.core.html

import com.mohamedrejeb.ksoup.entities.KsoupEntities

/**
 * Декодирование HTML-сущностей: именованных (`&amp;`, `&pcy;`) и числовых (`&#1234;`, `&#x4D2;`).
 *
 * Редактор (compose-rich-editor) в `toHtml()` кодирует библиотекой ksoup-entities почти все не-ASCII
 * символы, включая кириллицу («п» → `&pcy;`). Декодируем той же библиотекой — так кодирование
 * и декодирование гарантированно совпадают, а в базу Diarium попадает обычный текст.
 */
internal object HtmlEntities {

    fun decode(s: String): String = if ('&' in s) KsoupEntities.decodeHtml(s) else s
}
