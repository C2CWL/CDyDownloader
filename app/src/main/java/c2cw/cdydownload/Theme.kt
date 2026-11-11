package c2cw.cdydownload

import android.os.Build
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Neutral surfaces. The only color in the app comes from the chosen accent. */
private val Day = lightColorScheme(
    background = Color(0xFFF3F4F6), onBackground = Color(0xFF1A1B1E),
    surface = Color.White, onSurface = Color(0xFF1A1B1E),
    surfaceVariant = Color(0xFFE9EAEE), onSurfaceVariant = Color(0xFF686B73),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F8FA),
    surfaceContainer = Color(0xFFEDEEF1), surfaceContainerHigh = Color(0xFFE6E7EB),
    surfaceContainerHighest = Color(0xFFDFE0E5),
    outline = Color(0xFFB9BBC2), outlineVariant = Color(0xFFE1E2E6),
    error = Color(0xFFC62E3A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD9), onErrorContainer = Color(0xFF5C0A12)
)
private val Night = darkColorScheme(
    background = Color(0xFF0E0E10), onBackground = Color(0xFFF1F1F3),
    surface = Color(0xFF19191C), onSurface = Color(0xFFF1F1F3),
    surfaceVariant = Color(0xFF26262A), onSurfaceVariant = Color(0xFFA6A7AD),
    surfaceContainerLowest = Color(0xFF0B0B0C), surfaceContainerLow = Color(0xFF151517),
    surfaceContainer = Color(0xFF222225), surfaceContainerHigh = Color(0xFF2A2A2E),
    surfaceContainerHighest = Color(0xFF333337),
    outline = Color(0xFF5B5C62), outlineVariant = Color(0xFF2F2F34),
    error = Color(0xFFFF8A8F), onError = Color(0xFF4A0710),
    errorContainer = Color(0xFF6B1520), onErrorContainer = Color(0xFFFFDAD9)
)

private fun ColorScheme.withAccent(t: AccentTones): ColorScheme = copy(
    primary = Color(t.primary), onPrimary = Color(t.onPrimary),
    primaryContainer = Color(t.container), onPrimaryContainer = Color(t.onContainer),
    inversePrimary = Color(t.primary), surfaceTint = Color(t.primary),
    // Material chips, switches and segmented buttons read secondary/tertiary; keep them on the accent.
    secondary = Color(t.primary), onSecondary = Color(t.onPrimary),
    secondaryContainer = Color(t.container), onSecondaryContainer = Color(t.onContainer),
    tertiary = Color(t.primary), onTertiary = Color(t.onPrimary),
    tertiaryContainer = Color(t.container), onTertiaryContainer = Color(t.onContainer)
)

@Composable internal fun accentSeed(mode: AccentMode = AccentPreference.mode): Int? = when(mode) {
    AccentMode.RANDOM -> AccentPreference.randomSeed
    AccentMode.CUSTOM -> AccentPreference.custom
    AccentMode.WALLPAPER -> null
}

@Composable fun DownloaderTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val base = if(dark) Night else Day
    val seed = accentSeed()
    val colors = if(seed == null) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val dynamic = if(dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            base.withAccent(AccentTones(dynamic.primary.toArgbInt(), dynamic.onPrimary.toArgbInt(),
                dynamic.primaryContainer.toArgbInt(), dynamic.onPrimaryContainer.toArgbInt(),
                dynamic.surfaceContainerHigh.toArgbInt()))
        } else base.withAccent(accentTones(FALLBACK_ACCENT, dark))
    } else base.withAccent(accentTones(seed, dark))
    val miuixColors = if(dark) top.yukonga.miuix.kmp.theme.darkColorScheme(
        primary = colors.primary, onPrimary = colors.onPrimary, primaryVariant = colors.primary,
        primaryContainer = colors.primary, onPrimaryContainer = colors.onPrimary,
        disabledPrimary = colors.primaryContainer, disabledPrimaryButton = colors.primaryContainer,
        background = colors.background, surface = colors.background,
        surfaceContainer = colors.surface, onSurfaceContainer = colors.onSurface, onSurface = colors.onSurface,
        onBackground = colors.onSurface, onSurfaceContainerVariant = colors.onSurfaceVariant
    ) else top.yukonga.miuix.kmp.theme.lightColorScheme(
        primary = colors.primary, onPrimary = colors.onPrimary, primaryVariant = colors.primary,
        primaryContainer = colors.primary, onPrimaryContainer = colors.onPrimary,
        disabledPrimary = colors.primaryContainer, disabledPrimaryButton = colors.primaryContainer,
        background = colors.background, surface = colors.background,
        surfaceContainer = colors.surface, onSurfaceContainer = colors.onSurface, onSurface = colors.onSurface,
        onBackground = colors.onSurface, onSurfaceContainerVariant = colors.onSurfaceVariant
    )
    MiuixTheme(colors = miuixColors) {
        MaterialTheme(colorScheme = colors, typography = Typography(
            headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
            headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
            titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
            titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
            labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
        ) { CompositionLocalProvider(LocalOverscrollFactory provides null, content = content) }
    }
}

private fun Color.toArgbInt(): Int {
    fun c(v: Float) = (v.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
    return (c(alpha) shl 24) or (c(red) shl 16) or (c(green) shl 8) or c(blue)
}
