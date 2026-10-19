package ru.b3lka.linary.presentation.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.mohamedrejeb.richeditor.model.RichTextState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import ru.b3lka.linary.presentation.platform.ImageSource
import ru.b3lka.linary.presentation.platform.PlatformServices

/**
 * Действия редактора над текстом и вложениями: команды горячих клавиш, отмена/повтор,
 * вставка времени, картинки из буфера, файлов и перетаскивания.
 */
@Suppress("DEPRECATION") // ClipboardManager: новый Clipboard в Compose 1.8 только асинхронный
class EditorController(
    val text: RichTextState,
    private val session: EditorSession,
    val history: EditHistory,
    private val platform: PlatformServices,
    private val clipboard: ClipboardManager,
    private val scope: CoroutineScope,
    private val focus: FocusRequester,
) {
    /**
     * Растёт, когда текст нужно прокрутить в самый низ (время вставлено в конец записи).
     */
    var scrollToEndRequests by mutableIntStateOf(0)
        private set

    /**
     * Выполняет команду; [EditorCommand.Save] выполняет вызывающий — сохранение живёт выше.
     */
    fun run(command: EditorCommand, now: LocalDateTime) {
        when (command) {
            EditorCommand.Save -> Unit
            EditorCommand.InsertTime -> insertTime(now)
            EditorCommand.Undo -> undo()
            EditorCommand.Redo -> redo()
            EditorCommand.Bold -> TextFormatting.toggleBold(text)
            EditorCommand.Italic -> TextFormatting.toggleItalic(text)
            EditorCommand.Underline -> TextFormatting.toggleUnderline(text)
            EditorCommand.Copy -> copy()
            EditorCommand.Cut -> copy().also { text.replaceSelectedText("") }
            EditorCommand.Paste -> paste()
            EditorCommand.SelectAll -> TextFormatting.selectAll(text)
        }
    }

    /**
     * Вставляет отметку времени; она всегда начинается с новой строки.
     * Если курсор в конце записи, текст прокручивается вниз, к вставленному времени.
     */
    fun insertTime(now: LocalDateTime) {
        val stamp = TimeStamp.format(session.dateTime.date, now)
        val start = text.selection.min
        val content = text.annotatedString
        val atEnd = text.selection.max >= content.text.length
        val atLineStart = start == 0 || content.text[start - 1] == '\n' ||
            content.paragraphStyles.any { it.start == start }
        text.replaceSelectedText(if (atLineStart) stamp else "\n$stamp")
        if (atEnd) scrollToEndRequests++
        requestFocus()
    }

    fun undo() {
        history.undo()?.let { text.setHtml(it) }
    }

    fun redo() {
        history.redo()?.let { text.setHtml(it) }
    }

    /**
     * Готовит и прикрепляет картинки в фоне.
     */
    fun attach(sources: List<ImageSource>) {
        if (sources.isNotEmpty()) scope.launch { session.attach(sources) }
    }

    /**
     * Системный диалог выбора картинок.
     */
    fun choosePhotos(title: String, filterName: String) {
        scope.launch {
            session.attach(platform.chooseImages(title, filterName))
            requestFocus()
        }
    }

    private fun copy() {
        clipboard.setText(AnnotatedString(TextFormatting.selectedText(text)))
    }

    /**
     * Картинка из буфера (скриншот, скопированный файл) становится вложением, текст — вставляется.
     */
    private fun paste() {
        val images = platform.clipboardImages()
        if (images.isNotEmpty()) {
            attach(images)
        } else {
            clipboard.getText()?.let { text.replaceSelectedText(it.text) }
        }
    }

    private fun requestFocus() {
        runCatching { focus.requestFocus() }
    }
}
