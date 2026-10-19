package ru.b3lka.linary.ui.lock

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.lock.LockController
import ru.b3lka.linary.presentation.lock.LockStyle
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.app_icon
import ru.b3lka.linary.resources.lock_prompt
import ru.b3lka.linary.resources.lock_title
import ru.b3lka.linary.resources.lock_unlock
import ru.b3lka.linary.resources.lock_wait
import ru.b3lka.linary.resources.pin_wrong
import ru.b3lka.linary.ui.common.onEnter

private const val SCRIM_ALPHA = 0.35f

/**
 * Экран блокировки: закрывает всё окно — непрозрачно или вуалью поверх размытого приложения
 * ([LockStyle]) — и перехватывает мышь, пока не введён PIN.
 * Состояние приложения под ним (открытый редактор, выбранная запись) сохраняется.
 */
@Composable
fun LockScreen(lock: LockController) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val waiting = lock.retryAfterSeconds > 0
    val blurred = lock.style == LockStyle.Blurred
    val colors = MaterialTheme.colorScheme
    val submit = {
        if (!waiting && pin.isNotEmpty()) {
            wrong = lock.unlock(pin) != LockController.UnlockResult.Ok
            pin = ""
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Сплошной фон или полупрозрачная вуаль поверх размытого окна
            .background(if (blurred) colors.scrim.copy(alpha = SCRIM_ALPHA) else colors.surface)
            // Клики не проходят к приложению под экраном
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            shadowElevation = if (blurred) 8.dp else 0.dp,
        ) {
            Column(
                Modifier.width(320.dp).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(
                    painterResource(Res.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
                Text(stringResource(Res.string.lock_title), style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        pin = value.filter { it in '0'..'9' }.take(LockController.MAX_PIN_LENGTH)
                        wrong = false
                    },
                    label = { Text(stringResource(Res.string.lock_prompt)) },
                    leadingIcon = { Icon(Icons.Outlined.Lock, null) },
                    singleLine = true,
                    enabled = !waiting,
                    isError = wrong,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.focusRequester(focus).onEnter(submit),
                )
                LockMessage(lock.retryAfterSeconds, wrong)
                Button(onClick = submit, enabled = !waiting && pin.isNotEmpty()) {
                    Text(stringResource(Res.string.lock_unlock))
                }
            }
        }
    }
}

/**
 * Подсказка под полем PIN: сколько ждать после неудачных попыток или что PIN неверный.
 */
@Composable
private fun LockMessage(waitSeconds: Long, wrong: Boolean) {
    val message = when {
        waitSeconds > 0 -> stringResource(Res.string.lock_wait, waitSeconds)
        wrong -> stringResource(Res.string.pin_wrong)
        else -> return
    }
    Text(
        message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
    )
}
