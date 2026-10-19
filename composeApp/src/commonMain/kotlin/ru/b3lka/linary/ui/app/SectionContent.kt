package ru.b3lka.linary.ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.state.AppState
import ru.b3lka.linary.presentation.state.Section
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.entries_empty
import ru.b3lka.linary.resources.on_this_day_empty
import ru.b3lka.linary.resources.search_nothing_found
import ru.b3lka.linary.resources.search_prompt
import ru.b3lka.linary.resources.tags_choose
import ru.b3lka.linary.resources.tags_no_entries
import ru.b3lka.linary.ui.calendar.CalendarView
import ru.b3lka.linary.ui.common.LocalListPane
import ru.b3lka.linary.ui.editor.EntryEditor
import ru.b3lka.linary.ui.entry.EntryDetail
import ru.b3lka.linary.ui.entry.EntryList
import ru.b3lka.linary.ui.media.AttachmentsView
import ru.b3lka.linary.ui.sections.OnThisDayHeader
import ru.b3lka.linary.ui.sections.SearchHeader
import ru.b3lka.linary.ui.sections.TagsHeader

/**
 * Содержимое выбранного раздела: слева календарь или лента (её можно скрыть, см. [LocalListPane]),
 * справа открытая запись. «Вложения» занимают всю ширину.
 */
@Composable
internal fun SectionContent(state: AppState) {
    val colors = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    if (state.section == Section.Attachments) {
        AttachmentsView(
            state.diary,
            onOpenPhoto = { state.viewer = it },
            modifier = Modifier.background(colors.surfaceContainerLowest),
        )
        return
    }
    val paneVisible = LocalListPane.current?.visible ?: true
    Row(Modifier.fillMaxSize()) {
        // Левая панель выезжает и прячется, как выдвижное меню; запись при этом занимает всю ширину
        AnimatedVisibility(
            visible = paneVisible,
            enter = expandHorizontally(expandFrom = Alignment.Start),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start),
        ) {
            Row {
                Column(Modifier.width(470.dp).fillMaxHeight().background(colors.surfaceContainerLowest)) {
                    if (state.section == Section.Calendar) {
                        CalendarView(
                            diary = state.diary,
                            months = state.calendarMonths,
                            selected = state.selectedDate,
                            today = state.today,
                            onSelect = { state.selectDate(it) },
                            modifier = Modifier.fillMaxSize(),
                            onPickEntry = { state.selectEntry(it) },
                            onNewEntry = if (state.canEdit) ({ state.editing.startNewEntry(it) }) else null,
                        )
                    } else {
                        EntriesPane(state)
                    }
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.outlineVariant))
            }
        }
        val editing = state.editing
        val editor = editing.editor
        if (editor != null) {
            EntryEditor(
                session = editor,
                now = state::now,
                saving = editing.saving,
                onSave = { html -> scope.launch { editing.save(html) } },
                onClose = { editing.cancelEditing() },
                modifier = Modifier.weight(1f).background(colors.surface),
                onDelete = editor.original?.let { entry -> { editing.requestDelete(entry) } },
            )
        } else {
            EntryDetail(
                entry = state.selectedEntry,
                date = state.selectedDate,
                onOpenPhoto = { state.viewer = it },
                modifier = Modifier.weight(1f).background(colors.surface),
                dayEntries = state.entriesOf(state.selectedDate),
                onSelectEntry = { state.selectEntry(it) },
                onEdit = if (state.canEdit) editing::startEditing else null,
                onCreate = if (state.canEdit) ({ editing.startNewEntry() }) else null,
                onDelete = if (state.canEdit) editing::requestDelete else null,
            )
        }
    }
}

@Composable
private fun ColumnScope.EntriesPane(state: AppState) {
    val listed = state.listedEntries
    when (state.section) {
        Section.Search -> SearchHeader(state.query, { state.query = it }, listed.size)
        Section.Tags -> TagsHeader(state.diary, state.tagFilter) { state.tagFilter = it }
        Section.OnThisDay -> OnThisDayHeader(state.today)
        else -> Unit
    }
    EntryList(
        entries = listed,
        selectedId = state.selectedEntryId,
        onSelect = { state.selectEntry(it) },
        emptyText = stringResource(emptyTextFor(state)),
        modifier = Modifier.weight(1f),
    )
}

private fun emptyTextFor(state: AppState): StringResource = when (state.section) {
    Section.Search -> if (state.query.isBlank()) Res.string.search_prompt else Res.string.search_nothing_found
    Section.Tags -> if (state.tagFilter == null) Res.string.tags_choose else Res.string.tags_no_entries
    Section.OnThisDay -> Res.string.on_this_day_empty
    else -> Res.string.entries_empty
}
