package ru.b3lka.linary.presentation.state

import ru.b3lka.linary.core.model.DiaryMedia

/**
 * Состояние полноэкранного просмотра фото.
 */
data class ViewerState(
    val items: List<DiaryMedia>,
    val index: Int,
) {
    val current: DiaryMedia get() = items[index]

    val hasPrevious: Boolean get() = index > 0

    val hasNext: Boolean get() = index < items.lastIndex

    fun step(delta: Int): ViewerState = copy(index = (index + delta).coerceIn(0, items.lastIndex))
}
