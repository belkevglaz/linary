package ru.b3lka.linary.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.sync.SyncKeySettings
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.sync_key_clear
import ru.b3lka.linary.resources.sync_key_hint
import ru.b3lka.linary.resources.sync_key_not_set
import ru.b3lka.linary.resources.sync_key_section
import ru.b3lka.linary.resources.sync_key_set
import ru.b3lka.linary.ui.sync.SyncKeyField

/**
 * Страница «Синхронизация»: задан ли ключ, ввод нового ключа, удаление. Сам ключ не показывается.
 *
 * @param onKeyChanged ключ сохранён или удалён — дневник надо перечитать.
 */
@Composable
internal fun SyncPage(keys: SyncKeySettings, onKeyChanged: () -> Unit) {
    SettingsGroup(stringResource(Res.string.sync_key_section)) {
        Text(
            stringResource(if (keys.isSet) Res.string.sync_key_set else Res.string.sync_key_not_set),
            style = MaterialTheme.typography.bodyLarge,
        )
        Hint(stringResource(Res.string.sync_key_hint))
        SyncKeyField(keys, error = null, onSaved = onKeyChanged)
        if (keys.isSet) {
            OutlinedButton(onClick = {
                keys.clear()
                onKeyChanged()
            }) { Text(stringResource(Res.string.sync_key_clear)) }
        }
    }
}
