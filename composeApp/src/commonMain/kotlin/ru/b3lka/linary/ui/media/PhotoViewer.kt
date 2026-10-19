package ru.b3lka.linary.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.model.DiaryMedia
import ru.b3lka.linary.presentation.state.ViewerState
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_close
import ru.b3lka.linary.resources.action_next
import ru.b3lka.linary.resources.action_open_entry
import ru.b3lka.linary.resources.action_previous
import ru.b3lka.linary.resources.media_download
import ru.b3lka.linary.resources.media_set_cover
import ru.b3lka.linary.ui.common.LocalMediaActions
import ru.b3lka.linary.ui.common.Tooltip
import kotlin.math.abs

private const val FLIP_COOLDOWN_MS = 250L

/**
 * Полноэкранный просмотр фото: ←/→, колесо мыши или свайп по тачпаду листают, Esc закрывает.
 * Сверху справа — сделать обложкой дня (если фото ещё не первое в записи), скачать, закрыть.
 */
@Composable
fun PhotoViewer(
    state: ViewerState,
    onChange: (ViewerState?) -> Unit,
    onOpenEntry: ((DiaryMedia) -> Unit)? = null,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    // Актуальные значения для обработчика колеса (pointerInput не пересоздаётся при листании)
    val currentState by rememberUpdatedState(state)
    val currentOnChange by rememberUpdatedState(onChange)
    val go: (Int) -> Unit = { delta ->
        val next = currentState.step(delta)
        if (next != currentState) currentOnChange(next)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.94f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onChange(null)
            }
            .focusRequester(focus)
            .focusable()
            .scrollToFlip(go)
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.Escape -> onChange(null)
                    Key.DirectionLeft -> go(-1)
                    Key.DirectionRight -> go(1)
                    else -> return@onKeyEvent false
                }
                true
            },
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 72.dp, vertical = 48.dp), contentAlignment = Alignment.Center) {
            MediaImage(
                state.current,
                maxSize = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                placeholder = Color.Transparent,
                loading = { CircularProgressIndicator(color = Color.White) },
            )
        }
        ViewerControls(state, onClose = { onChange(null) }, onStep = go, onOpenEntry = onOpenEntry)
    }
}

@Composable
private fun BoxScope.ViewerControls(
    state: ViewerState,
    onClose: () -> Unit,
    onStep: (Int) -> Unit,
    onOpenEntry: ((DiaryMedia) -> Unit)?,
) {
    val buttonColors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
    Row(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
        val actions = LocalMediaActions.current
        val media = state.current
        if (actions != null && actions.canSetCover && !actions.isCover(media)) {
            ViewerButton(Icons.Outlined.StarOutline, stringResource(Res.string.media_set_cover)) {
                actions.setCover(media)
            }
        }
        if (actions != null) {
            ViewerButton(Icons.Outlined.Download, stringResource(Res.string.media_download)) { actions.download(media) }
        }
        val close = stringResource(Res.string.action_close)
        ViewerButton(Icons.Filled.Close, close, tooltip = "$close (Esc)", onClick = onClose)
    }
    if (onOpenEntry != null) {
        TextButton(
            onClick = { onOpenEntry(state.current) },
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        ) { Text(stringResource(Res.string.action_open_entry)) }
    }
    if (state.hasPrevious) {
        val previous = stringResource(Res.string.action_previous)
        Tooltip("$previous (←)", Modifier.align(Alignment.CenterStart).padding(8.dp)) {
            IconButton(onClick = { onStep(-1) }, colors = buttonColors, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.ChevronLeft, previous, Modifier.size(40.dp))
            }
        }
    }
    if (state.hasNext) {
        val next = stringResource(Res.string.action_next)
        Tooltip("$next (→)", Modifier.align(Alignment.CenterEnd).padding(8.dp)) {
            IconButton(onClick = { onStep(1) }, colors = buttonColors, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Filled.ChevronRight, next, Modifier.size(40.dp))
            }
        }
    }
    Text(
        "${state.index + 1} / ${state.items.size}",
        color = Color.White.copy(alpha = 0.8f),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
    )
}

/**
 * Колесо мыши и двухпальцевый свайп по тачпаду листают фото (не чаще раза в [FLIP_COOLDOWN_MS]).
 */
private fun Modifier.scrollToFlip(onStep: (Int) -> Unit): Modifier = pointerInput(Unit) {
    var lastFlip = 0L
    var accumulated = 0f
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull()
            if (event.type == PointerEventType.Scroll && change != null) {
                val d = change.scrollDelta
                accumulated += if (abs(d.x) > abs(d.y)) d.x else d.y
                if (abs(accumulated) >= 1f && change.uptimeMillis - lastFlip > FLIP_COOLDOWN_MS) {
                    onStep(if (accumulated > 0) 1 else -1)
                    lastFlip = change.uptimeMillis
                    accumulated = 0f
                }
                event.changes.forEach { it.consume() }
            }
        }
    }
}

@Composable
private fun ViewerButton(icon: ImageVector, label: String, tooltip: String = label, onClick: () -> Unit) {
    Tooltip(tooltip) {
        IconButton(onClick = onClick, colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)) {
            Icon(icon, contentDescription = label)
        }
    }
}
