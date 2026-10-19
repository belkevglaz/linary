package ru.b3lka.linary.core.html

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HtmlParserTest {
    @Test
    fun paragraphsAndEmptyLines() {
        val blocks = HtmlParser.parse("<p>Первая</p><p></p><p>Вторая &amp; третья</p>")
        assertEquals(listOf("Первая", "", "Вторая & третья"), blocks.map { it.text.text })
    }

    @Test
    fun whitespaceBetweenBlocksIsIgnored() {
        assertEquals(2, HtmlParser.parse("<p>a</p>\n  <p>b</p>").size)
    }

    @Test
    fun inlineStyles() {
        val block = HtmlParser.parse("<p>a <strong>b</strong> <span style=\"color: #E61B1B\">c</span></p>").single()
        assertEquals("a b c", block.text.text)
        assertTrue(block.text.spanStyles.any { it.item.color == Color(0xFFE61B1B) })
    }

    @Test
    fun lists() {
        val blocks = HtmlParser.parse("<ul><li>x</li><li>y</li></ul><ol><li>z</li></ol>")
        assertEquals(3, blocks.size)
        assertEquals("•", assertIs<HtmlBlock.ListItem>(blocks[0]).marker)
        assertEquals("1.", assertIs<HtmlBlock.ListItem>(blocks[2]).marker)
    }

    @Test
    fun plainText() {
        assertEquals("a\n\nb", HtmlParser.toPlainText("<p>a</p><p></p><p>b</p>"))
    }

    @Test
    fun colors() {
        assertEquals(Color(0xFFAABBCC), CssStyles.parseColor("#abc"))
        assertEquals(Color(10, 20, 30), CssStyles.parseColor("rgb(10, 20, 30)"))
        assertEquals(null, CssStyles.parseColor("nonsense"))
    }
}
