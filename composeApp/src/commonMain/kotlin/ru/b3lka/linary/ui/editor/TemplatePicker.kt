package ru.b3lka.linary.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.html.HtmlParser
import ru.b3lka.linary.core.model.DiaryTemplate
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_cancel
import ru.b3lka.linary.resources.template_pick_hint
import ru.b3lka.linary.resources.template_pick_title
import ru.b3lka.linary.resources.template_untitled

/**
 * Выбор шаблона для вставки в запись: название и начало текста каждого шаблона.
 */
@Composable
internal fun TemplatePickerDialog(
    templates: List<DiaryTemplate>,
    onPick: (DiaryTemplate) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.template_pick_title)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(Res.string.template_pick_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                templates.forEach { template -> TemplateRow(template) { onPick(template) } }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@Composable
private fun TemplateRow(template: DiaryTemplate, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(templateTitle(template), style = MaterialTheme.typography.titleSmall)
        val preview = HtmlParser.toPlainText(template.html).lines().filter { it.isNotBlank() }.joinToString(" · ")
        if (preview.isNotEmpty()) {
            Text(
                preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Название шаблона для списка; у безымянного — его заголовок или «Без названия».
 */
@Composable
internal fun templateTitle(template: DiaryTemplate): String =
    template.name.ifBlank { template.heading }.ifBlank { stringResource(Res.string.template_untitled) }
