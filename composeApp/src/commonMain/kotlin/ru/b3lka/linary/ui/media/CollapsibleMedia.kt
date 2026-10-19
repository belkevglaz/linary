package ru.b3lka.linary.ui.media

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ru.b3lka.linary.resources.Res
import ru.b3lka.linary.resources.action_collapse
import ru.b3lka.linary.resources.action_expand
import ru.b3lka.linary.resources.media_section
import ru.b3lka.linary.ui.common.LocalPhotoStrip
import ru.b3lka.linary.ui.common.Tooltip

/**
 * Сворачиваемая лента вложений: заголовок «Вложения: N» со стрелкой и сама лента под ним.
 * Свёрнута ли она, хранит [LocalPhotoStrip]; без него лента показывается всегда и без заголовка.
 */
@Composable
fun CollapsibleMedia(count: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val strip = LocalPhotoStrip.current
    if (strip == null) {
        Box(modifier) { content() }
        return
    }
    Column(modifier) {
        MediaSectionHeader(count, strip.expanded, onToggle = strip::toggle)
        AnimatedVisibility(
            visible = strip.expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Box(Modifier.padding(top = 8.dp)) { content() }
        }
    }
}

@Composable
private fun MediaSectionHeader(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val hint = stringResource(if (expanded) Res.string.action_collapse else Res.string.action_expand)
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Tooltip(hint) {
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(18.dp), tint = color)
            Text(
                stringResource(Res.string.media_section, count),
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color,
            )
        }
    }
}
