package com.nobifile.manager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

// Every scheme defines background + onBackground explicitly. No screen may rely on
// the window background for text contrast: every root is a Surface painted with
// colorScheme.background, and text always uses onBackground/onSurface from the
// SAME scheme. This is the permanent fix for the V1 dark-mode invisible-text bug,
// where window background and Compose scheme could disagree.
private val BlueLight = lightColorScheme(
    primary = Color(0xFF2563EB), onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE), onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF475569), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9), onSurface = Color(0xFF0F172A), onSurfaceVariant = Color(0xFF475569),
    background = Color(0xFFF8FAFC), onBackground = Color(0xFF0F172A),
    surfaceTint = Color(0xFF2563EB)
)
private val BlueDark = darkColorScheme(
    primary = Color(0xFF60A5FA), onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFF1E3A8A), onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8), surface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFF1E293B), onSurface = Color(0xFFF8FAFC), onSurfaceVariant = Color(0xFFCBD5E1),
    background = Color(0xFF0B1220), onBackground = Color(0xFFF8FAFC),
    surfaceTint = Color(0xFF60A5FA)
)
private val Amoled = darkColorScheme(
    primary = Color(0xFF60A5FA), onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E3A8A), onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8), surface = Color.Black,
    surfaceVariant = Color(0xFF111111), onSurface = Color.White, onSurfaceVariant = Color(0xFFCBD5E1),
    background = Color.Black, onBackground = Color.White,
    surfaceTint = Color(0xFF60A5FA)
)

private fun withAccent(base: androidx.compose.material3.ColorScheme, accent: String): androidx.compose.material3.ColorScheme {
    val primary = when (accent) {
        "violet" -> Color(0xFF8B5CF6)
        "teal" -> Color(0xFF0D9488)
        "amber" -> Color(0xFFD97706)
        else -> base.primary
    }
    val primaryContainer = when (accent) {
        "violet" -> Color(0xFFEDE9FE)
        "teal" -> Color(0xFFCCFBF1)
        "amber" -> Color(0xFFFEF3C7)
        else -> base.primaryContainer
    }
    return base.copy(primary = primary, primaryContainer = primaryContainer)
}

@Composable
fun NobiFileTheme(
    themeMode: String,
    accent: String,
    useDynamic: Boolean = true,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        "light" -> false
        "dark", "amoled" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val scheme = when {
        themeMode == "amoled" -> withAccent(Amoled, accent)
        useDynamic && Build.VERSION.SDK_INT >= 31 -> {
            val dyn = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            // Keep dynamic hues but guarantee the text/background pair from a known-good
            // scheme of the same brightness, so wallpaper colours can never make text
            // unreadable on the app background.
            val base = if (dark) BlueDark else BlueLight
            withAccent(dyn, accent).copy(
                background = base.background,
                onBackground = base.onBackground,
                surface = base.surface,
                onSurface = base.onSurface,
                surfaceVariant = base.surfaceVariant,
                onSurfaceVariant = base.onSurfaceVariant
            )
        }
        dark -> withAccent(BlueDark, accent)
        else -> withAccent(BlueLight, accent)
    }
    val view = LocalView.current
    SideEffect {
        try {
            if (!view.isInEditMode) {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowInsetsControllerCompat(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        } catch (e: Exception) {
            // Status-bar styling is cosmetic; never let it crash the app.
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
