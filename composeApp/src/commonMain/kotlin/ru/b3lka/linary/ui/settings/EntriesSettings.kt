package ru.b3lka.linary.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.DiaryTemplate
import ru.b3lka.linary.presentation.editor.DefaultTemplateState
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.settings_default_template
import ru.b3lka.linary.resources.settings_default_template_hint
import ru.b3lka.linary.resources.settings_templates_empty
import ru.b3lka.linary.resources.template_none
import ru.b3lka.linary.ui.editor.templateTitle

/**
 * Страница «Записи»: шаблон, которым заполняется новая запись.
 * Если выбранного шаблона больше нет в дневнике, выбранным считается «Нет».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EntriesPage(templates: List<DiaryTemplate>, defaultTemplate: DefaultTemplateState) {
    SettingsGroup(stringResource(Res.string.settings_default_template)) {
        if (templates.isEmpty()) {
            Hint(stringResource(Res.string.settings_templates_empty))
            return@SettingsGroup
        }
        val current = defaultTemplate.id?.takeIf { id -> templates.any { it.id == id } }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = current == null,
                onClick = { defaultTemplate.change(null) },
                label = { Text(stringResource(Res.string.template_none)) },
            )
            templates.forEach { template ->
                FilterChip(
                    selected = current == template.id,
                    onClick = { defaultTemplate.change(template.id) },
                    label = { Text(templateTitle(template)) },
                )
            }
        }
        Hint(stringResource(Res.string.settings_default_template_hint))
    }
}
