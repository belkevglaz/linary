package ru.b3lka.linary.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.on_this_day_subtitle
import ru.b3lka.linary.ui.common.LocalDateFormatter

/**
 * Шапка раздела «В этот день».
 */
@Composable
fun OnThisDayHeader(today: LocalDate) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(LocalDateFormatter.current.dayMonth(today), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(Res.string.on_this_day_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
