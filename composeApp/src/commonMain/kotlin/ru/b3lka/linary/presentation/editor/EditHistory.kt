package ru.b3lka.linary.presentation.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * История правок для «Отменить» и «Повторить»: снимки HTML с небольшой задержкой после ввода.
 */
class EditHistory(private val limit: Int = DEFAULT_LIMIT) {
    private val undoStack = ArrayDeque<String>()
    private val redoStack = ArrayDeque<String>()

    /**
     * Следующий снимок — это результат отмены/повтора, а не новая правка.
     */
    private var restoring = false

    var canUndo by mutableStateOf(false)
        private set
    var canRedo by mutableStateOf(false)
        private set

    fun reset(initial: String) {
        undoStack.clear()
        redoStack.clear()
        undoStack.addLast(initial)
        restoring = false
        update()
    }

    /**
     * Новый снимок текста. Новая правка очищает «Повторить»; снимок после отмены или повтора
     * только уточняет текущее состояние (редактор может записать тот же текст чуть иначе).
     */
    fun push(html: String) {
        if (restoring) {
            restoring = false
            if (undoStack.isNotEmpty()) undoStack[undoStack.lastIndex] = html
            return
        }
        if (undoStack.lastOrNull() == html) return
        undoStack.addLast(html)
        if (undoStack.size > limit) undoStack.removeFirst()
        redoStack.clear()
        update()
    }

    /**
     * Возвращает предыдущее состояние или null, если отменять нечего.
     */
    fun undo(): String? {
        if (undoStack.size < 2) return null
        redoStack.addLast(undoStack.removeLast())
        restoring = true
        update()
        return undoStack.last()
    }

    /**
     * Возвращает отменённое состояние или null, если повторять нечего.
     */
    fun redo(): String? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(next)
        restoring = true
        update()
        return next
    }

    private fun update() {
        canUndo = undoStack.size > 1
        canRedo = redoStack.isNotEmpty()
    }

    private companion object {
        const val DEFAULT_LIMIT = 100
    }
}
