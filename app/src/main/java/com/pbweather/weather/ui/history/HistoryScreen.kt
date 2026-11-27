package com.pbweather.weather.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import com.pbweather.weather.R
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.domain.DailyObservation
import com.pbweather.weather.domain.DateRange
import com.pbweather.weather.domain.HistorySummary
import com.pbweather.weather.domain.HistoryWindow
import com.pbweather.weather.domain.RangeProblem
import com.pbweather.weather.ui.components.ChoiceChip
import com.pbweather.weather.ui.components.CompactTextField
import com.pbweather.weather.ui.components.DeltaBadge
import com.pbweather.weather.ui.components.DetailRow
import com.pbweather.weather.ui.components.KeyValueGrid
import com.pbweather.weather.ui.components.PrimaryButton
import com.pbweather.weather.ui.components.Rule
import com.pbweather.weather.ui.components.SectionCard
import com.pbweather.weather.ui.components.SectionTitle
import com.pbweather.weather.ui.components.SheetHeader
import com.pbweather.weather.ui.components.SourceInfo
import com.pbweather.weather.ui.components.StatusBlock
import com.pbweather.weather.ui.components.number
import com.pbweather.weather.ui.components.prototypeSheetBorder
import com.pbweather.weather.ui.components.prototypeSheetModifier
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

    @Composable fun DateInput(value: String, onChange: (String) -> Unit, modifier: Modifier, label: String, isError: Boolean = false) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val description = stringResource(R.string.design_calendar, label)
        CompactTextField(value, onChange, modifier, label, isError = isError, trailingIcon = {
            Box(
                Modifier.size(Dims.weatherSmall).clickable {
                    val day = runCatching { java.time.LocalDate.parse(value) }.getOrDefault(window.end).coerceIn(window.start, window.end)
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, date ->
                            onChange(java.time.LocalDate.of(year, month + 1, date).toString())
                        },
                        day.year,
                        day.monthValue - 1,
                        day.dayOfMonth
                    ).apply {
                        datePicker.minDate = window.start.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        datePicker.maxDate = window.end.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }.show()
                }.semantics { contentDescription = description },
                contentAlignment = Alignment.Center
            ) {
                com.pbweather.weather.ui.components.AppIcon(
                    com.pbweather.weather.ui.components.UiIcon.CALENDAR,
                    null,
                    Modifier.size(Dims.smallIcon)
                )
            }
        })
    }
    LazyColumn(
        Modifier.padding(horizontal = Dims.gutter),
        verticalArrangement = Arrangement.spacedBy(Dims.gutter),
        contentPadding = PaddingValues(vertical = Dims.pageTop)
    ) {
        if (region == null) {
            item { TextButton(onClick = onSelect) { Text(stringResource(R.string.select_region)) } }
        } else {
            item {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Dims.tiny)) {
                        SectionTitle(stringResource(R.string.design_history_compare))
                        Text(
                            stringResource(
                                R.string.design_history_station,
                                region.asosStationName,
                                number(region.asosDistanceKm).orEmpty()
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val today = state.data.forecast.weekly.data?.firstOrNull { it.date == window.end.plusDays(1).toString() }
                    val yesterday = all.firstOrNull { it.date == window.end.toString() }
                    val delta = com.pbweather.weather.domain.YesterdayComparison.delta(today, yesterday)
                    val headers = listOf(R.string.design_high, R.string.design_low, R.string.daily_average, R.string.daily_rain)
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
                    Box {
                        if (LocalConfiguration.current.screenWidthDp <= 320 ||
                            LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f
                        ) {
                            Column {
                                headers.forEachIndexed { index, label ->
                                    DetailRow(
                                        stringResource(label),
                                        stringResource(R.string.history_compare_pair, current[index], previous[index])
                                    )
                                }
                            }
                        } else {
                            Column {
                                Row(Modifier.padding(vertical = Dims.small)) {
                                    Text(
                                        stringResource(R.string.comparison_kind),
                                        Modifier.weight(3.5f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    headers.forEach {
                                        Text(stringResource(it), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Rule()
                                Row(Modifier.padding(vertical = Dims.small)) {
                                    Text(
                                        stringResource(R.string.design_compare_forecast, window.end.plusDays(1).toString()),
                                        Modifier.weight(3.5f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    current.forEachIndexed { index, value ->
                                        Text(
                                            value,
                                            Modifier.weight(1f),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = when (index) {
                                                0 -> WeatherTheme.colors.warm
                                                1 -> WeatherTheme.colors.cool
                                                else -> WeatherTheme.colors.text
                                            }
                                        )
                                    }
                                }
                                Row(Modifier.padding(vertical = Dims.small)) {
                                    Text(
                                        stringResource(R.string.design_compare_observed, window.end.toString()),
                                        Modifier.weight(3.5f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    previous.forEach { Text(it, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Dims.small),
                        verticalArrangement = Arrangement.spacedBy(Dims.small)
                    ) {
                        listOf(
                            stringResource(R.string.design_high) to delta.high,
                            stringResource(R.string.design_low) to delta.low
                        ).forEach { (label, value) ->
                            val text = when {
                                value == null -> stringResource(R.string.design_delta, label, "—")
                                kotlin.math.abs(value) < .05 -> stringResource(R.string.design_delta_equal, label)
                                else -> stringResource(
                                    R.string.design_history_delta,
                                    label,
                                    number(kotlin.math.abs(value)).orEmpty(),
                                    stringResource(
                                        if (value >
                                            0
                                        ) {
                                            R.string.higher
                                        } else {
                                            R.string.lower
                                        }
                                    )
                                )
                            }
                            DeltaBadge(text, null)
                        }
                    }
                    Text(
                        stringResource(R.string.design_compare_note),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = WeatherTheme.type.meta.fontSize,
                            lineHeight = WeatherTheme.type.meta.fontSize * 1.5f
                        ),
                        color = WeatherTheme.colors.tertiary
                    )
                }
            }
            item {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(Dims.tiny)) {
                        SectionTitle(stringResource(R.string.history_trend))
                        Text(
                            stringResource(R.string.design_history_window, window.start.toString(), window.end.toString()),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Dims.small)) {
                        ChoiceChip(
                            stringResource(R.string.one_week),
                            state.range == DateRange(window.end.minusDays(6), window.end),
                            Modifier
                        ) {
                            error =
                                null
                            onRange(DateRange(window.end.minusDays(6), window.end))
                        }
                        ChoiceChip(stringResource(R.string.ten_days), state.range == window) {
                            error = null
                            onRange(window)
                        }
                        ChoiceChip(stringResource(R.string.custom_range), state.range.end != window.end) {
                            start =
                                state.range.start.toString()
                            end = state.range.end.toString()
                        }
                    }
                    val stacked =
                        LocalConfiguration.current.screenWidthDp <= 320 || LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f
                    if (stacked) {
                        Column(verticalArrangement = Arrangement.spacedBy(Dims.small)) {
                            DateInput(
                                start,
                                {
                                    start = it
                                    error = null
                                },
                                Modifier.fillMaxWidth(),
                                stringResource(R.string.start_date),
                                isError =
                                error != null
                            )
                            DateInput(
                                end,
                                {
                                    end = it
                                    error = null
                                },
                                Modifier.fillMaxWidth(),
                                stringResource(R.string.end_date),
                                isError =
                                error != null
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(Dims.small)) {
                            DateInput(
                                start,
                                {
                                    start = it
                                    error = null
                                },
                                Modifier.weight(1f),
                                stringResource(R.string.start_date),
                                isError =
                                error != null
                            )
                            DateInput(
                                end,
                                {
                                    end = it
                                    error = null
                                },
                                Modifier.weight(1f),
                                stringResource(R.string.end_date),
                                isError =
                                error != null
                            )
                        }
                    }
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
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        PrimaryButton(onClick = {
                            val validation = HistoryWindow.validate(start, end, clock)
                            error = validation.error
                            validation.range?.let(onRange)
                        }) { Text(stringResource(R.string.query_history)) }
                    }
                    StatusBlock(state.data.history, onRefresh)
                    HistoryChart(state.range, rows) { selected = it }
                    androidx.compose.material3.Surface(
                        color = WeatherTheme.colors.tonal,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(Dims.small)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(Dims.medium), verticalArrangement = Arrangement.spacedBy(Dims.tiny)) {
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
                }
            }
            items(state.range.dates.reversed(), key = { it.toString() }) { day ->
                val row = rows.firstOrNull { it.date == day.toString() }
                val description = stringResource(R.string.history_open_day, day.toString())
                Column(Modifier.fillMaxWidth().clickable { selected = day.toString() }.semantics { contentDescription = description }) {
                    Rule()
                    Row(Modifier.fillMaxWidth().padding(vertical = Dims.medium), horizontalArrangement = Arrangement.spacedBy(Dims.small)) {
                        Column(Modifier.weight(1f)) {
                            Text(day.toString(), style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.today_extrema, temperature(row?.maximum), temperature(row?.minimum)),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(rain(row?.precipitation), style = MaterialTheme.typography.bodySmall)
                        com.pbweather.weather.ui.components.AppIcon(com.pbweather.weather.ui.components.UiIcon.NEXT, null)
                    }
                }
            }
        }
    }
    selected?.let { date -> HistoryDaySheet(date, all.firstOrNull { it.date == date }, state) { selected = null } }
}

@Composable private fun rain(value: Double?): String =
    value?.let { stringResource(R.string.rain_value, number(it).orEmpty()) } ?: stringResource(R.string.history_missing)

@Composable private fun HistoryChart(range: DateRange, rows: List<DailyObservation>, onDay: (String) -> Unit) {
    val days = remember(range, rows) { range.dates.map { day -> rows.firstOrNull { it.date == day.toString() } } }
    val temperatures = remember(rows) { rows.flatMap { listOfNotNull(it.maximum, it.minimum) } }
    val low = temperatures.minOrNull() ?: 0.0
    val high = temperatures.maxOrNull() ?: low + 4
    val span = (high - low).coerceAtLeast(4.0)
    val rainMax = (rows.mapNotNull { it.precipitation }.maxOrNull() ?: 10.0).coerceAtLeast(10.0)
    val colors = WeatherTheme.colors
    val description = stringResource(R.string.history_chart_description, range.start.toString(), range.end.toString())
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dims.medium), verticalArrangement = Arrangement.spacedBy(Dims.tiny)) {
        listOf(
            R.string.design_chart_high to colors.warm,
            R.string.design_chart_low to colors.cool,
            R.string.design_chart_rain to colors.rain
        ).forEach { (label, color) ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dims.tiny), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Surface(
                    Modifier.size(Dims.small),
                    color = color,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(Dims.segmentGap)
                ) {
                }
                Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = colors.secondary)
            }
        }
    }
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val labelStyle = WeatherTheme.type.meta.copy(color = colors.tertiary)
    val ticks = remember(low, high, labelStyle) {
        listOf(high, (high + low) / 2, low).map {
            it to
                measurer.measure((number(it, 0) ?: "—") + "°", labelStyle)
        }
    }
    val dates = remember(range, labelStyle) { range.dates.map { measurer.measure("${it.monthValue}.${it.dayOfMonth}", labelStyle) } }
    val band = remember { androidx.compose.ui.graphics.Path() }
    Box(Modifier.fillMaxWidth().height(Dims.chart)) {
        Canvas(Modifier.matchParentSize().semantics { contentDescription = description }) {
            val svgWidth = maxOf(Dims.chartMinimumWidth.value, days.size * Dims.action.value)
            val scale = minOf(size.width / svgWidth, size.height / Dims.chart.value)
            val insetX = (size.width - svgWidth * scale) / 2
            val insetY = (size.height - Dims.chart.value * scale) / 2
            val plotWidth = svgWidth - Dims.control.value - Dims.gutter.value
            val plotHeight = Dims.chart.value - Dims.sheet.value - Dims.chartBottom.value
            fun x(index: Int) =
                insetX + (Dims.control.value + if (days.size == 1) plotWidth / 2 else plotWidth * index / (days.size - 1)) * scale
            fun y(value: Double) = insetY +
                (Dims.sheet.value + plotHeight - (value - low).toFloat() / span.toFloat() * plotHeight * .76f - plotHeight * .16f) *
                scale
            val bottom = insetY + (Dims.sheet.value + plotHeight) * scale
            ticks.forEach { (value, label) ->
                drawLine(
                    colors.border,
                    Offset(insetX + Dims.control.value * scale, y(value)),
                    Offset(insetX + (svgWidth - Dims.gutter.value) * scale, y(value)),
                    Dims.hairline.value * scale,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(Dims.segmentPad.value * scale, Dims.segmentPad.value * scale)
                    )
                )
                drawText(
                    label,
                    topLeft = Offset(
                        (insetX + (Dims.control.value - Dims.tiny.value) * scale - label.size.width).coerceAtLeast(0f),
                        y(value) - label.size.height / 2
                    )
                )
            }
            days.forEachIndexed { index, day ->
                val previous = days.getOrNull(index - 1)
                if (day?.maximum != null && day.minimum != null && previous?.maximum != null && previous.minimum != null) {
                    band.reset()
                    band.moveTo(x(index - 1), y(previous.maximum))
                    band.lineTo(x(index), y(day.maximum))
                    band.lineTo(x(index), y(day.minimum))
                    band.lineTo(x(index - 1), y(previous.minimum))
                    band.close()
                    drawPath(band, colors.accent.copy(alpha = .1f))
                }
                day?.precipitation?.takeIf { it > 0 }?.let { value ->
                    val height = maxOf(Dims.segmentPad.value, (plotHeight * .32 * value / rainMax).toFloat()) * scale
                    val width = minOf(Dims.section.value, maxOf(Dims.segmentPad.value * 2, plotWidth / days.size * .45f)) * scale
                    drawRoundRect(
                        colors.rain.copy(alpha = .45f),
                        Offset(x(index) - width / 2, bottom - height),
                        Size(width, height),
                        androidx.compose.ui.geometry.CornerRadius(Dims.segmentGap.value * scale)
                    )
                }
                listOf(false, true).forEach { maximum ->
                    val value = if (maximum) day?.maximum else day?.minimum
                    if (value != null) {
                        val color = if (maximum) colors.warm else colors.cool
                        val old = if (maximum) previous?.maximum else previous?.minimum
                        if (old !=
                            null
                        ) {
                            drawLine(color, Offset(x(index - 1), y(old)), Offset(x(index), y(value)), Dims.chartStroke.value * scale)
                        }
                        drawCircle(
                            color,
                            (if (maximum) Dims.historyHighDot.value else Dims.historyLowDot.value) * scale,
                            Offset(x(index), y(value))
                        )
                    }
                }
                if (index % 2 == 0 || index == days.lastIndex) {
                    val label = dates[index]
                    drawText(
                        label,
                        topLeft = Offset(
                            x(index) - label.size.width / 2,
                            insetY + (Dims.chart.value - Dims.small.value) * scale - label.size.height
                        )
                    )
                }
            }
        }
        Row(Modifier.matchParentSize().padding(start = Dims.control, end = Dims.gutter, top = Dims.sheet, bottom = Dims.chartBottom)) {
            range.dates.forEach { date ->
                val actionLabel = stringResource(R.string.design_chart_open_day, date.toString())
                androidx.compose.foundation.layout.Box(
                    Modifier.weight(1f).fillMaxHeight().clickable { onDay(date.toString()) }.semantics {
                        contentDescription =
                            actionLabel
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryDaySheet(date: String, day: DailyObservation?, state: WeatherUiState, onClose: () -> Unit) {
    ModalBottomSheet(
        contentWindowInsets = { androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0) },
        onDismissRequest = onClose,
        containerColor = WeatherTheme.colors.surface,
        scrimColor = modalScrim(WeatherTheme.mode),
        modifier = prototypeSheetModifier(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(Dims.sheetRadius),
        dragHandle = null,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            prototypeSheetBorder().fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp * .88f).verticalScroll(rememberScrollState()).padding(Dims.sheet),
            verticalArrangement = Arrangement.spacedBy(Dims.medium)
        ) {
            SheetHeader(date, onClose)
            Text(
                stringResource(
                    R.string.history_station,
                    state.preferences.selected?.asosStationName.orEmpty(),
                    number(state.preferences.selected?.asosDistanceKm).orEmpty()
                )
            )
            KeyValueGrid(
                listOf(
                    stringResource(R.string.daily_average) to temperature(day?.average),
                    stringResource(R.string.daily_rain) to rain(day?.precipitation),
                    stringResource(R.string.maximum) to temperature(day?.maximum),
                    stringResource(R.string.minimum) to temperature(day?.minimum),
                    stringResource(R.string.average_wind) to
                        (
                            day?.windMs?.let { stringResource(R.string.wind_unit, number(it).orEmpty()) }
                                ?: stringResource(R.string.history_missing)
                            ),
                    stringResource(R.string.average_humidity) to
                        (
                            day?.humidity?.let { stringResource(R.string.percent, number(it).orEmpty()) }
                                ?: stringResource(R.string.history_missing)
                            )
                ),
                LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f
            )
            SourceInfo(day?.meta, state.preferences.selected?.asosStationName, state.data.history)
            PrimaryButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.confirm)) }
        }
    }
}
