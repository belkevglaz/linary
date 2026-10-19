package ru.b3lka.linary.ui.entry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.b3lka.linary.core.html.HtmlBlock
import ru.b3lka.linary.core.html.HtmlParser

/**
 * Текст записи из HTML: абзацы, заголовки, списки, стили. Текст можно выделять и копировать.
 */
@Composable
fun EntryText(html: String) {
    val linkColor = MaterialTheme.colorScheme.primary
    val blocks = remember(html, linkColor) { HtmlParser.parse(html, linkColor) }
    val body = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp)
    SelectionContainer {
        Column {
            blocks.forEach { block -> Block(block, body) }
        }
    }
}

@Composable
private fun Block(block: HtmlBlock, body: TextStyle) {
    when (block) {
        is HtmlBlock.Paragraph -> Text(block.text.ifEmpty { AnnotatedString(" ") }, style = body)
        is HtmlBlock.Heading -> Text(
            block.text,
            style = when (block.level) {
                1 -> MaterialTheme.typography.headlineSmall
                2 -> MaterialTheme.typography.titleLarge
                else -> MaterialTheme.typography.titleMedium
            },
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
        is HtmlBlock.ListItem -> Row(Modifier.padding(start = (12 + 20 * block.level).dp)) {
            Text(block.marker, style = body, modifier = Modifier.widthIn(min = 18.dp))
            Spacer(Modifier.size(6.dp))
            Text(block.text, style = body)
        }
    }
}
