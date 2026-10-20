package ru.b3lka.linary.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.DiaryTag
import ru.b3lka.linary.core.model.TagChange
import ru.b3lka.linary.core.model.TagColor
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_cancel
import ru.b3lka.linary.resources.action_save_plain
import ru.b3lka.linary.resources.tag_color
import ru.b3lka.linary.resources.tag_duplicate
import ru.b3lka.linary.resources.tag_edit_title
import ru.b3lka.linary.resources.tag_name
import ru.b3lka.linary.resources.tag_new_title
import ru.b3lka.linary.resources.tag_no_color
import ru.b3lka.linary.ui.common.Tooltip
import ru.b3lka.linary.ui.common.onEnter
import ru.b3lka.linary.ui.theme.TagPalette

/**
 * Новый тег или правка существующего: название и цвет из палитры (или без цвета).
 *
 * @param tag изменяемый тег; null — новый.
 * @param existing все теги журнала — чтобы не завести второй с тем же названием.
 */
@Composable
fun TagDialog(tag: DiaryTag?, existing: List<DiaryTag>, onSave: (TagChange) -> Unit, onDismiss: () -> Unit) {
    var name by remember(tag) { mutableStateOf(tag?.value.orEmpty()) }
    var color by remember(tag) { mutableStateOf(tag?.displayColor) }
    val trimmed = name.trim()
    val duplicate = existing.any { it.id != tag?.id && it.value.equals(trimmed, ignoreCase = true) }
    val canSave = trimmed.isNotEmpty() && !duplicate
    val save = {
        if (canSave) onSave(TagChange(tag?.id, trimmed, color?.let { TagColor.format(it.toArgb()) }))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (tag == null) Res.string.tag_new_title else Res.string.tag_edit_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { value -> name = value.replace('\n', ' ') },
                    label = { Text(stringResource(Res.string.tag_name)) },
                    singleLine = true,
                    isError = duplicate,
                    supportingText = if (duplicate) ({ Text(stringResource(Res.string.tag_duplicate)) }) else null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { save() }),
                    modifier = Modifier.fillMaxWidth().onEnter(save),
                )
                Text(stringResource(Res.string.tag_color), style = MaterialTheme.typography.labelLarge)
                ColorPalette(color, onSelect = { color = it })
            }
        },
        confirmButton = {
            TextButton(onClick = save, enabled = canSave) { Text(stringResource(Res.string.action_save_plain)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPalette(selected: Color?, onSelect: (Color?) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val noColor = stringResource(Res.string.tag_no_color)
        Tooltip(noColor) {
            Swatch(color = null, selected = selected == null, onClick = { onSelect(null) }, description = noColor)
        }
        TagPalette.forEach { c ->
            Swatch(color = c, selected = selected?.toArgb() == c.toArgb(), onClick = { onSelect(c) })
        }
    }
}

@Composable
private fun Swatch(color: Color?, selected: Boolean, onClick: () -> Unit, description: String? = null) {
    val outline = MaterialTheme.colorScheme.outline
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .then(if (color == null) Modifier.border(1.dp, outline, CircleShape) else Modifier)
            .then(if (color != null) Modifier.background(color) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when {
            selected -> Icon(
                Icons.Filled.Check,
                contentDescription = description,
                tint = color?.let(::contentColorFor) ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
            color == null -> Icon(
                Icons.Filled.FormatColorReset,
                contentDescription = description,
                tint = outline,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
