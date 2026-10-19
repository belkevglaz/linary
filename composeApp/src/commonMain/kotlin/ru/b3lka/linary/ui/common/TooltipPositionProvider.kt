package ru.b3lka.linary.ui.common

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider

/**
 * Подсказка под элементом по центру; если снизу не помещается — над ним. Не выходит за края окна.
 */
internal class TooltipPositionProvider(private val gap: Int) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val centered = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val x = centered.coerceIn(0, maxOf(0, windowSize.width - popupContentSize.width))
        val below = anchorBounds.bottom + gap
        val y = if (below + popupContentSize.height <= windowSize.height) {
            below
        } else {
            anchorBounds.top - gap - popupContentSize.height
        }
        return IntOffset(x, y.coerceAtLeast(0))
    }
}
