package ru.b3lka.linary.ui.common

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import ru.b3lka.linary.presentation.format.DateFormatter
import ru.b3lka.linary.presentation.media.MediaActions
import ru.b3lka.linary.presentation.media.MediaCache
import ru.b3lka.linary.presentation.media.PhotoStripState
import ru.b3lka.linary.presentation.platform.PlatformServices

val LocalMediaCache = staticCompositionLocalOf<MediaCache> { error("MediaCache is not provided") }

val LocalPlatform = staticCompositionLocalOf<PlatformServices> { error("PlatformServices is not provided") }

val LocalDateFormatter = staticCompositionLocalOf<DateFormatter> { error("DateFormatter is not provided") }

/**
 * Скачать вложение, сделать обложкой дня. null — действия недоступны (например, на экране приветствия).
 */
val LocalMediaActions = staticCompositionLocalOf<MediaActions?> { null }

/**
 * Свёрнута ли лента вложений. null — лента всегда развёрнута и без заголовка.
 */
val LocalPhotoStrip = staticCompositionLocalOf<PhotoStripState?> { null }

/**
 * Приложение заблокировано: диалоги с содержимым дневника в это время не показываются
 * (всплывающие окна рисуются поверх экрана блокировки).
 */
val LocalAppLocked = compositionLocalOf { false }
