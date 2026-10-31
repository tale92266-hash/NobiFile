package com.nobifile.manager.ui.theme

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

private val BlueLight = lightColorScheme(
    primary = Color(0xFF2563EB), onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE), onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF475569), surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9), onSurface = Color(0xFF0F172A), onSurfaceVariant = Color(0xFF475569)
)
private val BlueDark = darkColorScheme(
    primary = Color(0xFF60A5FA), onPrimary = Color(0xFF0B1220),
    primaryContainer = Color(0xFF1E3A8A), onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8), surface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFF1E293B), onSurface = Color(0xFFF8FAFC), onSurfaceVariant = Color(0xFFCBD5E1)
)
private val Amoled = darkColorScheme(
    primary = Color(0xFF60A5FA), onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E3A8A), onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF94A3B8), surface = Color.Black,
    surfaceVariant = Color(0xFF111111), onSurface = Color.White, onSurfaceVariant = Color(0xFFCBD5E1),
    background = Color.Black
)

private fun withAccent(base: androidx.compose.material3.ColorScheme, accent: String): androidx.compose.material3.ColorScheme {
    val primary = when (accent) {
        "violet" -> Color(0xFF8B5CF6)
        "teal" -> Color(0xFF0D9488)
        "amber" -> Color(0xFFD97706)
        else -> base.primary
    }
    return base.copy(primary = primary)
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
            withAccent(dyn, accent)
        }
        dark -> withAccent(BlueDark, accent)
        else -> withAccent(BlueLight, accent)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
