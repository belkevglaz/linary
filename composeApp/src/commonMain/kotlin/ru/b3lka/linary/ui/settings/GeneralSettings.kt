package ru.b3lka.linary.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.window.WindowBehavior
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.app_name
import ru.b3lka.linary.resources.settings_close_hint
import ru.b3lka.linary.resources.settings_close_keeps_running
import ru.b3lka.linary.resources.settings_close_section
import ru.b3lka.linary.resources.settings_theme
import ru.b3lka.linary.resources.settings_tray
import ru.b3lka.linary.resources.settings_tray_section
import ru.b3lka.linary.resources.settings_tray_unsupported
import ru.b3lka.linary.resources.settings_version
import ru.b3lka.linary.resources.settings_version_dev
import ru.b3lka.linary.resources.theme_dark
import ru.b3lka.linary.resources.theme_light

private const val PROJECT_URL = "https://github.com/belkevglaz/linary"

/**
 * Страница «Оформление»: светлая или тёмная тема.
 */
@Composable
internal fun AppearancePage(dark: Boolean, onToggleDark: () -> Unit) {
    SettingsGroup(stringResource(Res.string.settings_theme)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !dark,
                onClick = { if (dark) onToggleDark() },
                label = { Text(stringResource(Res.string.theme_light)) },
            )
            FilterChip(
                selected = dark,
                onClick = { if (!dark) onToggleDark() },
                label = { Text(stringResource(Res.string.theme_dark)) },
            )
        }
    }
}

/**
 * Страница «Окно»: значок в трее и что делает закрытие окна.
 */
@Composable
internal fun WindowPage(window: WindowBehavior) {
    SettingsGroup(stringResource(Res.string.settings_tray_section)) {
        SwitchRow(stringResource(Res.string.settings_tray), window.trayIcon, enabled = window.traySupported) {
            window.changeTrayIcon(it)
        }
        if (!window.traySupported) Hint(stringResource(Res.string.settings_tray_unsupported))
    }
    SettingsGroup(stringResource(Res.string.settings_close_section)) {
        SwitchRow(stringResource(Res.string.settings_close_keeps_running), window.closeKeepsRunning) {
            window.changeCloseKeepsRunning(it)
        }
        if (window.closeKeepsRunning) Hint(stringResource(Res.string.settings_close_hint))
    }
}

/**
 * Страница «О программе»: версия и ссылка на проект.
 *
 * @param appVersion версия из пакета; null — запуск из исходников.
 */
@Composable
internal fun AboutPage(appVersion: String?, onOpenUrl: (String) -> Unit) {
    SettingsGroup(stringResource(Res.string.app_name)) {
        Text(
            if (appVersion != null) {
                stringResource(Res.string.settings_version, appVersion)
            } else {
                stringResource(Res.string.settings_version_dev)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = { onOpenUrl(PROJECT_URL) }) { Text(PROJECT_URL.removePrefix("https://")) }
    }
}
