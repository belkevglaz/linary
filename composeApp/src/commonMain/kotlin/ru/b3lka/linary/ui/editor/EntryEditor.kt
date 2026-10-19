package ru.b3lka.linary.ui.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.editor.EditHistory
import ru.b3lka.linary.presentation.editor.EditorCommand
import ru.b3lka.linary.presentation.editor.EditorController
import ru.b3lka.linary.presentation.editor.EditorSession
import ru.b3lka.linary.presentation.editor.ShortcutKeys
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.editor_placeholder
import ru.b3lka.linary.resources.media_chooser_filter
import ru.b3lka.linary.resources.media_chooser_title
import ru.b3lka.linary.ui.common.LocalAppLocked
import ru.b3lka.linary.ui.common.LocalPlatform

private const val HISTORY_DEBOUNCE_MS = 400L

/**
 * Редактор записи в стиле Diarium: заголовок с датой и оценкой, панель форматирования,
 * поле форматированного текста. Ctrl+S — сохранить, Ctrl+T — вставить время, Ctrl+Z / Ctrl+Shift+Z — отменить /
 * повторить, Ctrl+B/I/U — жирный, курсив, подчёркивание, Ctrl+V — вставить текст или картинку из буфера,
 * Esc — выйти (с подтверждением, если есть правки). Фото можно перетащить в редактор мышью.
 * Сочетания работают и в русской раскладке.
 */
@Composable
fun EntryEditor(
    session: EditorSession,
    now: () -> LocalDateTime,
    saving: Boolean,
    onSave: (html: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null,
) {
    val state = rememberRichTextState()
    val history = remember(session) { EditHistory() }

    @Suppress("DEPRECATION") // ClipboardManager: см. EditorController
    val clipboard = LocalClipboardManager.current
    var confirmDiscard by remember(session) { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val linkColor = MaterialTheme.colorScheme.primary
    val platform = LocalPlatform.current
    val scope = rememberCoroutineScope()
    val chooserTitle = stringResource(Res.string.media_chooser_title)
    val chooserFilter = stringResource(Res.string.media_chooser_filter)

    LaunchedEffect(session) {
        state.config.linkColor = linkColor
        state.setHtml(session.initialHtml)
        val baseline = state.toHtml()
        history.reset(baseline)
        focus.requestFocus()
        snapshotFlow { state.annotatedString }.collectLatest {
            delay(HISTORY_DEBOUNCE_MS)
            val html = state.toHtml()
            history.push(html)
            session.textChanged = html != baseline
        }
    }

    val controller = remember(session, state) {
        EditorController(state, session, history, platform, clipboard, scope, focus)
    }
    val save by rememberUpdatedState { if (!saving && session.preparingMedia == 0) onSave(state.toHtml()) }
    val close by rememberUpdatedState { if (session.isDirty) confirmDiscard = true else onClose() }
    val runCommand: (EditorCommand) -> Unit = { command ->
        if (command == EditorCommand.Save) save() else controller.run(command, now())
    }
    var dropActive by remember { mutableStateOf(false) }
    val dropTarget = remember(platform, controller) {
        platform.imageDropTarget(onActive = { dropActive = it }, onDrop = controller::attach)
    }

    Column(
        modifier
            .fillMaxSize()
            .onPreviewKeyEvent { handleEditorKeys(it, close, runCommand) }
            .then(dropTarget)
            .then(if (dropActive) Modifier.border(2.dp, MaterialTheme.colorScheme.primary) else Modifier),
    ) {
        EditorHeader(
            session,
            saving,
            onSave = save,
            onClose = close,
            onInsertTime = { controller.insertTime(now()) },
            onDelete = onDelete,
        )
        FormattingToolbar(
            controller,
            onAddPhoto = { controller.choosePhotos(chooserTitle, chooserFilter) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        EditorAttachments(session)
        EditorField(state, focus, controller.scrollToEndRequests, Modifier.weight(1f))
    }

    if (confirmDiscard && !LocalAppLocked.current) {
        DiscardDialog(onDiscard = onClose, onKeepEditing = { confirmDiscard = false })
    }
}

@Composable
private fun EditorField(state: RichTextState, focus: FocusRequester, scrollToEndRequests: Int, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface, lineHeight = 26.sp)
    val scroll = rememberScrollState()
    LaunchedEffect(scrollToEndRequests) {
        if (scrollToEndRequests == 0) return@LaunchedEffect
        // Высота текста меняется только после разметки — ждём кадр, чтобы знать новый низ
        withFrameNanos { }
        scroll.animateScrollTo(scroll.maxValue)
    }
    Box(modifier.fillMaxWidth().verticalScroll(scroll)) {
        BasicRichTextEditor(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp)
                .focusRequester(focus),
            textStyle = textStyle,
            cursorBrush = SolidColor(colors.primary),
            decorationBox = { inner ->
                Box {
                    if (state.annotatedString.text.isEmpty()) {
                        Text(stringResource(Res.string.editor_placeholder), style = textStyle, color = colors.outline)
                    }
                    inner()
                }
            },
        )
    }
}

/**
 * Горячие клавиши редактора (см. [ShortcutKeys]). Возвращает true, если событие обработано.
 */
private fun handleEditorKeys(event: KeyEvent, onClose: () -> Unit, onCommand: (EditorCommand) -> Unit): Boolean {
    if (event.type == KeyEventType.KeyUp) return false
    val isKeyDown = event.type == KeyEventType.KeyDown
    if (event.key == Key.Escape) {
        if (isKeyDown) onClose()
        return isKeyDown
    }
    val command = ShortcutKeys.command(event)
    if (command != null && isKeyDown) {
        onCommand(command)
        return true
    }
    // Ctrl + русская буква не должна печатать букву (в том числе отдельным событием ввода символа)
    return ShortcutKeys.isSuppressedTyping(event)
}
