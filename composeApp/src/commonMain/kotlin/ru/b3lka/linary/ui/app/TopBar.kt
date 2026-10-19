package ru.b3lka.linary.ui.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.state.Section
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_choose_source
import ru.b3lka.linary.resources.action_new_entry
import ru.b3lka.linary.resources.action_reload
import ru.b3lka.linary.resources.action_today
import ru.b3lka.linary.resources.pane_hide_calendar
import ru.b3lka.linary.resources.pane_hide_list
import ru.b3lka.linary.resources.pane_show_calendar
import ru.b3lka.linary.resources.pane_show_list
import ru.b3lka.linary.resources.status_loading
import ru.b3lka.linary.resources.status_read_only
import ru.b3lka.linary.ui.common.LocalListPane
import ru.b3lka.linary.ui.common.Tooltip
import ru.b3lka.linary.ui.common.entriesCountText
import ru.b3lka.linary.ui.common.title

/**
 * Верхняя панель: название раздела, число записей и действия.
 *
 * @param entriesCount null, пока журнал загружается.
 * @param readOnly источник только для чтения (резервная копия) — показывается рядом с числом записей.
 */
@Composable
internal fun TopBar(
    section: Section,
    entriesCount: Int?,
    readOnly: Boolean,
    onNewEntry: (() -> Unit)?,
    onToday: () -> Unit,
    onReload: () -> Unit,
    onChooseSource: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PaneToggle(section)
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(stringResource(section.title), style = MaterialTheme.typography.titleLarge)
                Text(
                    when {
                        entriesCount == null -> stringResource(Res.string.status_loading)
                        readOnly -> stringResource(Res.string.status_read_only, entriesCountText(entriesCount))
                        else -> entriesCountText(entriesCount)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onNewEntry != null) {
                val label = stringResource(Res.string.action_new_entry)
                Tooltip("$label (Ctrl+N)") { IconButton(onClick = onNewEntry) { Icon(Icons.Filled.Add, label) } }
            }
            TopBarButton(Icons.Filled.Today, stringResource(Res.string.action_today), onToday)
            TopBarButton(Icons.Filled.Sync, stringResource(Res.string.action_reload), onReload)
            TopBarButton(Icons.Filled.FolderOpen, stringResource(Res.string.action_choose_source), onChooseSource)
        }
    }
}

/**
 * Кнопка «скрыть / показать» левую панель (календарь или список). Во «Вложениях» панели нет — и кнопки тоже.
 */
@Composable
private fun PaneToggle(section: Section) {
    val pane = LocalListPane.current ?: return
    if (section == Section.Attachments) {
        Spacer(Modifier.width(48.dp))
        return
    }
    val calendar = section == Section.Calendar
    val label = stringResource(
        when {
            pane.visible && calendar -> Res.string.pane_hide_calendar
            pane.visible -> Res.string.pane_hide_list
            calendar -> Res.string.pane_show_calendar
            else -> Res.string.pane_show_list
        },
    )
    Tooltip("$label (Ctrl+\\)") {
        IconButton(onClick = pane::toggle) {
            Icon(if (pane.visible) Icons.AutoMirrored.Filled.MenuOpen else Icons.Filled.Menu, label)
        }
    }
}

@Composable
private fun TopBarButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Tooltip(label) { IconButton(onClick = onClick) { Icon(icon, label) } }
}
