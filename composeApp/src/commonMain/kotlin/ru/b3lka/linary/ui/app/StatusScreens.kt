package ru.b3lka.linary.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
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
import ru.b3lka.linary.core.repository.DiaryLoadException
import ru.b3lka.linary.core.repository.DiarySource
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_choose
import ru.b3lka.linary.resources.action_other_source
import ru.b3lka.linary.resources.action_retry
import ru.b3lka.linary.resources.app_name
import ru.b3lka.linary.resources.error_title
import ru.b3lka.linary.resources.welcome_text
import ru.b3lka.linary.ui.common.errorText
import ru.b3lka.linary.ui.common.sourceText

/**
 * Экран до выбора источника.
 */
@Composable
internal fun WelcomeScreen(onChooseSource: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 520.dp).padding(24.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(Res.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.welcome_text),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = onChooseSource) {
                Icon(Icons.Filled.FolderOpen, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.action_choose))
            }
        }
    }
}

/**
 * Экран ошибки загрузки.
 */
@Composable
internal fun ErrorScreen(
    error: DiaryLoadException,
    source: DiarySource,
    onRetry: () -> Unit,
    onChooseSource: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 600.dp).padding(24.dp),
        ) {
            Text(stringResource(Res.string.error_title), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                sourceText(source),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Text(errorText(error), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Row {
                Button(onClick = onRetry) { Text(stringResource(Res.string.action_retry)) }
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = onChooseSource) { Text(stringResource(Res.string.action_other_source)) }
            }
        }
    }
}
