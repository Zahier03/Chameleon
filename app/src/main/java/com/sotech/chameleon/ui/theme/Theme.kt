package com.sotech.chameleon.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.sotech.chameleon.data.AppColorScheme
import com.sotech.chameleon.data.ThemeMode
import com.sotech.chameleon.data.ThemeSettings

private val LightColorScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight
)

private val DarkColorScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark
)

// Ocean Theme
private val OceanLightColorScheme = lightColorScheme(
    primary = Color(0xFF006493),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCAE6FF),
    onPrimaryContainer = Color(0xFF001E30),
    secondary = Color(0xFF50606E),
    background = Color(0xFFF8FAFD),
    surface = Color(0xFFF8FAFD)
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8FCDFF),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004B70),
    onPrimaryContainer = Color(0xFFCAE6FF),
    secondary = Color(0xFFB8C8D8),
    background = Color(0xFF0F1418),
    surface = Color(0xFF0F1418)
)

// Forest Theme
private val ForestLightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0A290C),
    secondary = Color(0xFF52634F),
    background = Color(0xFFF5FBF5),
    surface = Color(0xFFF5FBF5)
)

private val ForestDarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003A03),
    primaryContainer = Color(0xFF005005),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFBCCBB8),
    background = Color(0xFF0E150E),
    surface = Color(0xFF0E150E)
)

// Sunset Theme
private val SunsetLightColorScheme = lightColorScheme(
    primary = Color(0xFFE65100),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = Color(0xFF391A00),
    secondary = Color(0xFF795548),
    background = Color(0xFFFFF8F5),
    surface = Color(0xFFFFF8F5)
)

private val SunsetDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFAB78),
    onPrimary = Color(0xFF5A1E00),
    primaryContainer = Color(0xFF7F2E00),
    onPrimaryContainer = Color(0xFFFFE0B2),
    secondary = Color(0xFFCCB5A7),
    background = Color(0xFF1A0F0A),
    surface = Color(0xFF1A0F0A)
)

// Monochrome Theme
private val MonochromeLightColorScheme = lightColorScheme(
    primary = Color(0xFF424242),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Color(0xFF141414),
    secondary = Color(0xFF616161),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFAFAFA)
)

private val MonochromeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBDBDBD),
    onPrimary = Color(0xFF212121),
    primaryContainer = Color(0xFF424242),
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Color(0xFF9E9E9E),
    background = Color(0xFF121212),
    surface = Color(0xFF121212)
)

@Immutable
data class CustomColors(
    val appTitleGradientColors: List<Color> = listOf(),
    val tabHeaderBgColor: Color = Color.Transparent,
    val modelCardBgColor: Color = Color.Transparent,
    val modelBgColors: List<Color> = listOf(),
    val modelBgGradientColors: List<List<Color>> = listOf(),
    val modelIconColors: List<Color> = listOf(),
    val modelIconShapeBgColor: Color = Color.Transparent,
    val homeBottomGradient: List<Color> = listOf(),
    val userBubbleBgColor: Color = Color.Transparent,
    val agentBubbleBgColor: Color = Color.Transparent,
    val linkColor: Color = Color.Transparent,
    val successColor: Color = Color.Transparent,
    val recordButtonBgColor: Color = Color.Transparent,
    val waveFormBgColor: Color = Color.Transparent,
    val modelInfoIconColor: Color = Color.Transparent
)

val LocalCustomColors = staticCompositionLocalOf { CustomColors() }

val lightCustomColors = CustomColors(
    appTitleGradientColors = listOf(Color(0xFF85B1F8), Color(0xFF3174F1)),
    tabHeaderBgColor = Color(0xFF3174F1),
    modelCardBgColor = surfaceContainerLowestLight,
    modelBgColors = listOf(
        Color(0xFFFFF5F5), // red
        Color(0xFFF4FBF6), // green
        Color(0xFFF1F6FE), // blue
        Color(0xFFFFFBF0)  // yellow
    ),
    modelBgGradientColors = listOf(
        listOf(Color(0xFFE25F57), Color(0xFFDB372D)), // red
        listOf(Color(0xFF41A15F), Color(0xFF128937)), // green
        listOf(Color(0xFF669DF6), Color(0xFF3174F1)), // blue
        listOf(Color(0xFFFDD45D), Color(0xFFCAA12A))  // yellow
    ),
    modelIconColors = listOf(
        Color(0xFFD93025), // red
        Color(0xFF34A853), // green
        Color(0xFF1967D2), // blue
        Color(0xFFE37400)  // yellow
    ),
    modelIconShapeBgColor = Color.White,
    homeBottomGradient = listOf(Color(0x00F8F9FF), Color(0xffFFEFC9)),
    agentBubbleBgColor = Color(0xFFe9eef6),
    userBubbleBgColor = Color(0xFF32628D),
    linkColor = Color(0xFF32628D),
    successColor = Color(0xff3d860b),
    recordButtonBgColor = Color(0xFFEE675C),
    waveFormBgColor = Color(0xFFaaaaaa),
    modelInfoIconColor = Color(0xFFCCCCCC)
)

val darkCustomColors = CustomColors(
    appTitleGradientColors = listOf(Color(0xFF85B1F8), Color(0xFF3174F1)),
    tabHeaderBgColor = Color(0xFF3174F1),
    modelCardBgColor = surfaceContainerHighDark,
    modelBgColors = listOf(
        Color(0xFF181210), // red
        Color(0xFF131711), // green
        Color(0xFF191924), // blue
        Color(0xFF1A1813)  // yellow
    ),
    modelBgGradientColors = listOf(
        listOf(Color(0xFFE25F57), Color(0xFFDB372D)), // red
        listOf(Color(0xFF41A15F), Color(0xFF128937)), // green
        listOf(Color(0xFF669DF6), Color(0xFF3174F1)), // blue
        listOf(Color(0xFFFDD45D), Color(0xFFCAA12A))  // yellow
    ),
    modelIconColors = listOf(
        Color(0xFFFFB4AB), // red
        Color(0xFF6DD58C), // green
        Color(0xFFAAC7FF), // blue
        Color(0xFFFFB955)  // yellow
    ),
    modelIconShapeBgColor = Color(0xFF202124),
    homeBottomGradient = listOf(Color(0x00F8F9FF), Color(0x1AF6AD01)),
    agentBubbleBgColor = Color(0xFF1b1c1d),
    userBubbleBgColor = Color(0xFF1f3760),
    linkColor = Color(0xFF9DCAFC),
    successColor = Color(0xFFA1CE83),
    recordButtonBgColor = Color(0xFFEE675C),
    waveFormBgColor = Color(0xFFaaaaaa),
    modelInfoIconColor = Color(0xFFCCCCCC)
)

val MaterialTheme.customColors: CustomColors
    @Composable @ReadOnlyComposable
    get() = LocalCustomColors.current

@Composable
fun ChameleonTheme(
    themeSettings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSettings.isDarkMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        themeSettings.useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            when (themeSettings.colorScheme) {
                AppColorScheme.OCEAN -> if (darkTheme) OceanDarkColorScheme else OceanLightColorScheme
                AppColorScheme.FOREST -> if (darkTheme) ForestDarkColorScheme else ForestLightColorScheme
                AppColorScheme.SUNSET -> if (darkTheme) SunsetDarkColorScheme else SunsetLightColorScheme
                AppColorScheme.MONOCHROME -> if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme
                else -> if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
    }

    val customColorsPalette = if (darkTheme) darkCustomColors else lightCustomColors
    val typography = scaledTypography(themeSettings.textScale)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalCustomColors provides customColorsPalette,
        LocalTextScale provides themeSettings.textScale
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}