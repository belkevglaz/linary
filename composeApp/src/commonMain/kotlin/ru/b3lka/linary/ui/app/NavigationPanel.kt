package ru.b3lka.linary.ui.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.presentation.state.Section
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_quit
import ru.b3lka.linary.resources.action_settings
import ru.b3lka.linary.resources.app_icon
import ru.b3lka.linary.ui.common.Tooltip
import ru.b3lka.linary.ui.common.icon
import ru.b3lka.linary.ui.common.title

/**
 * Боковая панель разделов — аналог выдвижного меню Diarium: иконка приложения сверху,
 * разделы с «таблеткой» выделения, настройки и выход внизу.
 *
 * @param current выбранный раздел; null — открыты настройки.
 * @param onQuit завершить приложение; null — кнопки выхода нет (закрытие окна и так завершает приложение).
 */
@Composable
internal fun NavigationPanel(
    current: Section?,
    onSelect: (Section) -> Unit,
    onOpenSettings: () -> Unit,
    onQuit: (() -> Unit)?,
    settingsOpen: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxHeight()) {
        Column(
            Modifier
                .width(96.dp)
                .fillMaxHeight()
                .background(colors.surfaceContainerLow)
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(
                painterResource(Res.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(16.dp))
            Section.entries.forEach { section ->
                val selected = current == section
                RailItem(
                    icon = section.icon(selected),
                    label = stringResource(section.title),
                    selected = selected,
                    onClick = { onSelect(section) },
                )
            }
            Spacer(Modifier.weight(1f))
            val settingsLabel = stringResource(Res.string.action_settings)
            Tooltip(settingsLabel) {
                RailItem(
                    icon = Icons.Outlined.Settings,
                    label = null,
                    description = settingsLabel,
                    selected = settingsOpen,
                    onClick = onOpenSettings,
                )
            }
            if (onQuit != null) {
                val quitLabel = stringResource(Res.string.action_quit)
                Tooltip("$quitLabel (Ctrl+Q)") {
                    RailItem(
                        icon = Icons.AutoMirrored.Outlined.Logout,
                        label = null,
                        description = quitLabel,
                        selected = false,
                        onClick = onQuit,
                    )
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.outlineVariant.copy(alpha = 0.6f)))
    }
}

/**
 * Пункт панели: иконка в «таблетке» и подпись под ней (до двух строк).
 * Выбранный пункт — тональная заливка, при наведении — лёгкая подсветка.
 */
@Composable
private fun RailItem(
    icon: ImageVector,
    label: String?,
    selected: Boolean,
    onClick: () -> Unit,
    description: String? = label,
) {
    val colors = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pill by animateColorAsState(
        when {
            selected -> colors.secondaryContainer
            hovered -> colors.onSurface.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.width(56.dp).height(32.dp).clip(CircleShape).background(pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
        if (label != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) colors.onSurface else colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
