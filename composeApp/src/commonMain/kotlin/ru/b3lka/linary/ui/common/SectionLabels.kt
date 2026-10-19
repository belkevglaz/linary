package ru.b3lka.linary.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import ru.b3lka.linary.presentation.state.Section
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.section_attachments
import ru.b3lka.linary.resources.section_calendar
import ru.b3lka.linary.resources.section_feed
import ru.b3lka.linary.resources.section_on_this_day
import ru.b3lka.linary.resources.section_search
import ru.b3lka.linary.resources.section_tags

val Section.title: StringResource
    get() = when (this) {
        Section.Calendar -> Res.string.section_calendar
        Section.Feed -> Res.string.section_feed
        Section.Attachments -> Res.string.section_attachments
        Section.Search -> Res.string.section_search
        Section.Tags -> Res.string.section_tags
        Section.OnThisDay -> Res.string.section_on_this_day
    }

/**
 * Иконка раздела: закрашенная — для выбранного, контурная — для остальных.
 */
fun Section.icon(selected: Boolean): ImageVector = when (this) {
    Section.Calendar -> if (selected) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth
    Section.Feed -> if (selected) Icons.Filled.ViewAgenda else Icons.Outlined.ViewAgenda
    Section.Attachments -> if (selected) Icons.Filled.GridView else Icons.Outlined.GridView
    Section.Search -> Icons.Filled.Search
    Section.Tags -> if (selected) Icons.Filled.Sell else Icons.Outlined.Sell
    Section.OnThisDay -> if (selected) Icons.Filled.EventRepeat else Icons.Outlined.EventRepeat
}
