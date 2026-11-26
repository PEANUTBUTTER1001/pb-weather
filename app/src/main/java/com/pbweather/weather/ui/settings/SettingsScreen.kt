package com.pbweather.weather.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import com.pbweather.weather.BuildConfig
import com.pbweather.weather.R
import com.pbweather.weather.domain.ForecastState
import com.pbweather.weather.ui.components.AppIcon
import com.pbweather.weather.ui.components.CompactOutlinedButton as OutlinedButton
import com.pbweather.weather.ui.components.Rule
import com.pbweather.weather.ui.components.UiIcon
import com.pbweather.weather.ui.components.temperature
import com.pbweather.weather.ui.components.timeLabel
import com.pbweather.weather.ui.navigation.WeatherActions
import com.pbweather.weather.ui.navigation.WeatherUiState
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.FontSize
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import com.pbweather.weather.ui.theme.weatherColors
import kotlin.math.ceil

@Composable
fun SettingsScreen(
    state: WeatherUiState,
    cacheBytes: Long?,
    clearing: Boolean,
    primaryForecast: ForecastState,
    actions: WeatherActions,
    onLocations: () -> Unit,
    onSystemSettings: () -> Unit,
    onDocument: (String) -> Unit,
    showLocation: Boolean = false
) {
    val list = rememberLazyListState()
    var locationShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(showLocation) {
        if (showLocation && !locationShown) {
            list.scrollToItem(2)
            locationShown = true
        }
        if (!showLocation) locationShown = false
    }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var preview by rememberSaveable { mutableStateOf(false) }
    val primary = state.preferences.favorites.firstOrNull { it.id == state.preferences.primaryId }
    LazyColumn(
        Modifier.testTag("settings-list").padding(horizontal = Dims.gutter),
        state = list,
        verticalArrangement = Arrangement.spacedBy(Dims.large),
        contentPadding = PaddingValues(top = Dims.pageTop, bottom = Dims.weatherSmall)
    ) {
        item {
            SettingsGroup(stringResource(R.string.settings_display)) {
                AdaptiveSettingRow(stringResource(R.string.settings_theme)) { expanded ->
                    SignatureSegments(
                        expanded,
                        ThemeMode.entries.map { mode ->
                            SettingChoice(
                                stringResource(
                                    when (mode) {
                                        ThemeMode.LIGHT -> R.string.theme_light
                                        ThemeMode.DARK -> R.string.theme_dark
                                        ThemeMode.HIGH_CONTRAST -> R.string.theme_contrast
                                    }
                                ),
                                state.preferences.theme == mode.name,
                                { actions.theme(mode) },
                                icon = when (mode) {
                                    ThemeMode.LIGHT -> UiIcon.SUN
                                    ThemeMode.DARK -> UiIcon.MOON
                                    else -> null
                                }
                            )
                        }
                    )
                }
                Rule()
                val contrast = state.preferences.theme == ThemeMode.HIGH_CONTRAST.name
                AdaptiveSettingRow(
                    stringResource(R.string.settings_palette),
                    enabled = !contrast
                ) { expanded ->
                    SignatureSegments(
                        expanded,
                        Palette.entries.map { palette ->
                            SettingChoice(
                                stringResource(
                                    when (palette) {
                                        Palette.INDIGO -> R.string.palette_indigo
                                        Palette.AMBER -> R.string.palette_amber
                                        Palette.TEAL -> R.string.palette_teal
                                        Palette.SLATE -> R.string.palette_slate
                                    }
                                ),
                                state.preferences.palette == palette.name,
                                {
                                    actions.palette(palette)
                                },
                                weatherColors(if (WeatherTheme.mode == ThemeMode.DARK) ThemeMode.DARK else ThemeMode.LIGHT, palette).accent
                            )
                        },
                        enabled = !contrast
                    )
                }
                Rule()
                AdaptiveSettingRow(stringResource(R.string.settings_font)) { expanded ->
                    SignatureSegments(
                        expanded,
                        FontSize.entries.map { font ->
                            SettingChoice(
                                stringResource(
                                    when (font) {
                                        FontSize.NORMAL -> R.string.font_normal
                                        FontSize.LARGE -> R.string.font_large
                                        FontSize.EXTRA_LARGE -> R.string.font_extra_large
                                    }
                                ),
                                state.preferences.font == font.name,
                                { actions.font(font) }
                            )
                        }
                    )
                }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_widget)) {
                AdaptiveSettingRow(
                    stringResource(R.string.primary_location),
                    primary?.name ?: stringResource(R.string.primary_not_set)
                ) { _ ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.open),
                            Modifier.heightIn(min = Dims.metaTouch).clickable(role = Role.Button) {
                                preview =
                                    true
                            }.padding(horizontal = Dims.small, vertical = Dims.tiny),
                            style = MaterialTheme.typography.labelLarge
                        )
                        OutlinedButton(onClick = onLocations) { Text(stringResource(R.string.change)) }
                    }
                }
                Rule()
                AdaptiveSettingRow(stringResource(R.string.auto_refresh), stringResource(R.string.widget_refresh_explanation)) { _ -> }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_location)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = Dims.gutter, vertical = Dims.medium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.approximate_location), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    if (state.location.granted) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dims.tiny), verticalAlignment = Alignment.CenterVertically) {
                            AppIcon(UiIcon.CHECK, null, Modifier.size(Dims.smallIcon), WeatherTheme.colors.statusOk)
                            Text(
                                stringResource(R.string.permission_allowed),
                                style = MaterialTheme.typography.labelLarge,
                                color = WeatherTheme.colors.statusOk
                            )
                        }
                    }
                }
                if (!state.location.granted) {
                    Text(
                        stringResource(R.string.location_denied),
                        Modifier.padding(horizontal = Dims.gutter),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(onClick = onSystemSettings, modifier = Modifier.padding(Dims.medium)) {
                        Text(stringResource(R.string.open_settings))
                    }
                }
                Rule()
                Row(
                    Modifier.padding(horizontal = Dims.gutter, vertical = Dims.medium),
                    horizontalArrangement = Arrangement.spacedBy(Dims.small),
                    verticalAlignment = Alignment.Top
                ) {
                    AppIcon(UiIcon.SHIELD, null, Modifier.size(Dims.smallIcon), WeatherTheme.colors.statusOk)
                    Text(
                        stringResource(R.string.location_privacy_promise),
                        style = MaterialTheme.typography.bodySmall,
                        color = WeatherTheme.colors.secondary
                    )
                }
            }
        }
        item {
            SettingsGroup(stringResource(R.string.settings_cleanup)) {
                val mb = if (cacheBytes ==
                    0L
                ) {
                    "0.0"
                } else {
                    String.format(java.util.Locale.KOREA, "%.1f", ceil((cacheBytes ?: 0L) / 1048576.0 * 10) / 10)
                }
                AdaptiveSettingRow(
                    stringResource(R.string.saved_weather),
                    if (cacheBytes ==
                        null
                    ) {
                        stringResource(R.string.loading)
                    } else {
                        stringResource(R.string.cache_size_summary, mb)
                    }
                ) { _ ->
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        enabled = (cacheBytes ?: 0) > 0 && !clearing,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WeatherTheme.colors.destructive),
                        borderColor = WeatherTheme.colors.destructive
                    ) {
                        Text(stringResource(if (clearing) R.string.clearing_cache else R.string.delete))
                    }
                }
                if (cacheBytes == 0L) Text(stringResource(R.string.no_saved_weather), Modifier.padding(Dims.gutter))
            }
        }
        item {
            SettingsGroup(stringResource(R.string.app_information)) {
                AdaptiveSettingRow(
                    stringResource(R.string.version),
                    stringResource(R.string.version_sources, BuildConfig.VERSION_NAME)
                ) { _ ->
                }
                listOf(
                    "help" to R.string.help,
                    "privacy" to R.string.privacy_policy,
                    "licenses" to R.string.open_source_licenses,
                    "usage" to R.string.usage_guide
                ).forEach { (target, title) ->
                    Rule()
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            onDocument(target)
                        }.heightIn(min = Dims.touch).padding(horizontal = Dims.gutter),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(title), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        AppIcon(UiIcon.NEXT, null, tint = WeatherTheme.colors.secondary)
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            shape = RoundedCornerShape(Dims.sheetRadius),
            containerColor = WeatherTheme.colors.surface,
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.clear_weather_title)) },
            text = { Text(stringResource(R.string.clear_weather_explanation)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        actions.clearCache()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WeatherTheme.colors.destructive,
                        contentColor = WeatherTheme.colors.onAccent
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (preview) {
        val data = if (primaryForecast.now.meta?.locationKey == primary?.key) primaryForecast else ForecastState()
        WidgetPreviewSheet(primary?.name, data, onLocations = {
            preview = false
            onLocations()
        }) { preview = false }
    }
}

@Composable private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dims.badgeInset)) {
        Text(title, Modifier.padding(start = Dims.gridGap), style = WeatherTheme.type.groupTitle, color = WeatherTheme.colors.secondary)
        Surface(
            color = WeatherTheme.colors.surface,
            shape = RoundedCornerShape(Dims.radius),
            border = BorderStroke(
                if (WeatherTheme.mode ==
                    ThemeMode.HIGH_CONTRAST
                ) {
                    Dims.strong
                } else {
                    Dims.hairline
                },
                WeatherTheme.colors.border
            )
        ) {
            Column(content = content)
        }
    }
}

@Composable private fun AdaptiveSettingRow(
    label: String,
    description: String? = null,
    enabled: Boolean = true,
    controls: @Composable (Boolean) -> Unit
) {
    val text: @Composable () -> Unit = {
        Column(Modifier.alpha(if (enabled) 1f else .55f), verticalArrangement = Arrangement.spacedBy(Dims.tiny)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = WeatherTheme.colors.secondary)
        }
    }
    SubcomposeLayout(Modifier.fillMaxWidth().padding(horizontal = Dims.gutter, vertical = Dims.medium)) { constraints ->
        val labelPlaceable = subcompose("measure-label") {
            androidx.compose.foundation.layout.Box(
                Modifier.clearAndSetSemantics {
                }
            ) { Text(label, style = MaterialTheme.typography.titleSmall) }
        }.single().measure(Constraints(maxWidth = constraints.maxWidth))
        val natural = subcompose("natural") {
            androidx.compose.foundation.layout.Box(Modifier.clearAndSetSemantics {}) { controls(false) }
        }.map { it.measure(Constraints()) }
        val naturalWidth = natural.maxOfOrNull { it.width } ?: 0
        val gap = if (naturalWidth == 0) 0 else Dims.medium.roundToPx()
        val fits = labelPlaceable.width + gap + naturalWidth <= constraints.maxWidth
        val actualLabel = subcompose(
            "actual-label",
            text
        ).single().measure(
            Constraints(
                maxWidth = if (fits &&
                    naturalWidth > 0
                ) {
                    (constraints.maxWidth - naturalWidth - gap).coerceAtLeast(0)
                } else {
                    constraints.maxWidth
                }
            )
        )
        val actual = subcompose("controls") { controls(!fits) }.map { it.measure(Constraints(maxWidth = constraints.maxWidth)) }
        val controlHeight = actual.maxOfOrNull { it.height } ?: 0
        val stackedGap = Dims.small.roundToPx()
        val height = if (fits) maxOf(actualLabel.height, controlHeight) else actualLabel.height + stackedGap + controlHeight
        layout(constraints.maxWidth, height) {
            actualLabel.placeRelative(0, if (fits) (height - actualLabel.height) / 2 else 0)
            actual.forEach {
                it.placeRelative(
                    if (fits) constraints.maxWidth - it.width else 0,
                    if (fits) {
                        (height - it.height) / 2
                    } else {
                        actualLabel.height +
                            stackedGap
                    }
                )
            }
        }
    }
}

private data class SettingChoice(
    val label: String,
    val selected: Boolean,
    val action: () -> Unit,
    val dot: Color? = null,
    val icon: UiIcon? = null
)

@Composable private fun SignatureSegments(expanded: Boolean, choices: List<SettingChoice>, enabled: Boolean = true) {
    val contrast = WeatherTheme.mode == ThemeMode.HIGH_CONTRAST
    val grid = expanded && choices.size == 4 && (
        LocalConfiguration.current.screenWidthDp <= 320 ||
            LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f || WeatherTheme.fontScale >= 1.42f
        )
    val reason = stringResource(R.string.palette_disabled)
    Surface(
        modifier = Modifier.alpha(if (enabled) 1f else .55f).semantics {
            if (!enabled) stateDescription = reason
        },
        shape = RoundedCornerShape(Dims.groupRadius),
        color = WeatherTheme.colors.tonal
    ) {
        Column(Modifier.padding(Dims.segmentPad), verticalArrangement = Arrangement.spacedBy(Dims.segmentGap)) {
            val rows = if (grid) choices.chunked(2) else listOf(choices)
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dims.segmentGap)) {
                    row.forEach { choice ->
                        val selectedColor = if (contrast) WeatherTheme.colors.accent else WeatherTheme.colors.text
                        val textColor = if (choice.selected && enabled) selectedColor else WeatherTheme.colors.secondary
                        Surface(
                            modifier = (if (expanded) Modifier.weight(1f) else Modifier)
                                .semantics {
                                    selected = choice.selected
                                    if (!enabled) disabled()
                                }
                                .clickable(enabled = enabled, role = Role.Button, onClick = choice.action),
                            shape = RoundedCornerShape(Dims.segmentRadius),
                            border = if (choice.selected) {
                                BorderStroke(
                                    Dims.hairline,
                                    if (contrast) WeatherTheme.colors.accent else WeatherTheme.colors.borderStrong
                                )
                            } else {
                                null
                            },
                            color = if (choice.selected) WeatherTheme.colors.surface else Color.Transparent,
                            contentColor = textColor
                        ) {
                            Row(
                                Modifier.heightIn(min = Dims.control).padding(horizontal = Dims.small, vertical = Dims.tiny),
                                horizontalArrangement = Arrangement.spacedBy(Dims.tiny, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                choice.icon?.let {
                                    AppIcon(
                                        it,
                                        null,
                                        Modifier.size(Dims.smallIcon),
                                        tint = if (it ==
                                            UiIcon.SUN
                                        ) {
                                            WeatherTheme.colors.sunIcon
                                        } else {
                                            WeatherTheme.colors.moonIcon
                                        }
                                    )
                                }
                                choice.dot?.let {
                                    androidx.compose.foundation.layout.Box(Modifier.size(Dims.small).background(it, CircleShape))
                                }
                                Text(choice.label, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
