package ru.b3lka.linary.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_clear
import ru.b3lka.linary.resources.search_found
import ru.b3lka.linary.resources.search_placeholder
import ru.b3lka.linary.ui.common.Tooltip
import ru.b3lka.linary.ui.common.entriesCountText

/**
 * Шапка раздела «Поиск»: поле ввода (получает фокус сразу) и число найденных записей.
 */
@Composable
fun SearchHeader(query: String, onQueryChange: (String) -> Unit, found: Int) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Column(Modifier.padding(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(stringResource(Res.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    val clear = stringResource(Res.string.action_clear)
                    Tooltip(clear) {
                        IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, clear) }
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        if (query.isNotBlank()) {
            Text(
                stringResource(Res.string.search_found, entriesCountText(found)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
            )
        }
    }
}
