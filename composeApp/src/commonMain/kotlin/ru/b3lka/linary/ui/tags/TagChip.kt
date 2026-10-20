package ru.b3lka.linary.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import ru.b3lka.linary.core.html.CssStyles
import ru.b3lka.linary.core.model.DiaryTag
import ru.b3lka.linary.ui.theme.TagTextOnDark
import ru.b3lka.linary.ui.theme.TagTextOnLight

private const val DARK_LUMINANCE = 0.5f

/**
 * Цвет тега для показа; null — цвет не задан (или прозрачный, как Diarium хранит «без цвета»).
 */
internal val DiaryTag.displayColor: Color?
    get() = color?.takeUnless { it.startsWith("#00") }?.let { CssStyles.parseColor(it) }

/**
 * Цвет текста поверх цвета тега.
 */
internal fun contentColorFor(background: Color): Color =
    if (background.luminance() < DARK_LUMINANCE) TagTextOnDark else TagTextOnLight

/**
 * Тег записи — плашка в цвет тега, как в Diarium; без цвета — плашка цвета темы.
 */
@Composable
fun TagChip(tag: DiaryTag, text: String, modifier: Modifier = Modifier) {
    val color = tag.displayColor
    Surface(
        shape = RoundedCornerShape(50),
        color = color ?: MaterialTheme.colorScheme.secondaryContainer,
        contentColor = color?.let(::contentColorFor) ?: MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/**
 * Кружок цвета тега — для списков и фильтров.
 */
@Composable
fun TagColorDot(color: Color?, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color ?: MaterialTheme.colorScheme.outlineVariant),
    )
}
