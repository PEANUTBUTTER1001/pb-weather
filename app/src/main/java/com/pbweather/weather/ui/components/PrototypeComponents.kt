package com.pbweather.weather.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.pbweather.weather.R
import com.pbweather.weather.ui.theme.Dims
import com.pbweather.weather.ui.theme.ThemeMode
import com.pbweather.weather.ui.theme.WeatherTheme

@Composable fun HomeLoadingSkeleton() {
    val label = stringResource(R.string.loading)
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).semantics {
            contentDescription = label
        }.padding(Dims.gutter),
        verticalArrangement = Arrangement.spacedBy(Dims.gutter)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(verticalArrangement = Arrangement.spacedBy(Dims.small)) {
                Skeleton(Modifier.width(Dims.skeletonTemperatureWidth).height(Dims.skeletonTemperatureHeight))
                Skeleton(Modifier.width(Dims.skeletonSubtitleWidth).height(Dims.large))
            }
            Skeleton(Modifier.size(Dims.weeklyWeather))
        }
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dims.medium)) {
                repeat(2) { Skeleton(Modifier.weight(1f).height(Dims.action)) }
            }
        }
        Skeleton(Modifier.fillMaxWidth().height(Dims.touch))
        listOf(Dims.skeletonTemperatureHeight, Dims.skeletonForecast, Dims.skeletonWeekly).forEach { height ->
            SectionCard { Skeleton(Modifier.fillMaxWidth().height(height)) }
        }
    }
}

@Composable private fun Skeleton(modifier: Modifier) {
    val colors = WeatherTheme.colors
    val fraction = if (WeatherTheme.mode != ThemeMode.HIGH_CONTRAST && android.animation.ValueAnimator.areAnimatorsEnabled()) {
        val transition = rememberInfiniteTransition(label = "loading")
        val phase by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing)), label = "shimmer")
        phase
    } else {
        0f
    }
    Surface(modifier, shape = RoundedCornerShape(Dims.small), color = colors.tonal) {
        Box(
            Modifier.fillMaxSize().drawWithCache {
                val start = size.width * fraction * 2
                val brush = Brush.horizontalGradient(
                    listOf(colors.tonal, colors.tonalStrong, colors.tonal),
                    startX = start,
                    endX =
                    start + size.width * 2
                )
                onDrawBehind { drawRect(brush) }
            }
        )
    }
}

@Composable fun EmptyWeather(onRetry: () -> Unit, onSelect: () -> Unit) {
    val large = LocalDensity.current.fontScale * WeatherTheme.fontScale >= 1.8f

    @Composable fun actions() {
        PrimaryButton(onRetry) { Text(stringResource(R.string.retry)) }
        CompactOutlinedButton(onSelect) { Text(stringResource(R.string.select_region)) }
    }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Dims.gutter, vertical = Dims.weatherSmall)) {
        Surface(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dims.radius),
            color = WeatherTheme.colors.surface,
            border = BorderStroke(Dims.hairline, WeatherTheme.colors.border)
        ) {
            Column(
                Modifier.padding(horizontal = Dims.sheet, vertical = Dims.emptyPadding),
                verticalArrangement = Arrangement.spacedBy(Dims.medium),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppIcon(UiIcon.EMPTY, null, Modifier.size(Dims.field))
                Text(
                    stringResource(R.string.design_no_saved_weather),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    stringResource(R.string.design_no_saved_explanation),
                    style = MaterialTheme.typography.bodyLarge,
                    color = WeatherTheme.colors.secondary,
                    textAlign = TextAlign.Center
                )
                if (large) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dims.small), horizontalAlignment = Alignment.CenterHorizontally) {
                        actions()
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Dims.small)) { actions() }
                }
            }
        }
    }
}

@Composable fun CompactOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(contentColor = WeatherTheme.colors.text),
    content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        OutlinedButton(
            onClick,
            modifier.heightIn(min = Dims.action),
            enabled,
            shape = RoundedCornerShape(Dims.small),
            colors = colors,
            border = BorderStroke(Dims.hairline, WeatherTheme.colors.borderStrong),
            contentPadding = PaddingValues(horizontal = Dims.medium, vertical = Dims.tiny),
            content = content
        )
    }
}

@Composable
fun PrimaryButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    Button(
        onClick,
        modifier.heightIn(min = Dims.field),
        enabled,
        shape = RoundedCornerShape(Dims.pill),
        contentPadding = PaddingValues(horizontal = Dims.gutter, vertical = Dims.small),
        content = content
    )
}

@Composable fun DeltaBadge(label: String, value: Double?, modifier: Modifier = Modifier) {
    val colors = WeatherTheme.colors
    val color = when {
        value == null || kotlin.math.abs(value) < .05 -> colors.text
        value > 0 -> colors.warm
        else -> colors.cool
    }
    Surface(
        modifier,
        shape = RoundedCornerShape(Dims.pill),
        color = colors.tonal,
        border = if (WeatherTheme.mode == ThemeMode.HIGH_CONTRAST) BorderStroke(Dims.hairline, colors.border) else null
    ) {
        Text(
            label,
            Modifier.padding(horizontal = Dims.small, vertical = Dims.segmentPad),
            color = color,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable fun SheetHeader(title: String, onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = WeatherTheme.type.sheetTitle)
        IconButton(onClick = onClose) { AppIcon(UiIcon.CLOSE, stringResource(R.string.close)) }
    }
}

@Composable fun KeyValueGrid(values: List<Pair<String, String>>, expanded: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Dims.gridGap)) {
        values.chunked(if (expanded) 1 else 2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dims.gridGap)) {
                row.forEach { (label, value) ->
                    Surface(Modifier.weight(1f), shape = RoundedCornerShape(Dims.small), color = WeatherTheme.colors.tonal) {
                        Column(Modifier.padding(horizontal = Dims.medium, vertical = Dims.gridGap)) {
                            Text(label, style = MaterialTheme.typography.bodySmall, color = WeatherTheme.colors.secondary)
                            Text(value, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable fun CompactTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dims.tiny)) {
        val colors = WeatherTheme.colors
        BasicTextField(
            value,
            onValueChange,
            Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.accent),
            decorationBox = { inner ->
                Column(verticalArrangement = Arrangement.spacedBy(Dims.tiny)) {
                    if (label != null) Text(label, style = WeatherTheme.type.meta, color = WeatherTheme.colors.secondary)
                    Surface(
                        shape = RoundedCornerShape(Dims.small),
                        color = colors.surface,
                        border = BorderStroke(Dims.hairline, if (isError) colors.error else colors.borderStrong)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = Dims.field).padding(horizontal = Dims.medium, vertical = Dims.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.weight(1f)) {
                                if (value.isEmpty() &&
                                    placeholder != null
                                ) {
                                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = colors.tertiary)
                                }
                                inner()
                            }
                            trailingIcon?.invoke()
                        }
                    }
                }
            }
        )
    }
}
