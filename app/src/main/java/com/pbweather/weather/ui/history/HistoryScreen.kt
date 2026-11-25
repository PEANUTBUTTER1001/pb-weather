package com.pbweather.weather.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pbweather.weather.R
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.domain.DailyObservation
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.HistorySummary
import com.pbweather.weather.domain.HistoryWindow
import com.pbweather.weather.domain.RangeProblem
import com.pbweather.weather.ui.components.ChoiceChip
import com.pbweather.weather.ui.components.DetailRow
import com.pbweather.weather.ui.components.Rule
import com.pbweather.weather.ui.components.SectionCard
import com.pbweather.weather.ui.components.SectionTitle
import com.pbweather.weather.ui.components.SourceInfo
import com.pbweather.weather.ui.components.StatusBlock
import com.pbweather.weather.ui.components.number
import com.pbweather.weather.ui.components.temperature
import com.pbweather.weather.ui.navigation.WeatherUiState
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.WeatherTheme
import com.pbweather.weather.ui.theme.modalScrim
import java.time.Clock
import java.time.Instant

@Composable
fun HistoryScreen(state: WeatherUiState, onRange: (DateRange) -> Unit, onRefresh: () -> Unit, onSelect: () -> Unit) {
    val region = state.preferences.selected
    val clock = remember(state.now) { Clock.fixed(Instant.ofEpochMilli(state.now), ReleaseTimeCalculator.KST) }
    val window = HistoryWindow.recent(clock)
    var start by rememberSaveable(state.range) { mutableStateOf(state.range.start.toString()) }
    var end by rememberSaveable(state.range) { mutableStateOf(state.range.end.toString()) }
    var error by rememberSaveable { mutableStateOf<RangeProblem?>(null) }
    var selected by rememberSaveable(region?.key) { mutableStateOf<String?>(null) }
    val all = state.data.history.data.orEmpty()
    val rows = all.filter { it.date in state.range.dates.map { day -> day.toString() } }
    val summary = HistorySummary.calculate(rows)
    LazyColumn(Modifier.padding(horizontal = Dims.gutter), verticalArrangement = Arrangement.spacedBy(Dims.gutter)) {
        if (region == null) {
            item { TextButton(onClick = onSelect) { Text(stringResource(R.string.select_region)) } }
        } else {
            item {
                SectionCard {
                    SectionTitle(stringResource(R.string.yesterday_compare))
                    val today = state.data.forecast.weekly.data?.firstOrNull { it.date == window.end.plusDays(1).toString() }
                    val yesterday = all.firstOrNull { it.date == window.end.toString() }
                    val delta = com.pbweather.weather.domain.YesterdayComparison.delta(today, yesterday)
                    val headers = listOf(R.string.maximum, R.string.minimum, R.string.daily_average, R.string.daily_rain)
                    val current =
                        listOf(
                            temperature(today?.maximum),
                            temperature(today?.minimum),
                            stringResource(R.string.unconfirmed),
                            stringResource(R.string.unconfirmed)
                        )
                    val previous =
                        listOf(
                            temperature(yesterday?.maximum),
                            temperature(yesterday?.minimum),
                            temperature(yesterday?.average),
                            rain(yesterday?.precipitation)
                        )
                    BoxWithConstraints {
                        if (maxWidth < Dims.narrow || LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f) {
                            Column {
                                headers.forEachIndexed { index, label ->
                                    DetailRow(
                                        stringResource(label),
                                        stringResource(R.string.history_compare_pair, current[index], previous[index])
                                    )
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(Dims.small)) {
                                Row {
                                    Text(stringResource(R.string.comparison_kind), Modifier.weight(1f))
                                    headers.forEach { Text(stringResource(it), Modifier.weight(1f)) }
                                }
                                Rule()
                                Row {
                                    Text(stringResource(R.string.today_forecast), Modifier.weight(1f))
                                    current.forEach { Text(it, Modifier.weight(1f)) }
                                }
                                Row {
                                    Text(stringResource(R.string.yesterday_observation), Modifier.weight(1f))
                                    previous.forEach { Text(it, Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.history_station, region.asosStationName, number(region.asosDistanceKm).orEmpty()),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(stringResource(R.string.history_delta, number(delta.high) ?: "—", number(delta.low) ?: "—"))
                    Text(stringResource(R.string.history_comparison_note), style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                SectionCard {
                    SectionTitle(stringResource(R.string.history_trend))
                    Row(horizontalArrangement = Arrangement.spacedBy(Dims.small)) {
                        ChoiceChip(
                            stringResource(R.string.one_week),
                            state.range == DateRange(window.end.minusDays(6), window.end),
                            Modifier.weight(1f)
                        ) {
                            error =
                                null
                            onRange(DateRange(window.end.minusDays(6), window.end))
                        }
                        ChoiceChip(stringResource(R.string.ten_days), state.range == window, Modifier.weight(1f)) {
                            error = null
                            onRange(window)
                        }
                        ChoiceChip(stringResource(R.string.custom_range), state.range.end != window.end, Modifier.weight(1f)) {
                            start =
                                state.range.start.toString()
                            end = state.range.end.toString()
                        }
                    }
                    Text(
                        stringResource(R.string.history_window, window.start.toString(), window.end.toString()),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        start,
                        {
                            start = it
                            error = null
                        },
                        Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.start_date)) },
                        singleLine = true,
                        isError =
                        error != null
                    )
                    OutlinedTextField(
                        end,
                        {
                            end = it
                            error = null
                        },
                        Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.end_date)) },
                        singleLine = true,
                        isError =
                        error != null
                    )
                    error?.let {
                        Text(
                            stringResource(
                                when (it) {
                                    RangeProblem.INVALID_FORMAT -> R.string.range_format
                                    RangeProblem.REVERSED -> R.string.range_reversed
                                    RangeProblem.OUTSIDE_WINDOW -> R.string.range_outside
                                }
                            ),
                            color = WeatherTheme.colors.error
                        )
                    }
                    TextButton(onClick = {
                        val validation = HistoryWindow.validate(start, end, clock)
                        error = validation.error
                        validation.range?.let(onRange)
                    }) { Text(stringResource(R.string.query_history)) }
                    StatusBlock(state.data.history, onRefresh)
                    HistoryChart(state.range, rows)
                    Text(
                        stringResource(
                            R.string.history_valid_days,
                            summary.maximumDays,
                            summary.minimumDays,
                            summary.averageDays,
                            summary.rainfallDays,
                            state.range.dates.size
                        )
                    )
                    Text(
                        stringResource(
                            R.string.history_extremes,
                            summary.highest?.first ?: "—",
                            temperature(summary.highest?.second),
                            summary.lowest?.first ?: "—",
                            temperature(summary.lowest?.second)
                        )
                    )
                    Text(
                        stringResource(
                            R.string.history_rain_total,
                            rain(summary.rainfallTotal),
                            state.range.dates.size - summary.rainfallDays
                        )
                    )
                }
            }
            items(state.range.dates, key = { it.toString() }) { day ->
                val row = rows.firstOrNull { it.date == day.toString() }
                val description = stringResource(R.string.history_open_day, day.toString())
                Column(
                    Modifier.fillMaxWidth().clickable {
                        selected = day.toString()
                    }.semantics { contentDescription = description }.padding(vertical = Dims.medium)
                ) {
                    Rule()
                    Text(day.toString(), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.today_extrema, temperature(row?.maximum), temperature(row?.minimum)))
                    Text(rain(row?.precipitation))
                }
            }
        }
    }
    selected?.let { date -> HistoryDaySheet(date, all.firstOrNull { it.date == date }, state) { selected = null } }
}

@Composable private fun rain(value: Double?): String =
    value?.let { stringResource(R.string.rain_value, number(it).orEmpty()) } ?: stringResource(R.string.history_missing)

@Composable private fun HistoryChart(range: DateRange, rows: List<DailyObservation>) {
    val days = range.dates.map { day -> rows.firstOrNull { it.date == day.toString() } }
    val temperatures = rows.flatMap { listOfNotNull(it.maximum, it.minimum) }
    val low = (temperatures.minOrNull() ?: 0.0) - 1
    val span = ((temperatures.maxOrNull() ?: low + 1) - low + 1).coerceAtLeast(1.0)
    val rainMax = (rows.mapNotNull { it.precipitation }.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    val colors = WeatherTheme.colors
    val description = stringResource(R.string.history_chart_description, range.start.toString(), range.end.toString())
    Text(stringResource(R.string.history_legend), style = MaterialTheme.typography.bodySmall)
    Canvas(Modifier.fillMaxWidth().height(Dims.chart).semantics { contentDescription = description }) {
        val width = size.width / days.size.coerceAtLeast(1)
        fun point(index: Int, value: Double) = Offset(width * (index + .5f), (size.height * (.9 - (value - low) / span * .8)).toFloat())
        days.forEachIndexed { index, row ->
            val previousDay = days.getOrNull(index - 1)
            if (row?.maximum != null && row.minimum != null && previousDay?.maximum != null && previousDay.minimum != null) {
                val path = androidx.compose.ui.graphics.Path()
                val corners =
                    listOf(
                        point(index - 1, previousDay.maximum),
                        point(index, row.maximum),
                        point(index, row.minimum),
                        point(index - 1, previousDay.minimum)
                    )
                path.moveTo(corners[0].x, corners[0].y)
                corners.drop(1).forEach { path.lineTo(it.x, it.y) }
                path.close()
                drawPath(path, colors.accent.copy(alpha = .1f))
            }
            row?.precipitation?.let { value ->
                val h = (size.height * .3 * value / rainMax).toFloat()
                drawRect(colors.rain.copy(alpha = .45f), Offset(width * (index + .2f), size.height - h), Size(width * .6f, h))
            }
            listOf(false, true).forEach { maximum ->
                val value = if (maximum) row?.maximum else row?.minimum
                if (value != null) {
                    val color = if (maximum) colors.warm else colors.cool
                    val previous = days.getOrNull(index - 1)?.let { if (maximum) it.maximum else it.minimum }
                    if (previous != null) drawLine(color, point(index - 1, previous), point(index, value), Dims.chartStroke.toPx())
                    drawCircle(color, Dims.chartDot.toPx(), point(index, value))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDaySheet(date: String, day: DailyObservation?, state: WeatherUiState, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = WeatherTheme.colors.surface, scrimColor = modalScrim(WeatherTheme.mode)) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(Dims.sheet),
            verticalArrangement = Arrangement.spacedBy(Dims.medium)
        ) {
            Text(date, style = WeatherTheme.type.sheetTitle)
            Text(
                stringResource(
                    R.string.history_station,
                    state.preferences.selected?.asosStationName.orEmpty(),
                    number(state.preferences.selected?.asosDistanceKm).orEmpty()
                )
            )
            DetailRow(stringResource(R.string.daily_average), temperature(day?.average))
            DetailRow(stringResource(R.string.maximum), temperature(day?.maximum))
            DetailRow(stringResource(R.string.minimum), temperature(day?.minimum))
            DetailRow(stringResource(R.string.daily_rain), rain(day?.precipitation))
            DetailRow(
                stringResource(R.string.average_wind),
                day?.windMs?.let { stringResource(R.string.wind_unit, number(it).orEmpty()) } ?: stringResource(R.string.history_missing)
            )
            DetailRow(
                stringResource(R.string.average_humidity),
                day?.humidity?.let { stringResource(R.string.percent, number(it).orEmpty()) } ?: stringResource(R.string.history_missing)
            )
            SourceInfo(day?.meta, state.preferences.selected?.asosStationName, state.data.history)
            TextButton(onClick = onClose) { Text(stringResource(R.string.close)) }
        }
    }
}
