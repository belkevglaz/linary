package ru.b3lka.linary.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.format_no_color

/**
 * Выпадающая палитра цветов; null в [onPick] означает «без цвета».
 */
@Composable
internal fun ColorPaletteMenu(
    expanded: Boolean,
    colors: List<Color>,
    onPick: (Color?) -> Unit,
    onDismiss: () -> Unit,
) {
    // Меню без фокуса, чтобы поле ввода не сбросило выделение, к которому применится цвет
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { color ->
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            .clickable {
                                onPick(color)
                                onDismiss()
                            },
                    )
                }
            }
        }
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.format_no_color)) },
            leadingIcon = { Icon(Icons.Filled.FormatColorReset, null) },
            onClick = {
                onPick(null)
                onDismiss()
            },
        )
    }
}
