package ru.b3lka.linary.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.desktop.data.createRepository
import ru.b3lka.linary.desktop.instance.SingleInstance
import ru.b3lka.linary.desktop.platform.DesktopPlatform
import ru.b3lka.linary.desktop.platform.bringToFront
import ru.b3lka.linary.desktop.settings.AppSettings
import ru.b3lka.linary.desktop.settings.SourceChooser
import ru.b3lka.linary.desktop.settings.WindowBounds
import ru.b3lka.linary.desktop.settings.saveWindowState
import ru.b3lka.linary.desktop.tray.AppTrayIcon
import ru.b3lka.linary.presentation.editor.ShortcutKeys
import ru.b3lka.linary.presentation.lock.LockController
import ru.b3lka.linary.presentation.sync.SyncKeySettings
import ru.b3lka.linary.presentation.window.WindowBehavior
import ru.b3lka.linary.presentation.window.WindowBehavior.CloseAction
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.app_icon
import ru.b3lka.linary.resources.app_name
import ru.b3lka.linary.resources.chooser_filter
import ru.b3lka.linary.resources.chooser_title
import ru.b3lka.linary.ui.app.App
import java.awt.Dimension
import java.io.File

private const val WINDOW_SAVE_DELAY_MS = 1000L
private val DefaultWindowSize = DpSize(1400.dp, 900.dp)

/**
 * Меньше окно не делается. Заодно защищает от KDE: при откреплении окна от края экрана (после Super+Стрелка)
 * он возвращал Java-окну крошечный размер, и окно «пропадало».
 */
private val MinimumWindowSize = Dimension(WindowBounds.MIN_WIDTH, WindowBounds.MIN_HEIGHT)

/**
 * Состояние окна из настроек: прежние размер и положение (если окно видно на экране) и развёрнутость.
 */
@Composable
private fun rememberSavedWindowState(settings: AppSettings): WindowState {
    val bounds = remember { settings.windowBounds }
    val visible = bounds?.takeIf { it.isOnScreen() }
    return rememberWindowState(
        placement = if (settings.windowMaximized) WindowPlacement.Maximized else WindowPlacement.Floating,
        position = visible?.let { WindowPosition(it.x.dp, it.y.dp) } ?: WindowPosition(Alignment.Center),
        size = bounds?.let { DpSize(it.width.dp, it.height.dp) } ?: DefaultWindowSize,
    )
}

/**
 * Сочетания уровня окна — работают при любом фокусе, в том числе на экране блокировки:
 * `Ctrl+Q` — выход (закрытие окна приложение может не завершать), `Ctrl+L` — заблокировать.
 * Работают и в русской раскладке.
 */
private fun handleWindowKeys(event: KeyEvent, lock: LockController, quit: () -> Unit): Boolean {
    if (event.type == KeyEventType.KeyUp) return false
    // Событие ввода символа (Ctrl+Д и т. п.) тоже поглощается, чтобы буква не напечаталась в редакторе
    val down = event.type == KeyEventType.KeyDown
    return when (ShortcutKeys.ctrlLetter(event)) {
        'Q' -> true.also { if (down) quit() }
        'L' -> lock.enabled.also { if (it && down) lock.lockNow() }
        else -> false
    }
}

/**
 * Окно дневника, значок в трее и связь с повторными запусками ([SingleInstance]).
 *
 * @param initialSource папка синхронизации или файл `.diary` из командной строки.
 */
@OptIn(FlowPreview::class)
@Composable
fun ApplicationScope.LinaryApplication(initialSource: String?, instance: SingleInstance) {
    val settings = remember { AppSettings() }
    var sourcePath by remember { mutableStateOf(initialSource ?: settings.source ?: SourceChooser.defaultSource()) }
    var dark by remember { mutableStateOf(settings.dark) }
    val syncKeys = remember { SyncKeySettings(settings) }
    val repository = remember(sourcePath) { sourcePath?.let { createRepository(File(it)) { syncKeys.key } } }
    val platform = remember { DesktopPlatform() }
    val chooserTitle = stringResource(Res.string.chooser_title)
    val chooserFilter = stringResource(Res.string.chooser_filter)
    val windowState = rememberSavedWindowState(settings)
    var windowVisible by remember { mutableStateOf(true) }
    // Каждое увеличение — просьба поднять окно и передать ему фокус
    var activations by remember { mutableIntStateOf(0) }

    val quit: () -> Unit = {
        saveWindowState(windowState, settings)
        exitApplication()
    }
    val behavior = remember { WindowBehavior(settings, AppTrayIcon.isSupported(), quit) }
    val lock = remember { LockController(settings) }
    val show: () -> Unit = {
        windowVisible = true
        windowState.isMinimized = false
        activations++
    }
    LaunchedEffect(instance) { instance.listen(show) }

    if (behavior.trayIcon) AppTray(onOpen = show, onQuit = quit)

    Window(
        onCloseRequest = {
            when (behavior.onCloseAction()) {
                CloseAction.Quit -> quit()
                CloseAction.HideToTray -> {
                    windowVisible = false
                }
                CloseAction.Minimize -> {
                    windowState.isMinimized = true
                }
            }
        },
        visible = windowVisible,
        title = stringResource(Res.string.app_name),
        icon = painterResource(Res.drawable.app_icon),
        state = windowState,
        onPreviewKeyEvent = { handleWindowKeys(it, lock, quit) },
    ) {
        LaunchedEffect(Unit) { window.minimumSize = MinimumWindowSize }
        LaunchedEffect(activations) { if (activations > 0) bringToFront(window) }
        LaunchedEffect(windowState) {
            // Сохраняем и по ходу работы — на случай, если приложение закроют не через окно
            snapshotFlow { Triple(windowState.placement, windowState.position, windowState.size) }
                .debounce(WINDOW_SAVE_DELAY_MS)
                .collect { saveWindowState(windowState, settings) }
        }
        platform.owner = window
        val scope = rememberCoroutineScope()
        App(
            repository = repository,
            platform = platform,
            lock = lock,
            photoPreferences = settings,
            windowBehavior = behavior,
            syncKeys = syncKeys,
            dark = dark,
            onToggleDark = {
                dark = !dark
                settings.dark = dark
            },
            onChooseSource = {
                scope.launch {
                    SourceChooser.choose(window, sourcePath, chooserTitle, chooserFilter)?.let {
                        sourcePath = it
                        settings.source = it
                    }
                }
            },
        )
    }
}
