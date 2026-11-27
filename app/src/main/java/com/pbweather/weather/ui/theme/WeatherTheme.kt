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
) {
    val borderStrong: Color get() = when (border) {
        Color(0xFFD6E0EE) -> Color(0xFF94A3B8)
        Color(0xFFDFD7CA) -> Color(0xFF9F9483)
        Color(0xFFCFE0E2) -> Color(0xFF87A3A7)
        Color(0xFFD6DCE5) -> Color(0xFF8A96A8)
        Color(0xFF283854) -> Color(0xFF4B6186)
        Color(0xFF3D3328) -> Color(0xFF6E5F4E)
        Color(0xFF243F44) -> Color(0xFF487077)
        Color(0xFF303644) -> Color(0xFF5A6376)
        else -> border
    }
    val sunFill: Color get() = Color(0xFFF59E0B)
    val tonalStrong: Color get() = when (tonal) {
        Color(0xFFE7EEF9) -> Color(0xFFDAE4F5)
        Color(0xFFEFEAE1) -> Color(0xFFE4DCCD)
        Color(0xFFE3EFEF) -> Color(0xFFD4E6E6)
        Color(0xFFE8EBF0) -> Color(0xFFDCE1E8)
        Color(0xFF1D2B44) -> Color(0xFF253654)
        Color(0xFF2D261E) -> Color(0xFF393026)
        Color(0xFF1B3135) -> Color(0xFF233F44)
        Color(0xFF242832) -> Color(0xFF2E3340)
        else -> Color(0xFF1F1F1F)
    }
    val sunGlow: Color get() = Color(0xFFFBBF24)
    val sunHalo: Color get() = Color(0xFFFDE047)
    val statusOk: Color get() = when (text) {
        Color.White -> Color(0xFF7DFF9B)
        Color(0xFFF1F5F9) -> Color(0xFF4ADE80)
        else -> Color(0xFF15803D)
    }
}

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
    val chip = 40.dp
    val control = 34.dp
    val emptyPadding = 28.dp
    val skeletonTemperatureWidth = 136.dp
    val skeletonTemperatureHeight = 62.dp
    val skeletonSubtitleWidth = 170.dp
    val skeletonForecast = 150.dp
    val skeletonWeekly = 220.dp
    val metaTouch = 32.dp
    val action = 36.dp
    val segmentPad = 3.dp
    val segmentGap = 2.dp
    val segmentRadius = 7.dp
    val groupRadius = 10.dp
    val navPillWidth = 52.dp
    val navPillHeight = 28.dp
    val pageTop = 14.dp
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
    val topBar = 60.dp
    val navBar = 68.dp
    val hourly = 60.dp
    val chartMinimumWidth = 300.dp
    val chartBottom = 30.dp
    val historyHighDot = 4.2.dp
    val historyLowDot = 3.6.dp
    val chart = 210.dp
    val trend = 184.dp
    val spline = 44.dp
    val symbol = 66.dp
    val heroSymbolBox = 74.dp
    val heroAccessibleSize = 47.2.dp
    val maxWidth = 760.dp
    val narrow = 360.dp
    val heroThreshold = 320.dp
    val icon = 20.dp
    val smallIcon = 16.dp
    val weatherSmall = 24.dp
    val field = 44.dp
    val labelCap = 120.dp
    val gridGap = 10.dp
    val weeklyDate = 74.dp
    val weeklyWeather = 72.dp
    val weeklyTemperature = 32.dp
    val rangeBar = 7.dp
    val chartStroke = 2.2.dp
    val chartDot = 2.8.dp
    val selectedDot = 5.dp
}
object WeatherTheme {
    val colors: WeatherColors @Composable get() = LocalWeatherColors.current
    val type: WeatherTypography @Composable get() = LocalWeatherType.current
    val mode: ThemeMode @Composable get() = LocalWeatherMode.current
    val fontScale: Float @Composable get() = LocalAppScale.current
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
    val type = weatherTypography(font)
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
            outlineVariant = colors.border,
            outline = colors.border, surfaceVariant = colors.tonal, surfaceTint = colors.accent,
            primaryContainer = colors.accentSoft, onPrimaryContainer = colors.accentOnSoft,
            secondary = colors.accent, onSecondary = colors.onAccent,
            tertiary = colors.accent, onTertiary = colors.onAccent,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface,
            surfaceContainerHighest = colors.tonal, surfaceContainerLow = colors.tonal,
            surfaceContainerLowest = colors.ground, surfaceDim = colors.ground, surfaceBright = colors.surface
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
            outlineVariant = colors.border,
            outline = colors.border, surfaceVariant = colors.tonal, surfaceTint = colors.accent,
            primaryContainer = colors.accentSoft, onPrimaryContainer = colors.accentOnSoft,
            secondary = colors.accent, onSecondary = colors.onAccent,
            tertiary = colors.accent, onTertiary = colors.onAccent,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surface,
            surfaceContainerHighest = colors.tonal, surfaceContainerLow = colors.tonal,
            surfaceContainerLowest = colors.ground, surfaceDim = colors.ground, surfaceBright = colors.surface
        )
    }
    CompositionLocalProvider(
        LocalWeatherColors provides colors,
        LocalWeatherType provides type,
        LocalWeatherMode provides mode,
        LocalAppScale provides font.scale
    ) {
        MaterialTheme(colorScheme = scheme, typography = type.material, content = content)
    }
}

fun heroColor(mode: ThemeMode, palette: Palette, sky: com.pbweather.weather.domain.SkyCondition): Color {
    if (mode == ThemeMode.HIGH_CONTRAST) return Color.Black
    if (sky == com.pbweather.weather.domain.SkyCondition.UNKNOWN) return weatherColors(mode, palette).ground
    val dark = mode == ThemeMode.DARK
    val light = when (sky) {
        com.pbweather.weather.domain.SkyCondition.CLEAR -> longArrayOf(0xFFD8EBFF, 0xFFF4E6CC, 0xFFD6F0EE, 0xFFE6ECF4)
        com.pbweather.weather.domain.SkyCondition.CLOUDY -> longArrayOf(0xFFDDE5F0, 0xFFE6E3DD, 0xFFDDE6E8, 0xFFE2E6EC)
        com.pbweather.weather.domain.SkyCondition.RAIN -> longArrayOf(0xFFCFDCED, 0xFFDBE1E8, 0xFFD0DFE5, 0xFFD7DFE8)
        com.pbweather.weather.domain.SkyCondition.SLEET, com.pbweather.weather.domain.SkyCondition.SNOW ->
            longArrayOf(0xFFD3E1EF, 0xFFDDE3EA, 0xFFD4E3E8, 0xFFDBE3EC)
        else -> longArrayOf(0xFFDCE9FB, 0xFFECE8E0, 0xFFDFEEEF, 0xFFE8ECF2)
    }
    val night = when (sky) {
        com.pbweather.weather.domain.SkyCondition.CLEAR -> longArrayOf(0xFF16305C, 0xFF352614, 0xFF12373A, 0xFF232936)
        com.pbweather.weather.domain.SkyCondition.CLOUDY -> longArrayOf(0xFF1C2638, 0xFF23201C, 0xFF172528, 0xFF1C2028)
        com.pbweather.weather.domain.SkyCondition.RAIN -> longArrayOf(0xFF142238, 0xFF1B222C, 0xFF12232C, 0xFF181F2A)
        com.pbweather.weather.domain.SkyCondition.SLEET, com.pbweather.weather.domain.SkyCondition.SNOW ->
            longArrayOf(0xFF16263D, 0xFF1D242F, 0xFF142731, 0xFF1A222E)
        else -> longArrayOf(0xFF182B4D, 0xFF282119, 0xFF132C30, 0xFF1F242E)
    }
    return Color((if (dark) night else light)[palette.ordinal])
}

fun gradeTokens(mode: ThemeMode, grade: Int?): Pair<Color, Color> {
    val i = ((grade ?: 2) - 1).coerceIn(0, 3)
    if (mode == ThemeMode.HIGH_CONTRAST) return Color.Black to Color(longArrayOf(0xFF7DFF9B, 0xFF7AD7FF, 0xFFFFBE5C, 0xFFFF7A7A)[i])
    val bg = if (mode == ThemeMode.DARK) {
        longArrayOf(0xFF123324, 0xFF172E54, 0xFF43230F, 0xFF45161B)
    } else {
        longArrayOf(0xFFDCFCE7, 0xFFDBEAFE, 0xFFFFEDD5, 0xFFFEE2E2)
    }
    val fg = if (mode == ThemeMode.DARK) {
        longArrayOf(0xFF86EFAC, 0xFF93C5FD, 0xFFFDBA74, 0xFFFCA5A5)
    } else {
        longArrayOf(0xFF14532D, 0xFF1E3A8A, 0xFF9A3412, 0xFF991B1B)
    }
    return Color(bg[i]) to Color(fg[i])
}
fun successColor(mode: ThemeMode): Color = when (mode) {
    ThemeMode.LIGHT -> Color(0xFF15803D)
    ThemeMode.DARK -> Color(0xFF4ADE80)
    ThemeMode.HIGH_CONTRAST -> Color(0xFF7DFF9B)
}

data class WeatherTypography(
    val hero: TextStyle,
    val heroAccessible: TextStyle,
    val airValue: TextStyle,
    val sheetTitle: TextStyle,
    val location: TextStyle,
    val meta: TextStyle,
    val material: Typography
)
private val LocalWeatherType = staticCompositionLocalOf { weatherTypography(FontSize.NORMAL) }
private val LocalWeatherMode = staticCompositionLocalOf { ThemeMode.LIGHT }
private val LocalAppScale = staticCompositionLocalOf { 1f }
fun weatherTypography(font: FontSize): WeatherTypography {
    fun style(size: Float, weight: FontWeight = FontWeight.Normal, line: Float = 1.4f, spacing: Float = 0f) = TextStyle(
        fontSize = (size * font.scale).sp,
        lineHeight = (size * font.scale * line).sp,
        fontWeight = weight,
        letterSpacing = (size * font.scale * spacing).sp,
        fontFeatureSettings = "tnum"
    )
    val hero = style(65.6f, FontWeight.Light, .96f, -.04f).copy(fontSize = 65.6.sp, lineHeight = (65.6 * .96).sp)
    val accessible = style(47.2f, FontWeight.Light, .96f, -.04f).copy(fontSize = 47.2.sp, lineHeight = (47.2 * .96).sp)
    val meta = style(11.4f, FontWeight.Medium, 1.35f)
    val material = Typography(
        displayLarge = hero, displayMedium = accessible, headlineMedium = style(36.8f, line = 1f, spacing = -.02f),
        titleLarge = style(16.8f, FontWeight.Bold, 1.3f), titleMedium = style(14.3f, FontWeight.Bold, 1.35f),
        titleSmall = style(13.8f, FontWeight.SemiBold, 1.35f), bodyLarge = style(13.2f, line = 1.5f),
        bodyMedium = style(13.2f, line = 1.5f), bodySmall = style(11.7f),
        labelLarge = style(11.7f, FontWeight.SemiBold), labelMedium = meta, labelSmall = style(9.6f, FontWeight.Bold, 1.2f)
    )
    return WeatherTypography(
        hero,
        accessible,
        style(17.2f, FontWeight.Bold, 1.2f),
        style(15.6f, FontWeight.Bold, 1.3f),
        style(15.3f, FontWeight.Bold, 1.25f),
        meta,
        material
    )
}
fun modalScrim(mode: ThemeMode): Color = if (mode == ThemeMode.HIGH_CONTRAST) Color(0xC2000000) else Color(0x8F0F172A)
data class StatusColors(val background: Color, val foreground: Color, val border: Color)
fun statusColors(mode: ThemeMode, error: Boolean): StatusColors = when (mode) {
    ThemeMode.HIGH_CONTRAST -> StatusColors(
        Color.Black,
        if (error) Color(0xFFFF7A7A) else Color(0xFFFFE600),
        if (error) Color(0xFFFF7A7A) else Color(0xFFFFE600)
    )
    ThemeMode.DARK -> if (error) {
        StatusColors(
            Color(0xFF421519),
            Color(0xFFFECACA),
            Color(0xFFF87171)
        )
    } else {
        StatusColors(Color(0xFF3B2A0E), Color(0xFFFDE68A), Color(0xFFF59E0B))
    }
    ThemeMode.LIGHT -> if (error) {
        StatusColors(
            Color(0xFFFEE2E2),
            Color(0xFF991B1B),
            Color(0xFFDC2626)
        )
    } else {
        StatusColors(Color(0xFFFEF3C7), Color(0xFF78350F), Color(0xFFD97706))
    }
}
fun snackbarColor(mode: ThemeMode): Color = if (mode == ThemeMode.HIGH_CONTRAST) Color.Black else Color(0xFF1E293B)

fun snackbarTextColor(): Color = Color.White
fun snackbarActionColor(mode: ThemeMode, palette: Palette): Color =
    weatherColors(if (mode == ThemeMode.HIGH_CONTRAST) mode else ThemeMode.DARK, palette).accent
