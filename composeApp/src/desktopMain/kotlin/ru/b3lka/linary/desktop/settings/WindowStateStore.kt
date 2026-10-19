package ru.b3lka.linary.desktop.settings

import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlin.math.roundToInt

/**
 * Запоминает развёрнутость, а размер и положение — только у обычного окна не меньше минимального,
 * чтобы после «развернуть → закрыть» окно восстанавливалось в прежнем размере.
 */
internal fun saveWindowState(state: WindowState, settings: AppSettings) {
    settings.windowMaximized = state.placement == WindowPlacement.Maximized
    val position = state.position
    val size = state.size
    val usable = size.isSpecified &&
        size.width.value >= WindowBounds.MIN_WIDTH && size.height.value >= WindowBounds.MIN_HEIGHT
    if (state.placement == WindowPlacement.Floating && position is WindowPosition.Absolute && usable) {
        settings.windowBounds = WindowBounds(
            x = position.x.value.roundToInt(),
            y = position.y.value.roundToInt(),
            width = size.width.value.roundToInt(),
            height = size.height.value.roundToInt(),
        )
    }
    settings.flush()
}
