package ru.b3lka.linary.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.lock.LockController
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_apply
import ru.b3lka.linary.resources.action_cancel
import ru.b3lka.linary.resources.pin_current
import ru.b3lka.linary.resources.pin_forget_hint
import ru.b3lka.linary.resources.pin_invalid
import ru.b3lka.linary.resources.pin_mismatch
import ru.b3lka.linary.resources.pin_new
import ru.b3lka.linary.resources.pin_repeat
import ru.b3lka.linary.resources.pin_wrong
import ru.b3lka.linary.ui.common.onEnter

/**
 * Ввод PIN: текущий ([askCurrent]) и/или новый с повтором ([askNew]).
 * [onConfirm] получает введённое и возвращает false, если текущий PIN неверный.
 */
@Composable
internal fun PinDialog(
    title: StringResource,
    askCurrent: Boolean,
    askNew: Boolean,
    onConfirm: (current: String, new: String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<StringResource?>(null) }
    val confirm = {
        error = when {
            askCurrent && current.isEmpty() -> Res.string.pin_wrong
            askNew && !LockController.isValidPin(new) -> Res.string.pin_invalid
            askNew && new != repeat -> Res.string.pin_mismatch
            !onConfirm(current, new) -> Res.string.pin_wrong
            else -> null
        }
        if (error == null) onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (askCurrent) PinField(current, Res.string.pin_current, confirm) { current = it }
                if (askNew) {
                    PinField(new, Res.string.pin_new, confirm) { new = it }
                    PinField(repeat, Res.string.pin_repeat, confirm) { repeat = it }
                    Text(
                        stringResource(Res.string.pin_forget_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(onClick = confirm) { Text(stringResource(Res.string.action_apply)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@Composable
private fun PinField(value: String, label: StringResource, onEnter: () -> Unit, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it in '0'..'9' }.take(LockController.MAX_PIN_LENGTH)) },
        label = { Text(stringResource(label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.onEnter(onEnter),
    )
}
