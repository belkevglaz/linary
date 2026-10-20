package ru.b3lka.linary.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Палитра Material 3 с бирюзовым акцентом (seed ≈ #006874).
 * Тёмная схема снята со скриншотов Diarium на Android.
 * Цвета можно перегенерировать в Material Theme Builder и заменить здесь целиком.
 */

internal val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF82D3E0),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF9EEFFD),
    secondary = Color(0xFFB1CBD0),
    onSecondary = Color(0xFF1C3438),
    secondaryContainer = Color(0xFF334B4F),
    onSecondaryContainer = Color(0xFFCDE7EC),
    tertiary = Color(0xFFBAC6EA),
    onTertiary = Color(0xFF24304D),
    background = Color(0xFF0E1415),
    onBackground = Color(0xFFDEE3E5),
    surface = Color(0xFF0E1415),
    onSurface = Color(0xFFDEE3E5),
    surfaceVariant = Color(0xFF3F484A),
    onSurfaceVariant = Color(0xFFBFC8CA),
    outline = Color(0xFF899294),
    outlineVariant = Color(0xFF3F484A),
    surfaceContainerLowest = Color(0xFF090F10),
    surfaceContainerLow = Color(0xFF171D1E),
    surfaceContainer = Color(0xFF1B2122),
    surfaceContainerHigh = Color(0xFF252B2C),
    surfaceContainerHighest = Color(0xFF303638),
    inverseSurface = Color(0xFFDEE3E5),
    inverseOnSurface = Color(0xFF2B3133),
    error = Color(0xFFFFB4AB),
)

internal val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9EEFFD),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF4A6267),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDE7EC),
    onSecondaryContainer = Color(0xFF051F23),
    tertiary = Color(0xFF525E7D),
    background = Color(0xFFF5FAFB),
    onBackground = Color(0xFF171D1E),
    surface = Color(0xFFF5FAFB),
    onSurface = Color(0xFF171D1E),
    surfaceVariant = Color(0xFFDBE4E6),
    onSurfaceVariant = Color(0xFF3F484A),
    outline = Color(0xFF6F797A),
    outlineVariant = Color(0xFFBFC8CA),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F6),
    surfaceContainer = Color(0xFFE9EFF0),
    surfaceContainerHigh = Color(0xFFE3E9EA),
    surfaceContainerHighest = Color(0xFFDEE3E5),
    inverseSurface = Color(0xFF2B3133),
    inverseOnSurface = Color(0xFFECF2F3),
)

/**
 * Цвета, которые можно выбрать для тега (сохраняются в формате Diarium `#AARRGGBB`).
 */
internal val TagPalette: List<Color> = listOf(
    Color(0xFFE53935), // красный
    Color(0xFFD81B60), // розовый
    Color(0xFF8E24AA), // фиолетовый
    Color(0xFF5E35B1), // тёмно-фиолетовый
    Color(0xFF3949AB), // индиго
    Color(0xFF1E88E5), // синий
    Color(0xFF039BE5), // голубой
    Color(0xFF00ACC1), // бирюзовый
    Color(0xFF00897B), // сине-зелёный
    Color(0xFF43A047), // зелёный
    Color(0xFF7CB342), // салатовый
    Color(0xFFC0CA33), // лаймовый
    Color(0xFFFDD835), // жёлтый
    Color(0xFFFFB300), // янтарный
    Color(0xFFFB8C00), // оранжевый
    Color(0xFFF4511E), // тёмно-оранжевый
    Color(0xFF6D4C41), // коричневый
    Color(0xFF757575), // серый
)

/**
 * Текст на цветном теге: светлый на тёмном цвете, тёмный на светлом.
 */
internal val TagTextOnDark = Color(0xFFFFFFFF)
internal val TagTextOnLight = Color(0xFF1B1B1B)
