package ru.b3lka.linary.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay

/**
 * Через сколько миллисекунд под курсором появляется подсказка.
 */
const val TOOLTIP_DELAY_MS = 2000L

/**
 * Подсказка к [content]: появляется, если курсор задержался над ним [TOOLTIP_DELAY_MS],
 * и прячется, когда курсор уходит или нажата кнопка мыши (до следующего наведения).
 *
 * @param modifier модификатор обёртки — сюда переносятся `align` и отступы, которые были у кнопки.
 */
@Composable
fun Tooltip(text: String?, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (text.isNullOrBlank()) {
        Box(modifier) { content() }
        return
    }
    var hovered by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(hovered, pressed) {
        visible = false
        if (hovered && !pressed) {
            delay(TOOLTIP_DELAY_MS)
            visible = true
        }
    }

    Box(
        modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    // Initial: видим события раньше кнопки и ничего у неё не забираем
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    when (event.type) {
                        PointerEventType.Enter -> hovered = true
                        PointerEventType.Exit -> {
                            hovered = false
                            pressed = false
                        }
                        PointerEventType.Press -> pressed = true
                    }
                }
            }
        },
    ) {
        content()
        if (visible) {
            val gap = with(LocalDensity.current) { 6.dp.roundToPx() }
            Popup(popupPositionProvider = remember(gap) { TooltipPositionProvider(gap) }) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    shadowElevation = 4.dp,
                ) {
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}
