package ru.b3lka.linary.ui.common

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Обработчик правого клика (на десктопе) — аналог долгого нажатия на телефоне.
 */
fun Modifier.onSecondaryClick(action: () -> Unit): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.forEach { it.consume() }
                action()
            }
        }
    }
}

/**
 * Любое движение мыши, клик или нажатие клавиши внутри — это активность пользователя (для блокировки по бездействию).
 * События не поглощаются.
 */
fun Modifier.trackActivity(onActivity: () -> Unit): Modifier = this
    .onPreviewKeyEvent {
        onActivity()
        false
    }
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                onActivity()
            }
        }
    }

/**
 * Enter (в том числе на цифровом блоке) вызывает [action] — для полей ввода PIN.
 */
fun Modifier.onEnter(action: () -> Unit): Modifier = onPreviewKeyEvent { event ->
    val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
    if (enter && event.type == KeyEventType.KeyDown) action()
    enter
}
