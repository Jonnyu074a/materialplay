package com.materialplay.launcher.ui.theme

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import android.app.Activity
import androidx.compose.runtime.staticCompositionLocalOf
import com.materialplay.launcher.data.settings.AppSettings
import com.materialplay.launcher.data.settings.ThemeMode
import java.util.Locale

private fun darkScheme(accent: Color): androidx.compose.material3.ColorScheme = darkColorScheme(
    primary = accent,
    onPrimary = Color(0xFF04110F),
    primaryContainer = accent.copy(alpha = 0.22f),
    onPrimaryContainer = accent,
    secondary = accent.copy(alpha = 0.85f),
    onSecondary = Color(0xFF07120F),
    tertiary = Color(0xFF9BB4FF),
    background = Ink,
    onBackground = Snow,
    surface = InkElevated,
    onSurface = Snow,
    surfaceVariant = InkCard,
    onSurfaceVariant = Fog,
    surfaceContainerLowest = Color(0xFF07090C),
    surfaceContainerLow = Ink,
    surfaceContainer = Color(0xFF101319),
    surfaceContainerHigh = InkElevated,
    surfaceContainerHighest = InkCard,
    outline = InkOutline,
    outlineVariant = Color(0xFF20252D),
    error = Color(0xFFFF7A85),
    onError = Color(0xFF2A0508)
)

private fun lightScheme(accent: Color): androidx.compose.material3.ColorScheme = lightColorScheme(
    primary = accent.copy(alpha = 1f),
    onPrimary = Color(0xFF04201B),
    primaryContainer = accent.copy(alpha = 0.35f),
    onPrimaryContainer = Color(0xFF06332C),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF11151B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF11151B),
    surfaceVariant = Color(0xFFEDF1F5),
    onSurfaceVariant = Color(0xFF55606D),
    outline = Color(0xFFCBD3DC),
    outlineVariant = Color(0xFFE1E7ED),
    tertiary = Color(0xFF6C7CFF)
)

@Composable
fun MaterialPlayTheme(
    settings: AppSettings,
    gaming: Boolean = false,
    content: @Composable () -> Unit
) {
    // Gaming Mode is a dark, console-style surface regardless of the Normal Mode theme.
    val dark = gaming || when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val accent = accentFor(settings.accentId)
    val context = LocalContext.current
    val scheme = when {
        !gaming && settings.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkScheme(accent)
        else -> lightScheme(accent)
    }
    val baseDensity = LocalDensity.current
    // Gaming Mode has its own Gaming Density (applied in GamingActivity). Applying the
    // Normal Mode UI scale here as well would stack both factors.
    val scaledDensity = if (gaming) baseDensity
    else Density(baseDensity.density * settings.uiScale.factor, baseDensity.fontScale)
    // Apply the selected language to the resources used by stringResource without
    // replacing LocalContext (which other code casts to Activity).
    val locale = when (settings.language) {
        com.materialplay.launcher.data.settings.AppLanguage.SYSTEM -> null
        com.materialplay.launcher.data.settings.AppLanguage.SPANISH -> Locale("es")
        com.materialplay.launcher.data.settings.AppLanguage.ENGLISH -> Locale.ENGLISH
    }
    val localizedContext = if (locale == null) context else {
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalDensity provides scaledDensity
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalContext provides localizedContext
        ) {
            MaterialTheme(
                colorScheme = scheme,
                typography = if (gaming) GamingTypography else PlayTypography,
                shapes = PlayShapes,
                content = content
            )
        }
    }
}

val LocalActivity = staticCompositionLocalOf<android.app.Activity?> { null }