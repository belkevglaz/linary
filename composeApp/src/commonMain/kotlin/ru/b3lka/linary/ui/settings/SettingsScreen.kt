package ru.b3lka.linary.ui.settings

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.lock.LockController
import ru.b3lka.linary.presentation.sync.SyncKeySettings
import ru.b3lka.linary.presentation.window.WindowBehavior
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_close
import ru.b3lka.linary.resources.action_quit
import ru.b3lka.linary.resources.settings_about_section
import ru.b3lka.linary.resources.settings_appearance
import ru.b3lka.linary.resources.settings_lock_section
import ru.b3lka.linary.resources.settings_lock_style
import ru.b3lka.linary.resources.settings_sync
import ru.b3lka.linary.resources.settings_title
import ru.b3lka.linary.resources.settings_window_section
import ru.b3lka.linary.ui.common.Tooltip

/**
 * Настройки на месте содержимого главного окна: список страниц слева, выбранная страница справа.
 * Новые настройки добавляются страницей в [SettingsPage] или группой ([SettingsGroup]) на существующей.
 * `Esc` или стрелка «назад» закрывают настройки.
 *
 * @param window настройки окна и трея; null — страницы «Окно» и кнопки «Выйти» нет.
 * @param syncKeys ключ синхронизации; null — страницы «Синхронизация» нет.
 * @param onSyncKeyChanged ключ сохранён или удалён — дневник надо перечитать.
 * @param appVersion версия из пакета; null — запуск из исходников.
 */
@Composable
fun SettingsScreen(
    page: SettingsPage,
    onPageChange: (SettingsPage) -> Unit,
    lock: LockController,
    window: WindowBehavior?,
    syncKeys: SyncKeySettings?,
    onSyncKeyChanged: () -> Unit,
    dark: Boolean,
    onToggleDark: () -> Unit,
    appVersion: String?,
    onOpenUrl: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Фокус уходит сюда, чтобы клавиши не попадали в редактор, оставшийся под настройками
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val pages = SettingsPage.entries.filter {
        when (it) {
            SettingsPage.Window -> window != null
            SettingsPage.Sync -> syncKeys != null
            else -> true
        }
    }
    val current = page.takeIf { it in pages } ?: SettingsPage.Lock

    Surface(
        modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { event ->
                (event.type == KeyEventType.KeyDown && event.key == Key.Escape).also { if (it) onClose() }
            },
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            SettingsHeader(onClose)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxSize()) {
                PageList(pages, current, onPageChange, onQuit = window?.quit)
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                PageContent(current) {
                    when (current) {
                        SettingsPage.Lock -> LockPage(lock, onLockNow = {
                            onClose()
                            lock.lockNow()
                        })
                        SettingsPage.LockScreen -> LockScreenPage(lock)
                        SettingsPage.Sync -> syncKeys?.let { SyncPage(it, onSyncKeyChanged) }
                        SettingsPage.Appearance -> AppearancePage(dark, onToggleDark)
                        SettingsPage.Window -> window?.let { WindowPage(it) }
                        SettingsPage.About -> AboutPage(appVersion, onOpenUrl)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsHeader(onClose: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val closeLabel = stringResource(Res.string.action_close)
        Tooltip(closeLabel) {
            IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, closeLabel) }
        }
        Text(
            stringResource(Res.string.settings_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun PageList(
    pages: List<SettingsPage>,
    current: SettingsPage,
    onSelect: (SettingsPage) -> Unit,
    onQuit: (() -> Unit)?,
) {
    Column(
        Modifier.width(260.dp).fillMaxHeight().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        pages.forEach { page ->
            val title = stringResource(page.title)
            NavigationDrawerItem(
                label = { Text(title) },
                icon = { Icon(page.icon, null) },
                selected = page == current,
                onClick = { onSelect(page) },
            )
        }
        if (onQuit != null) {
            Spacer(Modifier.weight(1f))
            NavigationDrawerItem(
                label = { Text(stringResource(Res.string.action_quit)) },
                icon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) },
                selected = false,
                onClick = onQuit,
            )
        }
    }
}

@Composable
private fun PageContent(page: SettingsPage, content: @Composable () -> Unit) {
    val scroll = rememberScrollState()
    LaunchedEffect(page) { scroll.scrollTo(0) }
    Box(Modifier.fillMaxSize().verticalScroll(scroll)) {
        Column(
            Modifier.widthIn(max = 680.dp).padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall)
            content()
        }
    }
}

private val SettingsPage.title: StringResource
    get() = when (this) {
        SettingsPage.Lock -> Res.string.settings_lock_section
        SettingsPage.LockScreen -> Res.string.settings_lock_style
        SettingsPage.Sync -> Res.string.settings_sync
        SettingsPage.Appearance -> Res.string.settings_appearance
        SettingsPage.Window -> Res.string.settings_window_section
        SettingsPage.About -> Res.string.settings_about_section
    }

private val SettingsPage.icon: ImageVector
    get() = when (this) {
        SettingsPage.Lock -> Icons.Outlined.Lock
        SettingsPage.LockScreen -> Icons.Outlined.BlurOn
        SettingsPage.Sync -> Icons.Outlined.Key
        SettingsPage.Appearance -> Icons.Outlined.Palette
        SettingsPage.Window -> Icons.Outlined.DesktopWindows
        SettingsPage.About -> Icons.Outlined.Info
    }
