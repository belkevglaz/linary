package ru.b3lka.linary.ui.app

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.core.repository.DiaryLoadException
import ru.b3lka.linary.core.repository.DiaryRepository
import ru.b3lka.linary.presentation.editor.ShortcutKeys
import ru.b3lka.linary.presentation.lock.LockController
import ru.b3lka.linary.presentation.lock.LockStyle
import ru.b3lka.linary.presentation.media.MediaActions
import ru.b3lka.linary.presentation.media.MediaCache
import ru.b3lka.linary.presentation.media.PhotoStripPreferences
import ru.b3lka.linary.presentation.media.PhotoStripState
import ru.b3lka.linary.presentation.platform.PlatformServices
import ru.b3lka.linary.presentation.state.AppState
import ru.b3lka.linary.presentation.state.Section
import ru.b3lka.linary.presentation.sync.SyncKeySettings
import ru.b3lka.linary.presentation.window.WindowBehavior
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.media_save_title
import ru.b3lka.linary.ui.common.LocalAppLocked
import ru.b3lka.linary.ui.common.LocalDateFormatter
import ru.b3lka.linary.ui.common.LocalMediaActions
import ru.b3lka.linary.ui.common.LocalMediaCache
import ru.b3lka.linary.ui.common.LocalPhotoStrip
import ru.b3lka.linary.ui.common.LocalPlatform
import ru.b3lka.linary.ui.common.rememberDateFormatter
import ru.b3lka.linary.ui.common.trackActivity
import ru.b3lka.linary.ui.lock.LockScreen
import ru.b3lka.linary.ui.media.PhotoViewer
import ru.b3lka.linary.ui.settings.SettingsPage
import ru.b3lka.linary.ui.settings.SettingsScreen
import ru.b3lka.linary.ui.sync.SyncKeyScreen
import ru.b3lka.linary.ui.theme.LinaryTheme

private const val LOCK_TICK_MS = 1000L
private const val TODAY_CHECK_MS = 60_000L

/**
 * Корневой экран приложения. [repository] == null — источник ещё не выбран.
 *
 * @param lock блокировка; создаёт её вызывающий, чтобы `Ctrl+L` работал на уровне окна при любом фокусе.
 * @param windowBehavior настройки окна и трея; null — платформа их не поддерживает.
 * @param syncKeys ключ синхронизации пользователя; null — платформа не открывает папки синхронизации.
 */
@Composable
fun App(
    repository: DiaryRepository?,
    platform: PlatformServices,
    lock: LockController,
    photoPreferences: PhotoStripPreferences,
    windowBehavior: WindowBehavior?,
    syncKeys: SyncKeySettings?,
    dark: Boolean,
    onToggleDark: () -> Unit,
    onChooseSource: () -> Unit,
) {
    val photoStrip = remember(photoPreferences) { PhotoStripState(photoPreferences) }
    LaunchedEffect(lock.locked) {
        if (lock.locked && lock.collapsePhotos) photoStrip.collapse()
    }
    LaunchedEffect(lock) {
        while (true) {
            delay(LOCK_TICK_MS)
            lock.tick()
        }
    }
    LinaryTheme(dark) {
        CompositionLocalProvider(
            LocalDateFormatter provides rememberDateFormatter(),
            LocalAppLocked provides lock.locked,
            LocalPhotoStrip provides photoStrip,
        ) {
            Box(Modifier.fillMaxSize().trackActivity(lock::onActivity)) {
                val blur = if (lock.locked && lock.style == LockStyle.Blurred) lock.blurRadius.dp else 0.dp
                Box(if (blur > 0.dp) Modifier.blur(blur) else Modifier) {
                    AppContent(repository, platform, lock, windowBehavior, syncKeys, dark, onToggleDark, onChooseSource)
                }
                if (lock.locked) LockScreen(lock)
            }
        }
    }
}

@Composable
private fun AppContent(
    repository: DiaryRepository?,
    platform: PlatformServices,
    lock: LockController,
    windowBehavior: WindowBehavior?,
    syncKeys: SyncKeySettings?,
    dark: Boolean,
    onToggleDark: () -> Unit,
    onChooseSource: () -> Unit,
) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        if (repository == null) {
            WelcomeScreen(onChooseSource)
        } else {
            val cache = remember(repository) { MediaCache(repository, platform) }
            // Ссылки в записях открываются системным браузером (xdg-open)
            val uriHandler = remember(platform) {
                object : UriHandler {
                    override fun openUri(uri: String) = platform.openUrl(uri)
                }
            }
            CompositionLocalProvider(
                LocalMediaCache provides cache,
                LocalPlatform provides platform,
                LocalUriHandler provides uriHandler,
            ) {
                DiaryScreen(repository, lock, windowBehavior, syncKeys, dark, onToggleDark, onChooseSource)
            }
        }
    }
}

@Composable
private fun DiaryScreen(
    repository: DiaryRepository,
    lock: LockController,
    windowBehavior: WindowBehavior?,
    syncKeys: SyncKeySettings?,
    dark: Boolean,
    onToggleDark: () -> Unit,
    onChooseSource: () -> Unit,
) {
    val state = remember(repository) { AppState(repository) }
    val scope = rememberCoroutineScope()
    val reload: () -> Unit = { scope.launch { state.reload() } }
    LaunchedEffect(repository) { state.reload() }
    // Правки с телефона, докачанные Dropbox, подхватываются сами — когда ничего не редактируется
    LaunchedEffect(repository) { repository.externalChanges.collect { state.onExternalChange() } }
    LaunchedEffect(state.externalChangePending, state.editing.editor, state.editing.saving) { state.reloadIfIdle() }
    KeepTodayCurrent(state)

    val cache = LocalMediaCache.current
    val platform = LocalPlatform.current
    val saveTitle = stringResource(Res.string.media_save_title)
    val mediaActions = remember(state, cache, platform, saveTitle) {
        MediaActions(state, cache, platform, scope, saveTitle)
    }

    var settingsOpen by remember { mutableStateOf(false) }
    // Последняя открытая страница настроек — с неё они откроются в следующий раз
    var settingsPage by remember { mutableStateOf(SettingsPage.Lock) }
    val rootFocus = rememberRootFocus(settingsOpen, editorOpen = state.editing.editor != null)
    // Фото на весь экран было бы видно сквозь размытый экран блокировки
    LaunchedEffect(lock.locked) {
        if (lock.locked && lock.collapsePhotos) state.viewer = null
    }

    CompositionLocalProvider(LocalMediaActions provides mediaActions) {
        Box(
            Modifier
                .fillMaxSize()
                .focusRequester(rootFocus)
                .focusable()
                .onPreviewKeyEvent { handleShortcut(it, state, reload, settingsOpen) },
        ) {
            Row(Modifier.fillMaxSize()) {
                NavigationPanel(
                    current = state.section.takeUnless { settingsOpen },
                    onSelect = {
                        state.section = it
                        settingsOpen = false
                    },
                    dark = dark,
                    onToggleDark = onToggleDark,
                    onOpenSettings = { settingsOpen = true },
                    settingsOpen = settingsOpen,
                )
                // Настройки ложатся поверх раздела, не убирая его: открытый редактор сохраняет свой текст
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    SectionScreen(state, repository, syncKeys, reload, onChooseSource)
                    if (settingsOpen) {
                        SettingsScreen(
                            page = settingsPage,
                            onPageChange = { settingsPage = it },
                            lock = lock,
                            window = windowBehavior,
                            syncKeys = syncKeys,
                            onSyncKeyChanged = reload,
                            dark = dark,
                            onToggleDark = onToggleDark,
                            appVersion = platform.appVersion,
                            onOpenUrl = platform::openUrl,
                            onClose = { settingsOpen = false },
                        )
                    }
                }
            }
            if (!LocalAppLocked.current) DiaryDialogs(state, scope)
            state.viewer?.let { viewer ->
                PhotoViewer(viewer, onChange = { state.viewer = it }, onOpenEntry = { state.openEntryOf(it) })
            }
        }
    }
}

/**
 * Приложение может работать сутками (в трее): раз в минуту проверяет, не наступил ли новый день.
 */
@Composable
private fun KeepTodayCurrent(state: AppState) {
    LaunchedEffect(state) {
        while (true) {
            delay(TODAY_CHECK_MS)
            state.refreshToday()
        }
    }
}

/**
 * Фокус для корня экрана: когда фокус ни на чём (при запуске, после закрытия настроек), горячие клавиши
 * ловит корень. Открытый редактор фокус не отдаёт.
 */
@Composable
private fun rememberRootFocus(settingsOpen: Boolean, editorOpen: Boolean): FocusRequester {
    val focus = remember { FocusRequester() }
    LaunchedEffect(settingsOpen) {
        if (!settingsOpen && !editorOpen) focus.requestFocus()
    }
    return focus
}

/**
 * Выбранный раздел: верхняя панель и его содержимое (или ошибка загрузки).
 */
@Composable
private fun SectionScreen(
    state: AppState,
    repository: DiaryRepository,
    syncKeys: SyncKeySettings?,
    reload: () -> Unit,
    onChooseSource: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(
            section = state.section,
            entriesCount = state.diary?.entries?.size,
            readOnly = !repository.isWritable,
            onNewEntry = if (state.canEdit) ({ state.editing.startNewEntry() }) else null,
            onToday = { state.goToToday() },
            onReload = reload,
            onChooseSource = onChooseSource,
        )
        if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        // Ошибка загрузки важна, только пока дневник ещё не загружен
        val error = state.error?.takeIf { state.diary == null }
        when {
            error == null -> SectionContent(state)
            syncKeys != null && needsKey(error, syncKeys) -> SyncKeyScreen(
                syncKeys,
                repository.source,
                wrongKey = error.reason == DiaryLoadException.Reason.DecryptionFailed,
                onSaved = reload,
                onChooseSource = onChooseSource,
            )
            else -> ErrorScreen(error, repository.source, onRetry = reload, onChooseSource = onChooseSource)
        }
    }
}

/**
 * Нужно ввести ключ: его нет, или заданный не подошёл к папке.
 */
private fun needsKey(error: DiaryLoadException, keys: SyncKeySettings): Boolean =
    error.reason == DiaryLoadException.Reason.KeyMissing ||
        error.reason == DiaryLoadException.Reason.DecryptionFailed && keys.isSet

/**
 * Подтверждение удаления и ошибки сохранения. Во время блокировки не показываются.
 */
@Composable
private fun DiaryDialogs(state: AppState, scope: CoroutineScope) {
    val editing = state.editing
    editing.pendingDelete?.let { entry ->
        DeleteEntryDialog(
            entry,
            onDelete = { scope.launch { editing.confirmDelete() } },
            onCancel = { editing.cancelDelete() },
        )
    }
    editing.error?.let { error ->
        SaveErrorDialog(
            error,
            onDismiss = { editing.error = null },
            onSaveAnyway = if (editing.canSaveOverConflict) {
                { scope.launch { editing.saveOverConflict() } }
            } else {
                null
            },
        )
    }
}

/**
 * Горячие клавиши: Ctrl+R — перечитать, Ctrl+F — поиск, Ctrl+N — новая запись, PgUp/PgDn — соседняя запись.
 * Ctrl+L и Ctrl+Q обрабатывает окно (LinaryApplication), чтобы они работали при любом фокусе.
 * Ctrl-сочетания работают и в русской раскладке (см. [ShortcutKeys]).
 */
private fun handleShortcut(
    event: KeyEvent,
    state: AppState,
    reload: () -> Unit,
    settingsOpen: Boolean,
): Boolean {
    if (event.type == KeyEventType.KeyUp) return false
    val letter = ShortcutKeys.ctrlLetter(event)
    // В настройках, редакторе и просмотре фото — свои горячие клавиши
    if (settingsOpen || state.viewer != null || state.editing.editor != null) return false
    val action: (() -> Unit)? = when (letter) {
        'N' -> ({ state.editing.startNewEntry() })
        'R' -> reload
        'F' -> ({ state.section = Section.Search })
        else -> when (event.key) {
            Key.PageDown -> ({ state.stepEntry(1) })
            Key.PageUp -> ({ state.stepEntry(-1) })
            else -> null
        }
    }
    // Событие ввода символа (в русской раскладке — «т», «к», «а») тоже поглощаем, но действие — только на нажатие
    if (action != null && event.type == KeyEventType.KeyDown) action()
    return action != null
}
