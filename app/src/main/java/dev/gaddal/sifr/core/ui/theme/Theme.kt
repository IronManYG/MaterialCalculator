package dev.gaddal.sifr.core.ui.theme

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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dev.gaddal.sifr.core.domain.settings.SifrPalette
import dev.gaddal.sifr.core.domain.settings.ThemeMode

@Composable
fun SifrTheme(
    palette: SifrPalette = SifrPalette.Layl,
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val context = LocalContext.current
    val sifr = remember(palette, dark, context) {
        if (palette == SifrPalette.Dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            dynamicToSifrColors(scheme, dark)
        } else {
            sifrColorsFor(palette, dark)
        }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !sifr.statusBarLightIcons
        }
    }

    CompositionLocalProvider(LocalSifrColors provides sifr) {
        MaterialTheme(
            colorScheme = sifr.toMaterialColorScheme(dark),
            typography = sifrTypography(sifr),
            content = content,
        )
    }
}

/** Bridge SifrColors → M3 ColorScheme so stock components (Scaffold, TopAppBar,
 *  Switch, RadioButton, TextField cursor) inherit the palette. Lossy by design. */
fun SifrColors.toMaterialColorScheme(dark: Boolean): androidx.compose.material3.ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = accentInk,
        secondary = accent,
        background = backgroundFlat,
        onBackground = text,
        surface = surface,
        onSurface = text,
        onSurfaceVariant = dim,
        surfaceVariant = surface,
        outline = hairline,
        outlineVariant = hairline,
        error = displayError,
        secondaryContainer = surface,
        onSecondaryContainer = displayExpression,
    )
}
