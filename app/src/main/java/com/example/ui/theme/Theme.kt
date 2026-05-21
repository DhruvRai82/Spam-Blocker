package com.example.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = ShieldPrimaryDark,
    secondary = ShieldSecondaryDark,
    background = ShieldBackgroundDark,
    surface = ShieldSurfaceDark,
    onPrimary = ShieldOnPrimaryDark,
    onBackground = ShieldOnBackgroundDark,
    onSurface = ShieldOnSurfaceDark,
    primaryContainer = Color(0xFF381E72),
    onPrimaryContainer = Color(0xFFEADDFF),
    surfaceVariant = Color(0xFF313033),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF49454F)
)

private val LightColorScheme = lightColorScheme(
    primary = ShieldPrimaryLight,
    secondary = ShieldSecondaryLight,
    background = ShieldBackgroundLight,
    surface = ShieldSurfaceLight,
    onPrimary = ShieldOnPrimaryLight,
    onBackground = ShieldOnBackgroundLight,
    onSurface = ShieldOnSurfaceLight,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to true for the Elegant Dark design
  dynamicColor: Boolean = false, // Disable dynamic colors so our Elegant Dark styling is preserved
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
