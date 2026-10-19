package ru.b3lka.linary.core.html

import kotlin.test.Test
import kotlin.test.assertEquals

class DiariumHtmlWriterTest {
    @Test
    fun diariumHtmlRoundTrips() {
        val html = "<p>a</p><p></p><p><strong>b</strong> <span style=\"color: #E61B1B\">c</span></p>" +
            "<ul><li>x</li><li>y</li></ul><p>z</p>"
        assertEquals(html, DiariumHtmlWriter.normalize(html))
    }

    @Test
    fun editorTagsAreConvertedToDiariumTags() {
        val html = "<p><b>bold</b> <i>it</i> <u>u</u> <s>s</s> " +
            "<span style=\"color: rgba(230, 27, 27, 1.0); background: rgba(255, 206, 96, 1.0)\">c</span></p>"
        assertEquals(
            "<p><strong>bold</strong> <em>it</em> <u>u</u> <s>s</s> " +
                "<span style=\"color: #E61B1B; background-color: #FFCE60\">c</span></p>",
            DiariumHtmlWriter.normalize(html),
        )
    }

    @Test
    fun nestedMarksAreClosedInOrder() {
        assertEquals(
            "<p><strong>a<em>b</em></strong><em>c</em></p>",
            DiariumHtmlWriter.normalize("<p><b>a<i>b</i></b><i>c</i></p>"),
        )
    }

    @Test
    fun linksKeepAddress() {
        assertEquals(
            "<p>see <a href=\"https://example.com/?a=1&amp;b=2\">site</a></p>",
            DiariumHtmlWriter.normalize("<p>see <a href=\"https://example.com/?a=1&amp;b=2\">site</a></p>"),
        )
    }

    @Test
    fun lineBreaksBecomeParagraphs() {
        assertEquals("<p>a</p><p>b</p><p></p>", DiariumHtmlWriter.normalize("<p>a<br>b</p><p><br></p>"))
    }

    @Test
    fun namedEntitiesFromEditorAreDecoded() {
        val html = "<p><b>&pcy;&rcy;&icy;&vcy;&iecy;&tcy;</b> &Pcy;&ocy;&kcy;&acy;</p>"
        assertEquals("<p><strong>привет</strong> Пока</p>", DiariumHtmlWriter.normalize(html))
    }

    @Test
    fun textIsEscaped() {
        assertEquals("<p>1 &lt; 2 &amp; 3</p>", DiariumHtmlWriter.normalize("<p>1 &lt; 2 &amp; 3</p>"))
    }
}
