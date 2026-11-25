package com.pbweather.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
        target.startsWith("hour:") -> stringResource(R.string.hour_details)
        target.startsWith("day:") -> stringResource(R.string.day_details)
        target.startsWith("air") -> stringResource(R.string.air_trend_title)
        target == "apparent" -> stringResource(R.string.apparent)
        target == "help" -> stringResource(R.string.help)
        else -> stringResource(R.string.source_details)
    }
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WeatherTheme.colors.surface,
        scrimColor = modalScrim(WeatherTheme.mode)
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp * .88f)
                .verticalScroll(rememberScrollState()).padding(Dims.sheet),
            verticalArrangement = Arrangement.spacedBy(Dims.medium)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = WeatherTheme.type.sheetTitle)
                val close = stringResource(R.string.close)
                IconButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = close }) { Text("×") }
            }
            when {
                target.startsWith("hour:") -> {
                    val hour = state.data.forecast.hourly.data?.hours?.firstOrNull { it.at.toString() == target.removePrefix("hour:") }
                    if (hour == null) {
                        Text(stringResource(R.string.details_missing))
                    } else {
                        Text(missing(dateTimeLabel(hour.at)), style = MaterialTheme.typography.titleLarge)
                        DetailRow(stringResource(R.string.temperature_label), temperature(hour.temperature))
                        DetailRow(stringResource(R.string.sky_condition), skyLabel(WeatherCondition.from(hour.sky, hour.precipitationType)))
                        DetailRow(
                            stringResource(R.string.probability),
                            hour.probability?.let {
                                stringResource(R.string.percent, it.toString())
                            } ?: stringResource(R.string.not_provided)
                        )
                        DetailRow(stringResource(R.string.precipitation_forecast), precipitation(hour.precipitation))
                        val hours = state.data.forecast.hourly.data?.hours.orEmpty().filter { it.at > state.now }
                        val index = hours.indexOfFirst { it.at == hour.at }
                        Row {
                            TextButton(enabled = index > 0, onClick = { onNavigate("hour:${hours[index - 1].at}") }) {
                                Text(stringResource(R.string.previous_hours))
                            }
                            TextButton(enabled = index >= 0 && index < hours.lastIndex, onClick = {
                                onNavigate("hour:${hours[index + 1].at}")
                            }) {
                                Text(stringResource(R.string.next_hours))
                            }
                        }
                        Rule()
                        hour.origins.forEach { (field, meta) ->
                            Text(fieldLabel(field), style = MaterialTheme.typography.titleMedium)
                            SourceInfo(meta, state.preferences.selected?.name, state.data.forecast.hourly)
                        }
                    }
                }
                target.startsWith("day:") -> {
                    val day = state.data.forecast.weekly.data?.firstOrNull { it.date == target.removePrefix("day:") }
                    if (day == null) {
                        Text(stringResource(R.string.details_missing))
                    } else {
                        Text(day.date, style = MaterialTheme.typography.titleLarge)
                        DetailRow(stringResource(R.string.minimum), temperature(day.minimum))
                        DetailRow(stringResource(R.string.maximum), temperature(day.maximum))
                        fun probability(value: Int?) = value?.toString()
                        if (day.morningWeather != null || day.morningProbability != null) {
                            DetailRow(
                                stringResource(R.string.morning),
                                conditionLabel(day.morningWeather) + " · " +
                                    (
                                        probability(day.morningProbability)?.let { stringResource(R.string.percent, it) }
                                            ?: stringResource(R.string.not_provided)
                                        )
                            )
                        }
                        if (day.afternoonWeather != null || day.afternoonProbability != null) {
                            DetailRow(
                                stringResource(R.string.afternoon),
                                conditionLabel(day.afternoonWeather) + " · " +
                                    (
                                        probability(day.afternoonProbability)?.let { stringResource(R.string.percent, it) }
                                            ?: stringResource(R.string.not_provided)
                                        )
                            )
                        }
                        if (day.wholeDayWeather != null || day.wholeDayProbability != null) {
                            DetailRow(
                                stringResource(R.string.whole_day),
                                conditionLabel(day.wholeDayWeather) + " · " +
                                    (
                                        probability(day.wholeDayProbability)?.let { stringResource(R.string.percent, it) }
                                            ?: stringResource(R.string.not_provided)
                                        )
                            )
                        }
                        if (day.origins.isEmpty()) Text(stringResource(R.string.not_provided))
                        val firstHour = state.data.forecast.hourly.data?.hours?.firstOrNull {
                            it.at > state.now && it.at <= state.now + 48 * 3600000L &&
                                java.time.Instant.ofEpochMilli(it.at).atZone(com.pbweather.weather.core.time.ReleaseTimeCalculator.KST)
                                    .toLocalDate().toString() == day.date
                        }
                        if (firstHour != null) {
                            TextButton(onClick = { onNavigate("hour:${firstHour.at}") }) {
                                Text(stringResource(R.string.show_hourly))
                            }
                        }
                        if (day.periodMaximum) {
                            Text(
                                stringResource(R.string.period_maximum_note),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        day.origins.forEach { (field, meta) ->
                            Rule()
                            Text(fieldLabel(field), style = MaterialTheme.typography.titleMedium)
                            SourceInfo(meta, state.preferences.selected?.name, state.data.forecast.weekly)
                        }
                    }
                }
                target.startsWith("air") -> AirTrendDetails(state.data.air, target == "air25")
                target == "apparent" -> {
                    val result = ApparentTemperature.calculate(state.data.forecast.now.data, state.data.forecast.now.meta?.observedAt)
                    Text(apparentLabel(result.status, result.celsius), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(if (result.summer) R.string.apparent_summer_explanation else R.string.apparent_winter_explanation))
                    Text(stringResource(R.string.apparent_input_note))
                    DetailRow(stringResource(R.string.temperature_label), temperature(state.data.forecast.now.data?.temperature))
                    DetailRow(stringResource(R.string.humidity), missing(number(state.data.forecast.now.data?.humidity)))
                    DetailRow(stringResource(R.string.wind), missing(number(state.data.forecast.now.data?.windMs)))
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
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.confirm)) }
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
    ApparentStatus.AVAILABLE -> temperature(value)
    ApparentStatus.MISSING -> stringResource(R.string.apparent_missing)
    ApparentStatus.OUTSIDE_RANGE -> stringResource(R.string.apparent_outside)
    ApparentStatus.INPUT_SPEC_UNCONFIRMED -> stringResource(R.string.apparent_unconfirmed)
}

@Composable fun SourceInfo(meta: SourceMeta?, region: String?, resource: Resource<*>) {
    if (meta == null) {
        Text(stringResource(R.string.details_missing))
        return
    }
    DetailRow(
        stringResource(R.string.provider),
        if (meta.provider == Provider.AIRKOREA) {
            stringResource(R.string.airkorea)
        } else {
            stringResource(R.string.kma)
        }
    )
    DetailRow(stringResource(R.string.data_region), missing(region))
    if (meta.issuedAt != null) DetailRow(stringResource(R.string.issued_at), missing(dateTimeLabel(meta.issuedAt)))
    if (meta.observedAt != null) DetailRow(stringResource(R.string.observed_at), missing(dateTimeLabel(meta.observedAt)))
    if (meta.validFrom != null) DetailRow(stringResource(R.string.valid_from), missing(dateTimeLabel(meta.validFrom)))
    if (meta.validTo != null) DetailRow(stringResource(R.string.valid_to), missing(dateTimeLabel(meta.validTo)))
    DetailRow(stringResource(R.string.received_at), missing(dateTimeLabel(meta.receivedAt)))
    if (resource.isStale) Text(stringResource(R.string.stale), color = WeatherTheme.colors.warning)
    if (resource.isOffline) Text(stringResource(R.string.cached_offline))
    resource.error?.let { Text(errorText(it), color = WeatherTheme.colors.error) }
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
    if (data == null) {
        StatusBlock(resource)
        return
    }
    var pm25 by remember(initialPm25) { mutableStateOf(initialPm25) }
    Text(data.station, style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.air_unconfirmed), style = MaterialTheme.typography.bodySmall)
    Row(horizontalArrangement = Arrangement.spacedBy(Dims.small)) {
        ChoiceChip(stringResource(R.string.air_pm10), !pm25, Modifier.weight(1f)) { pm25 = false }
        ChoiceChip(stringResource(R.string.air_pm25), pm25, Modifier.weight(1f)) { pm25 = true }
    }
    val segments = remember(data, pm25) { AirTrend.segments(data, pm25) }
    val values = data.readings.mapNotNull { if (pm25) it.pm25 else it.pm10 }
    val max = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val first = data.readings.first().at
    val span = (data.readings.last().at - first).coerceAtLeast(3600000L)
    val color = if (pm25) WeatherTheme.colors.pm25 else WeatherTheme.colors.rainText
    val chartDescription = stringResource(R.string.air_chart_summary, data.station, data.readings.size)
    Canvas(Modifier.fillMaxWidth().height(Dims.trend).semantics { contentDescription = chartDescription }) {
        fun point(at: Long, value: Double) = Offset(
            ((at - first).toDouble() / span * size.width).toFloat(),
            (size.height * (1 - value / max)).toFloat()
        )
        for (segment in segments) {
            for (i in 1 until segment.size) {
                val a = segment[i - 1]
                val b = segment[i]
                drawLine(
                    color,
                    point(a.at, (if (pm25) a.pm25 else a.pm10)!!),
                    point(b.at, (if (pm25) b.pm25 else b.pm10)!!),
                    Dims.chartStroke.toPx()
                )
            }
            for (row in segment) drawCircle(color, Dims.chartDot.toPx(), point(row.at, (if (pm25) row.pm25 else row.pm10)!!))
        }
    }
    var selected by remember { mutableStateOf<Long?>(null) }
    StatusBlock(resource)
    for (row in data.readings) {
        Rule()
        Column(Modifier.fillMaxWidth().clickable { selected = row.at }.padding(vertical = Dims.medium)) {
            Text(missing(dateTimeLabel(row.at)), style = MaterialTheme.typography.titleMedium)
            val value = if (pm25) row.pm25 else row.pm10
            val grade = if (pm25) row.pm25Grade else row.pm10Grade
            Text(
                (value?.let { stringResource(R.string.air_unit, number(it).orEmpty()) } ?: stringResource(R.string.not_provided)) +
                    " · " + gradeLabel(grade)
            )
            val flag = if (pm25) row.pm25Flag else row.pm10Flag
            if (flag != null) Text(stringResource(R.string.air_missing_flag, flag), color = WeatherTheme.colors.warning)
            if (selected == row.at) Text(row.originalTime, style = MaterialTheme.typography.bodySmall)
        }
    }
}
