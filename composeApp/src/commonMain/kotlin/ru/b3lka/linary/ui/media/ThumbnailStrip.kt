package ru.b3lka.linary.ui.media

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_next
import ru.b3lka.linary.resources.action_previous
import ru.b3lka.linary.ui.common.Tooltip
import ru.b3lka.linary.ui.common.wheelScrollsHorizontally

private const val PAGE_FRACTION = 0.8f

/**
 * Лента миниатюр, которую удобно листать мышью: колесо прокручивает её по горизонтали,
 * а по краям, когда есть что листать, появляются стрелки — прокрутка почти на ширину ленты.
 */
@Composable
fun ThumbnailStrip(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val wheelStep = with(LocalDensity.current) { 80.dp.toPx() }
    val page = { direction: Int ->
        scope.launch { scroll.animateScrollBy(direction * scroll.viewportSize * PAGE_FRACTION) }
        Unit
    }
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(scroll).wheelScrollsHorizontally(scroll, wheelStep),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
        if (scroll.canScrollBackward) {
            StripArrow(Icons.Filled.ChevronLeft, stringResource(Res.string.action_previous), Alignment.CenterStart) {
                page(-1)
            }
        }
        if (scroll.canScrollForward) {
            StripArrow(Icons.Filled.ChevronRight, stringResource(Res.string.action_next), Alignment.CenterEnd) {
                page(1)
            }
        }
    }
}

@Composable
private fun BoxScope.StripArrow(
    icon: ImageVector,
    label: String,
    alignment: Alignment,
    onClick: () -> Unit,
) {
    Tooltip(label, Modifier.align(alignment).padding(horizontal = 4.dp)) {
        FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(36.dp)) { Icon(icon, label) }
    }
}
