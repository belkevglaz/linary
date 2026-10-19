package ru.b3lka.linary.ui.sync

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.repository.DiarySource
import ru.b3lka.linary.presentation.sync.SyncKeySettings
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_other_source
import ru.b3lka.linary.resources.sync_key_screen_text
import ru.b3lka.linary.resources.sync_key_screen_title
import ru.b3lka.linary.resources.sync_key_wrong
import ru.b3lka.linary.ui.common.sourceText

/**
 * Экран «ключ не задан»: папку синхронизации не открыть без ключа, а в программу он не входит.
 * После сохранения ключа дневник перечитывается ([onSaved]).
 *
 * @param wrongKey ключ задан, но к папке не подошёл.
 */
@Composable
fun SyncKeyScreen(
    keys: SyncKeySettings,
    source: DiarySource,
    wrongKey: Boolean,
    onSaved: () -> Unit,
    onChooseSource: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 560.dp).padding(24.dp),
        ) {
            Icon(
                Icons.Outlined.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(Res.string.sync_key_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                sourceText(source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(Res.string.sync_key_screen_text),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            SyncKeyField(
                keys,
                error = if (wrongKey) stringResource(Res.string.sync_key_wrong) else null,
                onSaved = onSaved,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onChooseSource) { Text(stringResource(Res.string.action_other_source)) }
        }
    }
}
