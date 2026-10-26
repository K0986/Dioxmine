package io.github.rhythmcache.dioxamine.core

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.rhythmcache.dioxamine.R

enum class AppTheme(@StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
    AMOLED(R.string.theme_amoled)
}

val DioxamineDarkColorScheme = darkColorScheme(
    primary = Color(0xFF63E2E8),
    onPrimary = Color(0xFF003739),
    primaryContainer = Color(0xFF004F53),
    onPrimaryContainer = Color(0xFFA2F4F7),
    inversePrimary = Color(0xFF00696E),
    secondary = Color(0xFFA7DBB4),
    onSecondary = Color(0xFF123723),
    secondaryContainer = Color(0xFF294E39),
    onSecondaryContainer = Color(0xFFC2F8CE),
    tertiary = Color(0xFFD0BCFF),
    onTertiary = Color(0xFF37265E),
    tertiaryContainer = Color(0xFF4D3D76),
    onTertiaryContainer = Color(0xFFE9DDFF),
    background = Color(0xFF121318),
    onBackground = Color(0xFFE5E3EB),
    surface = Color(0xFF121318),
    onSurface = Color(0xFFE5E3EB),
    surfaceVariant = Color(0xFF34343E),
    onSurfaceVariant = Color(0xFFBFC5CE),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1A1B22),
    surfaceContainer = Color(0xFF202129),
    surfaceContainerHigh = Color(0xFF292B35),
    surfaceContainerHighest = Color(0xFF343640),
    outline = Color(0xFF8B929E),
    outlineVariant = Color(0xFF3D434E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

val DioxamineLightColorScheme = lightColorScheme(
    primary = Color(0xFF00696E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA2F4F7),
    onPrimaryContainer = Color(0xFF002021),
    inversePrimary = Color(0xFF63E2E8),
    secondary = Color(0xFF356347),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB9F0C9),
    onSecondaryContainer = Color(0xFF00210F),
    tertiary = Color(0xFF65558F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE9DDFF),
    onTertiaryContainer = Color(0xFF210F48),
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF191C23),
    surface = Color(0xFFF8F9FC),
    onSurface = Color(0xFF191C23),
    surfaceVariant = Color(0xFFE0E3EB),
    onSurfaceVariant = Color(0xFF444A55),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F2F7),
    surfaceContainer = Color(0xFFECEEF4),
    surfaceContainerHigh = Color(0xFFE5E7EF),
    surfaceContainerHighest = Color(0xFFDFE2EB),
    outline = Color(0xFF747B86),
    outlineVariant = Color(0xFFC4C8D2),
)

private fun ColorScheme.toAmoledScheme(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF1A1A1A),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF070707),
    surfaceContainer = Color(0xFF0D0D0D),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF262626)
)

val LocalDarkTheme = staticCompositionLocalOf { false }

private tailrec fun Context.hostActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.hostActivity()
    else -> null
}

@Composable
fun DioxamineTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    useMonet: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK, AppTheme.AMOLED -> true
    }

    val context = LocalContext.current
    val baseColorScheme = when {
        useMonet && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DioxamineDarkColorScheme
        else -> DioxamineLightColorScheme
    }

    val colorScheme = if (appTheme == AppTheme.AMOLED) {
        baseColorScheme.toAmoledScheme()
    } else {
        baseColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val barStyle = if (darkTheme) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            }
            (context.hostActivity() as? ComponentActivity)?.enableEdgeToEdge(
                statusBarStyle = barStyle,
                navigationBarStyle = barStyle,
            )
            context.hostActivity()?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DioxamineTypography,
            shapes = DioxamineShapes,
            content = content
        )
    }
}
