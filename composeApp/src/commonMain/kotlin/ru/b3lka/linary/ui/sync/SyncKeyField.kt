package ru.b3lka.linary.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.sync.SyncKeySettings
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.sync_key_invalid
import ru.b3lka.linary.resources.sync_key_label
import ru.b3lka.linary.resources.sync_key_save
import ru.b3lka.linary.resources.sync_key_supporting
import ru.b3lka.linary.ui.common.onEnter

/**
 * Поле ввода ключа синхронизации с кнопкой «Сохранить». Ключ не показывается после сохранения
 * и проверяется только по форме; подходит ли он к папке, покажет загрузка дневника.
 *
 * @param error текст ошибки под полем (например, «ключ не подходит»), если есть.
 * @param onSaved ключ сохранён.
 */
@Composable
fun SyncKeyField(keys: SyncKeySettings, error: String?, onSaved: () -> Unit, modifier: Modifier = Modifier) {
    var value by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    val save = {
        invalid = !keys.save(value)
        if (!invalid) {
            value = ""
            onSaved()
        }
    }
    val message = if (invalid) stringResource(Res.string.sync_key_invalid, SyncKeySettings.LENGTH) else error
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                value = it.trim()
                invalid = false
            },
            label = { Text(stringResource(Res.string.sync_key_label)) },
            leadingIcon = { Icon(Icons.Outlined.Key, null) },
            supportingText = {
                Text(message ?: stringResource(Res.string.sync_key_supporting, SyncKeySettings.LENGTH, value.length))
            },
            isError = message != null,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { save() }),
            modifier = Modifier.fillMaxWidth().onEnter(save),
        )
        Button(onClick = save, enabled = value.isNotEmpty()) { Text(stringResource(Res.string.sync_key_save)) }
    }
}
