package ru.b3lka.linary.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import ru.b3lka.linary.core.model.DiaryEntry
import ru.b3lka.linary.ui.common.onSecondaryClick
import ru.b3lka.linary.ui.media.MediaImage

/**
 * Плитка дня: день с фото — миниатюра на всю плитку, день с текстовой записью — плитка
 * акцентного цвета, сегодняшний день — номер в кольце, у дня с несколькими записями — их число
 * в правом верхнем углу. Долгое нажатие или правый клик открывают меню дня.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DayCell(
    date: LocalDate,
    entries: List<DiaryEntry>,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    onPickEntry: (DiaryEntry) -> Unit,
    onNewEntry: (() -> Unit)?,
) {
    var menu by remember { mutableStateOf(false) }
    val hasMenu = entries.size > 1 || onNewEntry != null
    val colors = MaterialTheme.colorScheme
    // Миниатюра — первое фото самой поздней записи дня
    val photo = remember(entries) { entries.asReversed().firstNotNullOfOrNull { it.images.firstOrNull() } }
    val textOnly = photo == null && entries.isNotEmpty()
    val numberColor = when {
        photo != null -> Color.White
        textOnly -> colors.onPrimary
        else -> colors.onSurface
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(if (textOnly) colors.primary else colors.surfaceContainerHigh)
            .combinedClickable(onClick = onClick, onLongClick = { menu = hasMenu })
            .onSecondaryClick { menu = hasMenu },
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            MediaImage(
                photo,
                maxSize = 220,
                modifier = Modifier.fillMaxSize(),
                placeholder = colors.surfaceContainerHigh,
            )
        }
        DayNumber(date.dayOfMonth, numberColor, withShadow = photo != null, ringed = isToday)
        if (entries.size > 1) {
            EntryCountBadge(entries.size, numberColor, Modifier.align(Alignment.TopEnd))
        }
        if (isSelected) {
            Box(Modifier.fillMaxSize().border(3.dp, if (textOnly) colors.onPrimary else colors.primary))
        }
        DayMenu(
            expanded = menu,
            entries = entries,
            onPick = onPickEntry,
            onNewEntry = onNewEntry,
            onDismiss = { menu = false },
        )
    }
}

@Composable
private fun EntryCountBadge(count: Int, color: Color, modifier: Modifier) {
    Text(
        count.toString(),
        modifier = modifier.padding(top = 3.dp, end = 5.dp),
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            shadow = if (color == Color.White) Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 1f), 4f) else null,
        ),
    )
}

@Composable
internal fun EmptyCell() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer))
}

@Composable
private fun DayNumber(day: Int, color: Color, withShadow: Boolean, ringed: Boolean) {
    val style = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        color = color,
        shadow = if (withShadow) Shadow(Color.Black.copy(alpha = 0.75f), Offset(0f, 1f), 6f) else null,
    )
    val ring = if (ringed) Modifier.border(2.dp, color, CircleShape) else Modifier
    Box(Modifier.size(42.dp).then(ring), contentAlignment = Alignment.Center) {
        Text(day.toString(), style = style)
    }
}
