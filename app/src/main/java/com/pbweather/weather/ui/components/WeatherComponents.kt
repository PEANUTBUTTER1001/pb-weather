package com.pbweather.weather.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pbweather.weather.R
import com.pbweather.weather.core.time.ReleaseTimeCalculator
import com.pbweather.weather.domain.DataKind
import com.pbweather.weather.domain.DomainError
import com.pbweather.weather.domain.Precipitation
import com.pbweather.weather.domain.PrecipitationKind
import com.pbweather.weather.domain.Resource
import com.pbweather.weather.domain.SkyCondition
import com.pbweather.weather.domain.SourceMeta
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.WeatherTheme
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

fun number(value: Double?, digits: Int = 1): String? = value?.takeIf { it.isFinite() }?.let {
    val rounded = java.math.BigDecimal.valueOf(it).setScale(digits, java.math.RoundingMode.HALF_UP)
    (if (rounded.signum() == 0) rounded.abs() else rounded).toPlainString()
}
fun timeLabel(at: Long?): String? = at?.let {
    Instant.ofEpochMilli(it).atZone(ReleaseTimeCalculator.KST).format(DateTimeFormatter.ofPattern("HH:mm"))
}
fun dateTimeLabel(at: Long?): String? = at?.let {
    Instant.ofEpochMilli(it).atZone(ReleaseTimeCalculator.KST).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm 'KST'"))
}
fun dateLabel(date: String): String = runCatching {
    java.time.LocalDate.parse(date).format(DateTimeFormatter.ofPattern("M.d (E)", Locale.KOREA))
}.getOrDefault(date)

@Composable fun missing(value: String?): String = value ?: stringResource(R.string.not_provided)

@Composable fun temperature(value: Double?, digits: Int = 1): String =
    value?.let { stringResource(R.string.temperature, number(it, digits).orEmpty()) } ?: stringResource(R.string.missing)

@Composable fun skyLabel(sky: SkyCondition): String = stringResource(
    when (sky) {
        SkyCondition.CLEAR -> R.string.sky_clear
        SkyCondition.PARTLY -> R.string.sky_partly
        SkyCondition.CLOUDY -> R.string.sky_cloudy
        SkyCondition.RAIN -> R.string.sky_rain
        SkyCondition.SLEET -> R.string.sky_sleet
        SkyCondition.SNOW -> R.string.sky_snow
        SkyCondition.UNKNOWN -> R.string.sky_unknown
    }
)

@Composable fun precipitation(value: Precipitation?): String = when {
    value == null -> stringResource(R.string.not_provided)
    value.kind == PrecipitationKind.NONE -> stringResource(R.string.rain_none)
    value.kind == PrecipitationKind.EXACT -> stringResource(R.string.rain_value, number(value.exactMm).orEmpty())
    value.kind == PrecipitationKind.QUALITATIVE -> when (value.text) {
        "1" -> stringResource(R.string.rain_weak)
        "2" -> stringResource(R.string.rain_moderate)
        "3" -> stringResource(R.string.rain_strong)
        else -> value.text
    }
    else -> value.text
}

@Composable fun errorText(error: DomainError): String = stringResource(
    when (error) {
        DomainError.OFFLINE -> R.string.error_offline
        DomainError.TIMEOUT -> R.string.error_timeout
        DomainError.NO_DATA -> R.string.error_no_data
        DomainError.QUOTA -> R.string.error_quota
        DomainError.KEY_CONFIGURATION -> R.string.error_key
        DomainError.REGION_MAPPING -> R.string.error_mapping
        DomainError.INVALID_RESPONSE -> R.string.error_invalid
        DomainError.UNKNOWN -> R.string.error_unknown
    }
)

@Composable fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dims.radius),
        color = WeatherTheme.colors.surface,
        border = BorderStroke(
            if (WeatherTheme.mode ==
                com.pbweather.weather.ui.theme.ThemeMode.HIGH_CONTRAST
            ) {
                Dims.strong
            } else {
                Dims.hairline
            },
            WeatherTheme.colors.border
        )
    ) {
        Column(Modifier.padding(Dims.gutter), verticalArrangement = Arrangement.spacedBy(Dims.medium), content = content)
    }
}

@Composable fun SectionTitle(title: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        action?.invoke()
    }
}

@Composable fun Rule() {
    HorizontalDivider(color = WeatherTheme.colors.border, thickness = Dims.hairline)
}

@Composable fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = Dims.small)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = WeatherTheme.colors.secondary)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable fun StatusBlock(resource: Resource<*>, onRetry: (() -> Unit)? = null) {
    val text = when {
        resource.isOffline -> stringResource(R.string.cached_offline)
        resource.error != null -> errorText(resource.error)
        resource.data == null && resource.isRefreshing -> stringResource(R.string.loading)
        resource.data == null -> stringResource(R.string.error_no_data)
        resource.isStale -> stringResource(R.string.stale)
        else -> return
    }
    Surface(color = WeatherTheme.colors.tonal, shape = RoundedCornerShape(Dims.small)) {
        Column(Modifier.fillMaxWidth().padding(Dims.medium)) {
            Text(text, color = if (resource.error != null) WeatherTheme.colors.error else WeatherTheme.colors.secondary)
            if (onRetry != null && resource.error != DomainError.QUOTA && !resource.isRefreshing) {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
            }
        }
    }
}

@Composable fun SourceLine(meta: SourceMeta?, resource: Resource<*>, now: Long, onClick: () -> Unit) {
    val time = if (meta?.kind == DataKind.NOWCAST || meta?.kind == DataKind.AIR) meta.observedAt else meta?.issuedAt
    val label = if (meta?.kind == DataKind.NOWCAST) {
        stringResource(R.string.source_now, missing(timeLabel(time)))
    } else {
        stringResource(R.string.source_forecast, missing(timeLabel(time)))
    }
    val received = meta?.receivedAt
    val minutes = received?.let { ((now - it).coerceAtLeast(0) / 60000).toInt() }
    val age = when {
        minutes == null -> ""
        minutes == 0 -> stringResource(R.string.received_now)
        minutes < 60 -> stringResource(R.string.received_minutes, minutes)
        else -> stringResource(R.string.received_hours, minutes / 60)
    }
    val text = listOf(label, age, if (resource.isStale) stringResource(R.string.stale) else "")
        .filter { it.isNotEmpty() }.joinToString(" · ") + " ⓘ"
    Text(
        text,
        Modifier.fillMaxWidth().heightIn(min = Dims.metaTouch).clickable(onClick = onClick).padding(vertical = Dims.segmentPad),
        style = WeatherTheme.type.meta,
        color = WeatherTheme.colors.secondary
    )
}

@Composable fun ChoiceChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = Dims.action),
        shape = RoundedCornerShape(Dims.pill),
        color = if (selected) WeatherTheme.colors.accent else WeatherTheme.colors.tonal,
        contentColor = if (selected) WeatherTheme.colors.onAccent else WeatherTheme.colors.text,
        border = BorderStroke(Dims.hairline, if (selected) WeatherTheme.colors.accent else WeatherTheme.colors.border)
    ) { Text(text, Modifier.padding(horizontal = Dims.medium, vertical = Dims.small), style = MaterialTheme.typography.labelLarge) }
}
