package com.pbweather.weather.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ThemeMode { LIGHT, DARK, HIGH_CONTRAST }
enum class Palette { INDIGO, AMBER, TEAL, SLATE }
enum class FontSize(val scale: Float) { NORMAL(1f), LARGE(1.18f), EXTRA_LARGE(1.42f) }

data class WeatherColors(
    val ground: Color,
    val surface: Color,
    val tonal: Color,
    val border: Color,
    val text: Color,
    val secondary: Color,
    val tertiary: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentOnSoft: Color,
    val onAccent: Color,
    val warm: Color,
    val cool: Color,
    val rain: Color,
    val rainText: Color,
    val pm25: Color,
    val warning: Color,
    val error: Color,
    val hero: Color
)

fun weatherColors(mode: ThemeMode, palette: Palette): WeatherColors {
    if (mode == ThemeMode.HIGH_CONTRAST) {
        return WeatherColors(
            Color.Black, Color(0xFF0A0A0A), Color(0xFF141414), Color.White,
            Color.White, Color(0xFFF5F5F5), Color(0xFFE0E0E0), Color(0xFFFFE600),
            Color(0xFF1A1A00), Color(0xFFFFE600), Color.Black, Color(0xFFFF9966),
            Color(0xFF7AD7FF), Color(0xFF7AD7FF), Color(0xFF7AD7FF), Color(0xFFF0ABFC),
            Color(0xFFFFE600), Color(0xFFFF7A7A), Color(0xFF0A0A0A)
        )
    }
    val dark = mode == ThemeMode.DARK
    val i = palette.ordinal
    fun pick(light: LongArray, night: LongArray) = Color((if (dark) night else light)[i])
    return WeatherColors(
        pick(longArrayOf(0xFFF2F5FA, 0xFFF7F5F1, 0xFFF2F6F7, 0xFFF4F5F7), longArrayOf(0xFF0C1322, 0xFF161310, 0xFF0B1618, 0xFF111317)),
        if (dark) pick(longArrayOf(), longArrayOf(0xFF152033, 0xFF211C17, 0xFF132326, 0xFF1A1D24)) else Color.White,
        pick(longArrayOf(0xFFE7EEF9, 0xFFEFEAE1, 0xFFE3EFEF, 0xFFE8EBF0), longArrayOf(0xFF1D2B44, 0xFF2D261E, 0xFF1B3135, 0xFF242832)),
        pick(longArrayOf(0xFFD6E0EE, 0xFFDFD7CA, 0xFFCFE0E2, 0xFFD6DCE5), longArrayOf(0xFF283854, 0xFF3D3328, 0xFF243F44, 0xFF303644)),
        if (dark) Color(0xFFF1F5F9) else Color(0xFF0F172A),
        if (dark) Color(0xFFCBD5E1) else Color(0xFF3F4F66),
        if (dark) Color(0xFF9FB0C7) else Color(0xFF52627A),
        pick(longArrayOf(0xFF2B4ACB, 0xFF9A5610, 0xFF0F766E, 0xFF273449), longArrayOf(0xFF84A9FF, 0xFFF2B45C, 0xFF5EEAD4, 0xFFCBD5E1)),
        pick(longArrayOf(0xFFE0E7FF, 0xFFFAEACC, 0xFFCCFBF1, 0xFFE2E8F0), longArrayOf(0xFF1F3263, 0xFF432F17, 0xFF134240, 0xFF2A313F)),
        pick(longArrayOf(0xFF1E3A8A, 0xFF6B3907, 0xFF115E59, 0xFF1E293B), longArrayOf(0xFFE0E7FF, 0xFFFDE6C1, 0xFFCCFBF1, 0xFFF1F5F9)),
        if (dark) pick(longArrayOf(), longArrayOf(0xFF081226, 0xFF1C1206, 0xFF04211E, 0xFF0F172A)) else Color.White,
        if (dark) Color(0xFFFB923C) else Color(0xFFB9380B),
        if (dark) Color(0xFF60A5FA) else Color(0xFF0369A1),
        if (dark) Color(0xFF38BDF8) else Color(0xFF0284C7),
        if (dark) Color(0xFF7CB3FF) else Color(0xFF0369A1),
        if (dark) Color(0xFFC4B5FD) else Color(0xFF7C3AED),
        if (dark) Color(0xFFFDE68A) else Color(0xFF78350F),
        if (dark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
        pick(longArrayOf(0xFFDCE9FB, 0xFFECE8E0, 0xFFDFEEEF, 0xFFE8ECF2), longArrayOf(0xFF182B4D, 0xFF282119, 0xFF132C30, 0xFF1F242E))
    )
}

object Dims {
    val zero = 0.dp
    val hairline = 1.dp
    val strong = 2.dp
    val progress = 3.dp
    val tiny = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val gutter = 16.dp
    val section = 14.dp
    val large = 20.dp
    val sheet = 18.dp
    val radius = 16.dp
    val sheetRadius = 20.dp
    val widgetRadius = 22.dp
    val pill = 999.dp
    val touch = 48.dp
    val topBar = 58.dp
    val navBar = 68.dp
    val hourly = 60.dp
    val chart = 210.dp
    val trend = 184.dp
    val spline = 44.dp
    val symbol = 66.dp
    val maxWidth = 760.dp
    val narrow = 360.dp
    val heroThreshold = 320.dp
}
object WeatherTheme {
    val colors: WeatherColors @Composable get() = LocalWeatherColors.current
}
private val LocalWeatherColors = staticCompositionLocalOf { weatherColors(ThemeMode.LIGHT, Palette.INDIGO) }

@Composable
fun WeatherTheme(
    mode: ThemeMode = ThemeMode.LIGHT,
    palette: Palette = Palette.INDIGO,
    font: FontSize = FontSize.NORMAL,
    content: @Composable () -> Unit
) {
    val colors = weatherColors(mode, palette)
    fun style(size: Float, weight: FontWeight = FontWeight.Normal) = TextStyle(
        fontSize = (size * font.scale).sp,
        lineHeight = (size * font.scale * 1.4f).sp,
        fontWeight = weight,
        fontFeatureSettings = "tnum"
    )
    val type = Typography(
        displayLarge = style(65.6f, FontWeight.Light), headlineMedium = style(36.8f),
        titleLarge = style(16.8f, FontWeight.Bold), titleMedium = style(14.3f, FontWeight.Bold),
        titleSmall = style(13.8f, FontWeight.SemiBold), bodyLarge = style(13.2f), bodyMedium = style(13.2f),
        bodySmall = style(11.7f), labelLarge = style(13.2f, FontWeight.SemiBold),
        labelMedium = style(11.7f), labelSmall = style(11.4f)
    )
    val scheme = if (mode ==
        ThemeMode.LIGHT
    ) {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.ground,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            onSurfaceVariant = colors.secondary,
            secondaryContainer = colors.accentSoft,
            onSecondaryContainer = colors.accentOnSoft,
            error = colors.error,
            outlineVariant = colors.border
        )
    } else {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.ground,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            onSurfaceVariant = colors.secondary,
            secondaryContainer = colors.accentSoft,
            onSecondaryContainer = colors.accentOnSoft,
            error = colors.error,
            outlineVariant = colors.border
        )
    }
    CompositionLocalProvider(LocalWeatherColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = type, content = content)
    }
}
