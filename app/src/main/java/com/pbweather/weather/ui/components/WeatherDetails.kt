package com.pbweather.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.pbweather.weather.R
import com.pbweather.weather.domain.AirQualityData
import com.pbweather.weather.domain.AirTrend
import com.pbweather.weather.domain.ApparentStatus
import com.pbweather.weather.domain.ApparentTemperature
import com.pbweather.weather.domain.Provider
import com.pbweather.weather.domain.Resource
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.domain.SourceMeta
import com.pbweather.weather.domain.WeatherCondition
import com.pbweather.weather.ui.navigation.WeatherUiState
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.WeatherTheme
import com.pbweather.weather.ui.theme.modalScrim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetails(target: String, state: WeatherUiState, onNavigate: (String) -> Unit = {}, onClose: () -> Unit) {
    val title = when {
        target.startsWith("hour:") -> target.removePrefix("hour:").toLongOrNull()?.let {
            stringResource(
                R.string.design_hour_sheet_title,
                java.time.Instant.ofEpochMilli(
                    it
                ).atZone(
                    com.pbweather.weather.core.time.ReleaseTimeCalculator.KST
                ).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            )
        }
            ?: stringResource(R.string.hour_details)
        target.startsWith("day:") -> stringResource(R.string.design_day_sheet_title, target.removePrefix("day:"))
        target.startsWith("air") -> stringResource(R.string.air_trend_title)
        target == "apparent" -> stringResource(R.string.design_apparent_title)
        target == "help" -> stringResource(R.string.help)
        else -> stringResource(R.string.source_details)
    }
    val scroll = rememberScrollState()
    LaunchedEffect(target) { scroll.scrollTo(0) }
    val expanded = LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f || LocalConfiguration.current.screenWidthDp <= 320
    ModalBottomSheet(
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) },
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WeatherTheme.colors.surface,
        shape = RoundedCornerShape(Dims.sheetRadius),
        dragHandle = null,
        modifier = prototypeSheetModifier(),
        scrimColor = modalScrim(WeatherTheme.mode)
    ) {
        Column(
            prototypeSheetBorder().fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp * .88f)
                .verticalScroll(scroll).padding(Dims.sheet),
            verticalArrangement = Arrangement.spacedBy(Dims.medium)
        ) {
            SheetHeader(title, onClose)
            when {
                target.startsWith("hour:") -> {
                    val hour = state.data.forecast.hourly.data?.hours?.firstOrNull { it.at.toString() == target.removePrefix("hour:") }
                    if (hour == null) {
                        Text(stringResource(R.string.details_missing))
                    } else {
                        Text(
                            state.preferences.selected?.name.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = WeatherTheme.colors.secondary
                        )
                        KeyValueGrid(
                            listOf(
                                stringResource(R.string.temperature_label) to temperature(hour.temperature),
                                stringResource(R.string.sky_condition) to skyLabel(WeatherCondition.from(hour.sky, hour.precipitationType)),
                                stringResource(R.string.probability) to
                                    (
                                        hour.probability?.let { stringResource(R.string.percent, it.toString()) }
                                            ?: stringResource(R.string.not_provided)
                                        ),
                                stringResource(R.string.precipitation_forecast) to precipitation(hour.precipitation)
                            ),
                            LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f
                        )
                        Rule()
                        hour.origins.entries.groupBy { it.value }.forEach { (meta, fields) ->
                            Text(
                                fields.map {
                                    fieldLabel(it.key)
                                }.distinct().joinToString(" · "),
                                style = MaterialTheme.typography.titleSmall
                            )
                            SourceInfo(meta, state.preferences.selected?.name, state.data.forecast.hourly)
                        }
                    }
                }
                target.startsWith("day:") -> {
                    val day = state.data.forecast.weekly.data?.firstOrNull { it.date == target.removePrefix("day:") }
                    if (day == null) {
                        Text(stringResource(R.string.details_missing))
                    } else {
                        Text(
                            state.preferences.selected?.name.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = WeatherTheme.colors.secondary
                        )
                        val whole =
                            day.wholeDayWeather != null || day.wholeDayProbability != null ||
                                java.time.LocalDate.parse(
                                    day.date
                                ).isAfter(
                                    java.time.Instant.ofEpochMilli(
                                        state.now
                                    ).atZone(com.pbweather.weather.core.time.ReleaseTimeCalculator.KST).toLocalDate().plusDays(7)
                                )

                        @Composable fun probability(value: Int?) =
                            value?.let { stringResource(R.string.percent, it.toString()) } ?: stringResource(R.string.not_provided)
                        val values =
                            listOf(
                                stringResource(R.string.minimum) to temperature(day.minimum),
                                stringResource(R.string.maximum) to temperature(day.maximum)
                            ) +
                                if (whole) {
                                    listOf(
                                        stringResource(R.string.design_whole_weather) to conditionLabel(day.wholeDayWeather),
                                        stringResource(R.string.design_whole_probability) to probability(day.wholeDayProbability)
                                    )
                                } else {
                                    listOf(
                                        stringResource(R.string.morning) to
                                            (conditionLabel(day.morningWeather) + " · " + probability(day.morningProbability)),
                                        stringResource(R.string.afternoon) to
                                            (conditionLabel(day.afternoonWeather) + " · " + probability(day.afternoonProbability))
                                    )
                                }
                        KeyValueGrid(values, expanded, listOf(WeatherTheme.colors.cool, WeatherTheme.colors.warm))
                        if (day.origins.isEmpty()) Text(stringResource(R.string.not_provided))
                        if (day.periodMaximum) {
                            Text(
                                stringResource(R.string.period_maximum_note),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        day.origins.entries.groupBy { it.value }.forEach { (meta, fields) ->
                            Rule()
                            Text(
                                fields.map {
                                    fieldLabel(it.key)
                                }.distinct().joinToString(" · "),
                                style = MaterialTheme.typography.titleSmall
                            )
                            SourceInfo(meta, state.preferences.selected?.name, state.data.forecast.weekly)
                        }
                    }
                }
                target.startsWith("air") -> AirTrendDetails(state.data.air, target == "air25")
                target == "apparent" -> {
                    val result = ApparentTemperature.calculate(state.data.forecast.now.data, state.data.forecast.now.meta?.observedAt)
                    val wind =
                        state.data.forecast.now.data?.windMs?.let {
                            stringResource(R.string.apparent_wind_units, number(it).orEmpty(), number(it * 3.6).orEmpty())
                        }
                            ?: stringResource(R.string.not_provided)
                    val humidity =
                        state.data.forecast.now.data?.humidity?.let { stringResource(R.string.percent, number(it).orEmpty()) }
                            ?: stringResource(R.string.not_provided)
                    KeyValueGrid(
                        listOf(
                            stringResource(R.string.design_calculation_result) to apparentLabel(result.status, result.celsius),
                            stringResource(R.string.design_calculation_basis) to stringResource(
                                when {
                                    state.data.forecast.now.meta?.observedAt == null -> R.string.not_provided
                                    result.summer -> R.string.design_summer_basis
                                    else -> R.string.design_winter_basis
                                }
                            ),
                            stringResource(R.string.design_observed_temperature) to temperature(state.data.forecast.now.data?.temperature),
                            stringResource(if (result.summer) R.string.humidity else R.string.wind) to if (result.summer) humidity else wind
                        ),
                        expanded
                    )
                    if (result.summer) MetaRow(stringResource(R.string.wind), wind)
                    androidx.compose.material3.Surface(shape = RoundedCornerShape(Dims.small), color = WeatherTheme.colors.tonal) {
                        Column(Modifier.padding(Dims.medium), verticalArrangement = Arrangement.spacedBy(Dims.tiny)) {
                            Text(
                                stringResource(
                                    if (result.summer) R.string.apparent_summer_explanation else R.string.apparent_winter_explanation
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(stringResource(R.string.apparent_input_note), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    SourceInfo(state.data.forecast.now.meta, state.preferences.selected?.name, state.data.forecast.now)
                }
                target == "help" -> Text(stringResource(R.string.help_text))
                target == "sourceAir" -> SourceInfo(state.data.air.meta, state.data.air.data?.station, state.data.air)
                target == "sourceHourly" -> SourceInfo(
                    state.data.forecast.hourly.meta,
                    state.preferences.selected?.name,
                    state.data.forecast.hourly
                )
                target == "sourceWeekly" -> {
                    state.data.forecast.weekly.data?.flatMap { it.origins.values }?.distinctBy { it.kind to it.issuedAt }?.forEach {
                        SourceInfo(it, state.preferences.selected?.name, state.data.forecast.weekly)
                        Rule()
                    }
                }
                else -> SourceInfo(state.data.forecast.now.meta, state.preferences.selected?.name, state.data.forecast.now)
            }
            DetailActions(target, state, expanded, onNavigate, onClose)
        }
    }
}

@Composable fun fieldLabel(field: String): String = stringResource(
    when (field) {
        "temperature" -> R.string.temperature_label
        "minimum" -> R.string.minimum
        "maximum" -> R.string.maximum
        "sky", "precipitationType" -> R.string.sky_condition
        "probability" -> R.string.probability
        "precipitation" -> R.string.precipitation_forecast
        "morning" -> R.string.morning
        "afternoon" -> R.string.afternoon
        "wholeDay" -> R.string.whole_day
        else -> R.string.source_details
    }
)

@Composable fun conditionLabel(value: String?): String = SkyCondition.entries.firstOrNull { it.name == value }?.let { skyLabel(it) }
    ?: missing(value)

@Composable fun apparentLabel(status: ApparentStatus, value: Double?): String = when (status) {
    ApparentStatus.AVAILABLE -> value?.let { stringResource(R.string.design_apparent_celsius, number(it).orEmpty()) }
        ?: stringResource(R.string.missing)
    ApparentStatus.MISSING -> stringResource(R.string.apparent_missing)
    ApparentStatus.OUTSIDE_RANGE -> stringResource(R.string.apparent_outside)
    ApparentStatus.INPUT_SPEC_UNCONFIRMED -> stringResource(R.string.apparent_unconfirmed)
}

@Composable fun SourceInfo(meta: SourceMeta?, region: String?, resource: Resource<*>) {
    if (meta == null) {
        Text(stringResource(R.string.details_missing))
        if (resource.error != null || resource.isOffline || resource.isStale || resource.isRefreshing) StatusBlock(resource)
        return
    }
    MetaRow(
        stringResource(R.string.provider),
        if (meta.provider == Provider.AIRKOREA) {
            stringResource(R.string.airkorea)
        } else {
            stringResource(R.string.kma)
        }
    )
    val kind = when (meta.kind) {
        com.pbweather.weather.domain.DataKind.NOWCAST -> R.string.design_kind_now
        com.pbweather.weather.domain.DataKind.ULTRA -> R.string.design_kind_ultra
        com.pbweather.weather.domain.DataKind.SHORT -> R.string.design_kind_short
        com.pbweather.weather.domain.DataKind.MID_LAND -> R.string.design_kind_land
        com.pbweather.weather.domain.DataKind.MID_TA -> R.string.design_kind_temperature
        com.pbweather.weather.domain.DataKind.AIR -> R.string.design_kind_air
        com.pbweather.weather.domain.DataKind.HISTORY -> R.string.design_kind_history
        com.pbweather.weather.domain.DataKind.EXTREMES -> R.string.design_kind_extremes
        com.pbweather.weather.domain.DataKind.STATIONS -> R.string.design_kind_stations
    }
    MetaRow(stringResource(R.string.design_data_kind), stringResource(kind))
    MetaRow(stringResource(R.string.data_region), missing(region))
    if (meta.issuedAt != null) MetaRow(stringResource(R.string.issued_at), missing(dateTimeLabel(meta.issuedAt)))
    if (meta.observedAt != null) MetaRow(stringResource(R.string.observed_at), missing(dateTimeLabel(meta.observedAt)))
    if (meta.validFrom != null) MetaRow(stringResource(R.string.valid_from), missing(dateTimeLabel(meta.validFrom)))
    if (meta.validTo != null) MetaRow(stringResource(R.string.valid_to), missing(dateTimeLabel(meta.validTo)))
    MetaRow(stringResource(R.string.received_at), missing(dateTimeLabel(meta.receivedAt)))
    val status = when {
        resource.isOffline -> stringResource(R.string.cached_offline)
        resource.isStale -> stringResource(R.string.stale)
        resource.isRefreshing -> stringResource(if (resource.data != null) R.string.design_data_refreshing else R.string.loading)
        resource.error != null -> stringResource(if (resource.data != null) R.string.design_last_success else R.string.design_data_failed)
        else -> stringResource(R.string.design_data_ok)
    }
    MetaRow(
        stringResource(R.string.design_data_status),
        status,
        if (resource.isOffline ||
            resource.isStale
        ) {
            WeatherTheme.colors.warning
        } else {
            WeatherTheme.colors.text
        }
    )
    resource.error?.let { MetaRow(stringResource(R.string.design_data_reason), errorText(it), WeatherTheme.colors.error) }
}

@Composable fun gradeLabel(grade: Int?): String = stringResource(
    when (grade) {
        1 -> R.string.grade_good
        2 -> R.string.grade_normal
        3 -> R.string.grade_bad
        4 -> R.string.grade_very_bad
        else -> R.string.grade_unknown
    }
)

@Composable private fun AirTrendDetails(resource: Resource<AirQualityData>, initialPm25: Boolean) {
    val data = resource.data
    if (data == null || data.readings.isEmpty()) {
        StatusBlock(resource.copy(data = null))
        return
    }
    var pm25 by remember(initialPm25) { mutableStateOf(initialPm25) }
    var selectedAt by rememberSaveable(data.station) { mutableStateOf<Long?>(null) }
    val selected = data.readings.firstOrNull { it.at == selectedAt } ?: data.readings.last()
    val colors = WeatherTheme.colors
    val expanded = LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f
    Text(
        stringResource(
            R.string.design_air_header,
            data.station,
            stringResource(
                R.string.air_actual_period,
                data.readings.size,
                dateTimeLabel(data.readings.first().at).orEmpty(),
                dateTimeLabel(data.readings.last().at).orEmpty()
            )
        ),
        style = WeatherTheme.type.metaRow,
        color = colors.secondary
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Dims.medium)) {
        listOf(false, true).forEach { fine ->
            val color = if (fine) colors.pm25 else colors.accent
            Row(
                Modifier.heightIn(min = Dims.metaTouch).clickable {
                    pm25 = fine
                },
                horizontalArrangement = Arrangement.spacedBy(Dims.tiny),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Surface(Modifier.size(width = Dims.medium, height = Dims.segmentPad), color = color) { }
                Text(
                    stringResource(if (fine) R.string.air_pm25 else R.string.air_pm10),
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
        }
    }
    val pm10Segments = remember(data) { AirTrend.segments(data, false) }
    val pm25Segments = remember(data) { AirTrend.segments(data, true) }
    val max = data.readings.flatMap { listOfNotNull(it.pm10, it.pm25) }.maxOrNull()?.coerceAtLeast(50.0) ?: 50.0
    val first = data.readings.first().at
    val span = (data.readings.last().at - first).coerceAtLeast(3600000L)
    val density = LocalDensity.current.density
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val axisStyle = WeatherTheme.type.chartAxis.copy(color = colors.tertiary)
    val yLabels = remember(max, measurer, axisStyle) {
        listOf(max, kotlin.math.round(max / 2), 0.0).map {
            it to
                measurer.measure(number(it, 0).orEmpty(), axisStyle)
        }
    }
    val times = remember(data, measurer, axisStyle) { data.readings.map { measurer.measure(timeLabel(it.at).orEmpty(), axisStyle) } }
    val chartDescription = stringResource(R.string.air_chart_summary, data.station, data.readings.size)
    BoxWithConstraints(Modifier.fillMaxWidth().height(Dims.trend)) {
        Canvas(Modifier.matchParentSize().semantics { contentDescription = chartDescription }) {
            val svgWidth = maxOf(Dims.trendMinimumWidth.value, data.readings.size * Dims.weatherSmall.value)
            val plotScale = minOf(size.width / svgWidth, size.height / Dims.trend.value)
            val left = (size.width - svgWidth * plotScale) / 2
            val top = (size.height - Dims.trend.value * plotScale) / 2
            val plotWidth = svgWidth - Dims.trendLeft.value - Dims.trendRight.value
            val plotHeight = Dims.trend.value - Dims.trendTop.value - Dims.trendBottom.value
            fun point(at: Long, value: Double) = Offset(
                left + (Dims.trendLeft.value + ((at - first).toDouble() / span * plotWidth).toFloat()) * plotScale,
                top + (Dims.trendTop.value + plotHeight - (value / max * plotHeight * .88).toFloat()) * plotScale
            )
            yLabels.forEach { (value, label) ->
                val y = point(first, value).y
                drawLine(
                    colors.border,
                    Offset(left + Dims.trendLeft.value * plotScale, y),
                    Offset(left + (svgWidth - Dims.trendRight.value) * plotScale, y),
                    Dims.hairline.value * plotScale,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(Dims.segmentPad.value * plotScale, Dims.segmentPad.value * plotScale)
                    )
                )
                withTransform({
                    translate(left + (Dims.trendLeft.value - Dims.tiny.value) * plotScale, y)
                    scale(
                        plotScale / density,
                        plotScale / density,
                        Offset.Zero
                    )
                }) { drawText(label, topLeft = Offset(-label.size.width.toFloat(), -label.size.height / 2f)) }
            }
            for (index in 0..1) {
                val fine = index == 1
                val color = if (fine) colors.pm25 else colors.accent
                for (segment in if (fine) pm25Segments else pm10Segments) {
                    for (i in 1 until segment.size) {
                        val a = segment[i - 1]
                        val b = segment[i]
                        drawLine(
                            color,
                            point(a.at, (if (fine) a.pm25 else a.pm10)!!),
                            point(b.at, (if (fine) b.pm25 else b.pm10)!!),
                            Dims.chartStroke.value * plotScale
                        )
                    }
                    for (row in segment) {
                        drawCircle(
                            color,
                            (
                                if (row.at ==
                                    selected.at
                                ) {
                                    Dims.selectedDot.value
                                } else {
                                    Dims.chartDot.value
                                }
                                ) * plotScale,
                            point(row.at, (if (fine) row.pm25 else row.pm10)!!)
                        )
                    }
                }
            }
            val selectedX = point(selected.at, 0.0).x
            drawLine(
                colors.accent.copy(alpha = .6f),
                Offset(selectedX, top + Dims.trendTop.value * plotScale),
                Offset(
                    selectedX,
                    top + (Dims.trendTop.value + plotHeight) * plotScale
                ),
                Dims.hairline.value * plotScale,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(Dims.segmentGap.value * plotScale, Dims.segmentGap.value * plotScale)
                )
            )
            times.forEachIndexed { index, label ->
                if (index == 0 || index == times.lastIndex || index % 4 == 0) {
                    val x = point(data.readings[index].at, 0.0).x
                    withTransform({
                        translate(x, top + (Dims.trend.value - Dims.small.value) * plotScale)
                        scale(
                            plotScale / density,
                            plotScale / density,
                            Offset.Zero
                        )
                    }) { drawText(label, topLeft = Offset(-label.size.width / 2f, -label.size.height.toFloat())) }
                }
            }
        }
        val svgWidth = maxOf(Dims.trendMinimumWidth.value, data.readings.size * Dims.weatherSmall.value)
        val plotScale = minOf(maxWidth.value / svgWidth, maxHeight.value / Dims.trend.value)
        val left = (maxWidth.value - svgWidth * plotScale) / 2
        val top = (maxHeight.value - Dims.trend.value * plotScale) / 2
        val plotWidth = svgWidth - Dims.trendLeft.value - Dims.trendRight.value
        val plotHeight = Dims.trend.value - Dims.trendTop.value - Dims.trendBottom.value
        data.readings.forEach { row ->
            val label = stringResource(R.string.design_air_point, dateTimeLabel(row.at).orEmpty())
            val x =
                left + (Dims.trendLeft.value + ((row.at - first).toDouble() / span * plotWidth).toFloat() - Dims.icon.value / 2) * plotScale
            Box(
                Modifier.offset(x = x.dp, y = (top + Dims.trendTop.value * plotScale).dp).width((Dims.icon.value * plotScale).dp).height(
                    (
                        plotHeight *
                            plotScale
                        ).dp
                ).clickable { selectedAt = row.at }.semantics { contentDescription = label }
            )
        }
    }
    StatusBlock(resource)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Dims.small)) {
        items(data.readings, key = {
            it.at
        }) { row -> ChoiceChip(timeLabel(row.at).orEmpty(), row.at == selected.at) { selectedAt = row.at } }
    }
    val order = if (pm25) listOf(true, false) else listOf(false, true)
    val values = order.map { fine ->
        val value = if (fine) selected.pm25 else selected.pm10
        val grade = if (fine) selected.pm25Grade else selected.pm10Grade
        val flag = if (fine) selected.pm25Flag else selected.pm10Flag
        stringResource(if (fine) R.string.air_pm25 else R.string.air_pm10) to
            (
                (value?.let { stringResource(R.string.air_unit, number(it).orEmpty()) } ?: stringResource(R.string.not_provided)) + " · " +
                    gradeLabel(grade) +
                    (flag?.let { " · " + stringResource(R.string.air_missing_flag, it) } ?: "")
                )
    }
    KeyValueGrid(values, expanded)
    MetaRow(stringResource(R.string.observed_at), dateTimeLabel(selected.at).orEmpty())
    MetaRow(stringResource(R.string.design_grade_basis), data.gradeBasis)
    MetaRow(stringResource(R.string.provider), stringResource(R.string.airkorea))
    MetaRow(stringResource(R.string.design_original_time), selected.originalTime)
    androidx.compose.material3.Surface(shape = RoundedCornerShape(Dims.small), color = colors.tonal) {
        Text(
            stringResource(R.string.air_unconfirmed),
            Modifier.padding(Dims.medium),
            style = MaterialTheme.typography.bodySmall,
            color = colors.secondary
        )
    }
}

@Composable
private fun DetailActions(target: String, state: WeatherUiState, expanded: Boolean, onNavigate: (String) -> Unit, onClose: () -> Unit) {
    val hours = state.data.forecast.hourly.data?.hours.orEmpty().filter { it.at > state.now && it.at <= state.now + 48 * 3600000L }.take(48)
    val hour = target.removePrefix("hour:").toLongOrNull()
    val index = hours.indexOfFirst { it.at == hour }
    val dayHour = if (target.startsWith("day:")) {
        hours.firstOrNull {
            java.time.Instant.ofEpochMilli(
                it.at
            ).atZone(com.pbweather.weather.core.time.ReleaseTimeCalculator.KST).toLocalDate().toString() ==
                target.removePrefix("day:")
        }
    } else {
        null
    }
    val previousLabel = stringResource(R.string.previous_hours)
    val nextLabel = stringResource(R.string.next_hours)

    @Composable fun navigation() {
        if (target.startsWith("hour:") && index >= 0) {
            SecondaryButton(
                onClick = { if (index > 0) onNavigate("hour:${hours[index - 1].at}") },
                enabled = index > 0,
                modifier = Modifier.semantics {
                    contentDescription =
                        previousLabel
                }
            ) { Text(stringResource(R.string.design_previous)) }
            SecondaryButton(
                onClick = {
                    onNavigate("hour:${hours[index + 1].at}")
                },
                enabled = index < hours.lastIndex,
                modifier = Modifier.semantics {
                    contentDescription =
                        nextLabel
                }
            ) { Text(stringResource(R.string.design_next)) }
        }
        if (dayHour != null) SecondaryButton(onClick = { onNavigate("hour:${dayHour.at}") }) { Text(stringResource(R.string.show_hourly)) }
    }
    if (expanded) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dims.small)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dims.small)) { navigation() }
            PrimaryButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.confirm)) }
        }
    } else {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dims.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navigation()
            Spacer(Modifier.weight(1f))
            PrimaryButton(onClick = onClose) { Text(stringResource(R.string.confirm)) }
        }
    }
}
