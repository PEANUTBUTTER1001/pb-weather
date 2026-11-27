package com.pbweather.weather.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.pbweather.weather.R
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.domain.ApparentTemperature
import com.pbweather.weather.domain.DailyForecast
import com.pbweather.weather.domain.ForecastHour
import com.pbweather.weather.domain.ForecastSummary
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.domain.WeatherCondition
import com.pbweather.weather.domain.YesterdayComparison
import com.pbweather.weather.ui.components.AppIcon
import com.pbweather.weather.ui.components.DeltaBadge
import com.pbweather.weather.ui.components.Rule
import com.pbweather.weather.ui.components.SectionCard
import com.pbweather.weather.ui.components.SectionTitle
import com.pbweather.weather.ui.components.SourceLine
import com.pbweather.weather.ui.components.StatusBlock
import com.pbweather.weather.ui.components.UiIcon
import com.pbweather.weather.ui.components.WeatherSymbol
import com.pbweather.weather.ui.components.apparentLabel
import com.pbweather.weather.ui.components.conditionLabel
import com.pbweather.weather.ui.components.dateLabel
import com.pbweather.weather.ui.components.gradeLabel
import com.pbweather.weather.ui.components.missing
import com.pbweather.weather.ui.components.number
import com.pbweather.weather.ui.components.precipitation
import com.pbweather.weather.ui.components.skyLabel
import com.pbweather.weather.ui.components.temperature
import com.pbweather.weather.ui.components.timeLabel
import com.pbweather.weather.ui.navigation.WeatherUiState
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.Palette
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme
import com.pbweather.weather.ui.theme.gradeTokens
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

fun currentSky(state: WeatherUiState): SkyCondition {
    val hour = state.data.forecast.hourly.data?.hours?.minByOrNull { abs(it.at - state.now) }
    return WeatherCondition.from(hour?.sky, state.data.forecast.now.data?.precipitationType ?: hour?.precipitationType)
}

@Composable
fun HomeScreen(
    state: WeatherUiState,
    mode: ThemeMode,
    palette: Palette,
    list: LazyListState,
    hoursList: LazyListState,
    selectedHour: Long?,
    onHour: (Long) -> Unit,
    onDetails: (String) -> Unit,
    onHistory: () -> Unit,
    onRetry: () -> Unit,
    onSelectRegion: () -> Unit = {}
) {
    val current = state.data.forecast.now
    val hasHomeData = current.data != null || !state.data.forecast.hourly.data?.hours.isNullOrEmpty() ||
        !state.data.forecast.weekly.data.isNullOrEmpty() || !state.data.air.data?.readings.isNullOrEmpty()
    if (!hasHomeData && current.isRefreshing) {
        com.pbweather.weather.ui.components.HomeLoadingSkeleton()
        return
    }
    if (!hasHomeData && current.isOffline) {
        com.pbweather.weather.ui.components.EmptyWeather(onRetry, onSelectRegion)
        return
    }
    LazyColumn(state = list, modifier = Modifier.focusRestorer().focusGroup(), verticalArrangement = Arrangement.spacedBy(Dims.zero)) {
        val resource = state.data.forecast.now
        if (resource.error != null || resource.isOffline || resource.isStale || resource.data == null) {
            item(key = "status") {
                Box(Modifier.padding(horizontal = Dims.gutter)) { StatusBlock(resource, onRetry) }
            }
        }
        item(key = "hero") {
            val heroVisible by remember(list) {
                derivedStateOf {
                    val items = list.layoutInfo.visibleItemsInfo
                    items.isEmpty() || items.any { it.key == "hero" }
                }
            }
            val hourOfDay = remember(state.now) {
                if (state.now > 0L) Instant.ofEpochMilli(state.now).atZone(ReleaseTimeCalculator.KST).hour else null
            }
            Box(Modifier.fillMaxWidth().testTag("home-hero")) {
                HeroWeather(
                    currentSky(state),
                    mode,
                    palette,
                    Modifier.matchParentSize(),
                    windMs = state.data.forecast.now.data?.windMs,
                    hourOfDay = hourOfDay,
                    motionEnabled = state.preferences.weatherMotion,
                    isVisible = heroVisible
                )
                Column(
                    Modifier.padding(start = Dims.gutter, end = Dims.gutter, top = Dims.small, bottom = Dims.gutter),
                    verticalArrangement = Arrangement.spacedBy(Dims.small)
                ) {
                    HeroContent(state, onDetails)
                    Rule()
                    AirStrip(state, mode, onDetails, onRetry)
                }
            }
        }
        item(key = "comparison") {
            Box(Modifier.padding(start = Dims.gutter, end = Dims.gutter, top = Dims.badgeInset, bottom = Dims.section)) {
                ComparisonStrip(state, onHistory)
            }
        }
        item(key = "hourly") {
            Box(Modifier.padding(start = Dims.gutter, end = Dims.gutter, bottom = Dims.section)) {
                HourlySection(state, hoursList, selectedHour, onHour, onDetails, onRetry)
            }
        }
        item(key = "weekly") {
            Box(Modifier.padding(horizontal = Dims.gutter)) {
                WeeklySection(state, onDetails, onRetry)
            }
        }
        item { Spacer(Modifier.height(Dims.gutter)) }
    }
}

@Composable private fun HeroContent(state: WeatherUiState, onDetails: (String) -> Unit) {
    val now = state.data.forecast.now
    val weather = now.data
    val day = state.data.forecast.weekly.data?.firstOrNull()
    val sky = currentSky(state)
    val large = LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f

    @Composable fun values() {
        Text(
            temperature(weather?.temperature),
            style = if (large) {
                val density = LocalDensity.current
                WeatherTheme.type.heroAccessible.copy(
                    fontSize = with(density) { Dims.heroAccessibleSize.toSp() },
                    lineHeight = with(density) { (Dims.heroAccessibleSize * .96f).toSp() }
                )
            } else {
                WeatherTheme.type.hero
            }
        )
        val skyText = skyLabel(sky)
        val extrema = stringResource(R.string.today_extrema, temperature(day?.maximum, 0), temperature(day?.minimum, 0))
        val secondary = WeatherTheme.colors.secondary
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(skyText) }
                append("  ")
                withStyle(SpanStyle(color = secondary)) { append(extrema) }
            },
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Box {
        if (large) {
            Column {
                values()
                Box(Modifier.size(Dims.heroSymbolBox), contentAlignment = Alignment.Center) { WeatherSymbol(sky) }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { values() }
                Box(Modifier.size(Dims.heroSymbolBox), contentAlignment = Alignment.Center) { WeatherSymbol(sky) }
            }
        }
    }
    val hour = state.data.forecast.hourly.data?.hours?.minByOrNull { abs(it.at - state.now) }
    Text(
        if (weather?.precipitationType != null && weather.precipitationType != 0) {
            stringResource(R.string.sky_observation_note)
        } else {
            stringResource(R.string.sky_forecast_note, missing(timeLabel(hour?.at)))
        },
        style = MaterialTheme.typography.bodySmall,
        color = WeatherTheme.colors.secondary
    )
    Rule()
    val humidity = weather?.humidity?.let { stringResource(R.string.percent, number(it, 0).orEmpty()) }
        ?: stringResource(R.string.not_provided)
    val directions = stringArrayResource(R.array.wind_directions)
    val direction = weather?.windDirection?.let { directions[((it / 22.5).roundToInt() % 16 + 16) % 16] }
    val wind = weather?.windMs?.let { stringResource(R.string.wind_value, number(it).orEmpty()) } ?: stringResource(R.string.not_provided)
    val apparent = ApparentTemperature.calculate(weather, now.meta?.observedAt)

    @Composable fun humidityCell(modifier: Modifier = Modifier) = Metric(stringResource(R.string.humidity), humidity, modifier)

    @Composable fun windCell(modifier: Modifier = Modifier) =
        Metric(stringResource(R.string.wind), listOfNotNull(direction, wind).joinToString(" "), modifier)

    @Composable fun rainCell(modifier: Modifier = Modifier) =
        Metric(stringResource(R.string.rain_hour), precipitation(weather?.precipitation), modifier)

    @Composable fun apparentCell(modifier: Modifier = Modifier) = Metric(
        stringResource(R.string.apparent),
        apparentLabel(apparent.status, apparent.celsius),
        modifier.clickable { onDetails("apparent") }
    )
    if (large) {
        humidityCell()
        windCell()
        rainCell()
        apparentCell()
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(Dims.medium)) {
            humidityCell(Modifier.weight(1f))
            windCell(Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dims.medium)) {
            rainCell(Modifier.weight(1f))
            apparentCell(Modifier.weight(1f))
        }
    }
    SourceLine(now.meta, now, state.now) { onDetails("sourceNow") }
}

@Composable private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = Dims.segmentGap)) {
        Text(label, color = WeatherTheme.colors.secondary, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable private fun AirStrip(state: WeatherUiState, mode: ThemeMode, onDetails: (String) -> Unit, onRetry: () -> Unit) {
    val resource = state.data.air
    val data = resource.data
    val latest = data?.readings?.lastOrNull()
    if (latest == null) {
        StatusBlock(resource, onRetry)
        return
    }
    if (data.isFallback) Text(stringResource(R.string.air_fallback), color = WeatherTheme.colors.warning)
    if (resource.error != null || resource.isOffline) StatusBlock(resource, onRetry)
    BoxWithConstraints {
        val stacked = maxWidth <= Dims.heroThreshold || LocalDensity.current.fontScale *
            WeatherTheme.fontScale >= 1.8f

        @Composable fun cell(pm25: Boolean, modifier: Modifier = Modifier) {
            val value = if (pm25) latest.pm25 else latest.pm10
            val grade = if (pm25) latest.pm25Grade else latest.pm10Grade
            val flag = if (pm25) latest.pm25Flag else latest.pm10Flag
            Column(modifier.clickable { onDetails(if (pm25) "air25" else "air10") }.padding(vertical = Dims.small)) {
                val tokens = gradeTokens(mode, grade)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (pm25) R.string.air_pm25 else R.string.air_pm10),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(shape = RoundedCornerShape(Dims.pill), color = tokens.first, contentColor = tokens.second) {
                        Text(
                            stringResource(
                                when (grade) {
                                    1 -> R.string.design_grade_good
                                    2 -> R.string.design_grade_normal
                                    3 -> R.string.design_grade_bad
                                    4 -> R.string.design_grade_very_bad
                                    else -> R.string.grade_unknown
                                }
                            ),
                            Modifier.padding(horizontal = Dims.small, vertical = Dims.segmentGap),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                val concentration = value?.let { number(it, 0).orEmpty() } ?: stringResource(R.string.not_provided)
                val unit = stringResource(R.string.design_air_unit)
                val unitSize = WeatherTheme.type.airUnit.fontSize
                Text(
                    buildAnnotatedString {
                        append(concentration)
                        if (value != null) {
                            withStyle(SpanStyle(fontSize = unitSize)) {
                                append(" ")
                                append(unit)
                            }
                        }
                    },
                    style = WeatherTheme.type.airValue
                )
                if (flag != null) Text(flag, style = MaterialTheme.typography.bodySmall, color = WeatherTheme.colors.warning)
            }
        }
        if (stacked) {
            Column {
                cell(false)
                cell(true)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Dims.medium)) {
                cell(false, Modifier.weight(1f))
                cell(true, Modifier.weight(1f))
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).heightIn(min = Dims.metaTouch).clickable { onDetails("sourceAir") }) {
            Text(
                stringResource(R.string.source_air, data.station, missing(timeLabel(resource.meta?.observedAt))),
                style = WeatherTheme.type.meta,
                color = WeatherTheme.colors.secondary
            )
            if (resource.isStale) Text(stringResource(R.string.stale), style = WeatherTheme.type.meta)
        }
        Text(
            stringResource(R.string.air_trend),
            Modifier.heightIn(min = Dims.metaTouch).clickable {
                onDetails("air")
            },
            style = WeatherTheme.type.meta,
            color = WeatherTheme.colors.accent
        )
    }
}

@Composable private fun ComparisonStrip(state: WeatherUiState, onClick: () -> Unit) {
    val today = state.data.forecast.weekly.data?.firstOrNull()
    val yesterdayDate = Instant.ofEpochMilli(state.now).atZone(ReleaseTimeCalculator.KST).toLocalDate().minusDays(1).toString()
    val yesterday = state.data.history.data?.firstOrNull { it.date == yesterdayDate }
    val delta = YesterdayComparison.delta(today, yesterday)
    Surface(
        Modifier.testTag("home-comparison").clickable(onClick = onClick),
        color = WeatherTheme.colors.surface,
        shape = RoundedCornerShape(Dims.radius),
        border = androidx.compose.foundation.BorderStroke(
            if (WeatherTheme.mode == com.pbweather.weather.ui.theme.ThemeMode.HIGH_CONTRAST) Dims.strong else Dims.hairline,
            WeatherTheme.colors.border
        )
    ) {
        Column(
            Modifier.padding(horizontal = Dims.gutter, vertical = Dims.section),
            verticalArrangement = Arrangement.spacedBy(Dims.segmentRadius)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.yesterday_compare),
                    Modifier.weight(1f),
                    style = WeatherTheme.type.meta,
                    color = WeatherTheme.colors.accent
                )
                AppIcon(UiIcon.NEXT, null, Modifier.size(Dims.smallIcon))
            }
            val high = delta.high
            Text(
                if (high == null) {
                    stringResource(R.string.error_no_data)
                } else {
                    stringResource(
                        R.string.comparison_high,
                        number(abs(high)).orEmpty(),
                        stringResource(
                            when {
                                abs(high) < .05 -> R.string.similar
                                high > 0 -> R.string.higher
                                else -> R.string.lower
                            }
                        )
                    )
                },
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dims.small)) {
                for ((label, value) in listOf(R.string.design_high to delta.high, R.string.design_low to delta.low)) {
                    val signed = value?.let { (if (it > 0) "+" else "") + number(it).orEmpty() } ?: "—"
                    DeltaBadge(stringResource(R.string.design_delta, stringResource(label), signed), value)
                }
            }
            Text(
                stringResource(
                    R.string.comparison_source,
                    state.preferences.selected?.asosStationName.orEmpty(),
                    number(state.preferences.selected?.asosDistanceKm).orEmpty()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = WeatherTheme.colors.secondary
            )
        }
    }
}

@Composable private fun MiniScrollButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable private fun HourlySection(
    state: WeatherUiState,
    list: LazyListState,
    selected: Long?,
    onHour: (Long) -> Unit,
    onDetails: (String) -> Unit,
    onRetry: () -> Unit
) {
    val resource = state.data.forecast.hourly
    val scope = rememberCoroutineScope()
    val hours = resource.data?.hours.orEmpty().filter { it.at > state.now && it.at <= state.now + 48 * 3600000L }.take(48)
    SectionCard {
        SectionTitle(stringResource(R.string.hourly_title), caption = {
            SourceLine(resource.meta, resource, state.now) { onDetails("sourceHourly") }
        }) {
            Row {
                MiniScrollButton(enabled = list.canScrollBackward, onClick = {
                    scope.launch { list.scrollToItem((list.firstVisibleItemIndex - 4).coerceAtLeast(0)) }
                }, modifier = Modifier.size(Dims.metaTouch).background(WeatherTheme.colors.tonal, RoundedCornerShape(Dims.pill))) {
                    AppIcon(UiIcon.PREVIOUS, stringResource(R.string.previous_hours))
                }
                MiniScrollButton(enabled = list.canScrollForward, onClick = {
                    scope.launch {
                        list.scrollToItem((list.firstVisibleItemIndex + 4).coerceAtMost(hours.lastIndex.coerceAtLeast(0)))
                    }
                }, modifier = Modifier.size(Dims.metaTouch).background(WeatherTheme.colors.tonal, RoundedCornerShape(Dims.pill))) {
                    AppIcon(UiIcon.NEXT, stringResource(R.string.next_hours))
                }
            }
        }
        if (resource.data == null || resource.error != null || resource.isOffline) StatusBlock(resource, onRetry)
        val summary = ForecastSummary.select(resource.data?.hours.orEmpty(), state.now)
        Surface(
            onClick = {
                summary?.let {
                    onHour(it.at)
                    onDetails("hour:${it.at}")
                }
            },
            color = WeatherTheme.colors.tonal,
            shape = RoundedCornerShape(Dims.small)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dims.gridGap, vertical = Dims.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val headline = if (summary == null) {
                    stringResource(R.string.summary_missing)
                } else if (summary.probability !=
                    null
                ) {
                    stringResource(R.string.summary_probability, missing(timeLabel(summary.at)), summary.probability)
                } else {
                    stringResource(
                        R.string.summary_temperature,
                        missing(timeLabel(summary.at)),
                        number(summary.temperature).orEmpty()
                    )
                }
                val rain = summary?.precipitation?.takeIf { it.kind != com.pbweather.weather.domain.PrecipitationKind.NONE }
                Text(
                    headline + (
                        rain?.let {
                            " · " + stringResource(R.string.summary_rain, precipitation(it))
                        } ?: ""
                        ),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge
                )
                AppIcon(UiIcon.NEXT, null)
            }
        }

        if (summary == null) TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        val min = hours.mapNotNull { it.temperature }.minOrNull() ?: 0.0
        val span = ((hours.mapNotNull { it.temperature }.maxOrNull() ?: min) - min).coerceAtLeast(1.0)
        val chartDescription = stringResource(R.string.chart_summary, hours.size)
        val hourWidth = Dims.hourly * (LocalDensity.current.fontScale * WeatherTheme.fontScale).coerceIn(1f, 2f)
        val activeHour = selected ?: hours.firstOrNull()?.at
        LazyRow(state = list, modifier = Modifier.fillMaxWidth()) {
            items(hours, key = { it.at }) { hour ->
                Column(
                    Modifier.width(hourWidth).clickable {
                        onHour(hour.at)
                        onDetails("hour:${hour.at}")
                    },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        Modifier.fillMaxWidth()
                            .background(
                                if (activeHour ==
                                    hour.at
                                ) {
                                    WeatherTheme.colors.accentSoft
                                } else {
                                    WeatherTheme.colors.surface
                                },
                                RoundedCornerShape(Dims.small)
                            )
                            .then(
                                if (activeHour ==
                                    hour.at
                                ) {
                                    Modifier.border(Dims.hairline, WeatherTheme.colors.accent, RoundedCornerShape(Dims.small))
                                } else {
                                    Modifier
                                }
                            )
                            .padding(vertical = Dims.segmentPad),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(timeLabel(hour.at).orEmpty(), style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (hour.origins.values.any { it.kind == com.pbweather.weather.domain.DataKind.ULTRA }) {
                                stringResource(R.string.short_tag)
                            } else {
                                ""
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                        WeatherSymbol(WeatherCondition.from(hour.sky, hour.precipitationType), Modifier.size(Dims.hourlyWeather))
                        Text(temperature(hour.temperature, 0), style = MaterialTheme.typography.titleMedium)
                    }
                    val color = WeatherTheme.colors.accent
                    val index = hours.indexOf(hour)
                    Canvas(Modifier.fillMaxWidth().height(Dims.spline).semantics { contentDescription = chartDescription }) {
                        fun point(value: Double, x: Float) = Offset(x, (size.height * (.85 - (value - min) / span * .7)).toFloat())
                        val value = hour.temperature
                        if (value != null) {
                            val center = point(value, size.width / 2)
                            val previous = hours.getOrNull(index - 1)?.temperature
                            val next = hours.getOrNull(index + 1)?.temperature
                            if (previous != null) drawLine(color, center, point((previous + value) / 2, 0f), Dims.chartStroke.toPx())
                            if (next != null) drawLine(color, center, point((next + value) / 2, size.width), Dims.chartStroke.toPx())
                            drawCircle(color, (if (activeHour == hour.at) Dims.selectedDot else Dims.chartDot).toPx(), center)
                        }
                    }
                    Text(
                        hour.probability?.let { stringResource(R.string.percent, it.toString()) }
                            ?: stringResource(R.string.missing),
                        style = MaterialTheme.typography.bodySmall,
                        color = WeatherTheme.colors.rainText
                    )
                }
            }
        }
    }
}

@Composable private fun WeeklySection(state: WeatherUiState, onDetails: (String) -> Unit, onRetry: () -> Unit) {
    val resource = state.data.forecast.weekly
    val days = resource.data.orEmpty()
    val min = days.mapNotNull { it.minimum }.minOrNull() ?: 0.0
    val max = days.mapNotNull { it.maximum }.maxOrNull() ?: min + 1
    val large = LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f
    SectionCard {
        SectionTitle(stringResource(R.string.weekly_title))
        val origins = days.flatMap { it.origins.values }
        val shortTime = origins.filter {
            it.kind == com.pbweather.weather.domain.DataKind.SHORT ||
                it.kind == com.pbweather.weather.domain.DataKind.EXTREMES
        }.mapNotNull { it.issuedAt }.maxOrNull()
        val midTime = origins.filter {
            it.kind == com.pbweather.weather.domain.DataKind.MID_LAND ||
                it.kind == com.pbweather.weather.domain.DataKind.MID_TA
        }.mapNotNull { it.issuedAt }.maxOrNull()
        Text(
            stringResource(R.string.weekly_sources, missing(timeLabel(shortTime)), missing(timeLabel(midTime))),
            Modifier.fillMaxWidth().heightIn(min = Dims.metaTouch).clickable { onDetails("sourceWeekly") },
            style = WeatherTheme.type.meta,
            color = WeatherTheme.colors.secondary
        )
        if (resource.data == null || resource.error != null || resource.isOffline) StatusBlock(resource, onRetry)
        Text(
            stringResource(R.string.weekly_missing_note),
            style = MaterialTheme.typography.bodySmall,
            color = WeatherTheme.colors.secondary
        )
        for (day in days) {
            Rule()
            Column(Modifier.fillMaxWidth().clickable { onDetails("day:${day.date}") }.padding(vertical = Dims.small)) {
                val weather = day.wholeDayWeather ?: day.afternoonWeather ?: day.morningWeather
                val pop = day.wholeDayProbability ?: day.afternoonProbability ?: day.morningProbability
                if (large) {
                    Text(dateLabel(day.date), style = MaterialTheme.typography.titleMedium)
                    Text(
                        conditionLabel(weather) + " · " + (
                            pop?.let { stringResource(R.string.percent, it.toString()) }
                                ?: stringResource(R.string.not_provided)
                            )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(temperature(day.minimum))
                        RangeBar(day, min, max, Modifier.weight(1f))
                        Text(temperature(day.maximum))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(dateLabel(day.date), Modifier.width(Dims.weeklyDate), style = MaterialTheme.typography.bodySmall)
                        Column(Modifier.width(Dims.weeklyWeather)) {
                            WeatherSymbol(
                                SkyCondition.entries.firstOrNull {
                                    it.name == weather
                                } ?: SkyCondition.UNKNOWN,
                                Modifier.size(Dims.weeklyIcon)
                            )
                            Text(
                                (if (day.wholeDayWeather != null) stringResource(R.string.whole_day) + " " else "") +
                                    (pop?.let { stringResource(R.string.percent, it.toString()) } ?: stringResource(R.string.missing)),
                                style = MaterialTheme.typography.bodySmall,
                                color = WeatherTheme.colors.rainText
                            )
                        }
                        Text(temperature(day.minimum, 0), Modifier.width(Dims.weeklyTemperature), color = WeatherTheme.colors.cool)
                        RangeBar(day, min, max, Modifier.weight(1f))
                        Text(temperature(day.maximum, 0), Modifier.width(Dims.weeklyTemperature), color = WeatherTheme.colors.warm)
                    }
                }
            }
        }
    }
}

@Composable private fun RangeBar(day: DailyForecast, min: Double, max: Double, modifier: Modifier) {
    val brush = Brush.horizontalGradient(listOf(WeatherTheme.colors.cool, WeatherTheme.colors.warm))
    val track = WeatherTheme.colors.tonal
    Canvas(modifier.padding(horizontal = Dims.tiny).height(Dims.rangeBar)) {
        drawRoundRect(track)
        val low = day.minimum
        val high = day.maximum
        if (low != null && high != null) {
            val start = ((low - min) / (max - min).coerceAtLeast(1.0) * size.width).toFloat()
            val end = ((high - min) / (max - min).coerceAtLeast(1.0) * size.width).toFloat()
            drawRoundRect(brush, Offset(start, 0f), androidx.compose.ui.geometry.Size((end - start).coerceAtLeast(1f), size.height))
        }
    }
}
