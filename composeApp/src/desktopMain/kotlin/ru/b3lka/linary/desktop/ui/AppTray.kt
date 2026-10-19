package ru.b3lka.linary.desktop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.desktop.tray.AppTrayIcon
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_quit
import ru.b3lka.linary.resources.app_name
import ru.b3lka.linary.resources.tray_open

/**
 * Значок в системном трее, пока эта функция в композиции: щелчок показывает окно,
 * в меню — «Открыть» и «Выйти». Значок создаётся в фоне: это обращения к D-Bus.
 */
@Composable
fun AppTray(onOpen: () -> Unit, onQuit: () -> Unit) {
    val title = stringResource(Res.string.app_name)
    val openLabel = stringResource(Res.string.tray_open)
    val quitLabel = stringResource(Res.string.action_quit)
    val open by rememberUpdatedState(onOpen)
    val quit by rememberUpdatedState(onQuit)
    LaunchedEffect(title, openLabel, quitLabel) {
        val icon = withContext(Dispatchers.IO) {
            AppTrayIcon.show(title, openLabel, quitLabel, onOpen = { open() }, onQuit = { quit() })
        }
        try {
            awaitCancellation()
        } finally {
            icon?.close()
        }
    }
}
