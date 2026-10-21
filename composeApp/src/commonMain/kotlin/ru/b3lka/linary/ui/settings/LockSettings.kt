package ru.b3lka.linary.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.lock.LockController
import ru.b3lka.linary.presentation.lock.LockStyle
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.lock_style_blurred
import ru.b3lka.linary.resources.lock_style_solid
import ru.b3lka.linary.resources.pin_change_title
import ru.b3lka.linary.resources.pin_create_title
import ru.b3lka.linary.resources.pin_disable_title
import ru.b3lka.linary.resources.settings_blur
import ru.b3lka.linary.resources.settings_change_pin
import ru.b3lka.linary.resources.settings_lock_collapse_photos
import ru.b3lka.linary.resources.settings_lock_enable
import ru.b3lka.linary.resources.settings_lock_hint
import ru.b3lka.linary.resources.settings_lock_now
import ru.b3lka.linary.resources.settings_lock_on_close
import ru.b3lka.linary.resources.settings_lock_on_close_hint
import ru.b3lka.linary.resources.settings_lock_screen_hint
import ru.b3lka.linary.resources.settings_lock_section
import ru.b3lka.linary.resources.settings_lock_style
import ru.b3lka.linary.resources.settings_lock_timeout
import ru.b3lka.linary.resources.settings_minutes
import ru.b3lka.linary.resources.settings_photos_section
import ru.b3lka.linary.resources.settings_pin_section
import ru.b3lka.linary.ui.common.LocalAppLocked
import kotlin.math.roundToInt

private enum class PinAction { Enable, Disable, Change }

/**
 * Страница «Блокировка»: включение, время бездействия, блокировка при закрытии окна, PIN.
 *
 * @param onLockNow заблокировать сразу (страница настроек при этом закрывается).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LockPage(lock: LockController, onLockNow: () -> Unit) {
    var pinAction by remember { mutableStateOf<PinAction?>(null) }
    SettingsGroup(stringResource(Res.string.settings_lock_section)) {
        SwitchRow(stringResource(Res.string.settings_lock_enable), lock.enabled) { on ->
            pinAction = if (on) PinAction.Enable else PinAction.Disable
        }
        Hint(stringResource(Res.string.settings_lock_hint))
    }
    if (lock.enabled) {
        SettingsGroup(stringResource(Res.string.settings_lock_timeout)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LockController.TIMEOUT_OPTIONS.forEach { minutes ->
                    FilterChip(
                        selected = lock.timeoutMinutes == minutes,
                        onClick = { lock.changeTimeout(minutes) },
                        label = { Text(stringResource(Res.string.settings_minutes, minutes)) },
                    )
                }
            }
            SwitchRow(stringResource(Res.string.settings_lock_on_close), lock.lockOnClose) {
                lock.changeLockOnClose(it)
            }
            Hint(stringResource(Res.string.settings_lock_on_close_hint))
        }
        SettingsGroup(stringResource(Res.string.settings_pin_section)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pinAction = PinAction.Change }) {
                    Text(stringResource(Res.string.settings_change_pin))
                }
                OutlinedButton(onClick = onLockNow) { Text(stringResource(Res.string.settings_lock_now)) }
            }
        }
    }
    // Диалоги — отдельные окна поверх экрана блокировки, поэтому во время блокировки не показываются
    if (!LocalAppLocked.current) PinActionDialog(lock, pinAction, onDone = { pinAction = null })
}

@Composable
private fun PinActionDialog(lock: LockController, action: PinAction?, onDone: () -> Unit) {
    when (action) {
        PinAction.Enable -> PinDialog(
            title = Res.string.pin_create_title,
            askCurrent = false,
            askNew = true,
            onConfirm = { _, new -> true.also { lock.enable(new) } },
            onDismiss = onDone,
        )
        PinAction.Disable -> PinDialog(
            title = Res.string.pin_disable_title,
            askCurrent = true,
            askNew = false,
            onConfirm = { current, _ -> lock.disable(current) },
            onDismiss = onDone,
        )
        PinAction.Change -> PinDialog(
            title = Res.string.pin_change_title,
            askCurrent = true,
            askNew = true,
            onConfirm = { current, new -> lock.changePin(current, new) },
            onDismiss = onDone,
        )
        null -> Unit
    }
}

/**
 * Страница «Экран блокировки»: сплошной или размытый, сила размытия, сворачивание фото.
 * Значение ползунка сохраняется, когда его отпускают, а не на каждом шаге.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LockScreenPage(lock: LockController) {
    if (!lock.enabled) Hint(stringResource(Res.string.settings_lock_screen_hint))
    SettingsGroup(stringResource(Res.string.settings_lock_style)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            LockStyle.entries.forEach { style ->
                FilterChip(
                    selected = lock.style == style,
                    onClick = { lock.changeStyle(style) },
                    label = { Text(stringResource(style.label)) },
                )
            }
        }
        if (lock.style == LockStyle.Blurred) {
            var radius by remember { mutableStateOf(lock.blurRadius.toFloat()) }
            FieldLabel(stringResource(Res.string.settings_blur, radius.roundToInt()))
            Slider(
                value = radius,
                onValueChange = { radius = it },
                onValueChangeFinished = { lock.changeBlurRadius(radius.roundToInt()) },
                valueRange = LockController.MIN_BLUR.toFloat()..LockController.MAX_BLUR.toFloat(),
                steps = (LockController.MAX_BLUR - LockController.MIN_BLUR) / LockController.BLUR_STEP - 1,
            )
        }
    }
    SettingsGroup(stringResource(Res.string.settings_photos_section)) {
        SwitchRow(stringResource(Res.string.settings_lock_collapse_photos), lock.collapsePhotos) {
            lock.changeCollapsePhotos(it)
        }
    }
}

private val LockStyle.label: StringResource
    get() = when (this) {
        LockStyle.Solid -> Res.string.lock_style_solid
        LockStyle.Blurred -> Res.string.lock_style_blurred
    }
